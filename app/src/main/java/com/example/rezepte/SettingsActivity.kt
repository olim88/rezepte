package com.example.rezepte

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.rezepte.ui.theme.RezepteTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = loadSettings(getSharedPreferences("com.example.rezepte.settings",ComponentActivity.MODE_PRIVATE))
        setContent{
            RezepteTheme {
                MainScreen(settings)
            }
        }

    }



    companion object {
        fun loadSettings(sharedPreference: SharedPreferences) : Map<String,String> {
            var settingDictionary  = mutableMapOf<String,String> ()

            for (setting in sharedPreference.all){
                settingDictionary[setting.key] = setting.value.toString()
            }
            return if (settingDictionary.isEmpty()){//return default setting
                convertToDictionary(createSettingsMenu(),"")
            } else{
                settingDictionary
            }
        }
        fun saveSettings(sharedPreference: SharedPreferences, settings: Map<String,String>){
            
            for (setting in settings){
                sharedPreference.edit().putString(setting.key,setting.value).apply()
            }

        }
        fun convertToDictionary(settings : List<SettingOptionInterface>, start: String) : Map<String,String>{
            var settingDictionary  = mutableMapOf<String,String> ()
            for (setting in settings){
                if (setting is SettingsOptionToggle) {//if its a toggle save bool
                   settingDictionary[start+setting.name] = setting.state.value.toString()
                }
                if (setting is SettingsOptionDropDown) {//if it is a drop down save value at index
                    settingDictionary[start+setting.name] = setting.options[setting.currentOptionIndex.value]
                }
                if (setting is SettingsSubMenu) {
                    settingDictionary += convertToDictionary(setting.subSettings,start+setting.name+".")
                }
            }
            return  settingDictionary

        }

        fun loadToOptions(settings: Map<String,String>, options: List<SettingOptionInterface>, start: String): List<SettingOptionInterface>{
            var editedOptions = options.toMutableList()
            try {
                for (option in editedOptions){
                    val key = start + option.name
                    if (option is SettingsOptionToggle) {//if its a toggle save bool
                        option.state = mutableStateOf((settings[key]!! == "true"))
                    }
                    if (option is SettingsOptionDropDown) {//if it is a drop down save value at index
                        option.currentOptionIndex = mutableStateOf(option.options.indexOf(settings[key]))

                        if (option.currentOptionIndex.value == -1) option.currentOptionIndex.value = 0 //of settings has updated and value can not be found

                    }
                    if (option is SettingsSubMenu) {
                        option.subSettings = loadToOptions(settings,option.subSettings,start + option.name + ".")
                    }
                }
            } catch (e : Exception){
                //if it dose not work probably updated the settings or something just return options
                println("test3")
                return options
            }
            return  editedOptions
        }




    }
}

fun  createSettingsMenu() : List<SettingOptionInterface> { //create the layout and values for the settings menu


    return listOf(
        SettingsSubMenu("Units","",listOf(
            SettingsOptionDropDown("Weight","unit for weight", mutableStateOf(0), listOf("metric","imperial")),
            SettingsOptionDropDown("Volume","unit for volume", mutableStateOf(0), listOf("metric","imperial")),
            SettingsOptionDropDown("Size","unit for size", mutableStateOf(0), listOf("metric","imperial")),
            SettingsOptionDropDown("Temperature","unit for temperature", mutableStateOf(0), listOf("metric","imperial")),
            SettingsOptionToggle("Fractional Numbers","display measurements as fractions or decimals", mutableStateOf(true)),
            )),
        SettingsSubMenu("Creation","",listOf(
            SettingsSubMenu("Website Loading","",listOf(
                SettingsOptionToggle("Generate cooking steps","when loading a website automatically find cooking steps from the instructions", mutableStateOf(false)),
                SettingsOptionDropDown("Split instructions","when loading a website automatically split instructions into smaller parts", mutableStateOf(0),listOf("off","intelligent","sentences"))
            )),
            SettingsOptionToggle("Separate Ingredients","show each line as a different colour", mutableStateOf(true)),
            SettingsOptionToggle("Separate Instructions","show each line as a different colour", mutableStateOf(true)),
            SettingsOptionToggle("Show split Instruction Buttons","show buttons to split instructions", mutableStateOf(true)),
        )),
        SettingsOptionToggle("Search Menu View","display search menu as list", mutableStateOf(true)),
        SettingsSubMenu("Local Saves","saves data locally so they it can be loaded quicker without internet",listOf(
            SettingsOptionToggle("Cache recipes","save a copy of recipes", mutableStateOf(true)),
            SettingsOptionToggle("Cache recipe names","save a copy of names", mutableStateOf(true)),
            SettingsOptionDropDown("Cache recipe image","save a copy of images (can use up more space )", mutableStateOf(0),listOf("none","thumbnail","full sized")),
        )),
        )
}



