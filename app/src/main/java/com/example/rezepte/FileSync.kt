package com.example.rezepte

import android.content.SharedPreferences
import android.graphics.Bitmap
import java.io.File

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
            var localThumbnailExists = false
            //sort local data
            if (data.priority != FilePriority.OnlineOnly){
                for (thumbnailName in file.fileNames){
                    localThumbnails[thumbnailName]= (LocalFilesTask.loadBitmap(file.localPath,"$thumbnailName.png")?.first)
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
            val onlineData = if (data.priority != FilePriority.LocalOnly){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    downloader.getThumbnails(file.dropboxPath , file.fileNames)
                }else {null}
            }else {null}
            //depending on the priority / dates see if the online file should be returned
            if (onlineData == null) return //if no online data do not need to check
            if (!localThumbnailExists){//no local just return online
                returnThumbnails(onlineData)
                return
            }
            when (data.priority){
                FilePriority.OnlineFirst->{
                    returnThumbnails(onlineData)
                }
                //new can not work as there are multiple dates
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
         * when given the newist the dates of both files are compared and the newest one is uploaded to the other location.
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
                    val onlineFile = downloader.getFile(file.dropboxPath , file.fileName)
                    val localFile = LocalFilesTask.loadFile(file.localPath,file.fileName)



                    if (localFile == null && onlineFile == null) return //no file to sync
                    else if (onlineFile ==  null && localFile != null){ //if online is null upload
                        val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                        uploadClient.uploadFile(localFile.first, file.dropboxPath + file.fileName)
                    }
                    else if (localFile == null && onlineFile != null){
                        LocalFilesTask.saveFile(onlineFile.first,file.localPath,file.fileName)
                    }
                    else if (onlineFile!!.second.toInstant().toEpochMilli() - localFile!!.second.toInstant().toEpochMilli() > 5000){//local old upload to local
                        LocalFilesTask.saveFile(onlineFile.first,file.localPath,file.fileName)
                    }else {//online old upload online
                        val uploadClient = UploadTask(DropboxClient.getClient(dropbox.first))
                        uploadClient.uploadFile(localFile.first, file.dropboxPath + file.fileName)
                    }

                }
                else -> {}
            }
            success()
        }
        fun syncThumbnail  (data : Data, file: FileBatchInfo, success: () -> Unit  ){ //todo might not be deleting old thumbnails
            val localThumbnails  = hashMapOf<String,Bitmap?>()
            //sort local data
            if (data.priority != FilePriority.OnlineOnly){
                for (thumbnailName in file.fileNames){
                    localThumbnails[thumbnailName]= (LocalFilesTask.loadBitmap(file.localPath,"$thumbnailName.png")?.first)
                }

            }

            //sort online
            val onlineData = if (data.priority != FilePriority.LocalOnly){
                val dropbox = getTokenAndOnline(data.dropboxPrefrence)
                if (dropbox.second) {
                    val downloader = DownloadTask(DropboxClient.getClient(dropbox.first))
                    downloader.getThumbnails(file.dropboxPath , file.fileNames)
                }else {null}
            }else {null}

            //depending on the priority / dates see if the online file should be returned
            if (onlineData == null) return //if no online data do not need to check
            when (data.priority){
                FilePriority.OnlineFirst->{
                    for (thumbnailKey in file.fileNames) {
                        if (onlineData[thumbnailKey] != null) {
                            if (onlineData[thumbnailKey]?.sameAs(localThumbnails[thumbnailKey]) == false){
                                LocalFilesTask.saveBitmap(
                                    onlineData[thumbnailKey]!!,
                                    file.localPath,
                                    "$thumbnailKey.png"
                                )
                            }
                        } else {
                            //delete the file if not on dropbox
                            LocalFilesTask.removeFile(
                                file.localPath,
                                "$thumbnailKey.png"
                            )
                        }

                    }
                    success()
                }
                FilePriority.OnlineOnly->{
                    for (thumbnailKey in file.fileNames) {
                        if (onlineData[thumbnailKey] != null) {
                            LocalFilesTask.saveBitmap(
                                localThumbnails[thumbnailKey]!!,
                                file.localPath,
                                "$thumbnailKey.png"
                            )
                        } else {
                            //delete the file if not on dropbox
                            LocalFilesTask.removeFile(
                                file.localPath,
                                "$thumbnailKey.png"
                            )
                        }
                    }
                    success()
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