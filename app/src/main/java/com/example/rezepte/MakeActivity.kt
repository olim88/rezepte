package com.example.rezepte

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dropbox.core.NetworkIOException
import com.dropbox.core.v2.files.DownloadErrorException
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MakeActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //get the users settings
        val settings = SettingsActivity.loadSettings(
            getSharedPreferences(
                "com.example.rezepte.settings",
                MODE_PRIVATE
            )
        )
        //get name of recipe to load
        val recipeName = intent.extras?.getString("recipe name")
        //get local saved image
        val localImage= if (settings["Local Saves.Cache recipe image"] == "full sized") {
            LocalFilesTask.loadBitmap("${this.filesDir}/image/", "$recipeName.png")
        }  else {null}
        //create image value
        val image: MutableState<Bitmap?> =mutableStateOf(localImage?.first)
        val token = DbTokenHandling(
                getSharedPreferences(
                    "com.example.rezepte.dropboxintegration",
                    MODE_PRIVATE
                )
                ).retrieveAccessToken()
        //create variable for recipe data
        val extractedData : MutableState<Recipe> = mutableStateOf(GetEmptyRecipe())
        //if local save load that data
        val localData = if (settings["Local Saves.Cache recipes"] == "true"){
            LocalFilesTask.loadFile("${this.filesDir}/xml/","$recipeName.xml")
        } else {null}
        //if there is locally saved data load that to extracted data
        if (localData != null ){
            extractedData.value =  XmlExtraction.getData(localData.first)
        }
        //set the content
        setContent {
            RezepteTheme {
                MainScreen(settings, extractedData,image)
            }
        }

        //load saved data about recipe and compare to local save to make sure they are synced with each other
        val downloader = DownloadTask(DropboxClient.getClient(token))
        CoroutineScope(Dispatchers.IO).launch{
            //make sure most updated data else replace with online and save online to file
            try {
                val data = downloader.getXml("/xml/$recipeName.xml")
                //compare the data of save for local and dropbox save
                if (localData != null) {
                    if (data.second.toInstant().toEpochMilli() - localData.second.toInstant()
                            .toEpochMilli() > 5000
                    ) {//if local is more than 5 seconds behind
                        LocalFilesTask.saveString(
                            data.first,
                            "${this@MakeActivity.filesDir}/xml/",
                            "$recipeName.xml"
                        )
                        extractedData.value = XmlExtraction.getData(data.first)
                    } else if (data.second.toInstant().toEpochMilli() - localData.second.toInstant()
                            .toEpochMilli() > -5000
                    ) {//if local save is over 5 seconds newer
                        //upload local to dropbox
                        val uploadClient = UploadTask(DropboxClient.getClient(token))
                        //upload recipe data
                        uploadClient.uploadXml(localData.first, "/xml/$recipeName.xml")
                    }


                } else { //if not saved locally save it and update ui version
                    if (settings["Local Saves.Cache recipes"] == "true") {//if we are saving recipes
                        LocalFilesTask.saveString(
                            data.first,
                            "${this@MakeActivity.filesDir}/xml/",
                            "$recipeName.xml"
                        )
                    }
                    extractedData.value = XmlExtraction.getData(data.first)
                }
                val onlineImage = downloader.getImage("/image/", recipeName!!)
                if (onlineImage != null) {
                    if (settings["Local Saves.Cache recipe image"] == "full sized") {
                        if (localImage != null) {
                            if (data.second.toInstant()
                                    .toEpochMilli() - localImage.second.toInstant()
                                    .toEpochMilli() > 5000
                            ) {//if local is more than 5 seconds behind
                                //use online save and save to device
                                image.value = onlineImage.first
                                LocalFilesTask.saveBitmap(
                                    onlineImage.first,
                                    "${this@MakeActivity.filesDir}/image/",
                                    "$recipeName.png"
                                )
                            } else if (data.second.toInstant()
                                    .toEpochMilli() - localImage.second.toInstant()
                                    .toEpochMilli() > -5000
                            ) {//if local is more than 5 seconds behind
                                //upload local to dropbox
                                val uploadClient = UploadTask(DropboxClient.getClient(token))
                                //upload recipe data
                                uploadClient.uploadBitmap(localImage.first, "/image/$recipeName")

                            }

                        } else { //if local is null save online to it
                            LocalFilesTask.saveBitmap(
                                onlineImage.first,
                                "${this@MakeActivity.filesDir}/image/",
                                "$recipeName.png"
                            )
                            image.value = onlineImage.first
                        }
                    } else {
                        image.value = onlineImage.first
                    }
                }else if (localImage != null){ //if there is no online image but a local one save that online
                    //upload local to dropbox
                    val uploadClient = UploadTask(DropboxClient.getClient(token))
                    //upload recipe data
                    uploadClient.uploadBitmap(localImage.first, "/image/$recipeName")
                }
            }
            catch (e : NetworkIOException){
                //can not reach dropbox so can only go of local files
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MakeActivity, "can't reach dropbox", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e : DownloadErrorException){
                //the file is not on dropbox so upload the local data

                //upload local to dropbox
                val uploadClient = UploadTask(DropboxClient.getClient(token))
                //upload recipe data
                if (localData != null) {
                    uploadClient.uploadXml(localData.first, "/xml/$recipeName.xml")
                }

                if (localImage != null) {
                    uploadClient.uploadBitmap(localImage.first, "/image/$recipeName")
                }


            }

        }

        /*if the keyboad is closed remove the focus on the input
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            window.decorView.getWindowVisibleDisplayFrame(r)
            val height = window.decorView.height
            if (height - r.bottom <= height * 0.1399) {
                findViewById<TextView>(R.id.multiInput).clearFocus()
            }
            
        }

         */

    }
}
private fun intToRoman(num: Int): String? {
    val M = arrayOf("", "M", "MM", "MMM")
    val C = arrayOf("", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM")
    val X = arrayOf("", "X", "XX", "XXX ", "XL", "L", "LX", "LXX", "LXXX", "XC")
    val I = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")
    return M[num / 1000] + C[num % 1000 / 100] + X[num % 100 / 10] + I[num % 10]
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataOutput(userSettings: Map<String,String>,recipeData: Recipe,multiplier : MutableState<Float>){
    var multiplierInput by remember { mutableStateOf("")}
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .animateContentSize()
    ) {
        Column {
            //author
            TextField(
                value = recipeData.data.author,
                onValueChange = {},
                readOnly = true,
                label = {Text("Author")},
                modifier = Modifier
                    .fillMaxWidth(),
            )
            //servings
            TextField(
                value = MakeFormatting.getCorrectUnitsAndValues(recipeData.data.serves,multiplier.value, userSettings),
                onValueChange = {},
                readOnly = true,
                label = {Text("Servings")},
                modifier = Modifier
                    .fillMaxWidth(),
            )
            //multiplier
            TextField(
                value = multiplierInput,
                onValueChange = { value -> multiplierInput = value //if valid number set recipe multiplier to that value
                                val temp = value.toFloatOrNull()
                                if (temp != null) multiplier.value = temp else multiplier.value = 1f
                                },
                label = {Text("Multiplier")},
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = true
            )
            //cooking steps
        }
    }

}
@Composable
fun StepsOutput(recipeData: Recipe){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .animateContentSize()
    ) {
        //title
        Row{
            Spacer(
                Modifier
                    .weight(1f)
            )
            Text(text = "Steps",style = MaterialTheme.typography.titleLarge, textDecoration = TextDecoration.Underline )
            Spacer(
                Modifier
                    .weight(1f)
            )
        }
        //display steps
        Column {
            for (step in recipeData.data.cookingSteps.list){
                cookingStepDisplay(step, getColor(index = step.index, default = MaterialTheme.colorScheme.surface ))
            }
        }
    }
}

