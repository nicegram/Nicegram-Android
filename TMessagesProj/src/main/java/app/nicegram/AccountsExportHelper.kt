package app.nicegram

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.appvillis.core_resources.CoreUiEntryPoint
import com.appvillis.feature_account_export.ExportAccountsEntryPoint
import com.appvillis.feature_account_export.domain.Account
import com.appvillis.feature_account_export.domain.ExportEventsBridge
import dagger.hilt.EntryPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.TLRPC
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object AccountsExportHelper {
    private const val BACKUP_FILE_NAME = "tgnet_backup.zip"
    private const val SESSION_FILE_NAME = "session.json"

    private lateinit var exportZipLauncher: ActivityResultLauncher<String>
    private lateinit var importZipLauncher: ActivityResultLauncher<Array<String>>
    private var importedCallback: ((List<Account>, Uri) -> Unit)? = null

    fun registerResultCallbacks(activity: FragmentActivity) {
        exportZipLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri: Uri? ->
            uri?.let { exportZipToUri(activity, File(activity.filesDir, BACKUP_FILE_NAME), it) }

            EntryPoints.get(activity.applicationContext, ExportAccountsEntryPoint::class.java).exportEventsBridge()
                .sendEvent(ExportEventsBridge.EVENT_EXPORT_IMPORT_COMPLETED)
        }

        importZipLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                importedCallback?.invoke(importAccounts(activity, it, listOf()), uri)
            }
        }
    }

    fun exportAccounts(accountsN: List<Int>) {
        val context = ApplicationLoader.applicationContext
        val backupOutputZip = File(context.filesDir, BACKUP_FILE_NAME)
        val exportRoot = File(context.filesDir, "session_export_temp")

        if (exportRoot.exists()) {
            val deleted = exportRoot.deleteRecursively()
            if (!deleted) {
                throw IOException("Failed to clear export directory: $exportRoot")
            }
        }
        exportRoot.mkdirs()

        val accounts = mutableListOf<Int>()
        for (i in 0..<UserConfig.MAX_ACCOUNT_COUNT) {
            if (UserConfig.getInstance(i).isClientActivated && accountsN.contains(i)) {
                accounts.add(i)
            }
        }

        accounts.forEach {
            val filesDir = context.filesDir
            val accountName = "account$it"

            val accountDir = File(exportRoot, accountName)
            accountDir.mkdirs()

            // 1. Write JSON file
            val serializedData = SerializedData()
            UserConfig.getInstance(it).currentUser.serializeToStream(serializedData)
            val userSerialized = Base64.encodeToString(serializedData.toByteArray(), Base64.DEFAULT)
            val jsonFile = File(accountDir, SESSION_FILE_NAME)
            val username = UserConfig.getInstance(it).currentUser.username ?: ""
            val firstName = UserConfig.getInstance(it).currentUser.first_name
            val lastName = UserConfig.getInstance(it).currentUser.last_name
            val id = UserConfig.getInstance(it).currentUser.id.toString()
            val resultName: String = if (username.isEmpty()) {
                if (lastName == null) firstName.trim()
                else if (firstName == null) lastName.trim()
                else "$firstName $lastName".trim()
            } else {
                "@$username"
            }
            jsonFile.writeText("""{"user": "$userSerialized", "id": "$id", "name": "$resultName", "extra": "${Build.MANUFACTURER + " " + Build.MODEL + ", " + "Android ${Build.VERSION.RELEASE}"}"}""")

            // 2. Copy all .dat files for the account
            val sourcePath = if (it == 0) "" else "$accountName/"
            val accountFilesDir = File(filesDir, sourcePath)

            if (accountFilesDir.exists() && accountFilesDir.isDirectory) {
                accountFilesDir.listFiles { file -> file.extension == "dat" }?.forEach { datFile ->
                    val destFile = File(accountDir, datFile.name)
                    datFile.copyTo(destFile, overwrite = true)
                    Timber.d("Copied ${datFile.name} for account $it")
                }
            } else {
                Timber.e("No .dat files found for account $it in $accountFilesDir")
            }
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(backupOutputZip))).use { zipOut ->
            exportRoot.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(exportRoot).path
                    zipOut.putNextEntry(ZipEntry(entryName))
                    file.inputStream().copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }

        exportToPickedPath()
    }

    private fun exportToPickedPath() {
        exportZipLauncher.launch("Nicegram Exported Accounts (${System.currentTimeMillis() / 1000}).zip")
    }

    private fun exportZipToUri(activity: Activity, zip: File, destUri: Uri) {
        try {
            activity.contentResolver.openOutputStream(destUri)?.use { out ->
                FileInputStream(zip).use { input ->
                    input.copyTo(out)
                }
            }

            EntryPoints.get(activity.applicationContext, CoreUiEntryPoint::class.java).toastDisplayHelper()
                .showToast(activity.getString(R.string.Common_SuccessNew), false)

            Timber.d("Exported to $destUri")
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(activity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun pickFileAndImport(importedCallback: (List<Account>, Uri) -> Unit) {
        this.importedCallback = importedCallback
        importZipLauncher.launch(arrayOf("application/zip"))
    }

    fun importAccounts(activity: FragmentActivity, zipFile: Uri, accounts: List<Account>): List<Account> {
        try {
            val result = mutableListOf<Account>()
            val onlyReturn = accounts.isEmpty()

            val context = ApplicationLoader.applicationContext
            val filesDir = context.filesDir
            val unzipTarget = File(filesDir, "session_restore_temp")
            if (unzipTarget.exists()) {
                val deleted = unzipTarget.deleteRecursively()
                if (!deleted) {
                    throw IOException("Failed to clear export directory: $unzipTarget")
                }
            }
            unzipTarget.mkdirs()

            // 0. Unzip the archive
            activity.contentResolver.openInputStream(zipFile)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry
                    while (entry != null) {
                        val file = File(unzipTarget, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            file.outputStream().use { zipIn.copyTo(it) }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            val availableIndexes = mutableListOf<Int>()
            for (i in 0..<UserConfig.MAX_ACCOUNT_COUNT) {
                if (!UserConfig.getInstance(i).isClientActivated) {
                    availableIndexes.add(i)
                }
            }

            // 1. For each folder, restore session if there is space
            unzipTarget.listFiles()?.forEachIndexed { i, accountDir ->
                val accountsToImport = accounts.map { it.n }
                if (!onlyReturn && !accountsToImport.contains(i)) return@forEachIndexed

                val accountIndex = availableIndexes.find { it == 0 } ?: availableIndexes.lastOrNull()
                if ((accountIndex != null || onlyReturn) && accountDir.isDirectory) {
                    val jsonFile = File(accountDir, SESSION_FILE_NAME)
                    val jsonString = if (jsonFile.exists()) jsonFile.readText() else "null"

                    Timber.d("Loaded JSON for ${accountDir.name}: $jsonString, will be placed at index: $accountIndex")

                    try {
                        val jsonObj = JSONObject(jsonString)
                        val userData = jsonObj.getString("user")
                        val name = jsonObj.getString("name")
                        val extra = jsonObj.getString("extra")
                        val id = jsonObj.getString("id")

                        for (a in 0..<UserConfig.MAX_ACCOUNT_COUNT) {
                            if (UserConfig.getInstance(a).clientUserId.toString() == id) {
                                return@forEachIndexed // account already logged in
                            }
                        }

                        result.add(Account(i, id, name, extra))
                        if (accountIndex != null) availableIndexes.remove(accountIndex)

                        if (!onlyReturn && accountIndex != null) {
                            MessagesController.getInstance(accountIndex).performLogout(0)

                            val bytes = Base64.decode(userData, Base64.DEFAULT)
                            val data = SerializedData(bytes)
                            val user = TLRPC.User.TLdeserialize(data, data.readInt32(false), false)
                            val currentAccount = accountIndex

                            // 2. Copy all .dat files back to app storage
                            val accountFilesDir = File(filesDir, if (accountIndex == 0) "" else "account$accountIndex")
                            accountFilesDir.mkdirs()

                            accountDir.listFiles { file -> file.extension == "dat" }?.forEach { datFile ->
                                val targetFile = File(accountFilesDir, datFile.name)
                                datFile.copyTo(targetFile, overwrite = true)

                                Timber.d("Restored ${targetFile.path}")
                            }


                            UserConfig.getInstance(currentAccount).setCurrentUser(user)
                            MessagesController.getInstance(currentAccount).putUser(user, false)

                            EntryPoints.get(activity.applicationContext, ExportAccountsEntryPoint::class.java).saveExportedAccountsUseCase().invoke(
                                listOf(id)
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }

            if (!onlyReturn) {
                EntryPoints.get(activity.applicationContext, ExportAccountsEntryPoint::class.java).exportEventsBridge()
                    .sendEvent(ExportEventsBridge.EVENT_EXPORT_IMPORT_COMPLETED)

                if (result.isNotEmpty()) {
                    EntryPoints.get(activity.applicationContext, CoreUiEntryPoint::class.java).toastDisplayHelper()
                        .showToast(activity.getString(R.string.Common_SuccessNew), false)
                    NicegramDoubleBottom.needToReloadDrawer = true
                }

                GlobalScope.launch(Dispatchers.IO) {
                    delay(500)
                    RebirthHelper.triggerRebirth(activity)
                }
            }

            return result
        } catch (e: Exception) {
            Timber.e(e)
            return emptyList()
        }
    }
}