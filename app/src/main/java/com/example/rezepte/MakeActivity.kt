package com.example.rezepte

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
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
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


class MakeActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recipeName = intent.extras?.getString("recipe name")
        var image: MutableState<Bitmap?> = mutableStateOf(null)
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
            val data: String = downloader.GetXml("/xml/$recipeName.xml")
            val extractedData = xmlExtraction().GetData(data)

            withContext(Dispatchers.Main) {
                setContent {
                    RezepteTheme {
                        MainScreen( extractedData,image)
                    }
                }
            }
            image.value = downloader.GetImage("/image/",recipeName!!)
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

private fun multiplyNumbersInString(string :String, multiplier: Float, rounding: RoundingOption) : String {
    val numbers = Regex("[0-9]+(\\d*\\.)?[0-9]*").findAll(string)
        .map(MatchResult::value)
        .toList()
    //replace numbers with multiplied value
    var output = string
    for (number in numbers){
        var value = number.toFloat() * multiplier
        when (rounding){
            RoundingOption.Int -> value = value.roundToInt().toFloat()
            RoundingOption.Little -> value = (value*10).roundToInt().toFloat()/10
            RoundingOption.Some -> value = (value*100).roundToInt().toFloat()/1000
            else -> continue
        }
        output = output.replace(number,(value).toString().replace(".0",""))
    }
    return output
}
enum class RoundingOption{
    None,
    Some,
    Little,
    Int,
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataOutput(recipeData: Recipe,multiplier : MutableState<Float>){
    var multiplierInput by remember { mutableStateOf("1")}
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
                value = multiplyNumbersInString(recipeData.data.serves,multiplier.value,RoundingOption.Little),
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
fun IngredientsOutput(recipeData: Recipe, mutiplyer: MutableState<Float>){
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
                        if (strikeIndex < recipeData.ingredients.list.count()) strikeIndex += 1
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
            for ((index,ingredient) in recipeData.ingredients.list.withIndex()) {
                Ingredient(
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
fun Ingredient (value : String,index : Int,isBig : Boolean, isStrike: Boolean, mutiplyer : MutableState<Float>){
    var style =  if (isBig) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall
    if (isStrike) style = style.copy(textDecoration = TextDecoration.LineThrough)
    Text ( text = "${intToRoman(index+1)}: ${multiplyNumbersInString(value,mutiplyer.value,RoundingOption.Little)}",
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        style = style
    )
}

@Composable
fun getColor (index: Int?, default : androidx.compose.ui.graphics.Color) :  androidx.compose.ui.graphics.Color{
    if (index == null)  return default
    return  when(index!!%3){
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.tertiary
        else -> default
    }

}
@Composable
fun InstructionsOutput(recipeData: Recipe){
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
                        if (strikeIndex < recipeData.instructions.list.count()) strikeIndex += 1
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
            for ((index,instruction) in recipeData.instructions.list.withIndex()) {
                instruction(
                    instruction.text,
                    instruction.index,
                    (index == strikeIndex),
                    (index < strikeIndex),
                    getColor(instruction.linkedCookingStepIndex,MaterialTheme.colorScheme.onBackground)
                )
                if (index == strikeIndex && instruction.linkedCookingStepIndex != null){
                    cookingStepDisplay(recipeData.data.cookingSteps.list[instruction.linkedCookingStepIndex!!],getColor(instruction.linkedCookingStepIndex,MaterialTheme.colorScheme.surface))
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
                CookingStage.prep ->  "prepare for ${step.time}"
                CookingStage.wait -> "wait for ${step.time}"
                CookingStage.hob -> "on the hob for ${step.time}"
                else -> "In the ${step.type.text} for ${step.time}"
            }

            //if there is a container
            if (step.container != null){
                text += " in a ${if (step.container!!.size != null) "${step.container!!.size}\"" else ""} ${step.container!!.type.text}"
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
private fun MainScreen(recipeData: Recipe, image : MutableState<Bitmap?>){
    var recipeData by remember { mutableStateOf(recipeData) }
    var multiplier = remember {mutableStateOf(1f)}
    // Fetching the Local Context
    val mContext = LocalContext.current
    Column (modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                Text(text = recipeData.data.name, style = MaterialTheme.typography.titleLarge,fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center)
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
                //.placeholder(R.drawable.book) //todo get better place holder
                .build()),
            contentDescription = "",
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .animateContentSize(),
            contentScale = ContentScale.FillWidth
        )
        //data
        DataOutput(recipeData,multiplier)
        //instruction steps
        StepsOutput(recipeData)
        //ingredients
        IngredientsOutput(recipeData,multiplier)
        //instructions
        InstructionsOutput(recipeData)
        //linked recipes
        LinkedRecipesOutput(recipeData)
        //edit and finish button
        Row{
            Button(onClick = {
                val intent = Intent(mContext, CreateActivity::class.java)
                intent.putExtra("creating", false)
                intent.putExtra("recipe name", recipeData.data.name)
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
        MainScreen(GetEmptyRecipe(), mutableStateOf(null))
    }
}

