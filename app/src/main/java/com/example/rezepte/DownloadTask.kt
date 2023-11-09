package com.example.rezepte

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.GetThumbnailBatchResultEntry
import com.dropbox.core.v2.files.ThumbnailArg
import com.dropbox.core.v2.files.ThumbnailFormat
import com.dropbox.core.v2.files.ThumbnailMode
import com.dropbox.core.v2.files.ThumbnailSize
import com.dropbox.core.v2.users.FullAccount
import java.io.BufferedReader
import java.util.Date
import java.util.LinkedList
import java.util.Queue


class DownloadTask(client: DbxClientV2)  {

    private val dbxClient: DbxClientV2 = client

    suspend fun listDir (dir : String) : List<String>? {
        val results = try {
            dbxClient.files().listFolder(dir)
        } catch (e: DbxException) {
            e.printStackTrace()
            return null
        }

        var output = ArrayList<String>()

        for (value in results.entries){
            output.add(value.name)
        }
        //todo add request more
        return output

    }
    suspend fun getUserAccount(): FullAccount?{
        try {
            //get the users FullAccount
            return dbxClient.users().currentAccount
        } catch (e: DbxException) {
            e.printStackTrace()
        }

        return null
    }
    suspend fun getXml(dir :String) : Pair<String, Date>{
        val results = dbxClient.files().download(dir)
        val time = results.result.serverModified //last modified time
        val reader = BufferedReader(results.inputStream.reader())
        val content = StringBuilder()
        reader.use { reader ->
            var line = reader.readLine()
            while (line != null) {
                content.append(line)
                line = reader.readLine()
            }
        }

        return Pair(content.toString() ,time)
    }
    suspend fun getImage(dir: String, name :String): Pair<Bitmap, Date>? {
        if (getImagePath(dir,name) == null) return null
        val results = dbxClient.files().download(getImagePath(dir,name))
        val time = results.result.serverModified //last modified time

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888


        return Pair(BitmapFactory.decodeStream(results.inputStream),time)
    }
    private suspend fun getImagePath(dir: String, name :String): String? {
        val nameOptions = listDir(dir) ?: return  null
        val fullName = nameOptions.filter { x -> x.contains(name) } //if the file extension is unknown
        if (fullName.isEmpty()) return null
        return "$dir${fullName[0]}"
    }
    suspend private fun getImagePathFromList(dir: String, name :String, nameOptions:List<String>): String? {
        val fullName = nameOptions.filter { x -> x.contains(name) } //if the file extension is unknown
        if (fullName.isEmpty()) return null
        return "$dir${fullName[0]}"
    }

    suspend fun getThumbnails(dir: String, fileNames: List<String>): Map<out String, Bitmap?>?{
        //set bitmap options
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        //create arguments for each thumbnail
        val actualFileNames = listDir(dir) ?: return  null
        val names : Queue<String> = LinkedList()
        var args = mutableListOf<ThumbnailArg>()
        for (name in fileNames){
            val path = getImagePathFromList(dir,name,actualFileNames)
            if (path != null){
                val arg = ThumbnailArg(path,ThumbnailFormat.JPEG,ThumbnailSize.W128H128,ThumbnailMode.BESTFIT)
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