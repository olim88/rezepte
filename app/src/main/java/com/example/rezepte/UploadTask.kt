package com.example.rezepte

import android.graphics.Bitmap
import android.util.Log
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream


class UploadTask (private val client: DbxClientV2){

    fun uploadXml(fileData: String, filepath: String){ //uploads string as a file
        try {
            //upload xml
            val inputStream2: InputStream =  fileData.byteInputStream(charset = Charsets.UTF_8)
            client.files()
                .uploadBuilder(filepath) //Path in the user's Dropbox to save the file.
                .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                .uploadAndFinish(inputStream2)
            Log.d("Upload Status", "Success-xml")

        } catch (e: DbxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun uploadFile(file : File, filepath: String){ //uploads a file to dropbox
        try {
            //upload xml
            val inputStream: InputStream = FileInputStream(file)
            client.files()
                .uploadBuilder(
                    "$filepath.${file.name.split(".").last()}"
                ) //Path in the user's Dropbox to save the file.
                .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                .uploadAndFinish(inputStream)
            Log.d("Upload Status", "Success-image")

        } catch (e: DbxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun uploadBitmap(bitmap : Bitmap, filepath: String){ //uploads a file to dropbox
        try {
            //upload bitmap
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG,0,bos)
            val inputStream: InputStream = ByteArrayInputStream(bos.toByteArray())
            client.files()
                .uploadBuilder(
                    "$filepath.png"
                ) //Path in the user's Dropbox to save the file.
                .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                .uploadAndFinish(inputStream)
            Log.d("Upload Status", "Success-image")

        } catch (e: DbxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun removeFile(path: String){
        client.files().deleteV2(path)
    }

    suspend fun removeImage(path: String){
        val imageName = path.split("/").last()
        val dir = path.removeSuffix(imageName)
        if (getImagePath(dir,imageName) == null) return
        client.files().deleteV2(getImagePath(dir,imageName))

    }
    private suspend fun getImagePath(dir: String, name :String): String? {
        val nameOptions = DownloadTask(client).listDir(dir) ?: return null
        val fullName = nameOptions.filter { x -> x.contains(name) } //if the file extension is unknown
        if (fullName.isEmpty()) return null
        return "$dir${fullName[0]}"
    }








}

