package com.example.rezepte

import java.io.File
import java.util.Date

class LocalFilesTask {
    companion object {
        fun saveFile (xml: String, filePath:String, fileName: String){
            val file = File(filePath,fileName)
            file.parentFile?.mkdirs()
            file.writeText(xml)
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

    }
}