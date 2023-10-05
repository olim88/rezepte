package com.example.rezepte

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.WriteMode
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.File
import java.nio.charset.Charset


class UploadTask internal constructor(
    private val dbxClient: DbxClientV2,
    file: File?,
    context: Context,
    name: String,
    stringData: String
) :
    AsyncTask<Any?, Any?, Any?>() {
    private val file: File?
    private val context: Context
    private val name: String
    private val stringData: String

    init {
        this.file = file
        this.context = context
        this.name = name
        this.stringData = stringData
    }


    override fun doInBackground(params: Array<Any?>): Any? {
        try {
            // Upload to Dropbox

            //upload xml
            val inputStream2: InputStream =  stringData.byteInputStream(charset = Charsets.UTF_8)
            dbxClient.files()
                .uploadBuilder("/xml/$name.xml") //Path in the user's Dropbox to save the file.
                .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                .uploadAndFinish(inputStream2)
            Log.d("Upload Status", "Success-xml")
            //upload image
            if (file != null) {
                val inputStream: InputStream = FileInputStream(file)
                dbxClient.files()
                    .uploadBuilder(
                        "/image/" + name + "." + file.getName().split(".").last()
                    ) //Path in the user's Dropbox to save the file.
                    .withMode(WriteMode.OVERWRITE) //always overwrite existing file
                    .uploadAndFinish(inputStream)
                Log.d("Upload Status", "Success-image")
            }
        } catch (e: DbxException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(o: Any?) {

        super.onPostExecute(o)
        //Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
    }
}

