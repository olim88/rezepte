package olim.android.rezepte.fileManagment.dropbox

import android.content.SharedPreferences
import android.util.Log
import com.dropbox.core.DbxException
import com.dropbox.core.oauth.DbxCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import olim.android.rezepte.MainActivity
import olim.android.rezepte.R


class DbTokenHandling(sharedPreferences: SharedPreferences) {

    private var prefs: SharedPreferences = sharedPreferences
    suspend fun retrieveAccessToken() : String? {


        checkExpiredThen()

        //check if ACCESS_TOKEN is stored on previous app launches
        val accessToken = prefs.getString("access-token", null)
        if (accessToken == null) {
            Log.d("AccessToken Status", "No token found")
            return(null)
        } else {
            //accessToken already exists
            Log.d("AccessToken Status", "Token exists")
            return(accessToken)
        }

    }

    fun isLoggedIn(): Boolean { //return if the user is logged in or not
        return prefs.getBoolean("logged-in", false)
    }

    private fun retrieveSavedData(label: String): String? {
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

    private suspend  fun checkExpiredThen() {
        //check if the token has expired
        val expireTime = prefs.getString("expired-at", null)
        val currentTime = System.currentTimeMillis();
        if (expireTime == null || expireTime.toLong() < currentTime) {
            Log.d("AccessToken Status", "Expired token getting refreshed")
            //refresh the token
            val updateToken = GlobalScope.async {
                refreshIfExpired {  }
            }
            updateToken.await()
        }
    }

    fun refreshIfExpired(onRefreshed: () -> Unit): Boolean {
        val accessToken = retrieveSavedData("access-token") ?: return true

        val refreshToken = retrieveSavedData("refresh-token")
        val expiresAt = retrieveSavedData("expired-at")


        if (refreshToken != null && expiresAt != null) {
            val cred = DbxCredential(
                accessToken, expiresAt.toLong(), refreshToken, MainActivity.Companion.resources?.getString(
                    R.string.dropbox_api_key
                )
            )
            val test = CoroutineScope(Dispatchers.IO).launch {
                val client = DropboxClient.getClient(cred)
                val tokens = try {
                    client.refreshAccessToken()
                } catch (e: DbxException) {
                    null
                }
                withContext(Dispatchers.Main) {
                    Log.d("Token Status", "saved new token")
                    //re-save new data
                    if (tokens != null) {//if its not null
                        prefs.edit().putString("expired-at", tokens.expiresAt.toString()).apply()
                        prefs.edit().putString("access-token", tokens.accessToken).apply()
                    }

                    //call the onRefreshed so that the caller will know tha the program can be used
                    onRefreshed()
                }
            }
        } else {
            return true
        }
        return false
    }
}