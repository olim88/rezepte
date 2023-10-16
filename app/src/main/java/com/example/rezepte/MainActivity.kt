package com.example.rezepte

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dropbox.core.v2.users.FullAccount
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homelayout);


        findViewById<Button>(R.id.btnCreateWebsite).setOnClickListener{
            Toast.makeText(this, "Create Recipe", Toast.LENGTH_SHORT).show()
            //move to create activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra("recipe name","thi") //todo remove
            intent.putExtra("preload option","website")
            intent.putExtra("preload data","http://www.yummly.com/recipe/Yogurt-Coffee-Cake-473376?prm-v1")//todo get website from user
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
        //get token
        val token = DbTokenHandling(
            getSharedPreferences(
                "com.example.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        ).retrieveAccessToken()
        var accountData : MutableState<FullAccount?> = mutableStateOf(null)
        setContent {
            RezepteTheme {
                MainScreen(accountData)
            }
        }
        //get account data
        GlobalScope.launch {
            accountData.value = DownloadTask(DropboxClient.getClient(token)).getUserAccount()
        }










    }












}

@Composable
private fun MainScreen(accountData: MutableState<FullAccount?>) {
    // Fetching the Local Context
    val mContext = LocalContext.current
    Column(modifier = Modifier.padding(10.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()),horizontalAlignment = Alignment.CenterHorizontally){
        //logo
        Image(painter = painterResource(id = R.drawable.book), contentDescription = "logo image", contentScale = ContentScale.FillHeight, modifier = Modifier.fillMaxHeight(0.6f).fillMaxWidth().weight(1f))
        //main options
        Spacer(
            Modifier
                .width(10.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp,5.dp)
                .animateContentSize()
        ) {
            //create buttons
            CreateButtonOptions()
            //search button
            Button(onClick = {
                val intent = Intent(mContext,SearchActivity::class.java)
                mContext.startActivity(intent);
            }, modifier = Modifier.padding(5.dp,0.dp,5.dp,5.dp).fillMaxWidth()) {
                Text(text = "Search")
            }
        }
        //dropbox text and button
        Spacer(
            Modifier
                .weight(0.9f)
        )
        DropboxInfo(accountData)

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropboxInfo(accountData : MutableState<FullAccount?>) {
    // Fetching the Local Context
    val mContext = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .animateContentSize()
    ) {
        Row {
            if (accountData.value != null){
                TextField(
                    value = "Account Name:${accountData.value!!.name.displayName}\n${accountData.value!!.email}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Dropbox Account") },
                    modifier = Modifier.padding(5.dp).width(220.dp)
                )
            }

            Spacer(
                Modifier
                    .weight(1f)
            )
            Button(onClick = {
                Toast.makeText(mContext, "Logging out...", Toast.LENGTH_SHORT).show()

                val prefs = mContext.getSharedPreferences("com.example.rezepte.dropboxintegration",
                    ComponentActivity.MODE_PRIVATE
                )
                prefs.edit().clear().apply()
                //Back to LoginActivity
                val intent = Intent(mContext, LoginActivity::class.java)
                mContext.startActivity(intent)

            }, modifier = Modifier.padding(5.dp).align(Alignment.CenterVertically)) {
                Text(text = "Logout", textAlign = TextAlign.Center)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateButtonOptions() {
    // Fetching the Local Context
    val mContext = LocalContext.current
    var urlInput by remember { mutableStateOf(false)}
    var urlValue by remember { mutableStateOf("")}
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(5.dp,5.dp)) {

        Button(onClick = {
            Toast.makeText(mContext, "Create Recipe", Toast.LENGTH_SHORT).show()
            //move to create activity
            val intent = Intent(mContext,CreateActivity::class.java)
            mContext.startActivity(intent)
        }, modifier = Modifier.padding(0.dp,0.dp).fillMaxWidth()) {
            Text(text =  "Create")
        }
        Row{
            Button(onClick = { urlInput = !urlInput},
                modifier = Modifier.padding(0.dp,5.dp)) {
                Text(text =  "Scrape Website", textAlign = TextAlign.Center)
            }
            Spacer(
                Modifier
                    .weight(1f)
            )
            Button(onClick = {
                //todo
            }, modifier = Modifier.padding(0.dp,5.dp)) {
                Text(text =  "Scrape Image", textAlign = TextAlign.Center)
            }
        }
        if (urlInput){
            TextField(
                value = urlValue,
                onValueChange = { value ->
                    urlValue = value //update its value
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        GlobalScope.launch {
                            val recipe = DownloadWebsite.main(urlValue)
                            withContext(Dispatchers.Main) {
                                //move to create activity
                                val intent = Intent(mContext,CreateActivity::class.java)
                                intent.putExtra("data",parseData(recipe.first))
                                intent.putExtra("imageData",recipe.second)
                                mContext.startActivity(intent)
                            }
                            //clear the url and reset
                            urlValue= ""
                            urlInput= false
                    } }
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                singleLine = true,
                shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                label = { Text("website") }

            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun homePreview() {
    RezepteTheme {
        MainScreen(mutableStateOf(null))
    }
}