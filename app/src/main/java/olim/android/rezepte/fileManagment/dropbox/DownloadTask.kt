package olim.android.rezepte.fileManagment.dropbox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.GetThumbnailBatchResultEntry
import com.dropbox.core.v2.files.ThumbnailArg
import com.dropbox.core.v2.files.ThumbnailFormat
import com.dropbox.core.v2.files.ThumbnailMode
import com.dropbox.core.v2.files.ThumbnailSize
import com.dropbox.core.v2.users.FullAccount
import java.io.BufferedReader
import java.io.File
import java.util.Date
import java.util.LinkedList
import java.util.Queue


class DownloadTask(client: DbxClientV2) {

    private val dbxClient: DbxClientV2 = client

    fun listDir(dir: String): List<String>? {
        val results = try {
            dbxClient.files().listFolder(dir)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val output = ArrayList<String>()

        for (value in results.entries) {
            output.add(value.name)
        }
        //todo add request more
        return output

    }

    fun getUserAccount(): FullAccount? {
        try {
            //get the users FullAccount
            return dbxClient.users().currentAccount
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun getXml(dir: String): Pair<String, Date> {
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

        return Pair(content.toString(), time)
    }

    fun getImage(dir: String, name: String): Pair<Bitmap, Date>? {
        val results = try {
            dbxClient.files().download("$dir$name") ?: return null
        } catch (e: Exception) {
            return null
        }
        val time = results.result.serverModified //last modified time

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888


        return Pair(BitmapFactory.decodeStream(results.inputStream), time)
    }

    fun getFile(dir: String, name: String): Pair<File, Date>? {
        try {
            val results = dbxClient.files().download("$dir$name") ?: return null
            val time = results.result.serverModified //last modified time
            val file: File = createTempFile()

            results.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return Pair(file, time)
        } catch (e : Exception) {
            return null
        }
    }

    fun getThumbnails(dir: String, fileNames: List<String>): Map<out String, Bitmap?>? {
        //set bitmap options
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        //create arguments for each thumbnail
        val names: Queue<String> = LinkedList()
        //get thumb nails in batches of 25
        var index = 0
        val output = hashMapOf<String, Bitmap?>()
        while (index < fileNames.size) {
            val args = mutableListOf<ThumbnailArg>()
            val startIndex = index
            for (name in fileNames.subList(startIndex, fileNames.size)) {
                val path = "$dir$name.jpg"
                val arg = ThumbnailArg(
                    path,
                    ThumbnailFormat.JPEG,
                    ThumbnailSize.W128H128,
                    ThumbnailMode.BESTFIT
                )
                args.add(arg)
                names.add(name)

                index++
                if (index - startIndex == 25) { //if there has been 25 thumbnails done complete the bach before going onto the next
                    break
                }

            }
            val thumbNailBach = dbxClient.files().getThumbnailBatch(args)

            for (thumbNail in thumbNailBach.entries) {
                val name = names.remove()
                if (thumbNail.tag() == GetThumbnailBatchResultEntry.Tag.SUCCESS) {
                    val temp = Base64.decode(thumbNail.successValue.thumbnail, Base64.DEFAULT)
                    output[name] = BitmapFactory.decodeByteArray(temp, 0, temp.size)
                }
            }
        }

        return output
    }

    fun getFileDate(dir: String, name: String): Date? {
        val results = try {
            dbxClient.files().getMetadata("$dir$name") as FileMetadata
        } catch (e: Exception) {
            return null
        }

        return results.serverModified //last modified time
    }

    fun getFilesDates(dir: String): Map<String, Date?>? {
        val results = try {
            dbxClient.files().listFolder(dir)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        val dates = mutableMapOf<String, Date?>()
        for (value in results.entries) {
            try {
                dates[value.name.removeSuffix(".jpg")] = (value as FileMetadata).serverModified
            } catch (ignore: Exception) {
                //it is not a file
            }
        }
        //todo add request more

        return dates
    }
}