package com.example.rezepte.fileManagment.dropbox

import android.os.AsyncTask
import com.dropbox.core.DbxException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.users.FullAccount


class UserAccountTask internal constructor(
    private val dbxClient: DbxClientV2,
    private val delegate: TaskDelegate
) :
    AsyncTask<Void?, Void?, FullAccount?>() {
    private var error: Exception? = null

    interface TaskDelegate {
        fun onAccountReceived(account: FullAccount?)
        fun onError(error: Exception?)
    }

    override fun onPostExecute(account: FullAccount?) {

        if (account != null && error == null) {
            //User Account received successfully

            delegate.onAccountReceived(account)
        } else {
            // Something went wrong
            delegate.onError(error)
        }
    }

    override fun doInBackground(vararg p0: Void?): FullAccount? {
        error = try {
            //get the users FullAccount
            return dbxClient.users().currentAccount
        } catch (e: DbxException) {
            e.printStackTrace()
            e
        }
        return null
    }
}
