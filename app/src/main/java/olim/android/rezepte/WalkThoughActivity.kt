package olim.android.rezepte

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import olim.android.rezepte.fileManagment.dropbox.DbTokenHandling
import olim.android.rezepte.recipeMaking.CookingStepDisplay
import olim.android.rezepte.recipeMaking.getColor
import olim.android.rezepte.ui.theme.RezepteTheme
import olim.android.rezepte.R

class WalkThoughActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //get the users settings


        setContent {
            RezepteTheme {
                MainScreen()
            }
        }

    }


}

@Composable
fun StartUpExplanationPage(title: String, explanation: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        //title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(10.dp))
        //logo
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = stringResource(id = R.string.logo_description),
            contentScale = ContentScale.Inside,
            modifier = Modifier
        )
        Spacer(modifier = Modifier.weight(1f))
        //description
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center

        )
        Spacer(modifier = Modifier.weight(5f))
    }
}

@Composable
fun StartUpDropboxPage(title: String, description: String) {
    // Fetching the Local Context
    val mContext = LocalContext.current
    //get current login status
    val login = DbTokenHandling(
        mContext.getSharedPreferences(
            "olim.android.rezepte.dropboxintegration",
            Context.MODE_PRIVATE
        )
    )
    var isLoggedIn by remember { mutableStateOf(login.isLoggedIn()) }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        //title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(10.dp))
        //description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center

        )
        Spacer(modifier = Modifier.weight(1f))
        //show the dropbox login options
        AnimatedContent(
            targetState = isLoggedIn, label = "",
            transitionSpec = {

                fadeIn() + expandIn() togetherWith
                        fadeOut() + shrinkOut()

            },
        ) {
            if (!it) {
                MainLoginUi {
                    isLoggedIn = true
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)

                ) {
                    Text(
                        text = stringResource(id = R.string.dropbox_logged_in_message),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.CenterHorizontally)

                    )
                }
            }
        }




        Spacer(modifier = Modifier.weight(5f))
    }
}


