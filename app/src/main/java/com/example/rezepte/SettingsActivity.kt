package com.example.rezepte

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
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
        setContent{
            RezepteTheme {
                MainScreen()
            }
        }

    }



    companion object {
        fun loadSettings(sharedPreference: SharedPreferences) : UserSettings {

            if (sharedPreference.contains("metric")) {//load settings if there are settings save else return default settings
                val metric = sharedPreference.getBoolean("metric", false)
                val cookingStepGenerationPreference = CookingStepGenerationPreference.valueOf(sharedPreference.getString("cookingStepGenerationPreference","")!!)


                return UserSettings(metric, cookingStepGenerationPreference)
            }
            else{
                return UserSettings.newSettings()
            }
        }
        fun saveSettings(sharedPreference: SharedPreferences, settings: UserSettings){
            sharedPreference.edit().putBoolean("metric",settings.metric).apply()
            sharedPreference.edit().putString("cookingStepGenerationPreference",settings.cookingStepGenerationPreference.toString()).apply()

            sharedPreference.edit().apply()
        }

        data class UserSettings ( val metric: Boolean, val cookingStepGenerationPreference : CookingStepGenerationPreference){
            companion object {
                fun newSettings(): UserSettings{ //return user settings with default values

                    return UserSettings(true, CookingStepGenerationPreference.Button)
                }
            }
        }
        enum class CookingStepGenerationPreference{
            Off,
            Button,
            Automatic,

        }
    }
}

fun  createSettingsMenu() : List<SettingOptionInterface> { //create the layout and values for the settings menu

    var visibleSettings : MutableList<SettingOptionInterface> = mutableListOf()
    visibleSettings.add(SettingsOptionToggle("test","testing", mutableStateOf(false)))
    visibleSettings.add(SettingsOptionDropDown("test","testing", mutableStateOf(0), listOf("option1","option2","option3")))
    visibleSettings.add(SettingsOptionToggle("test","testing", mutableStateOf(false)))
    visibleSettings.add(SettingsSubMenu("test","",listOf(SettingsOptionToggle("test","testing", mutableStateOf(false)))))
    return visibleSettings
}



interface SettingOptionInterface{
    val name : String
    val description: String
}
data class SettingsOptionToggle (override val name : String,override  val description: String, val state: MutableState<Boolean>) : SettingOptionInterface
data class SettingsOptionDropDown (override val name : String,override  val description: String, val currentOptionIndex : MutableState<Int>, val options: List<String>) : SettingOptionInterface

data class SettingsSubMenu (override val name : String,override  val description: String, val subSettings: List<SettingOptionInterface>) : SettingOptionInterface
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
    Row (modifier = Modifier.padding(5.dp)){
        Column {
            Text(text = header,style = MaterialTheme.typography.titleMedium)
            Text(text = body,style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = state.value,
            onCheckedChange = { state.value = ! state.value })
    }
}
@Composable
private fun settingsMenuDropDown(header: String, body: String,index : MutableState<Int>, options: List<String>){
    var mExpanded by remember { mutableStateOf(false) }
    // Up Icon when expanded and down icon when collapsed
    val icon = if (mExpanded)
        Icons.Filled.KeyboardArrowUp
    else
        Icons.Filled.KeyboardArrowDown
    Row (modifier = Modifier.padding(5.dp)){
        Column {
            Text(text = header,style = MaterialTheme.typography.titleMedium)
            Text(text = body,style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
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
private fun MainScreen(){
    // Fetching the Local Context
    val mContext = LocalContext.current
    //get the layout and data
    val allSettingsMenuData = createSettingsMenu()
    var update by remember {mutableStateOf(true)}
    //the direction the menu is going
    var direction by remember { mutableStateOf(false)} //false left true right
    var settingsMenuStack by remember { mutableStateOf(mutableListOf(Pair("Settings",allSettingsMenuData)))}//treating the list like a stack
    Column {
        if (update) {
        }  //make sure ui is updated
        settingsHeader(settingsMenuStack.last().first) {
            if (settingsMenuStack.size == 1){
                val intent = Intent(mContext,MainActivity::class.java) //todo fix e.g. finish or somthing like that
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
                            settingsMenuDropDown(
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
                                println(settingsMenuStack)
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
        MainScreen()
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
    settingsMenuDropDown("test","this is the test", mutableStateOf(0),listOf("option1","option2","option3"))
}