@Composable
fun IngredientConversions(userSettings: Map<String,String>, measurement: String, wholeIngredient: String, isVisable: Boolean){
    AnimatedVisibility(
        visible = isVisable,

        enter = scaleIn()
                + fadeIn(
            // Fade in with the initial alpha of 0.3f.
            initialAlpha = 0.3f
        )+ expandVertically (),
        exit = scaleOut() + fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 2.dp, bottom = 2.dp, top = 0.dp)
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceTint,
            )
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                //conversions
                Text(
                    text = MakeFormatting.getConvertions(measurement, wholeIngredient, userSettings)
                        .joinToString(" | "),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun IngredientsOutput(userSettings :Map<String,String>,recipeData: MutableState<Recipe>, mutiplyer: MutableState<Float>){
    var strikeIndex by remember {mutableStateOf(0)}
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (strikeIndex > 0) strikeIndex -= 1
                    },
                    onTap = {
                        if (strikeIndex < recipeData.value.ingredients.list.count()) strikeIndex += 1
                    }
                )
            }
            .animateContentSize()
    ) {
        Column {
            //title
            Row{
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(text = "Ingredients",style = MaterialTheme.typography.titleLarge, textDecoration = TextDecoration.Underline )
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            //update the ingredients list
            for ((index,ingredient) in recipeData.value.ingredients.list.withIndex()) {
                Ingredient(
                    userSettings,
                    ingredient.text,
                    ingredient.index,
                    (index == strikeIndex),
                    (index < strikeIndex),
                    mutiplyer
                )
            }
        }
    }
}
@Composable
fun Ingredient (userSettings: Map<String,String>,value : String,index : Int,isBig : Boolean, isStrike: Boolean, mutiplyer : MutableState<Float>){
    var style =  if (isBig) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall
    if (isStrike) style = style.copy(textDecoration = TextDecoration.LineThrough)
    val convertedText = MakeFormatting.getCorrectUnitsAndValues(value,mutiplyer.value, userSettings)//text adjusted to the user settings for the measurement options and the multiplier
    val mesurmentsInside = MakeFormatting.listUnitsInValue(convertedText) //measurements inside the text
    var showingMesurement by remember { mutableIntStateOf(-1) }//index inside of the measurements list of currently expanded measurement
    if (isStrike) showingMesurement = -1 //when it is showing conversions and then the settings is struck stop showing the conversions
    if (userSettings["Units.Show Conversions"]== "false" || mesurmentsInside.isEmpty()){//if can not find mesurements just show the ingredient or the setting is disabled
        Text ( text = "${intToRoman(index+1)}: $convertedText",
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            style = style
        )
    }else{//hightly measurements and make the clickable to be able to expand conversions on them
        Column (modifier = Modifier.padding(3.dp)) {
            Row (modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)){
                //ingredient number
                Text(
                    text = "${intToRoman(index+1)}: ",
                    textAlign = TextAlign.Center,
                    style = style
                )

                var ingredientLeft = convertedText
                for ((mesureIndex, mesusrment) in mesurmentsInside.withIndex()) {
                    //split on ingredient
                    val split = ingredientLeft.split("${mesusrment.split(" ")[0]}\\s*${mesusrment.split(" ")[1]}".toRegex(), limit = 2)
                    //text before measurement
                    Text(
                        text = split[0],
                        textAlign = TextAlign.Center,
                        style = style
                    )
                    //measurement in clickable
                    Card(
                        modifier = Modifier
                            .clickable {
                                showingMesurement =
                                    if (showingMesurement == mesureIndex) {//if showing this mesure disable it
                                        -1
                                    } else {//otherwise set it to this index to show
                                        mesureIndex
                                    }
                            }
                            ,
                        colors = CardDefaults.cardColors(
                            containerColor = if (mesureIndex == showingMesurement) MaterialTheme.colorScheme.surfaceTint else MaterialTheme.colorScheme.surfaceColorAtElevation(50.dp),
                        )
                    ) {
                        Text(
                            text = mesusrment,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp),
                            style = style,
                            textAlign = TextAlign.Center
                        )
                    }
                    //update ingredientLeft
                    if (split.count()<2){//if end of string break
                        break
                    }
                    ingredientLeft = split[1]

                }
                //render any text left
                if (ingredientLeft != "") {
                    Text(
                        text = ingredientLeft,
                        textAlign = TextAlign.Center,
                        style = style,
                    )
                }
            }
            //render extra mesure info

            IngredientConversions(
                userSettings,
                if (showingMesurement != -1) mesurmentsInside[showingMesurement] else "",
                convertedText,
                showingMesurement != -1
            )

        }

    }

}

