package com.example.rezepte

import android.content.SharedPreferences
import android.graphics.Bitmap
import java.io.File
import java.util.Date

class FileSync {

    companion object{

        private fun getTokenAndOnline ( dropboxToken: SharedPreferences) : Pair<String?,Boolean>{
            //get dropbox token and upload image and xml to dropbox
            val tokenHandling = DbTokenHandling( //get token
                dropboxToken
            )
            return  Pair(tokenHandling.retrieveAccessToken(),tokenHandling.isLoggedIn())
        }
        fun uploadString (data : Data, file: FileInfo, stringData : String, success : ()-> Unit){
            //if selected in file priority upload to device
            if (data.priority == FilePriority.LocalFirst || data.priority == FilePriority.LocalOnly || data.priority == FilePriority.Newist || data.priority == FilePriority.None){
                LocalFilesTask.saveString(stringData,file.localPath,file.fileName)
            }
            //if selected in file priority upload to dropbox
            if (data.priority == FilePriority.OnlineFirst || data.priority == FilePriority.OnlineOnly || data.priority == FilePriority.Newist|| data.priority == FilePriority.None){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                    uploadClient.uploadXml(stringData, file.dropboxPath + file.fileName)
                }
            }
            success()
        }
        fun downloadString (data : Data, file: FileInfo, returnString: (String) -> Unit  ): Boolean{
            //sort local data
            val localData = if (data.priority != FilePriority.OnlineOnly){
                LocalFilesTask.loadString(file.localPath,file.fileName)
            }else {null}
            //if there is a local file return this but still check the online save
            if (localData != null){
                returnString(localData.first)
            }



            //sort online
            val onlineData = if (data.priority != FilePriority.LocalOnly){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))

                    //if comparing the date (newest) do this now as the file may not need to be downloaded
                    if (data.priority != FilePriority.Newist){
                        val onlineDate = downloader.getFileDate(file.dropboxPath,file.fileName)
                        //if online is newer or no local use that file
                        if (onlineDate != null && localData != null) {
                            if (onlineDate.toInstant().toEpochMilli() - localData.second.toInstant().toEpochMilli() > 5000){ //if onlineDate is more than 5 seconds behind exit the program here
                                return  true
                            }
                        }
                    }

