package com.example.rezepte

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import com.dropbox.core.v2.users.FullAccount
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //account data
        val accountData: MutableState<FullAccount?> = mutableStateOf(null)
        //dropbox account handling
        val login = DbTokenHandling(getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE))
        val needToLogIn = login.refreshIfExpired(this) {
            //get token
            val token = DbTokenHandling(
                getSharedPreferences(
                    "com.example.rezepte.dropboxintegration",
                    MODE_PRIVATE
                )
            ).retrieveAccessToken()
            //get account data
            CoroutineScope(Dispatchers.IO).launch{
                accountData.value = DownloadTask(DropboxClient.getClient(token)).getUserAccount()
                withContext(Dispatchers.Main) {
                    if (accountData.value == null) {
                        Toast.makeText(this@MainActivity, "can't reach dropbox", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }

        if (needToLogIn) {
            //No token
            //Back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        else {

            setContent {
                RezepteTheme {
                    MainScreen(accountData)
                }
            }

        }

    }


}

@Composable
private fun MainScreen(accountData: MutableState<FullAccount?>) {
    // Fetching the Local Context
    val mContext = LocalContext.current

    Column(modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState()),horizontalAlignment = Alignment.CenterHorizontally){
        //logo
        Image(painter = painterResource(id = R.drawable.icon), contentDescription = "logo image", contentScale = ContentScale.Inside, modifier = Modifier
            .fillMaxHeight(0.6f)
            .fillMaxWidth()
            .weight(1f))
        //main options
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp, 15.dp)
                .animateContentSize()
        ) {
            //create buttons
            CreateButtonOptions()
            //search button
            Button(onClick = {
                val intent = Intent(mContext,SearchActivity::class.java)
                mContext.startActivity(intent);
            }, modifier = Modifier
                .padding(5.dp, 0.dp, 5.dp, 5.dp)
                .fillMaxWidth()) {
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
    //settings button
    Row {
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {
            val intent = Intent(mContext,SettingsActivity::class.java)
            mContext.startActivity(intent)
        }) {
            Icon(
                Icons.Filled.Settings, "settings",
                Modifier
                    .padding(10.dp)
                    .size(24.dp),
                tint =  MaterialTheme.colorScheme.primary)
        }
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
                    value = accountData.value!!.email,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Dropbox Account") },
                    modifier = Modifier
                        .padding(5.dp)
                        .width(220.dp)
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

            }, modifier = Modifier
                .padding(5.dp)
                .align(Alignment.CenterVertically)) {
                Text(text = "Logout", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun CreateButtonOptions() {
    // Fetching the Local Context
    val mContext = LocalContext.current
    var urlInput by remember { mutableStateOf(false)}
    var imageInput by remember { mutableStateOf(false)}
    var urlValue by remember { mutableStateOf("")}
    //get a local image



    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier
        .fillMaxWidth()
        .padding(5.dp, 5.dp)) {

        Button(
            onClick = {
                Toast.makeText(mContext, "Create Recipe", Toast.LENGTH_SHORT).show()
                //move to create activity
                val intent = Intent(mContext, CreateActivity::class.java)
                mContext.startActivity(intent)
            }, modifier = Modifier
                .padding(0.dp, 0.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Create")
        }
        Row {
            Button(
                onClick = { urlInput = !urlInput },
                modifier = Modifier.padding(0.dp, 5.dp)
            ) {
                Text(text = "Load Website", textAlign = TextAlign.Center)
            }
            Spacer(
                Modifier
                    .weight(1f)
            )
            Button(onClick = {
                imageInput = !imageInput
            }, modifier = Modifier.padding(0.dp, 5.dp)) {
                Text(text = "Load Image", textAlign = TextAlign.Center)

            }
        }
        //animate in or out the website link input
        androidx.compose.animation.AnimatedVisibility(
            visible = urlInput,

            enter = scaleIn()
                    + fadeIn(
                // Fade in with the initial alpha of 0.3f.
                initialAlpha = 0.3f
            ) + expandIn(),
            exit = scaleOut() + fadeOut() + shrinkOut()
        ) {
            //text field for link recipe
            TextField(
                value = urlValue,
                onValueChange = { value ->
                    urlValue = value //update its value
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        CoroutineScope(Dispatchers.IO).launch {
                            val settings =
                                SettingsActivity.loadSettings( //todo already have this loaded
                                    mContext.getSharedPreferences(
                                        "com.example.rezepte.settings",
                                        ComponentActivity.MODE_PRIVATE
                                    )
                                )
                            try {
                                val recipe = DownloadWebsite.main(urlValue, settings)
                                withContext(Dispatchers.Main) {
                                    //move to create activity
                                    val intent = Intent(mContext, CreateActivity::class.java)
                                    intent.putExtra("data", parseData(recipe.first))
                                    intent.putExtra("imageData", recipe.second)
                                    mContext.startActivity(intent)
                                }
                            } catch (_: Exception) {
                                //could not load the website
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(
                                        mContext,
                                        "Invalid website",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()

                                }

                            }

                            //clear the url and reset
                            urlValue = ""
                            urlInput = false
                        }

                    }
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                singleLine = true,
                shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                label = { Text("website") }

            )

        }
        if (imageInput){
            AddImageDialog{
                imageInput = false
            }

        }

    }
}
@Composable
fun AddImageDialog(onDismiss: () -> Unit){
    val mContext = LocalContext.current
    val getImageContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        //if the user gave an image convert it to a recipe and take that to the create
        if (uri != null){
            ImageToRecipe.convert(uri,mContext, settings = SettingsActivity.loadSettings(  mContext.getSharedPreferences("com.example.rezepte.settings",ComponentActivity.MODE_PRIVATE )), error = { Toast.makeText(mContext, "No Recipe Found", Toast.LENGTH_SHORT).show()})
            {
                //if the recipe is still empty don't start create just give error
                if (it == GetEmptyRecipe()){
                    Toast.makeText(mContext, "No Recipe Found", Toast.LENGTH_SHORT).show()
                    return@convert
                }
                //when loaded send the recipe to the create menu
                val intent = Intent(mContext,CreateActivity::class.java)

                intent.putExtra("data",parseData(it))
                //intent.putExtra("imageData",recipe.second) add image
                mContext.startActivity(intent)
                onDismiss()
            }

        }
    }
    val takeImageContent = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        //if the user gave an image convert it to a recipe and take that to the create
        if (bitmap != null) {
            ImageToRecipe.convert(
                bitmap,
                settings = SettingsActivity.loadSettings(
                    mContext.getSharedPreferences(
                        "com.example.rezepte.settings",
                        ComponentActivity.MODE_PRIVATE
                    )
                ),
                error = { Toast.makeText(mContext, "No Recipe Found", Toast.LENGTH_SHORT).show() })
            {
                //if the recipe is still empty don't start create just give error
                if (it == GetEmptyRecipe()) {
                    Toast.makeText(mContext, "No Recipe Found", Toast.LENGTH_SHORT).show()
                    return@convert
                }
                //when loaded send the recipe to the create menu
                val intent = Intent(mContext, CreateActivity::class.java)

                intent.putExtra("data", parseData(it))
                //intent.putExtra("imageData",recipe.second) add image
                mContext.startActivity(intent)
                onDismiss()
            }

        }
    }
    Dialog(onDismissRequest = { onDismiss() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column (modifier = Modifier.padding(10.dp)) {
                Text(text = "Select method to load Image", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(15.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    //camara button
                    Button(
                        onClick = {
                            takeImageContent.launch()
                        }, modifier = Modifier
                            .padding(0.dp, 5.dp)
                            .weight(1f)
                    ) {
                        Text(text = "Camara", textAlign = TextAlign.Center)

                    }
                    Spacer(modifier = Modifier.weight(0.2f))
                    //file button
                    Button(
                        onClick = {
                            getImageContent.launch("image/*")
                        }, modifier = Modifier
                            .padding(0.dp, 5.dp)
                            .weight(1f)
                    ) {
                        Text(text = "File", textAlign = TextAlign.Center)

                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "the image needs to be upright and only the text of one recipe visible and clearly readable",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
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