package com.example.rezepte

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dropbox.core.v2.users.FullAccount
import com.example.rezepte.DropboxClient.getClient
import com.example.rezepte.UserAccountTask.TaskDelegate
import com.example.rezepte.ui.theme.RezepteTheme
import java.io.File


class MainActivity : ComponentActivity() {

    //login handling
    private var ACCESS_TOKEN: String? = null
    private val IMAGE_REQUEST_CODE = 101



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homelayout);

        findViewById<Button>(R.id.btnCreate).setOnClickListener{
            Toast.makeText(this, "Create Recipe", Toast.LENGTH_SHORT).show()
            //move to create activity
            val intent = Intent(this,CreateActivity::class.java)
            startActivity(intent);
        }

        findViewById<Button>(R.id.btnCreateWebsite).setOnClickListener{
            Toast.makeText(this, "Create Recipe", Toast.LENGTH_SHORT).show()
            //move to create activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra("preload option","website")
            intent.putExtra("preload data","http://www.yummly.com/recipe/Yogurt-Coffee-Cake-473376?prm-v1")//todo get website from user
            startActivity(intent);
        }

        //setup search
        findViewById<Button>(R.id.btnSearch).setOnClickListener{
            val intent = Intent(this,SearchActivity::class.java)
            startActivity(intent);
        }

        //dropbox account handling

        val login = DbTokenHandling(getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE))

        if (login.refreshIfExpired()) {
            //No token
            //Back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        ACCESS_TOKEN = retrieveAccessToken()
        getUserAccount()

        //logout button
        findViewById<Button>(R.id.btnLogout).setOnClickListener{
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()

            val prefs = getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE)
            prefs.edit().clear().apply()

            //Back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }





    }

    private fun getUserAccount() {

        if (ACCESS_TOKEN == null) return

        UserAccountTask(getClient(ACCESS_TOKEN), object : TaskDelegate {
            override fun onAccountReceived(account: FullAccount?) {
                //Print account's info
                Log.d("User", account!!.email)
                Log.d("User", account.name.displayName)
                Log.d("User", account.accountType.name)
                updateUI(account)
            }

            override fun onError(error: Exception?) {
                Log.d("User", "Error receiving account details.")
            }
        }).execute()

    }
    private fun updateUI(account: FullAccount) {
        val output = findViewById<View>(R.id.dropBoxInfo) as TextView
        output.text = "Name: " + account.name.displayName + " Email:" + account!!.email

    }
    private fun tokenExists(): Boolean {
        val prefs = getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE)
        val accessToken = prefs.getString("access-token", null)
        return accessToken != null
    }

    private fun retrieveAccessToken(): String? {
        //check if ACCESS_TOKEN is stored on previous app launches
        val prefs = getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE)
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
    private fun upload() {
        if (ACCESS_TOKEN == null) return
        //Select image to upload
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(
            Intent.createChooser(
                intent,
                "Upload to Dropbox"
            ), IMAGE_REQUEST_CODE
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        // Check which request we're responding to
        if (requestCode == IMAGE_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                if (data != null){
                    val dataURI : Uri = data.data!!;
                    //Image URI received
                    val file = File(URI_to_Path.getPath(application, dataURI))
                    if (file != null) {
                        //Initialize UploadTask
                        //UploadTask(getClient(ACCESS_TOKEN), file, applicationContext,"testname").execute() //unused
                    }
                }
            }
        }
    }




}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RezepteTheme {
        Greeting("Android")
    }
}