@Composable
fun getColor (index: Int?, default : androidx.compose.ui.graphics.Color) :  androidx.compose.ui.graphics.Color{
    if (index == null)  return default
    return  when(index %3){
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.tertiary
        else -> default
    }

}
@Composable
fun InstructionsOutput(recipeData: MutableState<Recipe>){
    var strikeIndex by remember {mutableStateOf(0)}

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (strikeIndex > 0) strikeIndex -= 1
                    },
                    onTap = {
                        if (strikeIndex < recipeData.value.instructions.list.count()) strikeIndex += 1
                    }
                )
            }
            .animateContentSize()
    ) {
        Column {
            //title
            Row{
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(text = "Instructions",style = MaterialTheme.typography.titleLarge, textDecoration = TextDecoration.Underline )
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            //update the instructions list
            for ((index,instruction) in recipeData.value.instructions.list.withIndex()) {
                instruction(
                    instruction.text,
                    instruction.index,
                    (index == strikeIndex),
                    (index < strikeIndex),
                    getColor(instruction.linkedCookingStepIndex,MaterialTheme.colorScheme.onBackground)
                )
                if (index == strikeIndex && instruction.linkedCookingStepIndex != null){
                    cookingStepDisplay(recipeData.value.data.cookingSteps.list[instruction.linkedCookingStepIndex!!],getColor(instruction.linkedCookingStepIndex,MaterialTheme.colorScheme.surface))
                }

            }
        }
    }
}
@Composable
fun cookingStepDisplay (step: CookingStep, color : androidx.compose.ui.graphics.Color){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = color,
        )
    ){
        Column {
            var text = when (step.type){
                CookingStage.prep ->  "prepare${if (step.time != "")" for " else ""}${step.time}"
                CookingStage.wait -> "wait${if (step.time != "")" for " else ""}${step.time}"
                CookingStage.hob -> "on the hob${if (step.time != "")" for " else ""}${step.time}"
                else -> "In the ${step.type.text}${if (step.time != "")" for " else ""}${step.time}"
            }

            //if there is a container
            if (step.container != null){
                text += " in a${if (step.container!!.size != null) " ${step.container!!.size}\"" else ""} ${step.container!!.type.text}"
            }
            //if there is a temperature
            if (step.cookingTemperature != null){
                //if its a  oven
                text += if (step.cookingTemperature!!.temperature != null){
                    " at ${step.cookingTemperature!!.temperature}°C ${if (step.cookingTemperature!!.isFan == true) "fan" else ""}"
                } else{ // hob
                    " at ${step.cookingTemperature!!.hobTemperature.text} heat"
                }

            }
            text += "."
            Text(text = text, modifier = Modifier.padding(5.dp))
        }
    }
}
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun cookingStepDisplayPreview() {
    RezepteTheme {
        cookingStepDisplay(CookingStep(0,"20 mins",CookingStage.oven,
            CookingStepContainer(TinOrPanOptions.roundTin,9),
            CookingStepTemperature(250,HobOption.zero,true)
        ),MaterialTheme.colorScheme.primary)
    }
}
@Composable
fun instruction (value : String,index : Int,isBig : Boolean, isStrike: Boolean, linkedColor : androidx.compose.ui.graphics.Color){
    var style =  if (isBig) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall
    if (isStrike) style = style.copy(textDecoration = TextDecoration.LineThrough)
    Text ( text = "${intToRoman(index+1)}: $value",
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        style = style,
        color = linkedColor
    )
}

