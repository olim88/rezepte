package com.example.rezepte


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.redundent.kotlin.xml.xml
import java.io.File


class CreateActivity : ComponentActivity() {
    private var loadedRecipeName: String? = null
    private val IMAGE_REQUEST_CODE = 10
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadedRecipeName = intent.extras?.getString("recipe name")
        val recipe = intent.extras?.getString("data") //if a recipe has been sent as a xml to load
        val recipeLinkedImage = intent.extras?.getString("imageData")
        var image: MutableState<Bitmap?> = mutableStateOf(null)

        //check if there is preloaded recipe then name then just load empty recipe if neither is true
        if ( recipe != null){
            val extractedData = xmlExtraction().GetData(recipe)
            setContent {
                RezepteTheme {
                    MainScreen(
                        recipeDataInput = extractedData,
                        image,
                        { deleteRecipe() },
                        { recipe, uri, bitmap,linking -> finishRecipe(recipe, uri,bitmap,linking) })
                }
            }
            //if there is a link to an image download it to bitmap to add to the recipe
            if (recipeLinkedImage != null){
            GlobalScope.launch {
                    image.value = DownloadWebsite.downloadImageToBitmap(recipeLinkedImage)
                }
            }
        }
        else if (loadedRecipeName != null) {
            //get token
            val token = DbTokenHandling(
                getSharedPreferences(
                    "com.example.rezepte.dropboxintegration",
                    MODE_PRIVATE
                )
            ).retrieveAccessToken()


            //load saved data about recipe
            val downloader = DownloadTask(DropboxClient.getClient(token))
            GlobalScope.launch {
                //get data
                val data: String = downloader.getXml("/xml/$loadedRecipeName.xml")
                val extractedData = xmlExtraction().GetData(data)

                withContext(Dispatchers.Main) {
                    setContent {
                        RezepteTheme {
                            MainScreen(
                                recipeDataInput = extractedData,
                                image,
                                { deleteRecipe() },
                                { recipe, uri, bitmap,linking -> finishRecipe(recipe, uri,bitmap,linking) })
                        }
                    }

                }
                image.value = downloader.getImage("/image/", loadedRecipeName!!)
            }

        } else {
            setContent {
                RezepteTheme {
                    MainScreen(
                        recipeDataInput = GetEmptyRecipe(),
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
                        val token = DbTokenHandling( //get token
                            getSharedPreferences(
                                "com.example.rezepte.dropboxintegration",
                                MODE_PRIVATE
                            )
                        ).retrieveAccessToken()
                        val uploader = UploadTask(DropboxClient.getClient(token))
                        GlobalScope.launch {
                            //remove xml
                            uploader.removeFile("/xml/$loadedRecipeName.xml")
                            //remove image
                            uploader.removeImage("/image/$loadedRecipeName")
                        }
                        //go home
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent);
                    })

                .setNegativeButton(android.R.string.no, null).show()
        } else //otherwise just return home
        {
            //go home
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent);
        }
    }

    private fun finishRecipe(recipe: Recipe, image: Uri?,bitmapImage : Bitmap?,linking : Boolean) { //todo save image bitmap from website


        //make sure there is a name for the recipe else don't ext
        if (recipe.data.name == ""){
            return
        }
        //convert saved recipe to xml
        val data: String = parseData(recipe)

        //get image if one is set
        var file: File? = null
        if (image != null) {
            file = File(URI_to_Path.getPath(application, image!!))
        }

        //get the name of the recipe to save
        var name = recipe.data.name
        //get dropbox token and upload image and xml to dropbox
        val token = DbTokenHandling( //get token
            getSharedPreferences(
                "com.example.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        ).retrieveAccessToken()
        //upload xml and images
        GlobalScope.launch {
            val uploadClient = UploadTask(DropboxClient.getClient(token))
            //upload recipe data
            uploadClient.uploadXml(data,"/xml/$name.xml")
            //if there is a uri image upload that
            if (file != null){
                uploadClient.uploadFile(file,"/image/$name")
            }
            //otherwise upload the bitmap image if there is one
            else if (bitmapImage != null){
                uploadClient.uploadBitmap(bitmapImage,"/image/$name")
            }
            //if there is no image make sure that any saved image is deleted
            else{
                uploadClient.removeImage("/image/$name")
            }

        }
        //if the name has changed delete old files
        if (name != loadedRecipeName && loadedRecipeName != null) {
            val token = DbTokenHandling( //get token
                getSharedPreferences(
                    "com.example.rezepte.dropboxintegration",
                    MODE_PRIVATE
                )
            ).retrieveAccessToken()
            val uploader = UploadTask(DropboxClient.getClient(token))
            GlobalScope.launch {
                //remove xml
                uploader.removeFile("/xml/$loadedRecipeName.xml")
                //remove image
                uploader.removeImage("/image/$loadedRecipeName")

            }
        }
        //if linking move to the link page else go home
        if (linking){
            val intent = Intent(this, LinkStepsToInstructions::class.java)
            intent.putExtra("data",data)
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
    fun parseData(recipe : Recipe): String    {
    //get info from layout

    val recipe = xml("recipe") {
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
                                    if ( step.container!!.size != null){
                                        "tinSize"{
                                            - step.container!!.size.toString()
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

    return recipe.toString(true)


}






data class MyExtractedData(
    var text: String = "",
)
@Composable
fun ErrorDialog(errorTitle: String,errorBody: String){ //this is not working for some reason and i can not even use it where i want so ?
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog when the user clicks outside the dialog or on the back
                // button. If you want to disable that functionality, simply use an empty
                // onDismissRequest.
                openDialog.value = false
            },
            confirmButton = { openDialog.value = false},
            title = {Text(text = errorTitle, style = MaterialTheme.typography.titleLarge)},
            text = { Text(text = errorBody,style = MaterialTheme.typography.bodyMedium)},
            icon = {Icon(Icons.Filled.Info,"error icon")}

        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleInput(data : MutableState<Recipe>){
    TextField(
        value = data.value.data.name,
        onValueChange = { value ->
            data.value = data.value.copy(data = data.value.data.copy(name = value)) //update name
        },
        modifier = Modifier
            .fillMaxWidth(),
        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
        singleLine = true,
        shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
        label = { Text("Name") }

    )
}
@Composable
fun ImageInput( image : MutableState<Uri?>, savedBitmap: MutableState<Bitmap?>){
    // Fetching the Local Context
    val getImageContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        image.value = uri
        //set bitmap to nothing
        savedBitmap.value = null

    }
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
            Button(onClick = {         //Select image to upload
                getImageContent.launch("image/*")
                }
                ,modifier = Modifier.padding(5.dp)
            ) {
                Icon(if (image.value == null || savedBitmap.value != null) Icons.Filled.Add else Icons.Filled.Edit, "contentDescription")


            }

        }
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInput(data : MutableState<Recipe>,updatedSteps:MutableState<Boolean>){
    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 15.dp,
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
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
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
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                singleLine = true,
                shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                label = { Text("Servings") }
            )
            CookingStepsInput(data,updatedSteps)
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
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
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
    var linkedRecipes by remember { mutableStateOf(data.value.data.linked?.list) }
    var recipeCount by remember { mutableStateOf(data.value.data.linked?.list?.count()) }
    var isExpanded by remember { mutableStateOf(false)}
    val icon = if (isExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown
    // Fetching the Local Context
    val mContext = LocalContext.current

    val lifecycleEvent = rememberLifecycleEvent()
    LaunchedEffect(lifecycleEvent) {
        if (lifecycleEvent == Lifecycle.Event.ON_RESUME) {
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
                    recipeCount = 1
                }
                else{
                    linkedRecipes?.add(LinkedRecipe(name))
                    data.value.data.linked?.list = linkedRecipes!!
                    recipeCount = recipeCount?.plus(1)
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
                    if (linkedRecipes != null) {
                        var temp = recipeCount
                        for ((index, _) in linkedRecipes!!.withIndex()) {
                            temp = temp?.plus(1)
                            LinkedRecipe(data, index) { index ->
                                linkedRecipes!!.removeAt(index)
                                recipeCount = recipeCount?.minus(1)
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
            Text(text = data.value.data.linked!!.list[index].name, modifier = Modifier.padding(5.dp))
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
@Composable
fun CookingStepsInput(data : MutableState<Recipe>, updatedSteps :MutableState<Boolean>){
    var steps by remember { mutableStateOf(data.value.data.cookingSteps.list) }
    var stepCount by remember{ mutableStateOf(data.value.data.cookingSteps.list.count()) }
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
                Column {
                    var temp = stepCount
                    for (step in steps) {
                        temp -= 1
                        CookingStep(data, step.index) { index ->
                            steps.removeAt(index)
                            steps.forEach { edit -> if (edit.index > index) edit.index -= 1 }
                            data.value.data.cookingSteps.list = steps
                            stepCount -= 1
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
                                    CookingStepTemperature(0,HobOption.zero,false) //starts as oven so need temp set up
                                )
                            )
                            stepCount = steps.count()
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
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingStep(data : MutableState<Recipe>,index : Int,onItemClick: (Int) -> Unit){
    var isExpanded by remember { mutableStateOf(false) }
    var isFan by remember {  if (data.value.data.cookingSteps.list[index].cookingTemperature?.isFan == null) mutableStateOf(false) else mutableStateOf(data.value.data.cookingSteps.list[index].cookingTemperature?.isFan!!)}
    var timeInput by remember { mutableStateOf(data.value.data.cookingSteps.list[index].time)}
    var tinSize by remember { if (data.value.data.cookingSteps.list[index].container?.size == null) mutableStateOf("") else mutableStateOf(data.value.data.cookingSteps.list[index].container?.size.toString())}
    var cookingTemp by remember { if (data.value.data.cookingSteps.list[index].cookingTemperature?.temperature == null) mutableStateOf("") else mutableStateOf(data.value.data.cookingSteps.list[index].cookingTemperature?.temperature.toString())}
    Card(modifier = Modifier
        .clickable { isExpanded = !isExpanded }
        .padding(3.dp)
        .animateContentSize()) {
        if (!isExpanded) {
            Row {
                Text(
                    text  = if (data.value.data.cookingSteps.list[index].time == "")"${data.value.data.cookingSteps.list[index].type}" else "${data.value.data.cookingSteps.list[index].type} for ${data.value.data.cookingSteps.list[index].time}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(7.dp)
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Button(onClick = { onItemClick(index) }, modifier = Modifier.fillMaxHeight()) {
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
                MenuItemWithDropDown("Type",data.value.data.cookingSteps.list[index].type.toString(),CookingStage.values()) { value ->
                    data.value.data.cookingSteps.list[index].type = enumValueOf(value)
                    if (data.value.data.cookingSteps.list[index].type == CookingStage.oven){
                        data.value.data.cookingSteps.list[index].cookingTemperature = CookingStepTemperature(0,HobOption.zero,false)
                    }
                    else if (data.value.data.cookingSteps.list[index].type == CookingStage.hob){
                        data.value.data.cookingSteps.list[index].cookingTemperature = CookingStepTemperature(null,HobOption.zero,null)
                    }
                    else {
                        data.value.data.cookingSteps.list[index].cookingTemperature = null
                    }
                }
                //if it is oven or pan enable cooking temp options
                if (data.value.data.cookingSteps.list[index].type == CookingStage.oven){
                    Row {
                        TextField(
                            value = cookingTemp,
                            onValueChange = { value ->
                                cookingTemp = value
                                data.value.data.cookingSteps.list[index].cookingTemperature?.temperature = value.toIntOrNull()
                            },
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                            singleLine = true,
                            shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                            label = { Text("Oven Temperature") }
                        )
                        FilterChip(
                            onClick = {
                                isFan = !isFan
                                data.value.data.cookingSteps.list[index].cookingTemperature?.isFan = isFan
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
                if (data.value.data.cookingSteps.list[index].type == CookingStage.hob){
                    MenuItemWithDropDown("Hob Temperature",
                        data.value.data.cookingSteps.list[index].cookingTemperature?.hobTemperature.toString(),
                        HobOption.values()) { value ->
                        data.value.data.cookingSteps.list[index].cookingTemperature?.hobTemperature = enumValueOf(value)
                    }
                }
                TextField(
                    value = timeInput,
                    onValueChange = { value ->
                        timeInput = value
                        data.value.data.cookingSteps.list[index].time = value

                    },
                    modifier = Modifier
                        .fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                    singleLine = true,
                    shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                    label = { Text("Time") }
                )
                MenuItemWithDropDown("Container",
                    if (data.value.data.cookingSteps.list[index].container == null) "none" else data.value.data.cookingSteps.list[index].container?.type.toString(),
                    TinOrPanOptions.values()) { value ->
                    if (value == "none"){
                        data.value.data.cookingSteps.list[index].container = null
                    }
                    else{
                        if (data.value.data.cookingSteps.list[index].container == null){
                            data.value.data.cookingSteps.list[index].container = CookingStepContainer(enumValueOf(value),null)
                        }
                        else{
                            data.value.data.cookingSteps.list[index].container?.type = enumValueOf(value)
                        }
                    }

                }
                //if the container is a tin enable the size input
                if (data.value.data.cookingSteps.list[index].container?.type == TinOrPanOptions.roundTin ||data.value.data.cookingSteps.list[index].container?.type == TinOrPanOptions.rectangleTin){
                    TextField(
                        value =  tinSize,
                        onValueChange = { value ->
                            tinSize = value
                            data.value.data.cookingSteps.list[index].container?.size = value.toIntOrNull()

                        },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        singleLine = true,
                        shape = RectangleShape, // The TextFiled has rounded corners top left and right by default
                        label = { Text("Tin Size") }
                    )
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
                if (label is CookingStage ) { //todo do not repeate the code
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
fun getIngredients(data : MutableState<Recipe>) : String{
    var output = ""
    for (ingredient in data.value.ingredients.list){
        output = output.plus("${ingredient.text} \n")
    }
    return output
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientsInput(data : MutableState<Recipe>){
    var ingredientsInput by remember { mutableStateOf(getIngredients(data))}

    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 15.dp,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()
        ) {

        TextField(
            value = ingredientsInput,
            onValueChange = { value ->
                ingredientsInput = value
                //save value to data
                val ingredients : MutableList<Ingredient> = mutableListOf()
                var index = 0
                for ( ingredient in value.split("\n")){
                    if (!ingredient.matches("\\s*".toRegex())) {
                        ingredients.add(Ingredient(index, ingredient))
                        index += 1

                    }
                }
                data.value.ingredients = Ingredients(ingredients)
            },
            modifier = Modifier
                .fillMaxWidth(),
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            label = { Text(text = "Ingredients")}
        )
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
fun InstructionsInput(data : MutableState<Recipe>){
    var instructionsInput by remember { mutableStateOf(getInstructions(data))}

    Surface(
        shape = MaterialTheme.shapes.small, shadowElevation = 15.dp,
        modifier = Modifier
            .padding(all = 5.dp)
            .fillMaxWidth()
            .animateContentSize()
    ) {

        TextField(
            value = instructionsInput,
            onValueChange = { value ->
                instructionsInput = value
                //save value to data
                val instructions : MutableList<Instruction> = mutableListOf()
                var index = 0
                for (  instruction in value.split("\n")){
                    if (!instruction.matches("\\s*".toRegex())){
                        instructions.add(Instruction(index,instruction,null))
                        index += 1
                    }

                }
                data.value.instructions = Instructions(instructions)
            },
            modifier = Modifier
                .fillMaxWidth(),
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            label = { Text(text = "instructions")}
        )
    }
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
            .clickable { if(data.value.data.cookingSteps.list.isEmpty()) onFinish(data.value, image.value,imageBitmap.value, false) }
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
                            onFinish(data.value, image.value,imageBitmap.value, false)
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
                        .clickable { onFinish(data.value, image.value,imageBitmap.value, true) }
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
private fun MainScreen(recipeDataInput: Recipe,image : MutableState<Bitmap?>,onDeleteClick: () -> Unit,onFinishClick: (Recipe,Uri?, Bitmap?,Boolean) -> Unit){
    var recipeDataInput = remember { mutableStateOf(recipeDataInput) }
    var imageUri : MutableState<Uri?> = remember {mutableStateOf(null)}
    var updatedSteps = remember { mutableStateOf(false) }
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        TitleInput(recipeDataInput)
        ImageInput(imageUri,image)
        DataInput(recipeDataInput,updatedSteps)
        IngredientsInput(recipeDataInput)
        InstructionsInput(recipeDataInput)
        DeleteAndFinishButtons(recipeDataInput,updatedSteps,onDeleteClick,onFinishClick,imageUri,image)

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
        MainScreen(GetEmptyRecipe(), mutableStateOf(null),{},{ recipe, uri, bitmap, linking -> })
    }
}


