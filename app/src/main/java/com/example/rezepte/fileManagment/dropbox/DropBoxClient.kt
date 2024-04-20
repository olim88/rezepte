package com.example.rezepte.fileManagment.dropbox

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2


object DropboxClient {
    fun getClient(ACCESS_TOKEN: String?): DbxClientV2 {
        // Create Dropbox client
        val config = DbxRequestConfig("Repize/1.0")
        return DbxClientV2(config, ACCESS_TOKEN)
    }
    fun getClient(credential: DbxCredential): DbxClientV2 {
        // Create Dropbox client
        val config = DbxRequestConfig("Repize/1.0")
        return DbxClientV2(config, credential)
    }
}
