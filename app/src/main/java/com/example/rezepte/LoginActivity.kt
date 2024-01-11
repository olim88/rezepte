package com.example.rezepte

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.TokenAccessType
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LoginActivity : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RezepteTheme {
                MainScreen()
            }
        }

    }


}
@Composable
fun mainLoginUi(finished: ()-> Unit){
    // Fetching the Local Context
    val mContext = LocalContext.current
    val webAuth = DbxPKCEWebAuth(DbxRequestConfig("examples-authorize"), DbxAppInfo(mContext.resources.getString(R.string.dropbox_api_key)))
    var linkValue by remember { mutableStateOf("")}
    Column {
        //login button
        Spacer(modifier = Modifier.weight(0.1f))
        Button(onClick = {
            Toast.makeText(mContext, "Link Dropbox", Toast.LENGTH_SHORT).show()
            //start login
            //Auth.startOAuth2Authentication(applicationContext, "ktd7xc7sg55pb8d")
            val webAuthRequest = DbxWebAuth.newRequestBuilder()
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .build()

            val authorizeUrl = webAuth.authorize(webAuthRequest)
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUrl))
            mContext.startActivity(browserIntent)
        }, modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth()) {
            Text(text = "Login To Dropbox", modifier = Modifier.padding(15.dp))
        }

        //input for login
        Spacer(modifier = Modifier.height(20.dp))
        TextField(
            value = linkValue,
            onValueChange = { value ->
                linkValue = value //update its value
                CoroutineScope(Dispatchers.IO).launch {
                    val auth = try{
                        webAuth.finishFromCode(linkValue)
                    }
                    catch (e : Exception){
                        null
                    }
                    withContext(Dispatchers.Main) {
                        if (auth != null) { //make sure the auth is correct before using it
                            if (auth.accessToken != null) {
                                //Store accessToken in SharedPreferences
                                val prefs =
                                    mContext.getSharedPreferences(
                                        "com.example.rezepte.dropboxintegration",
                                        Context.MODE_PRIVATE
                                    )
                                prefs.edit().putString("access-token", auth.accessToken).apply()
                                prefs.edit().putString("refresh-token", auth.refreshToken).apply()
                                prefs.edit().putString("expired-at", auth.expiresAt.toString()).apply()
                                //set the status of the user being logged in to true
                                prefs.edit().putBoolean("logged-in", true).apply()


                                finished()
                            }
                        }
                    }
                    //upload the users files to dropbox
                    //get the files needed to upload
                    val recipes = LocalFilesTask.listFolder("${mContext.filesDir}/xml/")
                    val images = LocalFilesTask.listFolder("${mContext.filesDir}/image/")
                    val token = auth?.accessToken
                    val dbClient =DropboxClient.getClient(token)
                    val downloader = DownloadTask(dbClient)
                    val uploader = UploadTask(dbClient)
                    //loop though recipes and upload if there is not a newer version on dropbox
                    if (recipes != null) {
                        for (file in recipes){
                            val fileData = LocalFilesTask.loadString("${mContext.filesDir}/xml/",file)
                            if (fileData != null) {//there should not be possible to have a null file but just incase do not do anything if there is
                                val dbFile = try {
                                    downloader.getXml("/xml/${file}")
                                } catch (e: Exception) {
                                    null
                                }
                                if (dbFile != null) {//if the file already exists
                                    if (dbFile.second.toInstant().toEpochMilli() - fileData.second.toInstant().toEpochMilli() > 5000){ //and the online version is more than 5 seconds older
                                        //upload the new file
                                        uploader.uploadXml(fileData.first, "/xml/${file}")
                                    }

                                } else { //just upload the file if it dose not already exist
                                    uploader.uploadXml(fileData.first, "/xml/${file}")
                                }
                            }

                        }
                    }
                    //do the same for images
                    if (images != null) {
                        for (image in images){
                            val fileData = LocalFilesTask.loadString("${mContext.filesDir}/image/",image)
                            if (fileData != null) {//there should not be possible to have a null image but just incase do not do anything if there is
                                val dbFile = try {
                                    downloader.getXml("/image/${image}")
                                } catch (e: Exception) {
                                    null
                                }
                                if (dbFile != null) {//if the image already exists
                                    if (dbFile.second.toInstant().toEpochMilli() - fileData.second.toInstant().toEpochMilli() > 5000){ //and the online version is more than 5 seconds older
                                        //upload the new image
                                        uploader.uploadXml(fileData.first, "/image/${image}")
                                    }

                                } else { //just upload the image if it dose not already exist
                                    uploader.uploadXml(fileData.first, "/image/${image}")
                                }
                            }

                        }
                    }
                }
            },

            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            singleLine = true,
            shape = RoundedCornerShape(5.dp), // The TextFiled has rounded corners top left and right by default
            label = { Text("code") }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(){
    // Fetching the Local Context
    val mContext = LocalContext.current
    Column (modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())) {
        //return home button
        Button(onClick = {
            
            val intent = Intent(mContext, MainActivity::class.java)
            mContext.startActivity(intent)
        },modifier = Modifier.align(Alignment.End)){
            Icon(
                Icons.Filled.Home, "home",
                Modifier
                    .size(24.dp),)

        }
        //title
        Text(text = "Login to Dropbox to sync between devices", modifier = Modifier
            .padding(15.dp)
            .align(Alignment.CenterHorizontally), textAlign = TextAlign.Center, color =MaterialTheme.colorScheme.onBackground,style = MaterialTheme.typography.titleLarge )
        //logo
        Image(painter = painterResource(id = R.drawable.book), contentDescription = "logo image", contentScale = ContentScale.FillHeight, modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .fillMaxHeight()
            .weight(0.5f))
        mainLoginUi(){
            //Proceed to MainActivity
            val intent = Intent(mContext, MainActivity::class.java)
            mContext.startActivity(intent)
        }

        //description
        Spacer(modifier = Modifier.weight(0.4f))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp, 15.dp)
                .animateContentSize()
        ) {
            Text(
                text = "To link dropbox to app click the button and then once you get the code input that into the box above",
                modifier = Modifier.padding(15.dp)
            )
        }
    }

}


@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun loginPreview(){
    RezepteTheme {
        MainScreen()
    }
}