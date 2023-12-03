package com.example.rezepte

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rezepte.ui.theme.RezepteTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LinkStepsToInstructionsActivity: AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.extras?.getString("data")
        //if there is no data for some reason just go home
        if (data == null){
            Toast.makeText(this, "no data found", Toast.LENGTH_SHORT).show()
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }
        //convert the data back to object
        val extractedData = XmlExtraction.getData(data!!)
        //if there are no steps save and leave
        if(extractedData.data.cookingSteps.list.isEmpty()){
            finishRecipe(extractedData)
        }

        setContent {
            RezepteTheme {
                MainScreen(extractedData){data -> finishRecipe(data)}
            }
        }


    }
    private fun finishRecipe(recipe: Recipe){

        Toast.makeText(this, "Linked Steps", Toast.LENGTH_SHORT).show()

        //export saved recipe
        val data: String= parseData(recipe)
        var name = recipe.data.name
        val token = DbTokenHandling( //get token
            getSharedPreferences(
                "com.example.rezepte.dropboxintegration",
                MODE_PRIVATE
            )
        ).retrieveAccessToken()
        GlobalScope.launch {
            UploadTask(DropboxClient.getClient(token)).uploadXml(data, "/xml/$name.xml")
        }

        //move to home
        val intent = Intent(this,MainActivity::class.java)
        startActivity(intent);
    }
}

@Composable
private fun MainScreen(data: Recipe, onFinish: (Recipe) -> Unit) {
    //get local context and settings
    val mContext = LocalContext.current
    val settings =SettingsActivity.loadSettings( //todo already have this loaded
        mContext.getSharedPreferences(
            "com.example.rezepte.settings",
            ComponentActivity.MODE_PRIVATE
        )
    )

    Column (modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())){
        var stepIndex by remember {mutableStateOf(0)}
        var recipeData by remember { mutableStateOf(data)}
        var updateInstruction by remember{mutableStateOf(false)}
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)) {
            //label menu
            Text(text = "Link steps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(5.dp))
            //describe menu
            Text(
                text = "link the instructions to the steps:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(5.dp)
            )
        }
        //current step
        CookingStepDisplay(step = recipeData.data.cookingSteps.list[stepIndex] , color = getColor(stepIndex,MaterialTheme.colorScheme.surface),settings )
        //instructions left
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)) {
            for (instruction in recipeData.instructions.list) {
                //make sure this is updated
                if (updateInstruction) updateInstruction = false
                if (instruction.linkedCookingStepIndex == null || instruction.linkedCookingStepIndex == stepIndex){ //if the step has not already bean assigned
                    linkInstruction(instruction.text,getColor(instruction.linkedCookingStepIndex,MaterialTheme.colorScheme.surface) ) {
                        if (instruction.linkedCookingStepIndex == null) {
                            instruction.linkedCookingStepIndex = stepIndex
                        } else {
                            instruction.linkedCookingStepIndex = null
                        }
                        println(instruction.linkedCookingStepIndex)
                        updateInstruction = true
                    }
                }
            }
        }
        //next / finish button and reset button
        Row{
            Button(onClick = {
                stepIndex = 0
                recipeData.instructions.list.forEach { instruction -> instruction.linkedCookingStepIndex = null  }
                updateInstruction = true
            }) {
                Text(text =  "Reset")
            }
            Spacer(
                Modifier
                    .weight(1f)
            )
            Button(onClick = {
                if (stepIndex == recipeData.data.cookingSteps.list.count()-1){
                    //re save the recipe and go home
                    onFinish(recipeData)
                }
                else{
                    stepIndex += 1
                }
            }) {
                Text(text = if (stepIndex == recipeData.data.cookingSteps.list.count()-1)"Finish" else "Next")
            }
        }
    }

}
@Composable
fun linkInstruction(value: String, color : Color, onclick : () -> Unit){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onclick() }
            .padding(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = color,
        )
    ){
        Text(text = value, modifier = Modifier.padding(3.dp))
    }
}
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun linkStepsMainPeview(){
    RezepteTheme {
        MainScreen(GetEmptyRecipe()) {}
    }
}