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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.BuildConfig
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

    private const val EXPORT_TEMP_DIR = "session_export_temp"
    private const val IMPORT_TEMP_DIR = "session_restore_temp"
    private const val IMPORT_WORK_DIR = "session_import_work"

    private lateinit var exportZipLauncher: ActivityResultLauncher<String>
    private lateinit var importZipLauncher: ActivityResultLauncher<Array<String>>
    private var importedCallback: ((List<Account>, Uri) -> Unit)? = null

    // region Public API / registration
    @JvmStatic
    fun registerResultCallbacks(activity: FragmentActivity) {
        exportZipLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri: Uri? ->
            val appScope = appScope(activity)
            appScope.launch {
                try {
                    uri?.let {
                        withContext(Dispatchers.IO) {
                            exportZipToUri(activity, File(activity.filesDir, BACKUP_FILE_NAME), it)
                        }
                    }
                } catch (t: Throwable) {
                    Timber.e(t)
                } finally {
                    exportEventsBridge(activity).sendEvent(ExportEventsBridge.EVENT_EXPORT_IMPORT_COMPLETED)
                }
            }
        }

        importZipLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                parseAccountsFromArchiveAsync(activity, it, object : CompletionCallback<List<Account>> {
                    override fun onSuccess(result: List<Account>) {
                        importedCallback?.invoke(result, it)
                    }

                    override fun onError(t: Throwable) {
                        Timber.e(t)
                        Toast.makeText(activity, "Import parse failed: ${t.message}", Toast.LENGTH_LONG).show()
                        importedCallback?.invoke(emptyList(), it)
                    }
                })
            }
        }
    }

    /**
     * Picks a zip file via SAF, then the callback receives the list of accounts
     * (metadata parsed from session.json) and the selected Uri.
     * The callback is invoked on the Main thread.
     */
    @JvmStatic
    fun pickFileAndImport(importedCallback: (List<Account>, Uri) -> Unit) {
        this.importedCallback = importedCallback
        importZipLauncher.launch(arrayOf("application/zip"))
    }

    /**
     * Exports the selected accounts (by index). Runs on the IO dispatcher.
     * At the end, opens the SAF CreateDocument picker (via exportZipLauncher).
     */
    @JvmStatic
    fun exportAccountsAsync(
        activity: FragmentActivity,
        accountsN: List<Int>,
        callback: CompletionCallback<Unit>? = null
    ) {
        appScope(activity).launch {
            try {
                withContext(Dispatchers.IO) {
                    exportAccountsInternal(accountsN)
                }
                withContext(Dispatchers.Main) { callback?.onSuccess(Unit) }
            } catch (t: Throwable) {
                Timber.e(t)
                withContext(Dispatchers.Main) { callback?.onError(t) }
            }
        }
    }

    /**
     * Asynchronously parses the archive and returns the list of accounts for the UI (BottomSheet).
     * Does not import/restore anything.
     */
    @JvmStatic
    fun parseAccountsFromArchiveAsync(
        activity: FragmentActivity,
        zipUri: Uri,
        callback: CompletionCallback<List<Account>>
    ) {
        appScope(activity).launch {
            try {
                val accounts = withContext(Dispatchers.IO) {
                    importAccountsInternal(activity, zipUri, accounts = emptyList(), mode = Mode.ONLY_RETURN)
                }
                withContext(Dispatchers.Main) { callback.onSuccess(accounts) }
            } catch (t: Throwable) {
                Timber.e(t)
                withContext(Dispatchers.Main) { callback.onError(t) }
            }
        }
    }

    /**
     * Asynchronously performs the actual import/restore of the selected accounts.
     */
    @JvmStatic
    fun importAccountsAsync(
        activity: FragmentActivity,
        zipUri: Uri,
        selectedAccounts: List<Account>,
        callback: CompletionCallback<Unit>? = null
    ) {
        appScope(activity).launch {
            try {
                val importedMeta = withContext(Dispatchers.IO) {
                    importAccountsInternal(activity, zipUri, accounts = selectedAccounts, mode = Mode.IMPORT)
                }
                withContext(Dispatchers.Main) {
                    exportEventsBridge(activity).sendEvent(ExportEventsBridge.EVENT_EXPORT_IMPORT_COMPLETED)
                    if (importedMeta.isNotEmpty()) {
                        coreUi(activity).toastDisplayHelper()
                            .showToast(activity.getString(R.string.Common_SuccessNew), false)
                        NicegramDoubleBottom.needToReloadDrawer = true
                    }
                    callback?.onSuccess(Unit)

                    Timber.d("Activated accounts count = ${UserConfig.getActivatedAccountsCount()}")
                    delay(500)
                    RebirthHelper.triggerRebirth(activity)
                }
            } catch (t: Throwable) {
                Timber.e(t)
                withContext(Dispatchers.Main) {
                    callback?.onError(t)
                    coreUi(activity).toastDisplayHelper().showToast("Import failed: ${t.message}", true)
                }
            }
        }
    }

    // endregion
    // region Internals
    private enum class Mode { ONLY_RETURN, IMPORT }

    /**
     * Creates a snapshot of the selected accounts into a temp directory and builds a zip at
     * filesDir/BACKUP_FILE_NAME, then launches the SAF CreateDocument picker via the launcher
     * (on the Main thread).
     */
    private suspend fun exportAccountsInternal(accountsN: List<Int>) {
        val context = ApplicationLoader.applicationContext
        val filesDir = context.filesDir
        val backupOutputZip = File(filesDir, BACKUP_FILE_NAME)
        val exportRoot = File(filesDir, EXPORT_TEMP_DIR)

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
        // Snapshot per account
        for (accountIndex in accounts) {
            val accountName = "account$accountIndex"

            val accountSnapshotDir = File(exportRoot, accountName)
            accountSnapshotDir.mkdirs()

            val uc = UserConfig.getInstance(accountIndex)
            val currentUser = uc.currentUser ?: continue

            // 1. Write JSON file(session.json)
            val serializedData = SerializedData()
            try {
                currentUser.serializeToStream(serializedData)
                // in older version used DEFAULT.
                val userSerialized = Base64.encodeToString(serializedData.toByteArray(), Base64.NO_WRAP)
                val username = currentUser.username ?: ""
                val firstName = currentUser.first_name
                val lastName = currentUser.last_name
                val id = currentUser.id.toString()
                val resultName: String = if (username.isEmpty()) {
                    if (lastName == null) firstName.trim()
                    else if (firstName == null) lastName.trim()
                    else "$firstName $lastName".trim()
                } else {
                    "@$username"
                }
                writeSessionJson(
                    accountDir = accountSnapshotDir,
                    userSerializedBase64 = userSerialized,
                    id = id,
                    name = resultName
                )
            } finally {
                serializedData.cleanup()
            }

            // 2. Snapshot .dat files (atomic per-file copy)
            val accountFilesDir = getAccountFilesDir(filesDir, accountIndex)

            if (accountFilesDir.exists() && accountFilesDir.isDirectory) {
                accountFilesDir.listFiles { file ->
                    file.isFile && file.extension.equals("dat", ignoreCase = true)
                }?.toList().orEmpty()
                    .forEach { datFile ->
                        val destFile = File(accountSnapshotDir, datFile.name)
                        copyAtomicallyWithSync(datFile, destFile)
                        if (datFile.length() != destFile.length()) {
                            throw IOException("Export size mismatch for ${datFile.name}: ${datFile.length()} != ${destFile.length()}")
                        }
                    }
            } else {
                Timber.e("Account dir missing for export: $accountFilesDir")
            }
        }
        // 3) zip snapshot dir
        ZipOutputStream(BufferedOutputStream(FileOutputStream(backupOutputZip))).use { zipOut ->
            exportRoot.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = file.relativeTo(exportRoot).path
                    zipOut.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
        // 4) launch SAF picker on Main
        withContext(Dispatchers.Main) {
            exportToPickedPath()
        }
    }

    private fun writeSessionJson(
        accountDir: File,
        userSerializedBase64: String,
        id: String,
        name: String
    ) {
        val json = JSONObject().apply {
            put("user", userSerializedBase64)
            put("id", id)
            put("name", name)
            put("extra", "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
            put("appVersion", BuildConfig.BUILD_VERSION_STRING) // optional
            put("format", 1)    // optional
        }
        File(accountDir, SESSION_FILE_NAME).writeText(json.toString())
    }

    private fun readSessionJson(accountDir: File): JSONObject? {
        val f = File(accountDir, SESSION_FILE_NAME)
        if (!f.exists()) {
            Timber.e("Export file for ${accountDir.name} isn't exist")
            return null
        }
        return try {
            JSONObject(f.readText())
        } catch (e: Exception) {
            Timber.e("Can't read file for ${accountDir.name}")
            Timber.e(e)
            null
        }
    }

    // endregion
    // region File ops: staging / commit
    private fun getAccountFilesDir(filesDir: File, accountIndex: Int): File {
        return if (accountIndex == 0) filesDir else File(filesDir, "account$accountIndex")
    }

    private fun parseArchiveAccountIndex(folderName: String): Int? {
        if (!folderName.startsWith("account")) return null
        val n = folderName.removePrefix("account").toIntOrNull()
        return n
    }

    private fun copyAtomicallyWithSync(src: File, dst: File) {
        dst.parentFile?.mkdirs()
        val tmp = File(dst.parentFile, dst.name + ".tmp")
        if (tmp.exists()) tmp.delete()
        FileInputStream(src).use { input ->
            FileOutputStream(tmp).use { out ->
                input.copyTo(out)
                out.fd.sync()
            }
        }
        if (dst.exists()) dst.delete()
        if (!tmp.renameTo(dst)) {
            // fallback
            FileInputStream(tmp).use { input ->
                FileOutputStream(dst).use { out ->
                    input.copyTo(out)
                    out.fd.sync()
                }
            }
            tmp.delete()
        }
    }

    private fun stageToDir(src: File, stagingDir: File) {
        stagingDir.mkdirs()
        val stagedTmp = File(stagingDir, src.name + ".tmp")
        val staged = File(stagingDir, src.name)
        if (stagedTmp.exists()) stagedTmp.delete()
        if (staged.exists()) staged.delete()
        FileInputStream(src).use { input ->
            FileOutputStream(stagedTmp).use { out ->
                input.copyTo(out)
                out.fd.sync()
            }
        }
        if (!stagedTmp.renameTo(staged)) {
            // fallback
            FileInputStream(stagedTmp).use { input ->
                FileOutputStream(staged).use { out ->
                    input.copyTo(out)
                    out.fd.sync()
                }
            }
            stagedTmp.delete()
        }
        if (staged.length() <= 0) {
            throw IOException("Staged file is empty: ${staged.absolutePath}")
        }
    }

    private data class FileSwap(
        val name: String,
        val target: File,
        val backup: File,
        val staged: File
    )

    private fun commitStagedFilesTransactionally(
        targetDir: File,
        stagingDir: File,
        backupDir: File,
        fileNames: List<String>
    ) {
        targetDir.mkdirs()
        stagingDir.mkdirs()
        backupDir.mkdirs()
        val swaps = fileNames.distinct().map { name ->
            FileSwap(
                name = name,
                target = File(targetDir, name),
                backup = File(backupDir, "$name.bak"),
                staged = File(stagingDir, name)
            )
        }
        // Preconditions
        swaps.forEach { s ->
            if (!s.staged.exists()) throw IOException("Staged file missing: ${s.staged}")
            if (s.staged.length() <= 0) throw IOException("Staged file empty: ${s.staged}")
        }
        val committed = mutableListOf<FileSwap>()
        try {
            // 1) Backup
            for (s in swaps) {
                if (s.backup.exists()) s.backup.delete()
                if (s.target.exists()) {
                    if (!s.target.renameTo(s.backup)) {
                        // fallback copy+sync then delete original
                        copyAtomicallyWithSync(s.target, s.backup)
                        if (!s.target.delete()) {
                            throw IOException("Failed to delete target after backup: ${s.target}")
                        }
                    }
                }
            }
            // 2) Commit staged -> target
            for (s in swaps) {
                if (!s.staged.renameTo(s.target)) {
                    copyAtomicallyWithSync(s.staged, s.target)
                    s.staged.delete()
                }
                committed += s
            }
            // 3) Cleanup staging
            stagingDir.deleteRecursively()
            // Optional: delete backups
            backupDir.deleteRecursively()
        } catch (e: Exception) {
            Timber.e(e, "Commit failed, rolling back...")
            // rollback
            for (s in committed.asReversed()) {
                try {
                    if (s.target.exists()) s.target.delete()
                    if (s.backup.exists()) {
                        if (!s.backup.renameTo(s.target)) {
                            copyAtomicallyWithSync(s.backup, s.target)
                            s.backup.delete()
                        }
                    }
                } catch (re: Exception) {
                    Timber.e(re, "Rollback failed for ${s.name}")
                }
            }
            throw e
        }
    }

    // endregion
    private fun exportToPickedPath() {
        exportZipLauncher.launch("Nicegram Exported Accounts (${System.currentTimeMillis() / 1000}).zip")
    }

    // endregion
    // region unzip / SAF copy
    private fun unzipZip(activity: Activity, zipFile: Uri, unzipTarget: File) {
        activity.contentResolver.openInputStream(zipFile)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val outFile = File(unzipTarget, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out ->
                            zipIn.copyTo(out)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        } ?: throw IOException("Can't open input stream for uri=$zipFile")
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

    /**
     * mode=ONLY_RETURN: returns the list of Account entries parsed from the archive.
     * mode=IMPORT: restores/applies the import for the selected accounts (by n) and returns
     * metadata for the imported ones.
     */
    private fun importAccountsInternal(
        activity: FragmentActivity,
        zipFile: Uri,
        accounts: List<Account>,
        mode: Mode,
    ): List<Account> {
        val result = mutableListOf<Account>()
        val onlyReturn = mode == Mode.ONLY_RETURN

        val context = ApplicationLoader.applicationContext
        val filesDir = context.filesDir
        val unzipTarget = File(filesDir, IMPORT_TEMP_DIR)
        if (unzipTarget.exists()) {
            val deleted = unzipTarget.deleteRecursively()
            if (!deleted) {
                throw IOException("Failed to clear export directory: $unzipTarget")
            }
        }
        unzipTarget.mkdirs()

        // 0. Unzip the archive
        unzipZip(activity, zipFile, unzipTarget)

        val availableIndexes = mutableListOf<Int>()
        for (i in 0..<UserConfig.MAX_ACCOUNT_COUNT) {
            if (!UserConfig.getInstance(i).isClientActivated) {
                availableIndexes.add(i)
            }
        }
        // selection by stable "accountN" folder parsing
        val selectedNs = accounts.map { it.n }.toSet()
        // 1. For each folder, restore session if there is space
        unzipTarget.listFiles()?.filter { it.isDirectory }.orEmpty().forEach { accountDir ->
            val archiveN = parseArchiveAccountIndex(accountDir.name) ?: run {
                Timber.w("Skip unknown folder in archive: ${accountDir.name}")
                return@forEach
            }

            if (!onlyReturn && !selectedNs.contains(archiveN)) return@forEach
            val jsonObj = readSessionJson(accountDir) ?: return@forEach

            Timber.d("Loaded JSON for ${accountDir.name}: $jsonObj")

            try {
                val userData = jsonObj.getString("user")
                val name = jsonObj.getString("name")
                val extra = jsonObj.optString("extra", "")
                val id = jsonObj.getString("id")
                // already logged in?
                for (a in 0..<UserConfig.MAX_ACCOUNT_COUNT) {
                    if (UserConfig.getInstance(a).clientUserId.toString() == id) {
                        Timber.w("Skip import: user already logged in (id=$id)")
                        return@forEach // account already logged in
                    }
                }

                result.add(Account(archiveN, id, name, extra))
                if (onlyReturn) return@forEach
                val accountIndex = availableIndexes.find { it == 0 } ?: availableIndexes.minOrNull()
                Timber.d("availableIndexes=$availableIndexes -> chosen=$accountIndex")
                if (accountIndex == null) {
                    Timber.e("No free account slots")
                    return@forEach
                }
                Timber.d("Archive folder=${accountDir.name} (n=$archiveN), target slot=$accountIndex")
                availableIndexes.remove(accountIndex)
                // ---- Transactional import of .dat files ----
                val targetDir = getAccountFilesDir(filesDir, accountIndex)
                val workRoot = File(filesDir, "$IMPORT_WORK_DIR/account$accountIndex").apply {
                    if (exists()) deleteRecursively()
                    mkdirs()
                }
                val stagingDir = File(workRoot, "staging").apply { mkdirs() }
                val backupDir = File(workRoot, "backup").apply { mkdirs() }
                // stage all .dat from archive accountDir
                val datFiles = accountDir.listFiles { f ->
                    f.isFile && f.extension.equals("dat", ignoreCase = true)
                }?.toList().orEmpty()
                if (datFiles.isEmpty()) {
                    Timber.e("No .dat in archive folder: ${accountDir.name}")
                    return@forEach
                }
                val stagedNames = mutableListOf<String>()
                for (src in datFiles) {
                    stageToDir(src, stagingDir)
                    stagedNames += src.name
                }

                commitStagedFilesTransactionally(
                    targetDir = targetDir,
                    stagingDir = stagingDir,
                    backupDir = backupDir,
                    fileNames = stagedNames
                )
                // ---- Restore current user after files are committed ----
                val bytes = try {
                    Base64.decode(userData, Base64.DEFAULT)
                } catch (_: IllegalArgumentException) {
                    Timber.d("Fallback: try to decode another way")
                    Base64.decode(userData, Base64.NO_WRAP)
                }
                val data = SerializedData(bytes)
                val user = try {
                    TLRPC.User.TLdeserialize(data, data.readInt32(false), false)
                } finally {
                    data.cleanup()
                }

                val uc = UserConfig.getInstance(accountIndex)
                uc.setCurrentUser(user)
                uc.saveConfig(true)
                Timber.d("After setCurrentUser: slot=$accountIndex currentUser=${uc.currentUser?.id} isClientActivated=${uc.isClientActivated} clientUserId=${uc.clientUserId}")
                MessagesController.getInstance(accountIndex).putUser(user, false)

                exportAccountsEp(activity).saveExportedAccountsUseCase().invoke(listOf(id))
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        return result
    }

    // endregion
    // region EntryPoints helpers
    private fun exportAccountsEp(activity: FragmentActivity): ExportAccountsEntryPoint {
        return EntryPoints.get(activity.applicationContext, ExportAccountsEntryPoint::class.java)
    }

    private fun appScope(activity: FragmentActivity): CoroutineScope {
        return exportAccountsEp(activity).appScope()
    }

    private fun exportEventsBridge(activity: FragmentActivity): ExportEventsBridge {
        return exportAccountsEp(activity).exportEventsBridge()
    }

    private fun coreUi(activity: FragmentActivity): CoreUiEntryPoint {
        return EntryPoints.get(activity.applicationContext, CoreUiEntryPoint::class.java)
    }

    // endregion
    // region Callbacks
    interface CompletionCallback<T> {
        fun onSuccess(result: T)
        fun onError(t: Throwable)
    }
    // endregion
}
