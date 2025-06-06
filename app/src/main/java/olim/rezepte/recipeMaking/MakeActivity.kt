package olim.rezepte.recipeMaking

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import olim.rezepte.CookingStage
import olim.rezepte.CookingStep
import olim.rezepte.CookingStepContainer
import olim.rezepte.CookingStepTemperature
import olim.rezepte.HobOption
import olim.rezepte.MainActivity
import olim.rezepte.R
import olim.rezepte.Recipe
import olim.rezepte.SettingsActivity
import olim.rezepte.TinOrPanOptions
import olim.rezepte.XmlExtraction
import olim.rezepte.fileManagment.FileSync
import olim.rezepte.getEmptyRecipe
import olim.rezepte.recipeCreation.CreateActivity
import olim.rezepte.ui.theme.RezepteTheme

class MakeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //get name of recipe to load
        val recipeName = intent.extras?.getString("recipe name")

        //get the users settings
        val settings = SettingsActivity.loadSettings(
            getSharedPreferences(
                "olim.rezepte.settings",
                MODE_PRIVATE
            )
        )
        //get token
        val dropboxPreference =
            getSharedPreferences(
                "olim.rezepte.dropboxintegration",
                MODE_PRIVATE
            )

        //create image value
        val image: MutableState<Bitmap?> = mutableStateOf(null)
        //create variable for recipe data
        val extractedData: MutableState<Recipe> = mutableStateOf(getEmptyRecipe())

        //set the content
        setContent {
            RezepteTheme {
                MainScreen(settings, extractedData, image)
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            //load the file data
            val priority =
                if (settings["Local Saves.Cache recipes"] == "true") FileSync.FilePriority.Newest else FileSync.FilePriority.OnlineOnly
            val imagePriority =
                if (settings["Local Saves.Cache recipe image"] == "full sized") FileSync.FilePriority.Newest else FileSync.FilePriority.OnlineOnly
            val data = FileSync.Data(priority, dropboxPreference)
            val imageData = FileSync.Data(imagePriority, dropboxPreference)
            val file =
                FileSync.FileInfo("/xml/", "${this@MakeActivity.filesDir}/xml/", "$recipeName.xml")
            CoroutineScope(Dispatchers.IO).launch {
                FileSync.downloadString(data, file) {
                    extractedData.value = XmlExtraction.getData(it)
                }
            }

            val imageFile =
                FileSync.FileInfo(
                    "/image/",
                    "${this@MakeActivity.filesDir}/image/",
                    "$recipeName.jpg"
                )
            FileSync.downloadImage(imageData, imageFile) {
                image.value = it

            }
            //sync files

            FileSync.syncFile(data, file) {}
            FileSync.syncFile(imageData, imageFile) {}

            withContext(Dispatchers.Main) {
                if (extractedData.value == getEmptyRecipe()) { //if no data has been loaded show error and close window
                    Toast.makeText(this@MakeActivity,
                        R.string.make_recipe_not_exist_toast, Toast.LENGTH_SHORT)
                        .show()
                    this@MakeActivity.finish()
                }
            }
        }

        //if setting enabled add keep screen on flag
        if (settings["Making.Keep Screen On"] == "true"){
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun intToRoman(num: Int): String {
    val M = arrayOf("", "M", "MM", "MMM")
    val C = arrayOf("", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM")
    val X = arrayOf("", "X", "XX", "XXX ", "XL", "L", "LX", "LXX", "LXXX", "XC")
    val I = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")
    return M[num / 1000] + C[num % 1000 / 100] + X[num % 100 / 10] + I[num % 10]
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataOutput(
    userSettings: Map<String, String>,
    recipeData: Recipe,
    multiplier: MutableState<Float>
) {
    var multiplierInput by remember { mutableStateOf("") }
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
                label = { Text(stringResource(R.string.make_label_author)) },
                modifier = Modifier
                    .fillMaxWidth(),
            )
            //servings
            TextField(
                value = MakeFormatting.getCorrectUnitsAndValues(
                    recipeData.data.serves,
                    multiplier.value,
                    userSettings
                ),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.make_label_servings)) },
                modifier = Modifier
                    .fillMaxWidth(),
            )
            //multiplier
            TextField(
                value = multiplierInput,
                onValueChange = { value ->
                    multiplierInput = value //if valid number set recipe multiplier to that value
                    val temp = value.toFloatOrNull()
                    if (temp != null) multiplier.value = temp else multiplier.value = 1f
                },
                label = { Text(stringResource(R.string.make_label_multiplier)) },
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
fun StepsOutput(userSettings: Map<String, String>, recipeData: Recipe) {
    if (recipeData.data.cookingSteps.list.isNotEmpty()) {//only show field if steps exist
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .animateContentSize()
        ) {
            //title
            Row {
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(
                    text = stringResource(R.string.make_title_steps),
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            //display steps
            Column {
                for (step in recipeData.data.cookingSteps.list) {
                    CookingStepDisplay(
                        step,
                        getColor(index = step.index, default = MaterialTheme.colorScheme.surface),
                        userSettings
                    )
                }
            }
        }
    }
}

@Composable
fun NotesOutput(recipeData: Recipe) {
    if (recipeData.data.notes != null) {//only show field if steps exist
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
                .animateContentSize()
        ) {
            //title
            Row {
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(
                    text = stringResource(R.string.make_title_notes),
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            //show notes
            Text(
                text = recipeData.data.notes!!,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(5.dp)

            )
        }
    }
}

@Composable
fun IngredientConversions(
    userSettings: Map<String, String>,
    measurement: String,
    wholeIngredient: String,
    isVisable: Boolean
) {
    AnimatedVisibility(
        visible = isVisable,

        enter = scaleIn()
                + fadeIn(
            // Fade in with the initial alpha of 0.3f.
            initialAlpha = 0.3f
        ) + expandVertically(),
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
                    text = MakeFormatting.getConversions(measurement, wholeIngredient, userSettings)
                        .joinToString(" | "),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun IngredientsOutput(
    userSettings: Map<String, String>,
    recipeData: MutableState<Recipe>,
    multiplier: MutableState<Float>,
    sideBySide: Boolean
) {
    var strikeIndex by remember { mutableStateOf(0) }
    Card(
        modifier = Modifier
            //shrinks the width to just the side when needing to go side by side with instructions
            .fillMaxWidth(if (sideBySide) 0.4f else 1f)
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
            Row {
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(
                    text = stringResource(R.string.make_title_ingredients),
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            //update the ingredients list
            for ((index, ingredient) in recipeData.value.ingredients.list.withIndex()) {
                Ingredient(
                    userSettings,
                    ingredient.text,
                    ingredient.index,
                    (index == strikeIndex),
                    (index < strikeIndex),
                    multiplier
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Ingredient(
    userSettings: Map<String, String>,
    value: String,
    index: Int,
    isBig: Boolean,
    isStrike: Boolean,
    multiplier: MutableState<Float>
) {
    var style = if (userSettings["Making.Walk Though Ingredients"] == "false") {
        MaterialTheme.typography.bodyMedium
    } else if (isBig) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.bodySmall
    }
    if (isStrike && userSettings["Making.Walk Though Ingredients"] == "true") style =
        style.copy(textDecoration = TextDecoration.LineThrough)
    val convertedText = MakeFormatting.getCorrectUnitsAndValues(
        value,
        multiplier.value,
        userSettings
    )//text adjusted to the user settings for the measurement options and the multiplier
    val measurementsInside =
        MakeFormatting.listUnitsInValue(convertedText) //measurements inside the text
    var showingMeasurement by remember { mutableIntStateOf(-1) }//index inside of the measurements list of currently expanded measurement
    if (isStrike && userSettings["Making.Walk Though Ingredients"] == "true") showingMeasurement =
        -1 //when it is showing conversions and then the settings is struck stop showing the conversions
    if (userSettings["Units.Show Conversions"] == "false" || measurementsInside.isEmpty()) {//if can not find measurements just show the ingredient or the setting is disabled
        Text(
            text = stringResource(id = R.string.make_ingredient_item_format, intToRoman(index + 1), convertedText),
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            style = style
        )
    } else {//highlight measurements and make the clickable to be able to expand conversions on them
        Column(modifier = Modifier.padding(3.dp)) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
            ) {
                //ingredient number
                Text(
                    text = stringResource(id = R.string.make_ingredient_item_format, intToRoman(index + 1), ""),
                    textAlign = TextAlign.Center,
                    style = style
                )

                var ingredientLeft = convertedText
                for ((measureIndex, measurement) in measurementsInside.withIndex()) {
                    //split on ingredient
                    val split = ingredientLeft.split(
                        "${measurement.split(" ")[0]}\\s*${measurement.split(" ")[1]}".toRegex(),
                        limit = 2
                    )
                    //text before measurement
                    //add each word in the text one by one as this will let it be wrapped around properly when it is to long
                    for (word in split[0].split(" ")) {
                        Text(
                            text = "$word ",
                            textAlign = TextAlign.Center,
                            style = style
                        )
                    }
                    //measurement in clickable
                    Card(
                        modifier = Modifier
                            .clickable {
                                showingMeasurement =
                                    if (showingMeasurement == measureIndex) {//if showing this measure disable it
                                        -1
                                    } else {//otherwise set it to this index to show
                                        measureIndex
                                    }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (measureIndex == showingMeasurement) MaterialTheme.colorScheme.surfaceTint else MaterialTheme.colorScheme.surfaceColorAtElevation(
                                50.dp
                            ),
                        )
                    ) {
                        Text(
                            text = measurement,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp),
                            style = style,
                            textAlign = TextAlign.Center,
                        )
                    }
                    //update ingredientLeft
                    if (split.count() < 2) {//if end of string break
                        break
                    }
                    ingredientLeft = split[1]

                }
                //render any text left
                if (ingredientLeft != "") {
                    //add each word in the text one by one as this will let it be wrapped around properly when it is to long
                    for (word in ingredientLeft.split(" ")) {
                        Text(
                            text = word,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 2.dp, end = 2.dp),
                            style = style,
                        )
                    }

                }
            }
            //render extra measure info

            IngredientConversions(
                userSettings,
                if (showingMeasurement != -1) measurementsInside[showingMeasurement] else "",
                convertedText,
                showingMeasurement != -1
            )
        }
    }
}

@Composable
fun getColor(
    index: Int?,
    default: androidx.compose.ui.graphics.Color
): androidx.compose.ui.graphics.Color {
    if (index == null) return default
    return when (index % 3) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.tertiary
        else -> default
    }

}

@Composable
fun InstructionsOutput(
    settings: Map<String, String>,
    recipeData: MutableState<Recipe>,
    multiplier: MutableState<Float>
) {
    var strikeIndex by remember { mutableIntStateOf(0) }

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
            Row {
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(
                    text = stringResource(R.string.make_title_instructions),
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(
                    Modifier
                        .weight(1f)
                )
            }
            //update the instructions list
            for ((index, instruction) in recipeData.value.instructions.list.withIndex()) {
                val correctUnitsAndMultipliedText =
                    MakeFormatting.getCorrectUnitsAndValuesInIngredients(
                        instruction.text,
                        multiplier.value,
                        settings
                    )
                val colour = if (settings["Making.Walk Though Instructions"] == "true") {
                    getColor(
                        instruction.linkedCookingStepIndex,
                        MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    MaterialTheme.colorScheme.onBackground
                }
                instruction(
                    settings,
                    correctUnitsAndMultipliedText,
                    instruction.index,
                    (index == strikeIndex),
                    (index < strikeIndex),
                    colour
                )
                if (settings["Making.Walk Though Instructions"] == "true" && index == strikeIndex && instruction.linkedCookingStepIndex != null) {
                    CookingStepDisplay(
                        recipeData.value.data.cookingSteps.list[instruction.linkedCookingStepIndex!!],
                        getColor(
                            instruction.linkedCookingStepIndex,
                            MaterialTheme.colorScheme.surface
                        ), settings
                    )
                }
            }
        }
    }
}

@Composable
fun CookingStepDisplay(
    step: CookingStep,
    color: androidx.compose.ui.graphics.Color,
    settings: Map<String, String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = color,
        )
    ) {
        Column {
            Text(
                text = MakeFormatting.getCookingStepDisplayText(step, settings),
                modifier = Modifier.padding(5.dp)
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
fun CookingStepDisplayPreview() {
    RezepteTheme {
        CookingStepDisplay(
            CookingStep(
                0, "20 mins", CookingStage.oven,
                CookingStepContainer(TinOrPanOptions.roundTin, 9f, null, null),
                CookingStepTemperature(250, HobOption.zero, true)
            ), MaterialTheme.colorScheme.primary, mapOf()
        )
    }
}

@Composable
fun instruction(
    userSettings: Map<String, String>,
    value: String,
    index: Int,
    isBig: Boolean,
    isStrike: Boolean,
    linkedColor: androidx.compose.ui.graphics.Color
) {
    var style = if (userSettings["Making.Walk Though Instructions"] == "false") {
        MaterialTheme.typography.bodyMedium
    } else if (isBig) {
        MaterialTheme.typography.titleLarge
    } else {
        MaterialTheme.typography.bodySmall
    }

    if (isStrike && userSettings["Making.Walk Though Instructions"] == "true") style =
        style.copy(textDecoration = TextDecoration.LineThrough)
    Text(
        text = "${intToRoman(index + 1)}: $value",
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        style = style,
        color = linkedColor
    )
}

@Composable
fun LinkedRecipesOutput(recipeData: Recipe) {
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
                Row {
                    Spacer(
                        Modifier
                            .weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.make_title_relevant_recipes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(
                        Modifier
                            .weight(1f)
                    )
                }

                //button to open each of the linked recipes
                for (recipeName in recipeData.data.linked!!.list) {
                    Button(onClick = {
                        //open that recipe
                        val intent = Intent(mContext, MakeActivity::class.java)
                        intent.putExtra("recipe name", recipeName.name)
                        mContext.startActivity(intent)

                    }) {
                        Row {
                            Text(text = recipeName.name)
                        }
                        Spacer(
                            Modifier
                                .weight(1f)
                        )
                        Icon(Icons.Default.ArrowForward, stringResource(R.string.make_icon_go_to_arrow_description))

                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(
    userSettings: Map<String, String>,
    recipeData: MutableState<Recipe>,
    image: MutableState<Bitmap?>
) {

    val multiplier = remember { mutableFloatStateOf(1f) }
    // Fetching the Local Context
    val mContext = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        //title
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            //title
            Row {
                Spacer(
                    Modifier
                        .weight(1f)
                )
                Text(
                    text = recipeData.value.data.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center
                )
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
            contentDescription = stringResource(R.string.make_image_recipe_thumbnail_description),
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .fillMaxWidth()
                .animateContentSize(),
            contentScale = ContentScale.FillWidth
        )
        //data
        DataOutput(userSettings, recipeData.value, multiplier)
        //instruction steps
        StepsOutput(userSettings, recipeData.value)
        //Notes
        NotesOutput(recipeData.value)
        //put ingredients and instructions side by side when enabled
        if (mContext.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && userSettings["Making.Horizontal Layout"] == "true") {
            Row {
                //ingredients
                IngredientsOutput(userSettings, recipeData, multiplier, true)
                //instructions
                InstructionsOutput(userSettings, recipeData, multiplier)
            }
        } else {
            //ingredients
            IngredientsOutput(userSettings, recipeData, multiplier, false)
            //instructions
            InstructionsOutput(userSettings, recipeData, multiplier)
        }
        //linked recipes
        LinkedRecipesOutput(recipeData.value)
        //edit and finish button
        Row {
            Button(onClick = {
                val intent = Intent(mContext, CreateActivity::class.java)
                intent.putExtra("creating", false)
                intent.putExtra("recipe name", recipeData.value.data.name)
                mContext.startActivity(intent)
            }, modifier = Modifier.padding(5.dp)) {
                Text(text = stringResource(R.string.make_button_edit))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                val intent = Intent(mContext, MainActivity::class.java)
                mContext.startActivity(intent)
            }, modifier = Modifier.padding(5.dp)) {
                Text(text = stringResource(R.string.finish))
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
        MainScreen(mapOf(), mutableStateOf(getEmptyRecipe()), mutableStateOf(null))
    }
}

