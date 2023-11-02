package com.example.rezepte

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date

class LocalFilesTask {
    companion object {
        fun saveString (xml: String, filePath:String, fileName: String){
            val file = File(filePath,fileName)
            file.parentFile?.mkdirs()
            file.writeText(xml)
        }
        fun saveFile (fileToSave: File, filePath:String, fileName: String){
            val file = File(filePath,fileName)
            file.parentFile?.mkdirs()
            file.writeBytes(fileToSave.readBytes())
        }
        fun loadFile (filePath:String, fileName: String): Pair<String,Date>?{
            val file = File(filePath,fileName)
            if (file.isFile){
                return Pair(file.readText(), Date(file.lastModified()))
            }
            return  null
        }
        fun removeFile (filePath:String, fileName: String){
            val file = File(filePath,fileName)
            file.delete()
        }
        fun saveBitmap(bitmap: Bitmap, filePath:String, fileName: String){
            val file = File(filePath,fileName)
            file.parentFile?.mkdirs()
            //convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG,0,bos)
            //write to file
            file.writeBytes(bos.toByteArray())
        }
        fun loadBitmap(filePath:String, fileName: String):Pair<Bitmap,Date>? {
            val file = File(filePath,fileName)
            if (file.isFile){
                //setup bitmap factory
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                //decode and return image
                return Pair(BitmapFactory.decodeStream(file.readBytes().inputStream()), Date(file.lastModified()))
            }


            return null
        }

    }
}