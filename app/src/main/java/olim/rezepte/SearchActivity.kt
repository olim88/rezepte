package olim.rezepte


import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import olim.rezepte.fileManagment.FileSync
import olim.rezepte.fileManagment.LocalFilesTask
import olim.rezepte.fileManagment.dropbox.DbTokenHandling
import olim.rezepte.fileManagment.dropbox.DownloadTask
import olim.rezepte.fileManagment.dropbox.DropboxClient
import olim.rezepte.recipeCreation.CreateActivity
import olim.rezepte.recipeCreation.CreateAutomations
import olim.rezepte.recipeMaking.MakeActivity
import olim.rezepte.recipeMaking.getColor
import olim.rezepte.ui.theme.RezepteTheme
import java.util.Random


class SearchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //see if there are extra data sent e.g. geting name of recipe not just opening it
        val extras = intent.extras
        var returnName = (extras != null && extras.getBoolean("get recipe name"))

        //get settings
        val settings = SettingsActivity.loadSettings(
            getSharedPreferences(
                "olim.rezepte.settings",
                MODE_PRIVATE
            )
        )

        //set access token
        val tokenHandler = DbTokenHandling(
            getSharedPreferences(
                "olim.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        )
        val isOnline = tokenHandler.isLoggedIn()

        val recipeNameData: MutableState<MutableList<String>> = mutableStateOf(mutableListOf())
        val extraData = mutableStateMapOf<String, BasicData>()
        val thumbnails = mutableMapOf<String, Bitmap?>()
        val hasThumbnails = mutableStateOf(false)


        //set the content of the window
        setContent {
            RezepteTheme {
                MainScreen(
                    recipeNameData,
                    extraData,
                    thumbnails,
                    returnName,
                    hasThumbnails,
                    settings
                )
            }
        }
        //get token
        val dropboxPreference =
            getSharedPreferences(
                "olim.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        var localList: String? = null
        //load the file data
        val loadLocalFileData = FileSync.Data(FileSync.FilePriority.LocalOnly, dropboxPreference)
        val file =
            FileSync.FileInfo("", "${this@SearchActivity.filesDir}", "listOfRecipes.xml")
        CoroutineScope(Dispatchers.IO).launch {
            if (settings["Local Saves.Cache recipe names"] == "true") {
                FileSync.downloadString(loadLocalFileData, file) {
                    localList = it
                }
            }

            if (localList != null) {
                recipeNameData.value = localList.replace(".xml", "").split("\n").toMutableList()


            }
            //add local only files to that list
            val localFiles = LocalFilesTask.listFolder("${this@SearchActivity.filesDir}/xml/")
            if (settings["Local Saves.Cache recipes"] == "true" && !localFiles.isNullOrEmpty()) {
                for (fileName in localFiles) {
                    if (!recipeNameData.value.contains(fileName.removeSuffix(".xml"))) {//if the list dose not contain the file name add it to the list
                        recipeNameData.value.add(fileName.removeSuffix(".xml"))
                    }
                }
            }
            //if there are locally saved thumbnails load them if data is not empty
            if (recipeNameData.value.isNotEmpty()) {
                if (settings["Local Saves.Cache recipe image"] == "thumbnail" || settings["Local Saves.Cache recipe image"] == "full sized" || !isOnline) {
                    val thumbnailFiles = FileSync.FileBatchInfo(
                        R.string.recipe_thumbnail_content_description.toString(),
                        "${this@SearchActivity.filesDir}/thumbnail/",
                        recipeNameData.value
                    )
                    FileSync.downloadThumbnail(loadLocalFileData, thumbnailFiles) {
                        thumbnails.putAll(it)
                        hasThumbnails.value = true
                    }
                }
            }

            //load  extra data
            val priority =
                if (settings["Local Saves.Cache recipes"] == "true") FileSync.FilePriority.OnlineFirst else FileSync.FilePriority.OnlineOnly
            val searchDataData = FileSync.Data(priority, dropboxPreference)
            val searchDataFile =
                FileSync.FileInfo("/", "${this@SearchActivity.filesDir}/", "searchData.xml")
            FileSync.downloadString(searchDataData, searchDataFile) {
                val searchData = XmlExtraction.getSearchData(it)
                //convert to dictionary
                for (recipeData in searchData.data) {
                    extraData[recipeData.name] = recipeData
                }
            }
        }

        //if online supplement the recipe names list with a list of the online file names
        if (isOnline) {
            CoroutineScope(Dispatchers.IO).launch {
                val downloader = DownloadTask(DropboxClient.getClient(tokenHandler.retrieveAccessToken()))
                //get data
                val list = downloader.listDir("/xml/") ?: listOf()
                if (list.isNotEmpty()) {//if can get to dropbox
                    val onlineList = list.toMutableList()
                    //clean data
                    val iterate = onlineList.listIterator()
                    while (iterate.hasNext()) {
                        val oldValue = iterate.next()
                        iterate.set(oldValue.removeSuffix(".xml"))
                    }
                    if (localList == null || onlineList != localList.replace(".xml", "")
                            .split("\n")
                    ) { //if the lists are different use the online version and save to to local if enabled
                        recipeNameData.value = onlineList
                        if (settings["Local Saves.Cache recipe names"] == "true") {
                            FileSync.uploadString(
                                loadLocalFileData,
                                file,
                                onlineList.joinToString("\n")
                            ) {}
                        }
                    }
                }

                //load thumbnails when file names are finalised
                val priority =
                    if (settings["Local Saves.Cache recipe image"] == "thumbnail" || settings["Local Saves.Cache recipe image"] == "full sized") FileSync.FilePriority.Newest else FileSync.FilePriority.OnlineOnly
                val thumbNailsData = FileSync.Data(priority, dropboxPreference)
                val thumbnailFiles = FileSync.FileBatchInfo(
                    "/image/",
                    "${this@SearchActivity.filesDir}/thumbnail/",
                    recipeNameData.value
                )
                FileSync.downloadThumbnail(thumbNailsData, thumbnailFiles) {
                    for (thumbnailKey in recipeNameData.value) {
                        if (thumbnails.contains(thumbnailKey) && thumbnails[thumbnailKey] != null) {
                            if (!thumbnails[thumbnailKey]!!.sameAs(it[thumbnailKey])) {
                                thumbnails[thumbnailKey] = it[thumbnailKey]
                                hasThumbnails.value = true
                            }
                        } else if (it[thumbnailKey] != null) {
                            thumbnails[thumbnailKey] = it[thumbnailKey]
                            hasThumbnails.value = true
                        }
                    }

                }
                val thumbNailSyncData = FileSync.Data(
                    FileSync.FilePriority.Newest,
                    dropboxPreference
                ) //sync the thumbnails to device
                FileSync.syncThumbnail(thumbNailSyncData, thumbnailFiles) {}

            }

        }


    }

}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RecipeCard(name: String, extraData: BasicData?, thumbNail: Bitmap?, getName: Boolean) {

    // Fetching the Local Context
    val mContext = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 15.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .requiredHeightIn(60.dp)
            .clickable { isExpanded = !isExpanded }
            .animateContentSize()
            .padding(1.dp),

        ) {
        //image section
        Row {
            if (thumbNail != null) {
                val brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.4f to Color.White,
                        1f to Color.Transparent
                    )
                )
                AnimatedContent(
                    targetState = isExpanded,
                    transitionSpec = {
                        slideInHorizontally().with(
                            slideOutHorizontally()
                        )
                    },
                    label = "image transition",
                ) { targetState ->
                    when (targetState) {
                        true -> {
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


                                    .graphicsLayer { alpha = 0.99f }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(brush, blendMode = BlendMode.DstIn)
                                    },
                                contentScale = ContentScale.Crop,
                            )

                        }

                        false -> {
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
                                    .align(Alignment.CenterVertically)
                                    .animateEnterExit(
                                        slideInHorizontally(initialOffsetX = { w -> -w }),
                                        slideOutHorizontally(targetOffsetX = { w -> -w })
                                    )
                                    .graphicsLayer { alpha = 0.99f },
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }


            }
        }

        //text section
        Row(modifier = Modifier.height(if (isExpanded) 128.dp else 60.dp)) {
            if (thumbNail != null) {
                AnimatedContent(
                    targetState = isExpanded,
                    transitionSpec = {
                        slideInHorizontally().with(
                            slideOutHorizontally()
                        )
                    },
                    label = "image transition",
                ) { targetState ->
                    when (targetState) {
                        true -> {
                            Spacer(
                                Modifier.width(120.dp)
                            )
                        }

                        false -> {
                            Spacer(
                                Modifier.width(60.dp)
                            )

                        }
                    }
                }

            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(63.dp)
                    .weight(1f)
            ) {
                //recipe name
                Text(
                    name,
                    modifier = Modifier.padding(all = 1.dp),
                    style = MaterialTheme.typography.titleMedium,
                    softWrap = true,
                )
                if (extraData != null) {
                    Text(
                        stringResource(id = R.string.recipe_author_label, extraData.author),
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = true,
                    )
                    Text(
                        stringResource(id = R.string.recipe_servings_label, extraData.servings),
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        softWrap = true,
                    )
                }

            }

            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    slideInHorizontally().with(
                        slideOutHorizontally()
                    )
                },
                label = "image transition",
            ) { targetState ->
                when (targetState) {
                    true -> {
                        Spacer(
                            Modifier.width(110.dp)
                        )
                    }

                    false -> {

                    }
                }
            }

        }

        //button section
        Row {
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = isExpanded,
                enter = scaleIn()
                        + fadeIn(
                    // Fade in with the initial alpha of 0.3f.
                    initialAlpha = 0.3f
                ),
                exit = scaleOut() + fadeOut()
            ) {
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
                        if (getName) stringResource(id = R.string.recipe_action_link) else stringResource(id = R.string.recipe_action_make),
                        modifier = Modifier.padding(all = 2.dp),
                        style = MaterialTheme.typography.titleLarge
                    )


                }
            }

        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeList(
    names: MutableState<MutableList<String>>,
    extraData: SnapshotStateMap<String, BasicData>,
    thumbnails: MutableMap<String, Bitmap?>,
    searchFieldState: MutableState<TextFieldValue>,
    getName: Boolean,
    settings: Map<String, String>
) {
    var filteredNames: List<String>
    val filters = getFilters(names.value, extraData.values)
    val currentFilters = remember { mutableStateListOf<String>() }

    if (settings["Search menu.Search Menu List"] == "true") {
        //animated the suggesting in and out when they are in sue
        val lazyListState = rememberLazyListState()
        val firstItemTranslationY by remember {
            derivedStateOf {
                when {
                    lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty() && lazyListState.firstVisibleItemIndex == 0 -> lazyListState.firstVisibleItemScrollOffset * .6f
                    else -> 0f
                }
            }
        }
        val visibility by remember {
            derivedStateOf {
                when {
                    lazyListState.layoutInfo.visibleItemsInfo.isNotEmpty() && lazyListState.firstVisibleItemIndex == 0 -> {
                        val imageSize = lazyListState.layoutInfo.visibleItemsInfo[0].size
                        val scrollOffset = lazyListState.firstVisibleItemScrollOffset

                        scrollOffset / imageSize.toFloat()
                    }

                    else -> 1f
                }
            }
        }

        LazyColumn(state = lazyListState) {
            //add the filterer at the top
            if (settings["Search menu.Suggestion Filters"] == "true") {
                item {
                    SearchFilters(filters, firstItemTranslationY, visibility, settings) {
                        currentFilters.clear()
                        currentFilters.addAll(it)
                    }

                }
            }

            filteredNames =
                filterItems(
                    names.value,
                    extraData,
                    currentFilters,
                    searchFieldState.value.text,
                    settings
                )

            items(filteredNames) { name ->

                RecipeCard(name, extraData[name], thumbnails[name], getName)

            }
        }
    } else {

        //animated the suggesting in and out when they are in sue
        val lazyGridState = rememberLazyStaggeredGridState()
        val firstItemTranslationY by remember {
            derivedStateOf {
                when {
                    lazyGridState.layoutInfo.visibleItemsInfo.isNotEmpty() && lazyGridState.firstVisibleItemIndex == 0 -> lazyGridState.firstVisibleItemScrollOffset * .6f
                    else -> 0f
                }
            }
        }
        val visibility by remember {
            derivedStateOf {
                when {
                    lazyGridState.layoutInfo.visibleItemsInfo.isNotEmpty() && lazyGridState.firstVisibleItemIndex == 0 -> {
                        val imageSize = lazyGridState.layoutInfo.visibleItemsInfo[0].size
                        val scrollOffset = lazyGridState.firstVisibleItemScrollOffset

                        scrollOffset / imageSize.height.toFloat()
                    }

                    else -> 1f
                }
            }
        }
        var selectedName by remember { mutableStateOf("") }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(128.dp),
            state = lazyGridState
        ) {
            //add spacer for filters at the top if its enabled
            if (settings["Search menu.Suggestion Filters"] == "true") {
                item(span = StaggeredGridItemSpan.FullLine) {
                    SearchFilters(filters, firstItemTranslationY, visibility, settings) {
                        currentFilters.clear()
                        currentFilters.addAll(it)
                    }
                }
            }
            filteredNames =
                filterItems(
                    names.value,
                    extraData,
                    currentFilters,
                    searchFieldState.value.text,
                    settings
                )
            items(filteredNames) { name ->

                RecipeTile(
                    name,
                    getColor(
                        index = filteredNames.indexOf(name),
                        default = MaterialTheme.colorScheme.primary
                    ),
                    thumbnails[name]
                ) {
                    selectedName = name
                }
            }
        }
        //when a recipe is selected show an expanded bottom sheet with extra detail and the button to link or make the recipe
        RecipeExpanded(selectedName, extraData[selectedName], thumbnails[selectedName], getName) {
            selectedName = ""
        }
    }
}