@Composable
fun StartUpSettingsPage(
    currentSettings: List<SettingOptionInterface>,
    title: String,
    description: String,
    options: List<String>
) {

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {

        //title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(10.dp))
        //description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center

        )
        Spacer(modifier = Modifier.weight(1f))
        //show the settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)

        ) {
            for (setting in options) {//find the option interface in all settings for the named option
                var currentSubSettingsList = currentSettings
                val parts = setting.split(".")
                var foundSetting: SettingOptionInterface? = null
                for (part in parts) {//going though each part of the setting name to locate the setting
                    for (possiblePath in currentSubSettingsList) {
                        if (possiblePath.name == part && possiblePath is SettingsSubMenu) {
                            currentSubSettingsList = possiblePath.subSettings
                        } else if (possiblePath.name == part) {
                            foundSetting = possiblePath
                            break
                        }
                    }
                }
                if (foundSetting != null) {//setting is found now render it
                    if (foundSetting is SettingsOptionToggle) {//if its a toggle show a toggle
                        SettingsMenuToggle(
                            header = foundSetting.name,
                            body = foundSetting.description,
                            state = foundSetting.state
                        )
                    }
                    if (foundSetting is SettingsOptionDropDown) {//if it is a drop down show a dropdown option
                        SettingsMenuDropDown(
                            header = foundSetting.name,
                            body = foundSetting.description,
                            index = foundSetting.currentOptionIndex,
                            options = foundSetting.options
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(5f))

        Text(
            text = stringResource(id = R.string.settings_changeable_later_note),
            style = MaterialTheme.typography.labelSmall
        )

    }


}

@Composable
fun FinishedConfirmation(completed: List<Int>, onDismiss: () -> Unit) {
    var isFinished by remember { mutableStateOf(completed.count() >= CurrentScreen.values().size - 1) }
    //if the user is not finished ask the user if they are sure they want to skip the setup and  tell the user they can go though it again in the settings page
    if (!isFinished) {
        Dialog(onDismissRequest = { onDismiss() }) {
            // Draw a rectangle shape with rounded corners inside the dialog
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = stringResource(id = R.string.finish_confirmation_dialog_title),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        //camara button
                        Button(
                            onClick = {
                                onDismiss()
                            }, modifier = Modifier
                                .padding(0.dp, 5.dp)
                                .weight(1f)
                        ) {
                            Text(text = stringResource(R.string.common_back), textAlign = TextAlign.Center)

                        }
                        Spacer(modifier = Modifier.weight(0.2f))
                        //file button
                        Button(
                            onClick = {
                                isFinished = true
                            }, modifier = Modifier
                                .padding(0.dp, 5.dp)
                                .weight(1f)
                        ) {
                            Text(text = stringResource(R.string.common_confirm), textAlign = TextAlign.Center)

                        }
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.finish_confirmation_restart_note),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
    //save an go home if they are finished or have said they are finished
    if (isFinished) {
        Finish()
    }
}

@Composable
private fun Finish() {
    // Fetching the Local Context
    val mContext = LocalContext.current
    //finished the setup save this value so not shown again and take the user to the home screen
    val prefs =
        mContext.getSharedPreferences(
            "olim.android.rezepte.walkThrough",
            Context.MODE_PRIVATE
        )
    prefs.edit().putBoolean("completed", true).apply()
    //finish the walk though and go back to the activity before
    (mContext as Activity).finish()
}

@Composable
fun StartUpHomePage(
    donePages: MutableList<Int>,
    goToPage: (CurrentScreen) -> Unit,
    onFinish: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        //title
        Text(
            text = stringResource(id = R.string.startup_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(5.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        //logo
        Image(
            painter = painterResource(id = R.drawable.icon),
            contentDescription = stringResource(id = R.string.logo_description),
            contentScale = ContentScale.Inside,
            modifier = Modifier
        )
        Spacer(modifier = Modifier.height(10.dp))
        //description
        Text(
            text = stringResource(id = R.string.startup_welcome_explanation),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(5.dp)
        )
        //list of screens highlighted if they are done
        Spacer(modifier = Modifier.weight(1f))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)

        ) {
            for (page in CurrentScreen.values()
                .sortedBy { screen -> screen.ordinal }) {
                if (page != CurrentScreen.HomePage && page != CurrentScreen.FinishPage) { //do not show home page in list or finish as it have separate button
                    val colour =
                        if (page.ordinal in donePages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    Button(
                        onClick = {
                            goToPage(page)

                        }, modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colour)
                    ) {
                        Text(
                            text = page.text(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.weight(0.1f))
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        //show finish button
        val isFinished = donePages.count() >= CurrentScreen.values().size - 1
        val colour =
            if (isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        Button(
            onClick = {
                onFinish()
            }, modifier = if (isFinished) Modifier
                .padding(5.dp)
                .fillMaxWidth() else Modifier.padding(5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colour)
        ) {
            Text(
                text = CurrentScreen.FinishPage.text(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = if (isFinished) Modifier.padding(vertical = 10.dp) else Modifier
            )
        }

        Spacer(modifier = Modifier.weight(5f))
    }
}

@Composable
fun StartUpStepExample(title: String, description: String) {
    // Fetching the Local Context
    val mContext = LocalContext.current
    //get settings
    val settings = SettingsActivity.loadSettings(
        mContext.getSharedPreferences(
            "olim.android.rezepte.settings",
            AppCompatActivity.MODE_PRIVATE
        )
    )
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        //title
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(10.dp))
        //description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center

        )
        Spacer(modifier = Modifier.weight(1f))
        //show the example step
        val exampleStepData = CookingStep(
            0, "20 minutes", CookingStage.oven,
            CookingStepContainer(TinOrPanOptions.tray, null, null, null),
            CookingStepTemperature(200, HobOption.zero, true)
        )
        CookingStepDisplay(
            step = exampleStepData,
            color = getColor(0, MaterialTheme.colorScheme.surface),
            settings
        )
        Text(
            text = "or",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center

        )
        val exampleStepData2 = CookingStep(
            0, "5-10 minutes", CookingStage.hob,
            CookingStepContainer(TinOrPanOptions.saucePan, null, null, null),
            CookingStepTemperature(null, HobOption.highMedium, true)
        )
        CookingStepDisplay(
            step = exampleStepData2,
            color = getColor(1, MaterialTheme.colorScheme.surface),
            settings
        )
        Spacer(modifier = Modifier.weight(5f))
    }
}


@Composable
private fun MainScreen() {
    // Fetching the Local Context
    val mContext = LocalContext.current
    var currentScreen by remember { mutableStateOf(CurrentScreen.HomePage) }
    var animationDirection by remember { (mutableStateOf(false)) }
    val donePages = mutableListOf(0)//homepage is already done
    var finishConfirmation by remember { mutableStateOf(false) }
    //get settings
    val settings = SettingsActivity.loadSettings(
        mContext.getSharedPreferences(
            "olim.android.rezepte.settings",
            AppCompatActivity.MODE_PRIVATE
        )
    )
    //get the layout and data
    val allSettingsMenuData by remember {
        mutableStateOf(
            SettingsActivity.loadToOptions(
                settings,
                createSettingsMenu(),
                ""
            )
        )
    }
    //make the back gesture do the same as the back button
    BackHandler(enabled = true, onBack = {
        //save the settings
        SettingsActivity.saveSettings(
            mContext.getSharedPreferences(
                "olim.android.rezepte.settings",
                Context.MODE_PRIVATE
            ), SettingsActivity.convertToDictionary(allSettingsMenuData, "")
        )
        //set animation direction
        animationDirection = false
        //set next page

        currentScreen = CurrentScreen.HomePage
    })
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            //render pages
            AnimatedContent(
                targetState = currentScreen, label = "",
                transitionSpec = {
                    if (animationDirection) {
                        slideInHorizontally { width -> +width } togetherWith
                                slideOutHorizontally { width -> -width }
                    } else {
                        slideInHorizontally { width -> -width } togetherWith
                                slideOutHorizontally { width -> +width }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) { targetState ->
                when (targetState) {
                    CurrentScreen.HomePage -> {
                        //if on home page render that
                        StartUpHomePage(
                            donePages = donePages,
                            goToPage = {
                                currentScreen = it
                                animationDirection = true
                            },
                            onFinish = {
                                finishConfirmation = true
                            }
                        )
                    }

                    CurrentScreen.UnitsPage -> {
                        StartUpSettingsPage(
                            allSettingsMenuData,
                            title = stringResource(R.string.startup_units_title),
                            description = stringResource(R.string.startup_units_description),
                            options = listOf(
                                "Units.Temperature",
                                "Units.metric Volume",
                                "Units.metric Weight",
                                "Units.metric Lengths",
                            ),
                        )
                    }

                    CurrentScreen.UnitValuesPage -> {
                        StartUpSettingsPage(
                            allSettingsMenuData,
                            title = stringResource(R.string.startup_conversions_title),
                            description = stringResource(R.string.startup_conversions_description),
                            options = listOf(
                                "Units.Conversions.Teaspoon volume",
                                "Units.Conversions.Tablespoon volume",
                                "Units.Conversions.Cup volume",
                                "Units.Conversions.Fl oz volume",
                                "Units.Conversions.Pint volume",
                            ),
                        )
                    }

                    CurrentScreen.FinishPage -> {
                        StartUpExplanationPage(
                            title = stringResource(R.string.startup_finished_title),
                            explanation = stringResource(R.string.startup_finished_explanation)
                        )
                    }

                    CurrentScreen.DropboxPage -> {
                        StartUpDropboxPage(
                            title = stringResource(R.string.startup_dropbox_title),
                            description = stringResource(R.string.startup_dropbox_description) + "\n" + stringResource(
                                R.string.startup_dropbox_login_detail_instructions)
                        )
                    }

                    CurrentScreen.ReadabilityPage -> {
                        StartUpSettingsPage(
                            allSettingsMenuData,
                            title = stringResource(R.string.startup_formatting_title),
                            description = stringResource(R.string.startup_formatting_description),
                            options = listOf(
                                "Creation.Separate Ingredients",
                                "Creation.Separate Instructions",
                                "Making.Walk Though Ingredients",
                                "Making.Walk Though Instructions",
                            ),
                        )
                    }

                    CurrentScreen.ExplainingStepsPage -> {
                        StartUpStepExample(
                            title = stringResource(R.string.startup_steps_title),
                            description = stringResource(R.string.startup_steps_description)
                            )
                    }

                    CurrentScreen.AdvancedPage -> {
                        StartUpSettingsPage(
                            allSettingsMenuData,
                            title = stringResource(R.string.startup_advanced_title),
                            description = stringResource(R.string.startup_advanced_description),
                            options = listOf(
                                "Search menu.Search Menu List",
                                "Units.Fractional Numbers",
                                "Units.Show Conversions",
                                "Creation.show split Instruction Buttons",//todo local saves dependend on if they are logged into dropbox
                            ),
                        )
                    }
                }
            }

            //control buttons
            AnimatedVisibility(
                visible = currentScreen != CurrentScreen.HomePage, label = "",
                enter = expandVertically(initialHeight = { -it }, expandFrom = Alignment.Bottom),
                exit = shrinkVertically(targetHeight = { -it }, shrinkTowards = Alignment.Bottom),
            ) {
                Row {
                    //back button
                    Button(
                        onClick = {
                            //save the settings
                            SettingsActivity.saveSettings(
                                mContext.getSharedPreferences(
                                    "olim.android.rezepte.settings",
                                    Context.MODE_PRIVATE
                                ), SettingsActivity.convertToDictionary(allSettingsMenuData, "")
                            )
                            //set animation direction
                            animationDirection = false
                            //set next page

                            currentScreen = CurrentScreen.HomePage


                        }, modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth()
                            .weight(1f)

                    ) {
                        Text(text = stringResource(R.string.common_back), textAlign = TextAlign.Center)
                        if (!donePages.contains(currentScreen.ordinal)) {
                            donePages.add(currentScreen.ordinal)
                        }
                    }
                    Spacer(modifier = Modifier.weight(0.1f))
                    //finish button
                    Button(
                        onClick = {
                            //save the settings
                            SettingsActivity.saveSettings(
                                mContext.getSharedPreferences(
                                    "olim.android.rezepte.settings",
                                    Context.MODE_PRIVATE
                                ), SettingsActivity.convertToDictionary(allSettingsMenuData, "")
                            )
                            //set animation direction
                            animationDirection = true
                            //set next page
                            if (currentScreen.ordinal == CurrentScreen.values().size - 1) {
                                finishConfirmation = true
                            } else {
                                if (!donePages.contains(currentScreen.ordinal)) {
                                    donePages.add(currentScreen.ordinal)
                                }
                                currentScreen =
                                    if (currentScreen.ordinal == CurrentScreen.values().size - 2 && donePages.count() >= CurrentScreen.values().size - 1) {
                                        (CurrentScreen from currentScreen.ordinal + 1)!!
                                    } else if (currentScreen.ordinal == CurrentScreen.values().size - 2) {
                                        //if the user is not finished take them back to the home page
                                        CurrentScreen.HomePage
                                    } else {
                                        (CurrentScreen from currentScreen.ordinal + 1)!!
                                    }
                            }

                        }, modifier = Modifier
                            .padding(5.dp)
                            .fillMaxWidth()
                            .weight(1f)

                    ) {
                        Text(text = stringResource(R.string.common_done), textAlign = TextAlign.Center)
                    }

                }

            }
        }
        if (finishConfirmation) {//run the finished confirmation if its set to true
            FinishedConfirmation(donePages) {
                finishConfirmation = false
            }

        }
    }
}

enum class CurrentScreen(@StringRes val textResId: Int) { //this is in the order they get listed to the user
    HomePage(R.string.screen_home_page),
    DropboxPage(R.string.screen_online_syncing),
    UnitsPage(R.string.screen_unit_setup),
    UnitValuesPage(R.string.screen_conversions_setup),
    ExplainingStepsPage(R.string.screen_enhanced_steps),
    ReadabilityPage(R.string.screen_interface_options),
    AdvancedPage(R.string.screen_advanced_options),
    FinishPage(R.string.finish); // Assuming R.string.screen_finish is defined

    @Composable
    fun text(): String = stringResource(id = textResId)

    companion object {
        infix fun from(value: Int): CurrentScreen? = values().firstOrNull { it.ordinal == value }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun WalkThoughPreview() {
    RezepteTheme {
        MainScreen()
    }
}