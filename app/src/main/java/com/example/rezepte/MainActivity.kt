package com.example.rezepte

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.core.content.FileProvider
import com.dropbox.core.v2.users.FullAccount
import com.example.rezepte.recipeCreation.externalLoading.DownloadWebsite
import com.example.rezepte.recipeCreation.externalLoading.ImageToRecipe
import com.example.rezepte.fileManagment.dropbox.DbTokenHandling
import com.example.rezepte.fileManagment.dropbox.DownloadTask
import com.example.rezepte.fileManagment.dropbox.DropboxClient
import com.example.rezepte.recipeCreation.CreateActivity
import com.example.rezepte.recipeCreation.parseData
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //if the user has not completed the walk though take them there
        val prefs =
            this.getSharedPreferences(
                "com.example.rezepte.walkThrough",
                Context.MODE_PRIVATE
            )
        if (!prefs.getBoolean("completed", false)){
            val intent = Intent(this, WalkThoughActivity::class.java)
            this.startActivity(intent)
        }

        //account data
        val accountData: MutableState<Pair<FullAccount?,Boolean>> = mutableStateOf(Pair(null,true))
        //dropbox account handling
        val login = DbTokenHandling(getSharedPreferences("com.example.rezepte.dropboxintegration", MODE_PRIVATE))
        if(login.isLoggedIn()) { //only check the login if the user is logged in
                login.refreshIfExpired(this) {
                //get token
                val token = DbTokenHandling(
                    getSharedPreferences(
                        "com.example.rezepte.dropboxintegration",
                        MODE_PRIVATE
                    )
                ).retrieveAccessToken()
                //get account data
                CoroutineScope(Dispatchers.IO).launch {
                    accountData.value =
                        Pair(DownloadTask(DropboxClient.getClient(token)).getUserAccount(), true)
                    withContext(Dispatchers.Main) {
                        if (accountData.value.first == null) {
                            Toast.makeText(
                                this@MainActivity,
                                "can't reach dropbox",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                }
            }
        }else {
            accountData.value = Pair(null, false)
        }




        setContent {
            RezepteTheme {
                MainScreen(accountData)
            }
        }

        }

    }




@Composable
private fun MainScreen(accountData: MutableState<Pair<FullAccount?,Boolean>>) {
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
            //search button (my recipes)
            Button(onClick = {
                val intent = Intent(mContext, SearchActivity::class.java)
                mContext.startActivity(intent);
            }, modifier = Modifier
                .padding(5.dp, 0.dp, 5.dp, 5.dp)
                .fillMaxWidth()) {
                Text(text = "My Recipes")
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
            val intent = Intent(mContext, SettingsActivity::class.java)
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
fun DropboxInfo(accountData : MutableState<Pair<FullAccount?,Boolean>>) {
    // Fetching the Local Context
    val mContext = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .animateContentSize()
    ) {
        if (accountData.value.second) {//if the user is signed in
            Row {
                if (accountData.value.first != null) {
                    TextField(
                        value = accountData.value.first!!.email,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dropbox Account") },
                        modifier = Modifier
                            .padding(5.dp),
                        maxLines = 1
                    )
                }

                Spacer(
                    Modifier
                        .weight(1f)
                )
                Button(
                    onClick = {
                        Toast.makeText(mContext, "Logging out...", Toast.LENGTH_SHORT).show()

                        val prefs = mContext.getSharedPreferences(
                            "com.example.rezepte.dropboxintegration",
                            ComponentActivity.MODE_PRIVATE
                        )
                        prefs.edit().clear().apply()
                        //Back to LoginActivity
                        val intent = Intent(mContext, LoginActivity::class.java)
                        mContext.startActivity(intent)

                    }, modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(text = "Logout", textAlign = TextAlign.Center)
                }
            }
        } else{ //the user is not logged in (show the option to log in
            Row {
                Text(text = "Login to dropbox to be able to  sync between devices",style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f))
                Spacer(
                    Modifier
                        .weight(0.1f)
                )
                Button(
                    onClick = {
                        val intent = Intent(mContext, LoginActivity::class.java)
                        mContext.startActivity(intent)


                    }, modifier = Modifier
                        .padding(5.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(text = "Login", textAlign = TextAlign.Center)
                }
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

        if (urlInput){
            GetStringDialog(
                label = "Website",
                descriptionText = "Input the url of the website you would like to load",
                onDismiss = { urlInput = false }
            ){
                CoroutineScope(Dispatchers.IO).launch {
                    val settings =
                        SettingsActivity.loadSettings( //todo already have this loaded
                            mContext.getSharedPreferences(
                                "com.example.rezepte.settings",
                                ComponentActivity.MODE_PRIVATE
                            )
                        )
                    try {
                        val recipe = DownloadWebsite.main(it, settings)
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

                    urlInput = false
                }
            }
        }

        if (imageInput){
            AddImageDialog("the image needs to be upright and only the text of one recipe visible and clearly readable", onDismiss = { imageInput = false})
            {imageUri : Uri ->
                ImageToRecipe.convert(imageUri,mContext, settings = SettingsActivity.loadSettings(  mContext.getSharedPreferences("com.example.rezepte.settings",ComponentActivity.MODE_PRIVATE )), error = { Toast.makeText(mContext, "No Recipe Found", Toast.LENGTH_SHORT).show()})
                {
                    //if the recipe is still empty don't start create just give error
                    if (it == GetEmptyRecipe()){
                        Toast.makeText(mContext, "No Recipe Found", Toast.LENGTH_SHORT).show()
                        return@convert
                    }
                    //when loaded send the recipe to the create menu
                    val intent = Intent(mContext, CreateActivity::class.java)

                    intent.putExtra("data", parseData(it))
                    //intent.putExtra("imageData",recipe.second) add image
                    mContext.startActivity(intent)
                    imageInput = false
                }
            }

        }

    }
}
@Composable
fun GetStringDialog (label: String, descriptionText : String, onDismiss: () -> Unit, onReturnString: (String)-> Unit){
    var stringInput by remember { mutableStateOf("")}

    Dialog(onDismissRequest = { onDismiss() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column (modifier = Modifier.padding(10.dp)) {
                Text(text = label, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(15.dp))
                TextField(
                    value = stringInput,
                    onValueChange = { value ->
                        stringInput = value //update its value
                    },
                    keyboardActions = KeyboardActions(
                        onDone = {
                        onReturnString(stringInput)
                        }

                    ),
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                    singleLine = true,
                    shape = RectangleShape, // The TextFiled has rounded corners top left and right by default

                )
                Button(
                    onClick = {
                        onReturnString(stringInput)
                    }, modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Finish", textAlign = TextAlign.Center)

                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text =descriptionText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
@Composable
fun AddImageDialog(descriptionText : String,onDismiss: () -> Unit,onReturnUri: (Uri)-> Unit){
    val mContext = LocalContext.current
    val getImageContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        //if the user gave an image convert it to a recipe and take that to the create
        if (uri != null){
           onReturnUri(uri)

        }
    }
    val builder = VmPolicy.Builder()
    StrictMode.setVmPolicy(builder.build())
    val imageUri = FileProvider.getUriForFile(
        mContext,
        "package.fileprovider",
        createTempFile(mContext)
    )
    val takeImageContent = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { sucsess: Boolean ->
        //if the user gave an image convert it to a recipe and take that to the create
        if (sucsess) {
            onReturnUri(imageUri)

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
                            takeImageContent.launch(imageUri)
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
                        text =descriptionText,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }


}
fun createTempFile(context: Context): File {
    return File.createTempFile(
        System.currentTimeMillis().toString(),
        ".jpg",
        context.externalCacheDir
    )
}


@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun homePreview() {
    RezepteTheme {
        MainScreen(mutableStateOf(Pair(null,false)))
    }
}