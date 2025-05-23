package olim.rezepte.recipeCreation

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import olim.rezepte.MainActivity
import olim.rezepte.R
import olim.rezepte.Recipe
import olim.rezepte.SettingsActivity
import olim.rezepte.XmlExtraction
import olim.rezepte.fileManagment.FileSync
import olim.rezepte.getEmptyRecipe
import olim.rezepte.recipeMaking.CookingStepDisplay
import olim.rezepte.recipeMaking.getColor
import olim.rezepte.ui.theme.RezepteTheme

class LinkStepsToInstructionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent.extras?.getString("data")
        //if there is no data for some reason just go home
        if (data == null) {
            Toast.makeText(this, R.string.no_data_found_toast, Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        //convert the data back to object
        val extractedData = XmlExtraction.getData(data!!)
        //if there are no steps save and leave
        if (extractedData.data.cookingSteps.list.isEmpty()) {
            finishRecipe(extractedData)
        }

        setContent {
            RezepteTheme {
                MainScreen(extractedData) { data -> finishRecipe(data) }
            }
        }
    }

    private fun finishRecipe(recipe: Recipe) {
        Toast.makeText(this, R.string.linked_steps_title, Toast.LENGTH_SHORT).show()

        //export saved recipe
        val data: String = parseData(recipe)
        val name = recipe.data.name

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
        CoroutineScope(Dispatchers.IO).launch {
            //load the file data
            val priority =
                if (settings["Local Saves.Cache recipe names"] == "true") FileSync.FilePriority.None else FileSync.FilePriority.OnlineOnly
            val uploadData = FileSync.Data(priority, dropboxPreference)
            val xmlSaveFile =
                FileSync.FileInfo(
                    "/xml/",
                    "${this@LinkStepsToInstructionsActivity.filesDir}/xml/",
                    "$name.xml"
                )
            FileSync.uploadString(uploadData, xmlSaveFile, data) {}
        }

        //move to home
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent);
    }
}

@Composable
private fun MainScreen(data: Recipe, onFinish: (Recipe) -> Unit) {
    //get local context and settings
    val mContext = LocalContext.current
    val settings = SettingsActivity.loadSettings(
        mContext.getSharedPreferences(
            "olim.rezepte.settings",
            ComponentActivity.MODE_PRIVATE
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        var stepIndex by remember { mutableStateOf(0) }
        var recipeData by remember { mutableStateOf(data) }
        var updateInstruction by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            //label menu
            Text(
                text = stringResource(R.string.linked_steps_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(5.dp)
            )
            //describe menu
            Text(
                text = stringResource(R.string.linked_steps_message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(5.dp)
            )
        }
        //current step
        CookingStepDisplay(
            step = recipeData.data.cookingSteps.list[stepIndex],
            color = getColor(stepIndex, MaterialTheme.colorScheme.surface),
            settings
        )
        //instructions left
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp)
        ) {
            for (instruction in recipeData.instructions.list) {
                //make sure this is updated
                if (updateInstruction) updateInstruction = false
                if (instruction.linkedCookingStepIndex == null || instruction.linkedCookingStepIndex == stepIndex) { //if the step has not already bean assigned
                    linkInstruction(
                        instruction.text,
                        getColor(
                            instruction.linkedCookingStepIndex,
                            MaterialTheme.colorScheme.surface
                        )
                    ) {
                        if (instruction.linkedCookingStepIndex == null) {
                            instruction.linkedCookingStepIndex = stepIndex
                        } else {
                            instruction.linkedCookingStepIndex = null
                        }
                        updateInstruction = true
                    }
                }
            }
        }
        //next / finish button and reset button
        Row {
            Button(onClick = {
                stepIndex = 0
                recipeData.instructions.list.forEach { instruction ->
                    instruction.linkedCookingStepIndex = null
                }
                updateInstruction = true
            }) {
                Text(text = stringResource(R.string.button_reset))
            }
            Spacer(
                Modifier
                    .weight(1f)
            )
            Button(onClick = {
                if (stepIndex == recipeData.data.cookingSteps.list.count() - 1) {
                    //re save the recipe and go home
                    onFinish(recipeData)
                } else {
                    stepIndex += 1
                }
            }) {
                Text(text = if (stepIndex == recipeData.data.cookingSteps.list.count() - 1) stringResource(R.string.finish) else stringResource(R.string.button_next))
            }
        }
    }
}

@Composable
fun linkInstruction(value: String, color: Color, onclick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onclick() }
            .padding(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = color,
        )
    ) {
        Text(text = value, modifier = Modifier.padding(3.dp))
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun LinkStepsMainPreview() {
    RezepteTheme {
        MainScreen(getEmptyRecipe()) {}
    }
}