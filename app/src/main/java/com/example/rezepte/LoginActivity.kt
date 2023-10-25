package com.example.rezepte

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.TokenAccessType
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(){
    // Fetching the Local Context
    val mContext = LocalContext.current
    val webAuth = DbxPKCEWebAuth(DbxRequestConfig("examples-authorize"), DbxAppInfo("ktd7xc7sg55pb8d"))
    var linkValue by remember { mutableStateOf("")}
    Column (modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        //logo
        Image(painter = painterResource(id = R.drawable.book), contentDescription = "logo image", contentScale = ContentScale.FillHeight, modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxHeight().weight(0.5f))
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
        Spacer(modifier = Modifier.weight(0.1f))
        TextField(
            value = linkValue,
            onValueChange = { value ->
                linkValue = value //update its value
                GlobalScope.launch {

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
                                        MODE_PRIVATE
                                    )
                                prefs.edit().putString("access-token", auth.accessToken).apply()
                                prefs.edit().putString("refresh-token", auth.refreshToken).apply()
                                prefs.edit().putString("expired-at", auth.expiresAt.toString())
                                    .apply()
                                prefs.edit().putString("app-key", "ktd7xc7sg55pb8d").apply()


                                //Proceed to MainActivity
                                val intent = Intent(mContext, MainActivity::class.java)
                                mContext.startActivity(intent)
                            }
                        }
                    }
                }
            },

            modifier = Modifier
                .fillMaxWidth().padding(5.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            singleLine = true,
            shape = RoundedCornerShape(5.dp), // The TextFiled has rounded corners top left and right by default
            label = { Text("code") }
        )
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