package com.appvillis.nicegram

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object NicegramThemeApplyHelper {

    const val THEME_FILE_NAME = "goldietheme.attheme"
    const val THEME_NAME = "Gold Theme"

    @Throws(IOException::class)
    fun getNgGoldTheme(fileDir: File, context: Context): File {
        return try {
            getFileFromAssets(fileDir, context, THEME_FILE_NAME)
        } catch (e: Exception) {
            Timber.e(e)
            throw IOException(e.message)
        }
    }

    private fun getFileFromAssets(fileDir: File, context: Context, fileName: String): File {
        val outFile = File(fileDir, fileName)
        if (!outFile.exists()) {
            context.assets.open(fileName).use { inputStream ->
                FileOutputStream(outFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while ((inputStream.read(buffer).also { length = it }) > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                }
            }
        }
        return outFile
    }
}