                    try {
                        downloader.getXml(file.dropboxPath + file.fileName)
                    } catch (ignore: Exception){ return false}

                }else {null}
            }else {null}
            //depending on the priority / dates see if the online file should be returned
            if (onlineData == null) return false //if no online data do not need to check
            if (localData == null){//no local just return online
                returnString(onlineData.first)
                return true
            }
            when (data.priority){
                FilePriority.OnlineFirst->{
                    returnString(onlineData.first)
                }
                FilePriority.Newist -> {
                    //if the online file is newer return its data
                    if (onlineData.second.toInstant().toEpochMilli() - localData.second.toInstant().toEpochMilli() > 5000){ //if local is behind by 5seconds return the online file
                        returnString(onlineData.first)
                    }
                }
                else -> {}
            }
            return true

        }
        fun uploadImage(data : Data, file: FileInfo, imageData : Bitmap, success : ()-> Unit){
            //if selected in file priority upload to device
            if (data.priority == FilePriority.LocalFirst || data.priority == FilePriority.LocalOnly || data.priority == FilePriority.Newist|| data.priority == FilePriority.None){
                LocalFilesTask.saveBitmap(imageData,file.localPath,file.fileName)
            }
            //if selected in file priority upload to dropbox
            if (data.priority == FilePriority.OnlineFirst || data.priority == FilePriority.OnlineOnly || data.priority == FilePriority.Newist|| data.priority == FilePriority.None){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                    uploadClient.uploadBitmap(imageData, file.dropboxPath + file.fileName)
                }
            }
            success()
        }
        fun downloadImage (data : Data, file: FileInfo, returnImage: (Bitmap) -> Unit  ){
            //sort local data
            val localData = if (data.priority != FilePriority.OnlineOnly){
                LocalFilesTask.loadBitmap(file.localPath,file.fileName)
            }else {null}
            //if there is a local file return this but still check the online save
            if (localData != null){
                returnImage(localData.first)
            }

            //sort online
            val onlineData = if (data.priority != FilePriority.LocalOnly){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))

                    //if comparing the date (newest) do this now as the file may not need to be downloaded
                    if (data.priority != FilePriority.Newist){
                        val onlineDate = downloader.getFileDate(file.dropboxPath,file.fileName)
                        //if online is newer or no local use that file
                        if (onlineDate != null && localData != null) {
                            if (onlineDate.toInstant().toEpochMilli() - localData.second.toInstant().toEpochMilli() > 5000){ //if onlineDate is more than 5 seconds behind exit the program here
                                return
                            }
                        }
                    }

                    downloader.getImage(file.dropboxPath , file.fileName)
                }else {null}
            }else {null}
            //depending on the priority / dates see if the online file should be returned
            if (onlineData == null) return //if no online data do not need to check
            if (localData == null){//no local just return online
                returnImage(onlineData.first)
                return
            }
            when (data.priority){
                FilePriority.OnlineFirst->{
                    returnImage(onlineData.first)
                }
                FilePriority.Newist -> {
                    //if the online file is newer return its data
                    if (onlineData.second.toInstant().toEpochMilli() - localData.second.toInstant().toEpochMilli() > 5000){ //if local is behind by 5seconds return the online file
                        returnImage(onlineData.first)
                    }
                }
                else -> {}
            }
            return
        }
        fun downloadThumbnail  (data : Data, file: FileBatchInfo, returnThumbnails: (Map<out String, Bitmap?>) -> Unit  ){
            val localThumbnails  = hashMapOf<String,Bitmap?>()
            val localDates  = hashMapOf<String,Date?>()
            var localThumbnailExists = false
            //sort local data
            if (data.priority != FilePriority.OnlineOnly){
                for (thumbnailName in file.fileNames){
                    val localFile = LocalFilesTask.loadBitmap(file.localPath,"$thumbnailName.jpg")
                    localThumbnails[thumbnailName]= (localFile?.first)
                    localDates[thumbnailName]= (localFile?.second)
                    if (localThumbnails[thumbnailName] != null){
                        localThumbnailExists = true
                    }
                }

            }
            //if there is a local file return this but still check the online save
            if (localThumbnailExists){
                returnThumbnails(localThumbnails)
            }

            //sort online
            //val online dates if looking at newest else just get all online
            val onlineDates = if (data.priority == FilePriority.Newist){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    downloader.getFilesDates(file.dropboxPath )
                }else {null}
            }else {null}

            val onlineData = if (data.priority != FilePriority.LocalOnly && data.priority != FilePriority.Newist){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    downloader.getThumbnails(file.dropboxPath , file.fileNames)
                }else {null}
            }else {null}

            //depending on the priority / dates see if the online file should be returned
            if (onlineData == null && onlineDates== null) return //if no online data do not need to check
            if (!localThumbnailExists){//no local just return online
                if (onlineData != null) {
                    returnThumbnails(onlineData)
                }
                return
            }
            when (data.priority){
                FilePriority.OnlineFirst->{
                    if (onlineData != null) {
                        returnThumbnails(onlineData)
                    }
                }
                FilePriority.Newist -> {
                    if (onlineDates == null) return //complains without this
                    //find the files that have newer versions online
                    val neededNames = mutableListOf<String>()
                    val removedThumbnails = mutableMapOf<String,Bitmap?>()
                    for (thumbnailKey in file.fileNames) {
                        if (onlineDates[thumbnailKey] != null){//if there is a online file
                            //if the online file is newer add to download list
                            if (!localThumbnails.contains(thumbnailKey) || localThumbnails[thumbnailKey] == null || onlineDates[thumbnailKey]!!.toInstant().toEpochMilli() - localDates[thumbnailKey]?.toInstant()!!.toEpochMilli() > 5000){
                                neededNames.add(thumbnailKey)
                            }
                        } else{
                            //set the value to null for this name as the image no longer exists
                            removedThumbnails[thumbnailKey] = null
                        }
                    }
                    //get the needed thumbnails from dropbox if there is any
                    if (neededNames.isEmpty()) return
                    val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                    val neededThumbnails = if (dropbox.second) {
                        val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                        downloader.getThumbnails(file.dropboxPath , neededNames)
                    }else {null}
                    if (neededThumbnails != null){
                        returnThumbnails(localThumbnails + neededThumbnails + removedThumbnails)
                    }
                }
                //there is no other way to sync thumbnails currently
                else -> {}
            }
            return
        }
        fun uploadFile(data : Data, file: FileInfo, fileData : File, success : ()-> Unit){
            //if selected in file priority upload to device
            if (data.priority == FilePriority.LocalFirst || data.priority == FilePriority.LocalOnly || data.priority == FilePriority.Newist|| data.priority == FilePriority.None){
                LocalFilesTask.saveFile(fileData,file.localPath,file.fileName)
            }
            //if selected in file priority upload to dropbox
            if (data.priority == FilePriority.OnlineFirst || data.priority == FilePriority.OnlineOnly || data.priority == FilePriority.Newist|| data.priority == FilePriority.None){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                    uploadClient.uploadFile(fileData, file.dropboxPath + file.fileName)
                }
            }
            success()
        }
        fun deleteFile(data : Data, file : FileInfo, success : ()-> Unit){
            //delete file form the device
            if (data.priority != FilePriority.OnlineOnly) {
                LocalFilesTask.removeFile(file.localPath, file.fileName)
            }
            //if online delete from dropbox
            if (data.priority != FilePriority.LocalOnly) {
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val uploader = UploadTask(DropboxClient.getClient(dropbox.first))
                    try {
                        uploader.removeFile(file.dropboxPath + file.fileName)
                    }catch (ignore: Exception){
                        return
                    }

                }
            }
            success()
        }

        /**
         * syncs file version between local files and files on dropbox basted on the priority.
         * when given a first priority that file is uploaded to the other location.
         * when given the newest the dates of both files are compared and the newest one is uploaded to the other location.
         * 
         *
         * @param data The general data for syncing.
         * @param file The name and location of the files used.
         * @param success called when the function is finished.
         */
        fun syncFile(data : Data, file : FileInfo, success : ()-> Unit){
            val dropbox = getTokenAndOnline(data.dropboxPrefrence)
            if (!dropbox.second) return // not connected to dropbox so can not sync to it

            when (data.priority){
                FilePriority.OnlineFirst->{
                    //get the online file and upload that to the local location
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    val onlineFile = downloader.getFile(file.dropboxPath , file.fileName)
                    if (onlineFile != null){
                        LocalFilesTask.saveFile(onlineFile.first,file.localPath,file.fileName)
                    }
                }
                FilePriority.LocalFirst -> {
                    //get the local file and upload that to the online location
                    val localFile = LocalFilesTask.loadFile(file.localPath,file.fileName)
                    if (localFile != null){
                        val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                        uploadClient.uploadFile(localFile.first, file.dropboxPath + file.fileName)
                    }

                }
                FilePriority.Newist -> {
                    //compare data's on files and upload newest
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    val onlineDate = downloader.getFileDate(file.dropboxPath,file.fileName)
                    val localFile = LocalFilesTask.loadFile(file.localPath,file.fileName)



                    if (localFile == null && onlineDate == null) return //no file to sync
                    else if (onlineDate ==  null && localFile != null){ //if online is null upload
                        val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                        uploadClient.uploadFile(localFile.first, file.dropboxPath + file.fileName)
                    }
                    else if (localFile == null ){
                        val onlineFile = downloader.getFile(file.dropboxPath , file.fileName)
                        LocalFilesTask.saveFile(onlineFile!!.first,file.localPath,file.fileName)
                    }
                    else if (onlineDate!!.toInstant().toEpochMilli() - localFile.second.toInstant().toEpochMilli() > 5000){//local old upload to local
                        val onlineFile = downloader.getFile(file.dropboxPath , file.fileName)
                        LocalFilesTask.saveFile(onlineFile!!.first,file.localPath,file.fileName)
                    }else {//online old upload online
                        val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                        uploadClient.uploadFile(localFile.first, file.dropboxPath + file.fileName)
                    }

                }
                else -> {}
            }
            success()
        }
        fun syncThumbnail  (data : Data, file: FileBatchInfo, success: () -> Unit  ){ //todo add newist get time functionality
            val localThumbnails  = hashMapOf<String,Pair<Bitmap, Date>?>()
            //sort local data
            if (data.priority != FilePriority.OnlineOnly){
                for (thumbnailName in file.fileNames){
                    localThumbnails[thumbnailName]= (LocalFilesTask.loadBitmap(file.localPath,"$thumbnailName.jpg"))
                }

            }
            //sort online
            //val online dates if looking at newest else just get all online
            val onlineDates = if (data.priority == FilePriority.Newist){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    downloader.getFilesDates(file.dropboxPath )
                }else {null}
            }else {null}

            val onlineData = if (data.priority != FilePriority.LocalOnly && data.priority != FilePriority.Newist){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    downloader.getThumbnails(file.dropboxPath , file.fileNames)
                }else {null}
            }else {null}

            //depending on the priority / dates see if the online file should be returned
            if (onlineData == null && onlineDates == null) return //if no online data do not need to check
            when (data.priority){
                FilePriority.OnlineFirst->{
                    if (onlineData == null) return //complains without this
                    for (thumbnailKey in file.fileNames) {
                        if (onlineData[thumbnailKey] != null) {
                            if (onlineData[thumbnailKey]?.sameAs(localThumbnails[thumbnailKey]?.first) == false){
                                LocalFilesTask.saveBitmap(
                                    onlineData[thumbnailKey]!!,
                                    file.localPath,
                                    "$thumbnailKey.jpg"
                                )
                            }
                        } else {
                            //delete the file if not on dropbox
                            LocalFilesTask.removeFile(
                                file.localPath,
                                "$thumbnailKey.jpg"
                            )
                        }

                    }
                    success()
                }
                FilePriority.OnlineOnly->{
                    if (onlineData == null) return //complains without this
                    for (thumbnailKey in file.fileNames) {
                        if (onlineData[thumbnailKey] != null) {
                            LocalFilesTask.saveBitmap(
                                localThumbnails[thumbnailKey]?.first!!,
                                file.localPath,
                                "$thumbnailKey.jpg"
                            )
                        } else {
                            //delete the file if not on dropbox
                            LocalFilesTask.removeFile(
                                file.localPath,
                                "$thumbnailKey.jpg"
                            )
                        }
                    }
                    success()
                }
                FilePriority.Newist -> {
                    if (onlineDates == null) return //complains without this
                    //find the files that have newer versions online
                    val neededNames = mutableListOf<String>()
                    for (thumbnailKey in file.fileNames) {
                        if (onlineDates[thumbnailKey] != null){//if there is a online file
                            //if the online file is newer add to download list
                            if (!localThumbnails.contains(thumbnailKey) || localThumbnails[thumbnailKey] == null || onlineDates[thumbnailKey]!!.toInstant().toEpochMilli() - localThumbnails[thumbnailKey]?.second?.toInstant()!!.toEpochMilli() > 5000){
                                neededNames.add(thumbnailKey)
                            }
                        } else {
                            //delete the file if not on dropbox
                            LocalFilesTask.removeFile(
                                file.localPath,
                                "$thumbnailKey.jpg"
                            )
                        }
                    }
                    //get the needed thumbnails from dropbox if there is any
                    if (neededNames.isEmpty()) {success(); return}
                    val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                    val neededThumbnails = if (dropbox.second) {
                        val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                        downloader.getThumbnails(file.dropboxPath , file.fileNames)
                    }else {null}
                    if (neededThumbnails != null){
                        for (thumbnailKey in neededNames) {
                            if (neededThumbnails[thumbnailKey] != null) {
                                if (neededThumbnails[thumbnailKey]?.sameAs(localThumbnails[thumbnailKey]?.first) == false){
                                    LocalFilesTask.saveBitmap(
                                        neededThumbnails[thumbnailKey]!!,
                                        file.localPath,
                                        "$thumbnailKey.jpg"
                                    )
                                }
                            } else {
                                //delete the file if not on dropbox (this should not come up unless it get deleted while the function is running)
                                LocalFilesTask.removeFile(
                                    file.localPath,
                                    "$thumbnailKey.jpg"
                                )
                            }

                        }
                        success()
                    }
                }
                //there is no other way to sync thumbnails currently
                else -> {}
            }
            return
        }



    }
    data class FileInfo (val dropboxPath: String, val localPath : String, val fileName : String)

    data class FileBatchInfo (val dropboxPath: String, val localPath : String, val fileNames : List<String>)
    data class Data (val priority: FilePriority,val dropboxPrefrence: SharedPreferences)
    enum class FilePriority{
        LocalOnly,
        OnlineOnly,
        LocalFirst,
        OnlineFirst,
        Newist,
        None,
    }
}