interface SettingOptionInterface{
    val name : String
    val description: String
}
data class SettingsOptionToggle (override val name : String, override  val description: String, var state: MutableState<Boolean>) : SettingOptionInterface
data class SettingsOptionDropDown (override val name : String, override  val description: String, var currentOptionIndex : MutableState<Int>, val options: List<String>) : SettingOptionInterface

data class SettingsSubMenu (override val name : String, override  val description: String, var subSettings: List<SettingOptionInterface>) : SettingOptionInterface
@Composable
private fun settingsHeader(header: String,onclick: () -> Unit){
    Surface(
         tonalElevation = 15.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()

        ) {
        Row (modifier = Modifier.padding(15.dp)){
            Icon(Icons.Default.ArrowBack,contentDescription = "return ", modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable { onclick() })
            Spacer(modifier = Modifier.padding(10.dp))
            Text(text = header,  style = MaterialTheme.typography.titleLarge , modifier = Modifier.align(Alignment.CenterVertically)  )
        }
    }
}
@Composable
private fun settingsMenuSubMenuButton(header: String, body: String, onclick : () -> Unit){
    Row (modifier = Modifier
        .padding(5.dp)
        .fillMaxWidth()
        .clickable { onclick() }){
        Column {
            Text(text = header,style = MaterialTheme.typography.titleMedium)
            Text(text = body,style = MaterialTheme.typography.bodyMedium)
        }

    }
}
@Composable
private fun settingsMenuToggle(header: String, body: String, state: MutableState<Boolean>){
    Row (modifier = Modifier.padding(5.dp).clickable { state.value = ! state.value }){
        Column (modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(text = header,style = MaterialTheme.typography.titleMedium)
            Text(text = body,style = MaterialTheme.typography.bodyMedium)
        }

        Switch(checked = state.value,
            onCheckedChange = { state.value = ! state.value })
    }
}
@Composable
private fun SettingsMenuDropDown(header: String, body: String, index : MutableState<Int>, options: List<String>){
    var mExpanded by remember { mutableStateOf(false) }
    // Up Icon when expanded and down icon when collapsed
    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown
    Row (modifier = Modifier.padding(5.dp)){
        Column (modifier = Modifier.fillMaxWidth().weight(1f))  {
            Text(text = header,style = MaterialTheme.typography.titleMedium)
            Text(text = body,style = MaterialTheme.typography.bodyMedium)
        }
       
        Card (modifier = Modifier
            .align(Alignment.CenterVertically)
            .clickable { mExpanded = !mExpanded }) {
            Row(modifier = Modifier.padding(3.dp)) {
                Text(text = options[index.value], style = MaterialTheme.typography.bodyMedium,modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(10.dp, 0.dp, 15.dp, 0.dp))
                Icon(icon, "contentDescription",
                    Modifier
                        .padding(10.dp)
                        .size(24.dp))
                DropdownMenu(expanded = mExpanded,
                    onDismissRequest = { mExpanded = false }) {
                    for ((indexClick,option) in options.withIndex()) {
                        DropdownMenuItem(onClick = {
                            index.value = indexClick
                            mExpanded = false
                        }, text = { Text(option) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MainScreen(loadedSettings : Map<String,String>){
    // Fetching the Local Context
    val mContext = LocalContext.current
    //get the layout and data
    val allSettingsMenuData = SettingsActivity.loadToOptions(loadedSettings, createSettingsMenu(),"")
    //update ui when stack is changed
    var update by remember {mutableStateOf(true)}
    //the direction the menu is going
    var direction by remember { mutableStateOf(false)} //false left true right
    var settingsMenuStack by remember { mutableStateOf(mutableListOf(Pair("Settings",allSettingsMenuData)))}//treating the list like a stack
    //make the back gesture do the same as the back button
    BackHandler(enabled = true, onBack = {
        if (settingsMenuStack.size == 1){
            //save settings
            SettingsActivity.saveSettings(mContext.getSharedPreferences("com.example.rezepte.settings",ComponentActivity.MODE_PRIVATE),SettingsActivity.convertToDictionary(settingsMenuStack.last().second,""))
            //go to main menu
            val intent = Intent(mContext,MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            mContext.startActivity(intent)

        }
        else {
            settingsMenuStack.removeAt(settingsMenuStack.size - 1) //pop
            update = true
            direction = false
        }
        })

    Column {
        if (update) {
        }  //make sure ui is updated
        //set the header and when the back arrow on the header is pressed either move up in the settings or save the settings and exit
        settingsHeader(settingsMenuStack.last().first) {
            if (settingsMenuStack.size == 1){
                //save settings
                SettingsActivity.saveSettings(mContext.getSharedPreferences("com.example.rezepte.settings",ComponentActivity.MODE_PRIVATE),SettingsActivity.convertToDictionary(settingsMenuStack.last().second,""))
                //go to main menu
                val intent = Intent(mContext,MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                mContext.startActivity(intent)

            }
            else{
                settingsMenuStack.removeAt(settingsMenuStack.size-1) //pop
                update = true
                direction = false
            }
            }
        //show inputs related to the current settings
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()


        ) {
            AnimatedContent(targetState = settingsMenuStack.last().first, label = "",
                transitionSpec = {
                    if (direction){
                        slideInHorizontally { width -> +width }  with
                                slideOutHorizontally { width -> -width }
                    }
                    else{
                        slideInHorizontally { width -> -width }  with
                                slideOutHorizontally { width -> +width }
                    }
                    }
            ) {
                Column {
                    for (menu in settingsMenuStack.last().second) {//peek

                        if (menu is SettingsOptionToggle) {//if its a toggle show a toggle
                            settingsMenuToggle(
                                header = menu.name,
                                body = menu.description,
                                state = menu.state
                            )
                        }
                        if (menu is SettingsOptionDropDown) {//if it is a drop down show a dropdown option
                            SettingsMenuDropDown(
                                header = menu.name,
                                body = menu.description,
                                index = menu.currentOptionIndex,
                                options = menu.options
                            )
                        }
                        if (menu is SettingsSubMenu) {
                            settingsMenuSubMenuButton(header = menu.name, body = menu.description) {
                                //when clicked add to the stack
                                settingsMenuStack.add(Pair(menu.name, menu.subSettings))
                                update = true
                                direction = true
                            }
                        }

                    }
                }
            }
            update = false
        }
    }

}
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun settingsPreview(){
    RezepteTheme {
        MainScreen(mapOf())
    }
}
@SuppressLint("UnrememberedMutableState")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun settingsTogglePreview(){
    settingsMenuToggle("test","this is the test", mutableStateOf(false))
}
@SuppressLint("UnrememberedMutableState")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun settingsDropDownPreview(){
    SettingsMenuDropDown("test","this is the test", mutableStateOf(0),listOf("option1","option2","option3"))
}



