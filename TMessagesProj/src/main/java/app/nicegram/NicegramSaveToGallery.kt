package app.nicegram

import android.content.Context
import android.widget.Toast
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MediaController

object NicegramSaveToGallery {

    private val filesToSave = mutableSetOf<String>()

    fun addToSaveList(filename: String) {
        filesToSave.add(filename)
    }

    fun removeFromSaveList(filename: String) {
        filesToSave.remove(filename)
    }

    fun tryToSave(filename: String, path: String, withContext: Context) {
        if (filesToSave.contains(filename)) {
            MediaController.saveFile(path, withContext, 0, null, null)
            Toast.makeText(withContext, LocaleController.getString("VideoSavedHint"), Toast.LENGTH_SHORT).show()
            removeFromSaveList(filename)
        }
    }

    fun clearSaveList(){
        filesToSave.clear()
    }
}