fun filterItems(
    names: List<String>, authors: Map<String, BasicData>,
    filters: SnapshotStateList<String>, searchFilter: String,
    settings: Map<String, String>
): List<String> {
    val resultList = mutableListOf<String>()
    //check all of the recipe names to see which one fits the filters
    for (name in names) {
        //combine the name and author so both can be searched at once if the author is not null
        val searchedValue = "$name ${if (authors[name] != null) authors[name]?.author else ""}"
        //if the name (or author) contains what is in the search box
        if (searchedValue.contains(searchFilter, ignoreCase = true)) {
            //see if it also fits the enabled search filter buttons
            var matches = filters.isEmpty() || settings["Search menu.Filters behavior"] != "or"
            for (filter in filters) {
                //if there is a filter enabled see if the filter word is in the name (or author)
                // if settings is "and" / "single" remove item if dose not match
                // if setting is "or" only remove it it there are no matches
                if (!searchedValue.contains(
                        filter,
                        ignoreCase = true
                    ) && (settings["Search menu.Filters behavior"] == "and" || settings["Search menu.Filters behavior"] == "single")
                ) {
                    matches = false
                    break
                } else if (searchedValue.contains(
                        filter,
                        ignoreCase = true
                    ) && settings["Search menu.Filters behavior"] == "or"
                ) {
                    matches = true
                    break
                }
            }
            //if still matches the extra filters add it to the results
            if (matches) {
                resultList.add(name)
            }
        }
    }
    return resultList
}