@Composable
fun LinkedRecipesOutput(recipeData: Recipe){
    //only show if there are linked recipes
    if (recipeData.data.linked != null) {
        // Fetching the Local Context
        val mContext = LocalContext.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .animateContentSize()
        ) {
            Column {
                //title
                Row{
                    Spacer(
                        Modifier
                            .weight(1f)
                    )
                    Text(text = "Relevant Recipes", style = MaterialTheme.typography.titleLarge,fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                    Spacer(
                        Modifier
                            .weight(1f)
                    )
                }

                //button to open each of the linked recipes
                for (recipeName in recipeData.data.linked!!.list){
                    Button(onClick = {
                        //open that recipe
                        val intent = Intent(mContext, MakeActivity::class.java)
                        intent.putExtra("recipe name", recipeName.name)
                        mContext.startActivity(intent)

                    }) {
                        Row{
                            Text(text = recipeName.name)
                        }
                        Spacer(
                            Modifier
                                .weight(1f)
                        )
                        Icon(Icons.Default.ArrowForward,"go to arrow")

                    }
                }
            }
        }
    }

}

@Composable
private fun MainScreen(userSettings :Map<String,String>,recipeData: MutableState<Recipe>, image : MutableState<Bitmap?>){

    var multiplier = remember {mutableStateOf(1f)}
    // Fetching the Local Context
    val mContext = LocalContext.current
    Column (modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())) {
        //title
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)) {
            //title
            Row{
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(text = recipeData.value.data.name, style = MaterialTheme.typography.titleLarge,fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center)
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }

        }
        //image
        AsyncImage(
            model = (ImageRequest.Builder(LocalContext.current)
                .data(image.value)
                .build()),
            contentDescription = "",
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .fillMaxWidth()
                .animateContentSize(),
            contentScale = ContentScale.FillWidth
        )
        //data
        DataOutput(userSettings,recipeData.value,multiplier)
        //instruction steps
        StepsOutput(recipeData.value)
        //ingredients
        IngredientsOutput(userSettings,recipeData,multiplier)
        //instructions
        InstructionsOutput(recipeData)
        //linked recipes
        LinkedRecipesOutput(recipeData.value)
        //edit and finish button
        Row{
            Button(onClick = {
                val intent = Intent(mContext, CreateActivity::class.java)
                intent.putExtra("creating", false)
                intent.putExtra("recipe name", recipeData.value.data.name)
                mContext.startActivity(intent)
            }, modifier = Modifier.padding(5.dp)) {
                Text (text = "Edit")
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                val intent = Intent(mContext, MainActivity::class.java)
                mContext.startActivity(intent)
            }, modifier = Modifier.padding(5.dp)) {
                Text (text = "Finish")
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
        MainScreen(mapOf(), mutableStateOf(GetEmptyRecipe()), mutableStateOf(null))
    }
}

