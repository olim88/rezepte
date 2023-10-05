package com.example.rezepte

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.TokenAccessType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LoginActivity : AppCompatActivity()
{
    private val webAuth = DbxPKCEWebAuth(DbxRequestConfig("examples-authorize"), DbxAppInfo("ktd7xc7sg55pb8d"))
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login);

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            Toast.makeText(this, "Link Dropbox", Toast.LENGTH_SHORT).show()
            //start login
            //Auth.startOAuth2Authentication(applicationContext, "ktd7xc7sg55pb8d")
            val webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .build()

            val authorizeUrl = webAuth.authorize(webAuthRequest)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl))
            startActivity(browserIntent)
        }

        findViewById<TextView>(R.id.authCodeInput).doOnTextChanged { text, start, before, count ->


            GlobalScope.launch {
                val auth = webAuth.finishFromCode(text.toString())
                withContext(Dispatchers.Main) {


                    if (auth.accessToken != null) {
                        //Store accessToken in SharedPreferences
                        val prefs =
                            getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE)
                        prefs.edit().putString("access-token", auth.accessToken).apply()
                        prefs.edit().putString("refresh-token", auth.refreshToken).apply()
                        prefs.edit().putString("expired-at", auth.expiresAt.toString()).apply()
                        prefs.edit().putString("app-key", "ktd7xc7sg55pb8d").apply()



                        //Proceed to MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                    }
                }


            }




            }
        }

    /*
    override fun onResume() {
        super.onResume()
        println("test")
        getAccessToken()
    }


    fun getAccessToken() {
            val accessToken = Auth.getOAuth2Token() //generate Access Token

        val cred : DbxCredential? = Auth.getDbxCredential()
        /*
        if (cred != null) {
            println(cred)
            println(cred.accessToken)
            val refreshToken = cred.getRefreshToken()
            val expiredAt = cred.expiresAt.toString()
            val appKey = cred.appKey
        */
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
                val prefs =
                    getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE)
                prefs.edit().putString("access-token", accessToken).apply()
                /*
                prefs.edit().putString("refresh-token", refreshToken)
                prefs.edit().putString("expired-at", expiredAt)
                prefs.edit().putString("app-key", appKey).apply()
                println("saving data: $accessToken $refreshToken $expiredAt $appKey")

                 */

                //Proceed to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

     */


}