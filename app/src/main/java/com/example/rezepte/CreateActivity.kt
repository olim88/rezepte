package com.example.rezepte


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rezepte.ui.theme.RezepteTheme
import com.google.android.material.internal.ContextUtils.getActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.redundent.kotlin.xml.xml


class CreateActivity : ComponentActivity() {
    private var loadedRecipeName: String? = null


    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadedRecipeName = intent.extras?.getString("recipe name")
        val recipe = intent.extras?.getString("data") //if a recipe has been sent as a xml to load
        val recipeLinkedImage = intent.extras?.getString("imageData")
        val image: MutableState<Bitmap?> = mutableStateOf(null)

        //get the users settings
        val settings = SettingsActivity.loadSettings(
            getSharedPreferences(
                "com.example.rezepte.settings",
                MODE_PRIVATE
            )
        )

        //check if there is preloaded recipe then name then just load empty recipe if neither is true
        if ( recipe != null){
            val extractedData = XmlExtraction.getData(recipe)
            setContent {
                RezepteTheme {
                    MainScreen(
                        settings,
                        recipeDataInput = mutableStateOf(extractedData),
                        image,
                        { deleteRecipe() },
                        { recipe, uri, bitmap,linking -> finishRecipe(recipe, uri,bitmap,linking) })
                }
            }
            //if there is a link to an image download it to bitmap to add to the recipe
            if (recipeLinkedImage != null){
                CoroutineScope(Dispatchers.IO).launch {
                    image.value = DownloadWebsite.downloadImageToBitmap(recipeLinkedImage)
                }
            }
        }
        else if (loadedRecipeName != null) {
            //get token
            val dropboxPreference =
                getSharedPreferences(
                    "com.example.rezepte.dropboxintegration",
                    MODE_PRIVATE
                )

            //create variable for recipe data
            val extractedData : MutableState<Recipe> = mutableStateOf(GetEmptyRecipe())

            //set the context to show window
            setContent {
                RezepteTheme {
                    MainScreen(
                        settings,
                        recipeDataInput = extractedData,
                        image,
                        { deleteRecipe() },
                        { recipe, uri, bitmap,linking -> finishRecipe(recipe, uri,bitmap,linking) })
                }
            }
            CoroutineScope(Dispatchers.IO).launch {
                //load the file data
                val priority = if (settings["Local Saves.Cache recipes"] == "true") FileSync.FilePriority.OnlineFirst else FileSync.FilePriority.OnlineOnly
                val imagePriority = if (settings["Local Saves.Cache recipe image"]== "full sized") FileSync.FilePriority.Newist else FileSync.FilePriority.OnlineOnly
                val data = FileSync.Data(priority, dropboxPreference)
                val imageData = FileSync.Data(imagePriority, dropboxPreference)
                val file =
                    FileSync.FileInfo("/xml/", "${this@CreateActivity.filesDir}/xml/", "$loadedRecipeName.xml")
                CoroutineScope(Dispatchers.IO).launch {
                    FileSync.downloadString(data, file) {
                        extractedData.value = XmlExtraction.getData(it)

                    }
                }

                val imageFile =
                    FileSync.FileInfo("/image/", "${this@CreateActivity.filesDir}/image/", "$loadedRecipeName.jpg")
                FileSync.downloadImage(imageData, imageFile) {
                    image.value = it

                }
                //sync files

                FileSync.syncFile(data, file) {}
                FileSync.syncFile(imageData, imageFile) {}

                withContext(Dispatchers.Main) {
                    if (extractedData.value == GetEmptyRecipe()){ //if no data has been loaded show error and close window
                        Toast.makeText(this@CreateActivity, "Recipe doesn't exist", Toast.LENGTH_SHORT)
                            .show()
                        this@CreateActivity.finish()
                    }
                }
            }

        } else {
            setContent {
                RezepteTheme {
                    MainScreen(
                        settings,
                        recipeDataInput = mutableStateOf(GetEmptyRecipe()),
                        image,
                        { deleteRecipe() },
                        { recipe, uri, bitmap,linking -> finishRecipe(recipe, uri,bitmap,linking) })
                }
            }
        }


    }

    private fun deleteRecipe() {
        //comfirm delete

        if (loadedRecipeName != null && !intent.extras!!.getBoolean("creating")) //only need to delete files if it was save before
        {
            AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Do you really want to delete this recipe?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes,
                    DialogInterface.OnClickListener { dialog, whichButton ->
                        Toast.makeText(
                            this@CreateActivity,
                            "deleted recipe",
                            Toast.LENGTH_SHORT
                        ).show()
                        //delete the recipe
                        //remove files
                        deleteRecipeData()

                        //go home
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                    })

                .setNegativeButton(android.R.string.no, null).show()
        } else //otherwise just return home
        {
            //go home
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun deleteRecipeData(){
        val settings = SettingsActivity.loadSettings(getSharedPreferences(
            "com.example.rezepte.settings",
            MODE_PRIVATE
        ))
        //get token
        val dropboxPreference =
            getSharedPreferences(
                "com.example.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        CoroutineScope(Dispatchers.IO).launch {
            //load the file data
            val data = FileSync.Data(FileSync.FilePriority.None, dropboxPreference)
            val file =
                FileSync.FileInfo(
                    "/xml/",
                    "${this@CreateActivity.filesDir}/xml/",
                    "$loadedRecipeName.xml"
                )
            val imageFile =
                FileSync.FileInfo(
                    "/image/",
                    "${this@CreateActivity.filesDir}/image/",
                    "$loadedRecipeName.jpg"
                )
            FileSync.deleteFile(data, file) {}
            FileSync.deleteFile(data, imageFile) {}
            val thumbnailData =
                FileSync.Data(FileSync.FilePriority.LocalOnly, dropboxPreference)
            val thumbnailFile =
                FileSync.FileInfo(
                    "",
                    "${this@CreateActivity.filesDir}/thumbnail/",
                    "$loadedRecipeName.jpg"
                )
            FileSync.deleteFile(thumbnailData, thumbnailFile) {}

            //update search data to remove recipe
            val priority =
                if (DbTokenHandling(dropboxPreference).isLoggedIn()) FileSync.FilePriority.OnlineOnly else FileSync.FilePriority.LocalOnly
            val searchDataData = FileSync.Data(priority, dropboxPreference)
            val searchDataFile = FileSync.FileInfo(
                "/",
                "${this@CreateActivity.filesDir}/",
                "searchData.xml"
            )
            var searchData: SearchData? = null
            val searchDataExists =
                FileSync.downloadString(searchDataData, searchDataFile) {
                    searchData = XmlExtraction.getSearchData(it)
                }
            if (!searchDataExists || searchData == null) {
                searchData = getEmptySeachData()
            }
            //remove name
            if (loadedRecipeName != null) {
                for (index in 0 until searchData!!.data.size) {
                    if (searchData!!.data[index].name == loadedRecipeName) {
                        searchData!!.data.removeAt(index)
                        break
                    }
                }
            }
            val uploadPriority = if (settings["Local Saves.Cache recipe names"] == "true") FileSync.FilePriority.None else FileSync.FilePriority.OnlineOnly
            val uploadData = FileSync.Data(uploadPriority, dropboxPreference)
            FileSync.uploadString(
                uploadData,
                searchDataFile,
                parseSearchData(searchData!!)
            ) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun finishRecipe(recipe: Recipe, image: Uri?, bitmapImage : Bitmap?, linking : Boolean) {
        val settings = SettingsActivity.loadSettings(getSharedPreferences(
            "com.example.rezepte.settings",
            MODE_PRIVATE
        ))
        //make sure there is a name for the recipe else don't ext
        if (recipe.data.name == ""){
            Handler(Looper.getMainLooper()).post { //show why can not be saved
                Toast.makeText(
                    this,
                    "Invalid Name",
                    Toast.LENGTH_SHORT
                )
                    .show()

            }
            return
        }
        //convert saved recipe to xml
        val xmlData: String = parseData(recipe)

        //get image if one is set
        var uriBitmap: Bitmap? = null
        if (image != null) {
            uriBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.contentResolver,image))
        }
        //get the name of the recipe to save
        val name = recipe.data.name

        //get token
        val dropboxPreference =
            getSharedPreferences(
                "com.example.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        CoroutineScope(Dispatchers.IO).launch {
            //upload the file data
            val priorityXml = if (settings["Local Saves.Cache recipe names"] == "true" ) FileSync.FilePriority.None else FileSync.FilePriority.OnlineOnly
            val priorityImage = if (settings["Local Saves.Cache recipe image"]== "full sized"  || settings["Local Saves.Cache recipe image"]== "thumbnail") FileSync.FilePriority.None else FileSync.FilePriority.OnlineOnly
            val dataXml = FileSync.Data(priorityXml, dropboxPreference)
            val dataImage = FileSync.Data(priorityImage, dropboxPreference)
            val xmlSaveFile =
                FileSync.FileInfo(
                    "/xml/",
                    "${this@CreateActivity.filesDir}/xml/",
                    "$name.xml"
                )
            val imageSaveFile =
                FileSync.FileInfo(
                    "/image/",
                    "${this@CreateActivity.filesDir}/image/",
                    "$name.jpg"
                )
            FileSync.uploadString(dataXml, xmlSaveFile, xmlData) {}
            if (uriBitmap != null) {
                FileSync.uploadImage(dataImage, imageSaveFile, uriBitmap) {}
            } else if (bitmapImage != null) {
                FileSync.uploadImage(dataImage, imageSaveFile, bitmapImage) {}
            } else {
                FileSync.deleteFile(dataImage, imageSaveFile) {}



            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            //update search data to include recipe

            val priority =
                if (DbTokenHandling(dropboxPreference).isLoggedIn()) FileSync.FilePriority.OnlineOnly else FileSync.FilePriority.LocalOnly
            val searchDataData = FileSync.Data(priority, dropboxPreference)
            val searchDataFile = FileSync.FileInfo(
                "/",
                "${this@CreateActivity.filesDir}/",
                "searchData.xml"
            )
            var searchData: SearchData? = null
            val searchDataExists =
                FileSync.downloadString(searchDataData, searchDataFile) {
                    searchData = XmlExtraction.getSearchData(it)
                }
            if (!searchDataExists || searchData == null) {
                searchData = getEmptySeachData()
            }
            //remove name
            if (loadedRecipeName != null) {
                for (index in 0 until searchData!!.data.size) {
                    if (searchData!!.data[index].name == name) {
                        searchData!!.data.removeAt(index)
                        break
                    }
                }
            }
            //add new recipe
            searchData!!.data.add(BasicData(name,recipe.data.serves,recipe.data.author))
            val prioritySave = if (settings["Local Saves.Cache recipe names"] == "true" ) FileSync.FilePriority.None else FileSync.FilePriority.OnlineOnly
            val searchDataUpload = FileSync.Data(prioritySave, dropboxPreference)
            FileSync.uploadString(
                searchDataUpload,
                searchDataFile,
                parseSearchData(searchData!!)
            ) {}

            //if the name has changed delete old files
            if (name != loadedRecipeName && loadedRecipeName != null) {
                deleteRecipeData()
            }
        }
        //tell user that the recipe is saved
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                this@CreateActivity,
                "Saved Recipe",
                Toast.LENGTH_SHORT
            )
                .show()

        }

        //if linking move to the link page else go home
        if (linking){
            val intent = Intent(this, LinkStepsToInstructionsActivity::class.java)
            intent.putExtra("data",xmlData)
            startActivity(intent)
        }
        else{
            //move to home
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }




    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

    }


}
fun parseSearchData(searchData: SearchData): String{
    val recipeXml = xml("searchData") {
        "list"{
            for(data in searchData.data){
                "entry"{
                    "name"{
                        -data.name
                    }
                    "servings"{
                        -data.servings
                    }
                    "author"{
                        -data.author
                    }
                }
            }
        }
    }
    return recipeXml.toString(true)
}

    fun parseData(recipe : Recipe): String    {
    //get info from layout

    val recipeXml = xml("recipe") {
        "data" {
            "name" {
                - recipe.data.name
            }
            "author" {
                - recipe.data.author
            }
            "servings" {
                - recipe.data.serves
            }
            "cookingSteps" {
                "list" {

                    for ( step in recipe.data.cookingSteps.list){
                        "entry" {
                            attribute("index", step.index)
                            "time"{
                                - step.time
                            }
                            "cookingStage"{
                                - step.type.toString()
                            }
                            if( step.container != null){
                                "cookingStepContainer"{
                                    "type"{
                                        - step.container!!.type.toString()
                                    }
                                    if ( step.container!!.dimensionOne != null){
                                        "tinSize"{
                                            - step.container!!.dimensionOne.toString()
                                        }
                                    }
                                    if ( step.container!!.dimensionTwo != null){
                                        "tinSizeTwo"{
                                            - step.container!!.dimensionTwo.toString()
                                        }
                                    }
                                    if ( step.container!!.volume != null){
                                        "tinVolume"{
                                            - step.container!!.volume.toString()
                                        }
                                    }

                                }
                            }
                            if (step.cookingTemperature != null){
                                "cookingStepTemperature"{
                                    if(step.cookingTemperature!!.temperature != null){
                                        "temperature"{
                                            - step.cookingTemperature!!.temperature.toString()
                                        }
                                    }
                                    "hobTemperature"{
                                        - step.cookingTemperature!!.hobTemperature.toString()
                                    }
                                    if(step.cookingTemperature!!.isFan != null){
                                        "isFan"{
                                            - step.cookingTemperature!!.isFan.toString()
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
            if ( recipe.data.website != null){
                "website"{
                    - recipe.data.website!!
                }
            }
            if ( recipe.data.notes != null){
                "notes"{
                    - recipe.data.notes!!
                }
            }
            if( recipe.data.linked != null){
                "linkedRecipes"{
                    "list" {
                        for (linkedRecipe in recipe.data.linked!!.list) {
                            "entry" {
                                "value" {
                                    -linkedRecipe.name
                                }
                            }
                        }
                    }

                }
            }
        }
        "ingredients" {
            "list" {
                for (ingredient in recipe.ingredients.list) {
                    "entry" {
                        attribute("index", ingredient.index)
                        "value" {
                            -ingredient.text
                        }
                    }

                }
            }
        }
        "instructions" {
            "list" {
                for (instruction in recipe.instructions.list) {
                    "entry" {
                        attribute("index", instruction.index)
                        "value" {
                            -instruction.text
                        }
                        if (instruction.linkedCookingStepIndex != null){
                            "linkedStep"{
                                -instruction.linkedCookingStepIndex.toString()!!
                            }
                        }

                    }
                }
            }
        }


    }

    return recipeXml.toString(true)


}








@Composable
fun TitleInput(data : MutableState<Recipe>){
    var name by remember { mutableStateOf(data.value.data.name)}
    TextField(
        value = name,
        onValueChange = { value ->
            name = value
            data.value.data.name = name.trim() //update name
        },
        modifier = Modifier
            .fillMaxWidth(),
        textStyle = TextStyle( fontSize = 18.sp),
        singleLine = true,
        shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
        label = { Text("Name") }

    )
}
@Composable
fun ImageInput( image : MutableState<Uri?>, savedBitmap: MutableState<Bitmap?>){
    //get a local image]
    var showImageSelectionMenu by remember {mutableStateOf(false)}

    val model = if (savedBitmap.value == null || image.value != null) (ImageRequest.Builder(LocalContext.current)
        .data(image.value)
        .build())
        else (ImageRequest.Builder(LocalContext.current)
        .data(savedBitmap.value)
        .build())
    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 15.dp,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()
            .padding(1.dp),

        ) {

        AsyncImage(
            model = model,
            contentDescription = "",
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .animateContentSize()
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    (RoundedCornerShape(5.dp))
                ),
            contentScale = ContentScale.FillWidth
        )

        Row{
            if (image.value == null || savedBitmap.value != null){
                Text (text = "Image", modifier = Modifier.align(Alignment.CenterVertically),   style = MaterialTheme.typography.titleLarge )
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            Button(onClick =
            {         //Show menu to add image
                showImageSelectionMenu = true
            }
            ,modifier = Modifier.padding(5.dp)
            ) {
                Icon(if (image.value == null || savedBitmap.value != null) Icons.Filled.Add else Icons.Filled.Edit, "contentDescription")
            }

        }
    }
    if (showImageSelectionMenu){
        AddImageDialog(descriptionText = "Take a picture or find save image for the recipe", onDismiss = { showImageSelectionMenu = false; image.value = null ; savedBitmap.value = null }){ uri: Uri? ->
            image.value = uri
            //set bitmap to nothing
            savedBitmap.value = null
            showImageSelectionMenu = false //re hide the menu when got image
        }
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInput(settings: Map<String, String>, data : MutableState<Recipe>, updatedSteps:MutableState<Boolean>){
    Surface(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()
            .padding(1.dp),

        ) {
        Column {
            TextField(
                value = data.value.data.author,
                onValueChange = { value ->
                    data.value = data.value.copy(data = data.value.data.copy(author = value)) //update name
                },
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = TextStyle( fontSize = 18.sp),
                singleLine = true,
                shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                label = { Text("Author") }
            )
            TextField(
                value = data.value.data.serves,
                onValueChange = { value ->
                    data.value = data.value.copy(data = data.value.data.copy(serves = value)) //update servings
                },
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = TextStyle( fontSize = 18.sp),
                singleLine = true,
                shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                label = { Text("Servings") }
            )
            CookingStepsInput(settings,data,updatedSteps)
            LinkedRecipesInput(data)
            TextField(
                value = if (data.value.data.website == null) "" else data.value.data.website!!,
                onValueChange = { value ->
                    if (value == ""){
                        data.value = data.value.copy(data = data.value.data.copy(website = null))
                    }
                    else{
                        data.value = data.value.copy(data = data.value.data.copy(website = value)) //update website
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = TextStyle( fontSize = 18.sp),
                singleLine = true,
                shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                label = { Text("Website") }
            )
        }
    }

}
@SuppressLint("RestrictedApi") //todo remove this idk another way to do it
@Composable
fun LinkedRecipesInput(data : MutableState<Recipe>){
    var linkedRecipes =data.value.data.linked?.list
    var isExpanded by remember { mutableStateOf(false)}
    var update by remember { mutableStateOf(false)}
    val icon = if (isExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown
    // Fetching the Local Context
    val mContext = LocalContext.current

    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
            if (!isExpanded) return@LaunchedEffect //if hidden do not try to add recipe
            val extras = getActivity(mContext)?.intent?.extras
            var name = ""
            var creating = false
            if (extras != null){
                name = if (extras.getString("recipe name") == null) "" else extras.getString("recipe name")!!
                creating = (extras.getBoolean("creating"))

            }
            if (creating && name != ""){ //if right name and its not blank add it to the list of linked recipes
                if (linkedRecipes == null){
                    linkedRecipes  = mutableListOf(LinkedRecipe(name))
                    data.value.data.linked = LinkedRecipes(linkedRecipes!!)
                    update = true
                }
                else if (!linkedRecipes!!.contains(LinkedRecipe(name))){
                    linkedRecipes?.add(LinkedRecipe(name))
                    data.value.data.linked?.list = linkedRecipes!!
                    update = true
                }
            }

        }
    }

    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 5.dp,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()


    ) {
        Column {
            Row {
                Text(
                    text = "Linked Recipes",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Icon(icon, "contentDescription",
                    Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(10.dp)
                        .size(24.dp))
            }
            if (isExpanded ) {
                Column {
                    if (update){
                        update = false
                    }
                    if (linkedRecipes != null) {

                        for ((index, _) in linkedRecipes!!.withIndex()) {
                            LinkedRecipe(data, index) { linkedIndex ->
                                linkedRecipes!!.removeAt(linkedIndex)
                                if ( linkedRecipes!!.isEmpty()){ //if there are none left set the value to null
                                    data.value.data.linked = null
                                }
                                update = true
                            }
                        }
                    }
                    //add new step linked recipe button
                    Button(
                        onClick = {
                            //open search to find recipe
                            val intent = Intent(mContext,SearchActivity::class.java)
                            intent.putExtra("get recipe name",true)
                            mContext.startActivity(intent)


                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
            }
        }

    }
}
@Composable
fun rememberLifecycleEvent(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current): Lifecycle.Event {
    var state by remember { mutableStateOf(Lifecycle.Event.ON_ANY) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            state = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return state
}
@Composable
fun LinkedRecipe(data : MutableState<Recipe>,index : Int,onItemClick: (Int) -> Unit){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(5.dp)
        ) {
        Row{
            Text(text = data.value.data.linked!!.list[index].name, modifier = Modifier.padding(5.dp).align(Alignment.CenterVertically), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
            Spacer(
                Modifier
                    .weight(1f)
            )
            Button(onClick = { onItemClick(index) }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "",
                    modifier = Modifier
                        .size(24.dp)
                )
            }
        }

    }
}
@SuppressLint("UnrememberedMutableState")
@Composable
fun CookingStepsInput(settings: Map<String, String>, data : MutableState<Recipe>, updatedSteps :MutableState<Boolean>){
    var steps = data.value.data.cookingSteps.list

    var isExpanded by remember { mutableStateOf(false)}
    val icon = if (isExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown
    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 5.dp,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()


        ) {
        Column {
            Row {
                Text(
                    text = "Cooking Steps",
                    style = MaterialTheme.typography.titleLarge,

                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Icon(icon, "contentDescription",
                    Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(10.dp)
                        .size(24.dp))
            }
            if (isExpanded) {
                if (updatedSteps.value ){}
                Column {

                    for (step in steps) {

                        CookingStep(settings,step,mutableStateOf(false)) { index ->
                            steps.removeAt(index)
                            steps.forEach { edit -> if (edit.index > index) edit.index -= 1 }
                            data.value.data.cookingSteps.list = steps

                            updatedSteps.value = true
                        }
                    }
                    //add new stage button
                    Button(
                        onClick = {
                            steps.add(
                                CookingStep(
                                    data.value.data.cookingSteps.list.count(),
                                    "",
                                    CookingStage.oven,
                                    null,
                                    CookingStepTemperature(
                                        0,
                                        HobOption.zero,
                                        false
                                    ) //starts as oven so need temp set up
                                )
                            )
                            data.value.data.cookingSteps.list = steps
                            updatedSteps.value = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "",
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                    //if empty display auto generate button
                    if (data.value.data.cookingSteps.list.isEmpty()) {
                        Button(
                            onClick = {
                                //get the value and then save that to the data
                                val output =
                                    CreateAutomations.autoGenerateStepsFromInstructions(data.value.instructions)
                                data.value.instructions = output.second
                                steps = CookingSteps(output.first.toMutableList()).list
                                data.value.data.cookingSteps.list = steps
                                updatedSteps.value = true

                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Auto Generate")
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingStep(settings: Map<String,String>,data : CookingStep, isExpanded : MutableState<Boolean>, onItemClick: (Int) -> Unit){

    var isFan by remember {  if (data.cookingTemperature?.isFan == null) mutableStateOf(false) else mutableStateOf(data.cookingTemperature?.isFan!!)}
    var timeInput by remember { mutableStateOf(data.time)}
    //get all the values and make sure the are in the correct units
    var dimensionOneValue by remember {
        if (data.container?.dimensionOne == null) {//if saved
            mutableStateOf("")
        }else {
            if (settings["Units.metric Lengths"]== "false") { //convert to imperial if needed
                mutableStateOf((data.container?.dimensionOne?.times(0.3937008)).toString())
            } else{
                mutableStateOf(data.container?.dimensionOne.toString())
            }
        }
    }
    var dimensionTwoValue by remember {
        if (data.container?.dimensionTwo == null) {//if saved
            mutableStateOf("")
        }else {
            if (settings["Units.metric Lengths"]== "false") { //convert to imperial if needed
                mutableStateOf((data.container?.dimensionTwo?.times(0.3937008)).toString())
            } else{
                mutableStateOf(data.container?.dimensionTwo.toString())
            }
        }
    }
    var dimensionVolume by remember {
        if (data.container?.volume == null) {//if saved
            mutableStateOf("")
        }else {
            if (settings["Units.metric Volume"]== "false") { //convert to imperial if needed
                mutableStateOf((data.container?.volume?.times(1.759754)).toString())
            } else{
                mutableStateOf(data.container?.volume.toString())
            }
        }
    }
    var cookingTemp by remember {
        if (data.cookingTemperature?.temperature == null) {//if saved
            mutableStateOf("")
        }else {
            if (settings["Units.Temperature"]== "false") { //convert to imperial if needed
                mutableStateOf(((data.cookingTemperature?.temperature?.times(9/5))?.plus(32)).toString())
            } else{
                mutableStateOf(data.cookingTemperature?.temperature.toString())
            }
        }
    }
    Card(modifier = Modifier
        .clickable { isExpanded.value = !isExpanded.value }
        .padding(3.dp)
        .animateContentSize()) {
        if (!isExpanded.value) {
            Row {
                Text(
                    text  = if (data.time == "")"${data.type}" else "${data.type} for ${data.time}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(7.dp)
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Button(onClick = { onItemClick(data.index) }, modifier = Modifier.fillMaxHeight()) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "",
                        modifier = Modifier
                            .size(24.dp)
                    )


                }


            }
        }
        else{
            Column (modifier = Modifier.padding(3.dp)) {
                MenuItemWithDropDown("Type",data.type.toString(),CookingStage.values()) { value ->
                    data.type = enumValueOf(value)
                    when (data.type) {
                        CookingStage.oven ->{
                            data.cookingTemperature = CookingStepTemperature(0, HobOption.zero, false)
                        }
                        CookingStage.hob -> {
                        data.cookingTemperature = CookingStepTemperature(null, HobOption.zero, null)
                        }
                        else -> {
                            data.cookingTemperature = null
                        }
                    }
                }
                //if it is oven or pan enable cooking temp options
                if (data.type == CookingStage.oven){
                    Row {
                        TextField(
                            value = cookingTemp,
                            onValueChange = { value ->
                                cookingTemp = value
                                if (settings["Units.Temperature"]== "true"){ //convert the saved value to C if the input is in F
                                    data.cookingTemperature?.temperature = value.toIntOrNull() //degrees C
                                } else {
                                    val temp = value.toIntOrNull()
                                    if (temp != null) {
                                        data.cookingTemperature?.temperature = ((5/9f) * (temp-32)).toInt() //degrees F
                                    } else {
                                        data.cookingTemperature?.temperature = null
                                    }
                                }

                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle( fontSize = 18.sp),
                            singleLine = true,
                            shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                            label = { Text("Oven Temperature (${if (settings["Units.Temperature"]== "true") "°C" else "°F"})") } //depending on settings show C or F
                        )
                        FilterChip(
                            onClick = {
                                isFan = !isFan
                                data.cookingTemperature?.isFan = isFan
                            },
                            label = {
                                Text("Fan Oven")
                            },
                            modifier = Modifier.align(Alignment.CenterVertically),
                            selected = isFan,
                            leadingIcon = if (isFan) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }


                }
                if (data.type == CookingStage.hob){
                    MenuItemWithDropDown("Hob Temperature",
                        data.cookingTemperature?.hobTemperature.toString(),
                        HobOption.values()) { value ->
                        data.cookingTemperature?.hobTemperature = enumValueOf(value)
                    }
                }
                TextField(
                    value = timeInput,
                    onValueChange = { value ->
                        timeInput = value
                        data.time = value

                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    textStyle = TextStyle( fontSize = 18.sp),
                    singleLine = true,
                    shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                    label = { Text("Time") }
                )
                MenuItemWithDropDown("Container",
                    if (data.container == null) "none" else data.container?.type.toString(),
                    TinOrPanOptions.values()) { value ->
                    if (value == "none"){
                        data.container = null
                    }
                    else{
                        if (data.container == null){
                            data.container = CookingStepContainer(enumValueOf(value),null,null,null)
                        }
                        else{
                            data.container?.type = enumValueOf(value)
                        }
                    }

                }
                //if container needs show size configure inputs
                when (data.container?.type?.sizeing) {
                    TinOrPanSizeOptions.OneDimension -> { //show input when just one d is needed
                        TextField(
                            value = dimensionOneValue,
                            onValueChange = { value ->
                                dimensionOneValue = value
                                if (settings["Units.metric Lengths"] == "true") { //if the user is inputting a metric or imperial volume
                                    data.container?.dimensionOne = value.toFloatOrNull()
                                } else { //its imperial so convert to metric value to save
                                    data.container?.dimensionOne =
                                        (value.toIntOrNull()?.times(2.54f))
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp),
                            singleLine = true,
                            shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                            label = { Text("Size (${if (settings["Units.metric Lengths"] == "true") "cm" else "in"})") } //size plus the units the user has selected
                        )
                    }
                    TinOrPanSizeOptions.TwoDimension -> { //show input when just one d is needed
                        TextField(
                            value = dimensionOneValue,
                            onValueChange = { value ->
                                dimensionOneValue = value
                                if (settings["Units.metric Lengths"] == "true") { //if the user is inputting a metric or imperial volume
                                    data.container?.dimensionOne = value.toFloatOrNull()
                                } else { //its imperial so convert to metric value to save
                                    data.container?.dimensionOne =
                                        (value.toIntOrNull()?.times(2.54f))
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp),
                            singleLine = true,
                            shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                            label = { Text("Length (${if (settings["Units.metric Lengths"] == "true") "cm" else "in"})") } //size plus the units the user has selected
                        )
                        TextField(
                            value = dimensionTwoValue,
                            onValueChange = { value ->
                                dimensionTwoValue = value
                                if (settings["Units.metric Lengths"] == "true") { //if the user is inputting a metric or imperial volume
                                    data.container?.dimensionTwo= value.toFloatOrNull()
                                } else { //its imperial so convert to metric value to save
                                    data.container?.dimensionTwo =
                                        (value.toIntOrNull()?.times(2.54f))
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp),
                            singleLine = true,
                            shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                            label = { Text("Width (${if (settings["Units.metric Lengths"] == "true") "cm" else "in"})") } //size plus the units the user has selected
                        )
                    }
                    TinOrPanSizeOptions.Volume -> {
                        TextField(
                            value = dimensionTwoValue,
                            onValueChange = { value ->
                                dimensionTwoValue = value
                                if (settings["Units.metric Lengths"] == "true") { //if the user is inputting a metric or imperial volume
                                    data.container?.volume= value.toFloatOrNull()
                                } else { //its imperial so convert to metric value to save
                                    data.container?.volume =
                                        (value.toIntOrNull()?.times(0.5682612f))
                                }
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth(),
                            textStyle = TextStyle(fontSize = 18.sp),
                            singleLine = true,
                            shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                            label = { Text("Volume (${if (settings["Units.metric Volume"] == "true") "l" else "pt"})") } //size plus the units the user has selected
                        )
                    }
                    else -> {//there is no need for sizing options so set all values to empty
                        dimensionOneValue = ""
                        dimensionTwoValue = ""
                        dimensionVolume = ""
                        data.container?.dimensionOne = null
                        data.container?.dimensionTwo = null
                        data.container?.volume = null


                    }
                }
            }
        }
    }
}
@Composable
inline fun <reified T> MenuItemWithDropDown(textLabel: String, value: String, values: Array<T>, crossinline onItemClick: (String) -> Unit) {
    var mExpanded by remember { mutableStateOf(false) }
    var changedValue by remember { mutableStateOf(value) }
    // Up Icon when expanded and down icon when collapsed
    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown
    Row {
        Text(
            text = "$textLabel: $changedValue",
            //
            style = MaterialTheme.typography.titleLarge,
            )
        Spacer(
            Modifier
                .weight(1f)
        )
        Icon(icon, "contentDescription",
            Modifier
                .clickable { mExpanded = !mExpanded }
                .padding(10.dp)
                .size(24.dp))
        DropdownMenu(expanded = mExpanded,
            onDismissRequest = { mExpanded = false }) {
            values.forEach { label ->
                if (label is CookingStage ) { //todo do not repeat the code
                    DropdownMenuItem(onClick = {
                        onItemClick(label.name)
                        changedValue = label.text
                        mExpanded = false
                    }, text = { Text(label.text) })
                }
                if (label is TinOrPanOptions){
                    DropdownMenuItem(onClick = {
                        onItemClick(label.name)
                        changedValue = label.text
                        mExpanded = false
                    }, text = { Text(label.text) })
                }
                if (label is HobOption){
                    DropdownMenuItem(onClick = {
                        onItemClick(label.name)
                        changedValue = label.text
                        mExpanded = false
                    }, text = { Text(label.text) })
                }

            }
        }
    }
}

@Composable
fun Notes(data: MutableState<Recipe>) {
    var notes by remember { mutableStateOf(data.value.data.notes)}
    Card(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()
    ) {
        TextField(
            value = notes ?: "",
            onValueChange = { value ->
                notes = value
                data.value.data.notes =if (notes == ""){ //if empty set to null
                     null
                }else {
                     notes
                }
            },
            modifier = Modifier
                .fillMaxWidth(),
            textStyle = TextStyle( fontSize = 18.sp),
            label = { Text(text = "Notes") }

        )
    }
}
fun getIngredients(data : MutableState<Recipe>) : String{
    var output = ""
    for (ingredient in data.value.ingredients.list){
        output = output.plus("${ingredient.text} \n")
    }
    return output
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsInput(userSettings: Map<String,String>,data : MutableState<Recipe>){
    var ingredientsInput by remember { mutableStateOf(getIngredients(data))}
    val textLineColors = listOf(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.secondary,MaterialTheme.colorScheme.tertiary)
    Card(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()
        ) {
        if (userSettings["Creation.Separate Ingredients"]== "true") { //only apply  different line colours if enabled in the settings
            TextField(
                value = ingredientsInput,
                onValueChange = { value ->
                    ingredientsInput = value
                    //save value to data
                    val ingredients: MutableList<Ingredient> = mutableListOf()
                    var index = 0
                    for (ingredient in value.split("\n")) {
                        if (!ingredient.matches("\\s*".toRegex())) {
                            ingredients.add(Ingredient(index, ingredient))
                            index += 1

                        }
                    }
                    data.value.ingredients = Ingredients(ingredients)
                },
                visualTransformation = {
                    TransformedText(
                        buildAnnotatedStringWithColors(ingredientsInput,textLineColors),
                        OffsetMapping.Identity
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = TextStyle( fontSize = 18.sp),
                label = { Text(text = "Ingredients") }
            )
        }
        else{
            TextField(
                value = ingredientsInput,
                onValueChange = { value ->
                    ingredientsInput = value
                    //save value to data
                    val ingredients: MutableList<Ingredient> = mutableListOf()
                    var index = 0
                    for (ingredient in value.split("\n")) {
                        if (!ingredient.matches("\\s*".toRegex())) {
                            ingredients.add(Ingredient(index, ingredient))
                            index += 1

                        }
                    }
                    data.value.ingredients = Ingredients(ingredients)
                },
                modifier = Modifier
                    .fillMaxWidth(),
                textStyle = TextStyle( fontSize = 18.sp),
                label = { Text(text = "Ingredients") }
            )
        }
    }
}
fun getInstructions(data : MutableState<Recipe>) : String{
    var output = ""
    for (instruction in data.value.instructions.list){
        output = output.plus("${instruction.text} \n")
    }
    return output
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionsInput(userSettings: Map<String,String>,data : MutableState<Recipe>){
    var instructionsInput by remember { mutableStateOf(getInstructions(data))}
    val textLineColors = listOf(MaterialTheme.colorScheme.primary,MaterialTheme.colorScheme.secondary,MaterialTheme.colorScheme.tertiary)

    Card(
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {

            if (userSettings["Creation.Separate Instructions"] == "true") { //only apply  different line colours if enabled in the settings
                TextField(
                    value = instructionsInput,
                    onValueChange = { value ->
                        instructionsInput = value
                        //save value to data
                        val instructions: MutableList<Instruction> = mutableListOf()
                        var index = 0
                        for (instruction in value.split("\n")) {
                            if (!instruction.matches("\\s*".toRegex())) {
                                instructions.add(Instruction(index, instruction, null))
                                index += 1
                            }

                        }
                        data.value.instructions = Instructions(instructions)
                    },
                    visualTransformation = {
                        TransformedText(
                            buildAnnotatedStringWithColors(instructionsInput, textLineColors),
                            OffsetMapping.Identity
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    textStyle = TextStyle( fontSize = 18.sp),
                    label = { Text(text = "instructions") }
                )
            } else {
                TextField(
                    value = instructionsInput,
                    onValueChange = { value ->
                        instructionsInput = value
                        //save value to data
                        val instructions: MutableList<Instruction> = mutableListOf()
                        var index = 0
                        for (instruction in value.split("\n")) {
                            if (!instruction.matches("\\s*".toRegex())) {
                                instructions.add(Instruction(index, instruction, null))
                                index += 1
                            }

                        }
                        data.value.instructions = Instructions(instructions)
                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    textStyle = TextStyle( fontSize = 18.sp),
                    label = { Text(text = "instructions") }
                )
            }
            if (userSettings["Creation.Show split Instruction Buttons"] == "true") {//show buttons to separate instructions if enabled
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)) {
                    Button(onClick = {
                                        data.value.instructions = CreateAutomations.autoSplitInstructions(data.value.instructions,
                                            CreateAutomations.Companion.InstructionSplitStrength.Intelligent)
                                        instructionsInput = getInstructions(data)
                                     },
                        modifier = Modifier.weight(1f)) {
                        Text(text = "Smart Split", textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.weight(0.5f))
                    Button(onClick = {
                        data.value.instructions = CreateAutomations.autoSplitInstructions(data.value.instructions,
                            CreateAutomations.Companion.InstructionSplitStrength.Sentences)
                        instructionsInput = getInstructions(data)
                    },
                        modifier = Modifier.weight(1f)) {
                        Text(text = "Sentence Split", textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}


fun buildAnnotatedStringWithColors(text:String, colors:  List<Color>): AnnotatedString{
    val lines = text.split("\n")
    val builder = AnnotatedString.Builder()
    for ((count, line) in lines.withIndex()) {
        builder.withStyle(style = SpanStyle(color = colors[count%colors.count()])) {
            append(line)
            if (count!= lines.size-1){//do not add the extra /n
                append("\n")
            }
        }
    }
    return builder.toAnnotatedString()
}
@Composable
fun DeleteAndFinishButtons(data : MutableState<Recipe>,updatedSteps:MutableState<Boolean>,onDeleteClick: () -> Unit,onFinishClick: (Recipe,Uri?, Bitmap?,Boolean) -> Unit,imageUri : MutableState<Uri?>,imageBitmap: MutableState<Bitmap?>){
    Row{
        Button(onClick = onDeleteClick,
            modifier = Modifier.padding(all = 5.dp)) {
            Text(
                "Delete",
                modifier = Modifier.padding(all = 2.dp),
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(
            Modifier

                .width(10.dp)
        )

        FinishButton(data, imageUri,updatedSteps,onFinishClick,imageBitmap)
    }
}
@Composable
fun FinishButton(data: MutableState<Recipe>,image: MutableState<Uri?>,update: MutableState<Boolean>,onFinish: (Recipe, Uri?, Bitmap?,Boolean) -> Unit,imageBitmap: MutableState<Bitmap?>){
    Card(
        modifier = Modifier
            .padding(5.dp)
            .animateContentSize()
            .fillMaxWidth()
            .clickable {
                if (data.value.data.cookingSteps.list.isEmpty()) onFinish(
                    data.value,
                    image.value,
                    imageBitmap.value,
                    false
                )
            }
            .clip(RoundedCornerShape(50.dp)),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        )){
        //buttons
        Row(modifier = Modifier
            .padding(8.dp)
            .height(IntrinsicSize.Min)) {
            if (update.value) update.value = false
            if (data.value.data.cookingSteps.list.isNotEmpty()){
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(text = "Finish ",style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable {
                            onFinish(data.value, image.value, imageBitmap.value, false)
                        }
                        .padding(2.dp))
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Divider(
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxHeight()  //fill the max height
                        .width(1.dp)
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(text = " Link",style = MaterialTheme.typography.titleLarge
                ,modifier = Modifier
                        .clickable { onFinish(data.value, image.value, imageBitmap.value, true) }
                        .padding(2.dp))
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            else{
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(text = "Finish",style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(2.dp)
                    )
                Spacer(
                    Modifier
                        .weight(1f)
                )

            }
        }
    }
}

@Composable
private fun MainScreen(userSettings: Map<String,String>,recipeDataInput: MutableState<Recipe>,image : MutableState<Bitmap?>,onDeleteClick: () -> Unit,onFinishClick: (Recipe,Uri?, Bitmap?,Boolean) -> Unit){
    val imageUri : MutableState<Uri?> = remember {mutableStateOf(null)}
    val updatedSteps = remember { mutableStateOf(false) }

    if (recipeDataInput.value != GetEmptyRecipe()){
        Column(modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
        ) {
            TitleInput(recipeDataInput)
            ImageInput(imageUri,image)
            DataInput(userSettings,recipeDataInput,updatedSteps)
            Notes(recipeDataInput)
            IngredientsInput(userSettings,recipeDataInput)
            InstructionsInput(userSettings,recipeDataInput)
            Spacer(modifier = Modifier.weight(1f))
            DeleteAndFinishButtons(recipeDataInput,updatedSteps,onDeleteClick,onFinishClick,imageUri,image)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }else {
        Column(modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
        ) {
            TitleInput(recipeDataInput)
            ImageInput(imageUri,image)
            DataInput(userSettings,recipeDataInput,updatedSteps)
            Notes(recipeDataInput)
            IngredientsInput(userSettings,recipeDataInput)
            InstructionsInput(userSettings,recipeDataInput)
            Spacer(modifier = Modifier.weight(1f))
            DeleteAndFinishButtons(recipeDataInput,updatedSteps,onDeleteClick,onFinishClick,imageUri,image)
            Spacer(modifier = Modifier.height(10.dp))
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
        MainScreen(mapOf(), mutableStateOf(GetEmptyRecipe()), mutableStateOf(null),{},{ recipe, uri, bitmap, linking -> })
    }
}


