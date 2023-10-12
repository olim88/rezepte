package com.example.rezepte

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.GetThumbnailBatchResultEntry
import com.dropbox.core.v2.files.ThumbnailArg
import com.dropbox.core.v2.files.ThumbnailFormat
import com.dropbox.core.v2.files.ThumbnailMode
import com.dropbox.core.v2.files.ThumbnailSize
import java.io.BufferedReader
import java.util.LinkedList
import java.util.Queue


class DownloadTask(client: DbxClientV2)  {

    private val dbxClient: DbxClientV2 = client

    fun listDir (dir : String) : List<String>
    {
        val results = dbxClient.files().listFolder(dir) //todo error handling

        var output = ArrayList<String>()

        for (value in results.entries){
            output.add(value.name)
        }

        //todo add request more

        return output

    }
    fun GetXml(dir :String) : String{



        val results = dbxClient.files().download(dir)

        val reader = BufferedReader(results.inputStream.reader())
        val content = StringBuilder()
        try {
            var line = reader.readLine()
            while (line != null) {
                content.append(line)
                line = reader.readLine()
            }
        } finally {
            reader.close()
        }

        return content.toString()
    }
    fun GetImage(dir: String, name :String): Bitmap? {
        if (GetImagePath(dir,name) == null) return null
        val results = dbxClient.files().download(GetImagePath(dir,name))

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888


        return BitmapFactory.decodeStream(results.inputStream)
    }
    fun GetImagePath(dir: String, name :String): String? {
        val nameOptions = listDir(dir)
        val fullName = nameOptions.filter { x -> x.contains(name) } //if the file extension is unknown
        if (fullName.isEmpty()) return null
        return "$dir${fullName[0]}"
    }
    fun GetImagePathFromList(dir: String, name :String,nameOptions:List<String>): String? {
        val fullName = nameOptions.filter { x -> x.contains(name) } //if the file extension is unknown
        if (fullName.isEmpty()) return null
        return "$dir${fullName[0]}"
    }
    fun RemoveFile(dir: String, name:String){
        dbxClient.files().deleteV2("$dir$name")
    }

    fun RemoveImage(dir: String, name:String){
        if (GetImagePath(dir,name) == null) return
        dbxClient.files().deleteV2(GetImagePath(dir,name))

    }
    fun GetThumbnails(dir: String,fileNames: List<String>): Map<out String, Bitmap?>{
        //set bitmap options
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        //create arguments for each thumbnail
        val actualFileNames = listDir(dir)
        val names : Queue<String> = LinkedList()
        var args = mutableListOf<ThumbnailArg>()
        for (name in fileNames){
            val path = GetImagePathFromList(dir,name,actualFileNames)
            if (path != null){
                val arg = ThumbnailArg(path,ThumbnailFormat.JPEG,ThumbnailSize.W64H64,ThumbnailMode.BESTFIT)
                args.add(arg)
                names.add(name)
            }

        }
        val thumbNails  = dbxClient.files().getThumbnailBatch(args)
        var output = hashMapOf<String,Bitmap?>()
        for (thumbNail in thumbNails.entries)
        {
            if (thumbNail.tag() == GetThumbnailBatchResultEntry.Tag.SUCCESS){
                val temp = Base64.decode(thumbNail.successValue.thumbnail,Base64.DEFAULT)
                output[names.remove()] = BitmapFactory.decodeByteArray(temp,0, temp.size)
            }
        }

        return output
    }
}