@Composable
fun RecipeTile(name: String, color: Color, thumbNail: Bitmap?, onSelect: () -> Unit) {
    // Fetching the Local Context

    val random = Random()
    var isExpanded by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 15.dp,
        color = color,
        modifier = Modifier
            .padding(all = 5.dp)
            .requiredWidthIn(128.dp)
            .requiredHeightIn(64.dp)
            .clickable {
                isExpanded = !isExpanded
                onSelect()
            }
            .animateContentSize()
            .padding(1.dp),

        ) {
        Column {
            //image section

            if (thumbNail != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbNail)
                        .build(),
                    contentDescription = "Contact profile picture",
                    modifier = Modifier
                        // Set image size to 50 dp
                        .size(128.dp)
                        // Clip image to be shaped as a circle
                        .clip(RoundedCornerShape(5.dp)),


                    contentScale = ContentScale.Crop,
                )
            }
            //recipe name
            Text(
                name,
                modifier = Modifier.padding(all = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                softWrap = true,
            )
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeExpanded(
    name: String,
    basicData: BasicData?,
    thumbNail: Bitmap?,
    getName: Boolean,
    onSelect: () -> Unit
) {
    // Fetching the Local Context
    val mContext = LocalContext.current
    if (name != "") {
        ModalBottomSheet(onDismissRequest = {
            onSelect()
        }) {
            Row {
                if (thumbNail != null) {
                    val brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.4f to Color.White,
                            1f to Color.Transparent
                        )
                    )
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


                            .graphicsLayer { alpha = 0.99f }
                            .drawWithContent {
                                drawContent()
                                drawRect(brush, blendMode = BlendMode.DstIn)

                            },
                        contentScale = ContentScale.Crop,
                    )
                }
                Column {
                    Text(
                        name,
                        modifier = Modifier
                            .padding(all = 2.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Left,
                        softWrap = true,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (basicData != null) {
                        Text(
                            text = "Author: ${basicData.author}",
                            modifier = Modifier.padding(5.dp)
                        )
                        Text(
                            text = "Servings: ${basicData.servings}",
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
            }
            Button(
                onClick = {
                    onSelect()
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
                    .padding(top = 15.dp)
                    .padding(5.dp)
                    .fillMaxWidth()

            ) {
                Text(
                    if (getName) "Link" else "Make",
                    modifier = Modifier.padding(all = 2.dp),
                    style = MaterialTheme.typography.titleLarge
                )


            }
            Spacer(Modifier.weight(1f))
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
        textStyle = TextStyle(fontSize = 18.sp),
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = stringResource(id = R.string.search_icon_content_description),
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
                        contentDescription = stringResource(id = R.string.clear_search_icon_content_description),
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
fun SearchFilters(
    filters: Map<String, MutableState<Boolean>>,
    firstItemTranslationY: Float,
    visibility: Float,
    settings: Map<String, String>,
    updateSelectedFilters: (List<String>) -> Unit,
) {
    val currentFilters by remember { mutableStateOf(filters ) }

    Row(modifier = Modifier
        .horizontalScroll(rememberScrollState())
        .graphicsLayer {
            translationY = firstItemTranslationY
            alpha = 1f - visibility
        }) {
        for (filter in currentFilters) {
            FilterChip(
                onClick = {
                    //if single setting enabled disable all active filers
                    if (settings["Search menu.Filters behavior"] == "single") {
                        val state = filter.value.value
                        currentFilters.forEach { newState -> newState.value.value = false }
                        filter.value.value = !state
                    } else {
                        filter.value.value = !filter.value.value
                    }


                    //get current filters that are set and update that value
                    val active = currentFilters.filter { state -> state.value.value }
                    updateSelectedFilters(active.keys.toList())
                },
                label = {
                    Text(filter.key)
                },
                selected = filter.value.value,
                leadingIcon = if (filter.value.value) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = stringResource(id = R.string.filter_selected_icon_description),
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.background)


            )
            Spacer(modifier = Modifier.width(5.dp))

        }
    }
}

fun getFilters(
    recipeNames: List<String>,
    authors: MutableCollection<BasicData>
): Map<String, MutableState<Boolean>> { //get common words to use as suggested filters
    val popularWords = mutableMapOf<String, MutableState<Boolean>>()
    val usedWordsCount = mutableMapOf<String, Int>()
    for (name in recipeNames) {
        for (word in CreateAutomations.getWords(name)) {
            if (usedWordsCount[word] == null) {
                usedWordsCount[word] = 1
            } else {
                usedWordsCount[word] = usedWordsCount[word]!! + 1
            }
        }
    }
    for (author in authors) {
        if (author.author == "") continue // do do this for empty authors
        if (usedWordsCount[author.author] == null) {
            usedWordsCount[author.author] = 1
        } else {
            usedWordsCount[author.author] = usedWordsCount[author.author]!! + 1
        }
    }

    for (word in usedWordsCount) {
        if (word.key == "and" || word.key == "with") continue //this is not a useful filter
        if (word.value >= 3) {//todo settings for this value and filters at all
            popularWords[word.key] = mutableStateOf(false)
        }
    }

    return popularWords
}

@Composable
private fun MainScreen(
    names: MutableState<MutableList<String>>,
    extraData: SnapshotStateMap<String, BasicData>, thumbnails: MutableMap<String, Bitmap?>,
    getName: Boolean, updatedThumbnail: MutableState<Boolean>,
    settings: Map<String, String>
) {
    val textState = remember { mutableStateOf(TextFieldValue("")) }
    if (names.value.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SearchView(textState)

            RecipeList(names, extraData, thumbnails, textState, getName, settings)
            if (updatedThumbnail.value) {
                //this will update the thumbnails
                updatedThumbnail.value = false
            }
        }
    } else {//if there are not recipes loaded show message say none found and to login or create with buttons
        NoReciepsFoundOuput()
    }
}

@Composable
fun NoReciepsFoundOuput() {
    // Fetching the Local Context
    val mContext = LocalContext.current
    val tokenHandler = DbTokenHandling(
        mContext.getSharedPreferences(
            "olim.rezepte.dropboxintegration",
            AppCompatActivity.MODE_PRIVATE
        )
    )
    val isOnline = tokenHandler.isLoggedIn()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(30.dp)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                text = stringResource(id = R.string.no_recipes_found_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)

            )
            Spacer(modifier = Modifier.width(40.dp))

            Text(
                text =stringResource(id = R.string.no_recipes_prompt_create),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)

            )
            Button(
                onClick = {
                    val intent = Intent(mContext, CreateActivity::class.java)
                    mContext.startActivity(intent)
                }, modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.create_button_text),
                    textAlign = TextAlign.Center,
                )
            }

            if (!isOnline) {//only suggest login if the user is not logged in
                Text(
                    text = stringResource(id = R.string.no_recipes_prompt_login),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Button(
                    onClick = {
                        val intent = Intent(mContext, LoginActivity::class.java)
                        mContext.startActivity(intent)
                    }, modifier = Modifier
                        .padding(15.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.dropbox_login_button),
                        textAlign = TextAlign.Center,
                    )
                }
            }
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
        MainScreen(
            (mutableStateOf(mutableListOf("Carrot Cake", "other Cake"))),
            mutableStateMapOf(), hashMapOf(), false, mutableStateOf(false), mapOf()
        )
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
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)

@Composable
fun previewRecipeCard() {
    RezepteTheme {
        Surface {
            RecipeCard(name = "Carrot Cake", null, null, false)

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
fun previewRecipeCards() {
    val textState = remember { mutableStateOf(TextFieldValue("")) }
    val filters = remember {
        mutableStateOf(
            mapOf(
                Pair("cake", mutableStateOf(false)),
                Pair("dffffffffffdf2", mutableStateOf(false)),
                Pair("fasdf", mutableStateOf(false)),
                Pair("asfdasdf", mutableStateOf(false))
            )
        )
    }

    RezepteTheme {
        Surface {
            RecipeList(
                (mutableStateOf(mutableListOf("Carrot Cake", "other Cake"))),
                mutableStateMapOf(),
                hashMapOf(),
                textState,
                false,
                mapOf()
            )
        }
    }
}