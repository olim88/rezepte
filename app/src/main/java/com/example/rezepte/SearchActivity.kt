package com.example.rezepte


import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.rezepte.ui.theme.CreateAutomations
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale


class SearchActivity : ComponentActivity() {
    private var ACCESS_TOKEN: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //see if there are extra data sent e.g. geting name of recipe not just opening it
        val extras = intent.extras
        var returnName =  (extras != null && extras.getBoolean("get recipe name"))

        //get settings
        val settings = SettingsActivity.loadSettings(getSharedPreferences(
            "com.example.rezepte.settings",
            MODE_PRIVATE
        ))

        //set access token
        ACCESS_TOKEN = DbTokenHandling(
            getSharedPreferences(
                "com.example.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        ).retrieveAccessToken()

        val data : MutableState<List<String>> = mutableStateOf(mutableListOf())
        val thumbnails = mutableMapOf<String,Bitmap?>()
        val hasThumbnails = mutableStateOf(false)
        //load local save list if enabled in settings
        val localList = if (settings["Local Saves.Cache recipe names"] == "true") {
            LocalFilesTask.loadFile("${this.filesDir}","listOfRecipes.xml")
        } else {null}
        if (localList != null){
            data.value = localList.first.replace(".xml","").split("\n")

            //if there are locally saved thumbnails load them if data is not empty
            if (settings["Local Saves.Cache recipe image"]== "thumbnail"||settings[""]== "full sized"){
                for (name in data.value){
                    thumbnails[name] = LocalFilesTask.loadBitmap("${this@SearchActivity.filesDir}/thumbnail/","$name.png")?.first
                }
                hasThumbnails.value = true
            }
        }

        val downloader = DownloadTask(DropboxClient.getClient(ACCESS_TOKEN))




        //set the content of the window
        setContent {
            RezepteTheme {
                MainScreen(data, thumbnails,returnName,hasThumbnails)
            }
        }


        CoroutineScope(Dispatchers.IO).launch {
            //get data
            val list = downloader.listDir("/xml/") ?: listOf()
            if( list.isNotEmpty()) {//if can get to dropbox
                val onlineList = list.toMutableList()
                //clean data
                val iterate = onlineList.listIterator()
                while (iterate.hasNext()) {
                    val oldValue = iterate.next()
                    iterate.set(oldValue.removeSuffix(".xml"))
                }
                if (localList == null || onlineList != localList.first.replace(".xml", "")
                        .split("\n")) { //if the lists are different use the online version and save to to local if enabled
                    data.value = onlineList
                    if (settings["Local Saves.Cache recipe names"] == "true") {
                        LocalFilesTask.saveString(
                            onlineList.joinToString("\n"),
                            "${this@SearchActivity.filesDir}",
                            "listOfRecipes.xml"
                        )
                    }
                }
                //if there were no local saved names thumbnails need to be grabbed now
                if (localList == null){
                    //if there are locally saved thumbnails load them
                    if (settings["Local Saves.Cache recipe image"]== "thumbnail"||settings[""]== "full sized"){
                        for (name in data.value){
                            thumbnails[name] = LocalFilesTask.loadBitmap("${this@SearchActivity.filesDir}/thumbnail/","$name.png")?.first
                        }
                        hasThumbnails.value = true
                    }
                }



                //get thumbnails
                val thumbnailsDownloaded = downloader.getThumbnails("/image/", data.value)
                if (thumbnailsDownloaded != null) {
                    if (settings["Local Saves.Cache recipe image"]== "thumbnail"||settings[""]== "full sized"){
                        for (thumbnailKey in thumbnails.keys){
                            if (thumbnailsDownloaded[thumbnailKey]?.sameAs(thumbnails[thumbnailKey]) == false){
                                //if they are not the same save the thumbnail to device update the value and set hasThumbnails to true
                                thumbnails[thumbnailKey] = thumbnailsDownloaded[thumbnailKey]
                                hasThumbnails.value = true

                                if (thumbnailsDownloaded[thumbnailKey] != null) {
                                    LocalFilesTask.saveBitmap(
                                        thumbnailsDownloaded[thumbnailKey]!!,
                                        "${this@SearchActivity.filesDir}/thumbnail/",
                                        "$thumbnailKey.png"
                                    )
                                } else {
                                    //delete the file if not on dropbox
                                    LocalFilesTask.removeFile("${this@SearchActivity.filesDir}/thumbnail/","$thumbnailKey.png")
                                }
                            }
                        }
                    }else {//if setting not enabled just load the thumbnails from dropbox
                        thumbnails.putAll(thumbnailsDownloaded)
                        hasThumbnails.value = true
                    }

                }
            }


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
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .requiredHeightIn(63.dp)
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
fun RecipeList(
    names: MutableState<List<String>>,
    thumbnails: MutableMap<String, Bitmap?>,
    state: MutableState<TextFieldValue>,
    filters: MutableState<Map<String, MutableState<Boolean>>>,
    getName: Boolean
){
    var filteredNames: List<String>

    LazyColumn {
        val searchedText = state.value.text

        val resultList = mutableListOf<String>()
        for (name in names.value) {
            if (name.lowercase(Locale.getDefault())
                    .contains(searchedText.lowercase(Locale.getDefault()))
            ) {
                var filted = true
                for (filter in filters.value){
                    if (filter.value.value && !name.lowercase(Locale.getDefault())
                            .contains(filter.key.lowercase(Locale.getDefault()))
                    ){
                        filted = false
                    }
                }
                if (filted){
                    resultList.add(name)
                }

            }


        }
        filteredNames = resultList

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
        textStyle = TextStyle( fontSize = 18.sp),
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilters(filters : MutableState<Map<String,MutableState<Boolean>>>){

    Row (modifier = Modifier.horizontalScroll(rememberScrollState())) {
        for (filter in filters.value){
            FilterChip(
                onClick = { filter.value.value = !filter.value.value },
                label = {
                    Text(filter.key)
                },
                selected =  filter.value.value,
                leadingIcon = if ( filter.value.value) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Done icon",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
            )
            Spacer(modifier = Modifier.width(5.dp))

        }
    }
}
fun getFilters (recipeNames: List<String>): Map<String,MutableState<Boolean>>{ //get common words to use as suggested filters
    val popularWords = mutableMapOf<String,MutableState<Boolean>>()
    val usedWordsCount = mutableMapOf<String,Int>()
    for (name in recipeNames){
        for (word in CreateAutomations.getWords(name)){
            if (usedWordsCount[word] == null){
                usedWordsCount[word] = 1
            }else {
                usedWordsCount[word] = usedWordsCount[word]!! + 1
            }
        }
    }

    for (word in usedWordsCount){
        if (word.key == "and") continue //this is not a useful filter
        if(word.value >=3){//todo settings for this value and filters atall
            popularWords[word.key] = mutableStateOf(false)
        }
    }


    return  popularWords
}
@Composable
private fun MainScreen(names: MutableState<List<String>>, thumbnails: MutableMap<String,Bitmap?>,getName : Boolean, updatedThumbnail : MutableState<Boolean>) {
    val textState = remember { mutableStateOf(TextFieldValue("")) }
    val filters = remember { mutableStateOf( getFilters(names.value))}
    Column (modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)){
        SearchView(textState)
        SearchFilters(filters)
        RecipeList(names,thumbnails,textState,filters,getName)
        if (updatedThumbnail.value){
            //this will update the thumbnails
            updatedThumbnail.value= false
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
        MainScreen((mutableStateOf(listOf("Carrot Cake", "other Cake"))), hashMapOf(),false, mutableStateOf(false))
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
@SuppressLint("UnrememberedMutableState")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun previewRecipeCards(){
    val textState = remember { mutableStateOf(TextFieldValue("")) }
    val filters = remember { mutableStateOf(mapOf(Pair("cake", mutableStateOf(false)),Pair("dffffffffffdf2", mutableStateOf(false)),Pair("fasdf", mutableStateOf(false)),Pair("asfdasdf", mutableStateOf(false))))}

    RezepteTheme {
        Surface {
            RecipeList(
                (mutableStateOf(listOf("Carrot Cake", "other Cake" ))),
                hashMapOf(),
                textState,
                filters,
                false
            )
        }
    }}