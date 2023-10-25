package com.example.rezepte


import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


class SearchActivity : ComponentActivity() {
    private var ACCESS_TOKEN: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //see if there are extra data sent e.g. geting name of recipe not just opening it
        val extras = intent.extras
        var returnName =  (extras != null && extras.getBoolean("get recipe name"))



        //set access token
        ACCESS_TOKEN = DbTokenHandling(
            getSharedPreferences(
                "com.example.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        ).retrieveAccessToken()

        val downloader = DownloadTask(DropboxClient.getClient(ACCESS_TOKEN))
        var thumbnails = mutableMapOf<String,Bitmap?>()
        var hasThumbnals = mutableStateOf(false)
        GlobalScope.launch {
            //get data
            val data = downloader.listDir("/xml/").toMutableList()
            //clean data
            val iterate = data.listIterator()
            while (iterate.hasNext()) {
                val oldValue = iterate.next()
                iterate.set(oldValue.removeSuffix(".xml"))
            }
            //get thumbnails
            withContext(Dispatchers.Main) {
                setContent {
                    RezepteTheme {
                        MainScreen(data, thumbnails,returnName,hasThumbnals)
                    }
                }
            }
            thumbnails.putAll(downloader.getThumbnails("/image/", data))
            hasThumbnals.value = true

            }
    }

}


@Composable
fun RecipeCard(name: String, thumbNail : Bitmap?, getName : Boolean){

    // Fetching the Local Context
    val mContext = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 15.dp,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize()
            .padding(1.dp),

        ) {
        if (isExpanded){
            Row {
                if (thumbNail != null) {
                    val brush = Brush.horizontalGradient(colorStops = arrayOf(0.4f to Color.White, 1f to Color.Transparent))
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbNail)
                            .build(),
                        contentDescription = "Contact profile picture",
                        modifier = Modifier
                            // Set image size to 50 dp
                            .size(128.dp)
                            // Clip image to be shaped as a circle
                            .clip(RoundedCornerShape(5.dp))
                            .align(Alignment.CenterVertically)
                            .animateContentSize()
                            .graphicsLayer { alpha = 0.99f }
                            .drawWithContent {
                                drawContent()
                                drawRect(brush, blendMode = BlendMode.DstIn)
                            },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Row {
                if (thumbNail != null){
                    Spacer(
                        Modifier.width(120.dp)
                    )
                }
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    //recipe name
                    Text(
                        "$name",
                        modifier = Modifier.padding(all = 2.dp),
                        style = MaterialTheme.typography.titleMedium,
                        softWrap = true,
                    )
                    //recipe details
                    Column {
                        Text(
                            "servings:",
                            modifier = Modifier.padding(all = 2.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            " Speed:",
                            modifier = Modifier.padding(all = 2.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "otherthing:",
                            modifier = Modifier.padding(all = 2.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        }
                }

                Spacer(
                    Modifier
                )
                Button(
                    onClick = {
                        if (getName) { //if returning a name to createing
                            val intent = Intent(mContext, CreateActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            intent.putExtra("creating", true)
                            intent.putExtra("recipe name", name)
                            mContext.startActivity(intent)
                        } else {
                            val intent = Intent(mContext, MakeActivity::class.java)
                            intent.putExtra("recipe name", name)
                            mContext.startActivity(intent)
                        }
                    },
                    modifier = Modifier
                        .padding(all = 5.dp)
                        .requiredWidth(IntrinsicSize.Max)
                ) {
                    Text(
                        if (getName) "Link" else "Make",
                        modifier = Modifier.padding(all = 2.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

            }
        }
        else {

            Row {

                //recipe image
                if (thumbNail != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(thumbNail)
                            .build(),
                        contentDescription = "Contact profile picture",
                        modifier = Modifier
                            // Set image size to 50 dp
                            .size(60.dp)
                            // Clip image to be shaped as a circle
                            .clip(RoundedCornerShape(5.dp))
                            .animateContentSize()
                            .align(Alignment.CenterVertically)
                            .border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(5.dp)
                            ),
                        contentScale = ContentScale.Crop

                    )
                }
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    //recipe name
                    Text(
                        "$name",
                        modifier = Modifier.padding(all = 2.dp),
                        style = MaterialTheme.typography.titleMedium,
                        softWrap = true,
                    )
                    //recipe details
                    Text(
                        "servings:   Speed:  otherthing:    ",
                        modifier = Modifier.padding(all = 2.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )

                }


            }
        }


    }}





@Composable
fun RecipeList(names: List<String>, thumbnails: MutableMap<String,Bitmap?>, state: MutableState<TextFieldValue>,getName : Boolean){
    var filteredNames: List<String>

    LazyColumn {
        val searchedText = state.value.text
        filteredNames = if (searchedText.isEmpty()) {
            names
        } else {
            val resultList = mutableListOf<String>()
            for (name in names) {
                if (name.lowercase(Locale.getDefault())
                        .contains(searchedText.lowercase(Locale.getDefault()))
                ) {
                    resultList.add(name)
                }
            }
            resultList
        }

        items(filteredNames) { name ->

            RecipeCard(name, thumbnails[name], getName)

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar() {
    TopAppBar(
        title = { Text(text = stringResource(R.string.app_name), fontSize = 18.sp) },

        )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchView(state: MutableState<TextFieldValue>) {
    TextField(
        value = state.value,
        onValueChange = { value ->
            state.value = value
        },
        modifier = Modifier
            .fillMaxWidth(),
        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "",
                modifier = Modifier
                    .padding(15.dp)
                    .size(24.dp)
            )
        },
        trailingIcon = {
            if (state.value != TextFieldValue("")) {
                IconButton(
                    onClick = {
                        state.value =
                            TextFieldValue("") // Remove text from TextField when you press the 'X' icon
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "",
                        modifier = Modifier
                            .padding(15.dp)
                            .size(24.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RectangleShape, // The TextFiled has rounded corners top left and right by default

    )
}
@Composable
private fun MainScreen(names: List<String>, thumbnails: MutableMap<String,Bitmap?>,getName : Boolean, updatedThumbnail : MutableState<Boolean>) {
    val textState = remember { mutableStateOf(TextFieldValue("")) }
    Column {
        SearchView(textState)
        RecipeList(names,thumbnails,textState,getName)
        if (updatedThumbnail.value){
            //this will update the thumbnails
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
private fun MainScreenPreview() {
    RezepteTheme {
        MainScreen((listOf("Carrot Cake", "other Cake")), hashMapOf(),false, mutableStateOf(false))
    }
}
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun SearchViewPreview() {
    RezepteTheme {
        val textState = remember { mutableStateOf(TextFieldValue("")) }
        SearchView(textState)
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun TopBarPreview() {
    RezepteTheme {
        Surface {
            MyTopBar()
        }
    }}

@SuppressLint("UnrememberedMutableState")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)

@Composable
fun previewRecipeCard(){
    RezepteTheme {
        Surface {
            RecipeCard(name = "Carrot Cake",null,false)

        }
    }}
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun previewRecipeCards(){
    val textState = remember { mutableStateOf(TextFieldValue("")) }
    RezepteTheme {
        Surface {
            RecipeList( (listOf("Carrot Cake", "other Cake" )), hashMapOf(),textState,false)
        }
    }}