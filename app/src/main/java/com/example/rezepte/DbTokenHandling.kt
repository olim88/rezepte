package com.example.rezepte

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dropbox.core.DbxException
import com.dropbox.core.oauth.DbxCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class  DbTokenHandling(sharedPreferences: SharedPreferences) : AppCompatActivity() {

    private var prefs :SharedPreferences = sharedPreferences




    fun retrieveAccessToken(): String? {
        //check if ACCESS_TOKEN is stored on previous app launches

        val accessToken = prefs.getString("access-token", null)
        return if (accessToken == null) {
            Log.d("AccessToken Status", "No token found")
            null
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists")
            accessToken
        }
    }
    fun isLoggedIn(): Boolean{ //return if the user is logged in or not
        return prefs.getBoolean("logged-in", false)
    }

    private fun retrieveSavedData(label :String): String? {
        //check if ACCESS_TOKEN is stored on previous app launches

        val accessToken = prefs.getString(label, null)
        return if (accessToken == null) {
            Log.d("SavedData Status", "No $label found")
            null
        } else {
            //accessToken already exists
            Log.d("SavedData Status", "$label exists")
            accessToken
        }
    }

     fun refreshIfExpired(context: Context, onRefreshed : () -> Unit) : Boolean {
        val accessToken = retrieveSavedData("access-token") ?: return true

        val refreshToken = retrieveSavedData("refresh-token")
        val expiresAt = retrieveSavedData("expired-at")


         if (refreshToken != null && expiresAt != null ) {
             val cred = DbxCredential(accessToken, expiresAt.toLong(), refreshToken, context.resources.getString(R.string.dropbox_api_key))
             CoroutineScope(Dispatchers.IO).launch {
                 val client = DropboxClient.getClient(cred)
                 val tokens = try{
                     client.refreshAccessToken()
                 }catch (e : DbxException){
                     null
                 }
                 withContext(Dispatchers.Main) {
                     //resave new data
                     if (tokens!= null){//if its not null
                         prefs.edit().putString("expired-at", tokens.expiresAt.toString()).apply()
                         prefs.edit().putString("access-token", tokens.accessToken).apply()
                     }

                     //call the onRefreshed so that the caller will know tha the program can be used
                     onRefreshed()

                 }
             }
         }
         else {
             return true
         }
        return false
    }
}