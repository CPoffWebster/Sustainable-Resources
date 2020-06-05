package com.example.clarksustainableresources

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PhotoViewModel(application: Application) : AndroidViewModel(application) {

    val bitmap = MutableLiveData<Bitmap>()

    fun saveBitmap(bm: Bitmap) {
        bitmap.value = bm
        viewModelScope.launch {
            async { save(bm) }
        }
    }

    suspend fun save(bm: Bitmap) = withContext(Dispatchers.IO) {
        val context = getApplication<Application>().applicationContext
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

        if (Build.VERSION.SDK_INT >= 29) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis() / 1000)
            values.put(MediaStore.Images.Media.IS_PENDING, true)

            val uri: Uri? = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            )
            if (uri != null) {
                saveImageToStream(bm, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory = File(Environment.getExternalStorageDirectory().toString() + "/Pictures")
            val fileName = "photo.png"
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            saveImageToStream(bm, FileOutputStream(file))
            if (file.absolutePath != null) {
                values.put(MediaStore.Images.Media.DATA, file.absolutePath)
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
                )
            }
        }
    }

    suspend fun saveImageToStream(bm: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bm.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}