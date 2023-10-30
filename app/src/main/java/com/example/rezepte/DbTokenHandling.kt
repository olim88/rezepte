package com.example.rezepte

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

    var prefs :SharedPreferences = sharedPreferences



    public fun retrieveAccessToken(): String? {
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

     fun refreshIfExpired() : Boolean {
        val accessToken = retrieveSavedData("access-token") ?: return true

        val refreshToken = retrieveSavedData("refresh-token")
        val expiresAt = retrieveSavedData("expired-at")
        val appKey = retrieveSavedData("app-key")

         if (refreshToken != null && expiresAt != null && appKey != null) {
             val cred = DbxCredential(accessToken, expiresAt.toLong(), refreshToken, appKey)
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

                 }
             }
         }
         else {
             return true
         }
        return false
    }
}