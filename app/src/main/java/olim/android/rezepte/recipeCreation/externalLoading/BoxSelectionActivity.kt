package olim.android.rezepte.recipeCreation.externalLoading

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import olim.android.rezepte.MainActivity.Companion.resources
import olim.android.rezepte.R
import olim.android.rezepte.SettingsActivity
import olim.android.rezepte.getEmptyRecipe
import olim.android.rezepte.recipeCreation.CreateActivity
import olim.android.rezepte.recipeCreation.parseData
import olim.android.rezepte.ui.theme.RezepteTheme
import kotlin.math.abs
import kotlin.math.min

class BoxSelectionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bounds: RecipeBounds? = intent.parcelable("bounds")
        val blocks: List<Block>? = intent.parcelableList("blocks")
        val uri: Uri? = intent.parcelable("image_uri")
        val imageWidth: Int? = intent.extras?.getInt("image_width")
        val imageHeight: Int? = intent.extras?.getInt("image_height")

        if (bounds != null && blocks != null)
            setContent {
                RezepteTheme {
                    MainScreen(bounds, blocks, uri, imageWidth!!, imageHeight!!)

                }
            }
    }
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

inline fun <reified T : Parcelable> Intent.parcelableList(key: String): List<T>? {
    return if (Build.VERSION.SDK_INT >= 33) {
        getParcelableArrayListExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableArrayListExtra<T>(key)
    }
}


@Composable
private fun MainScreen(
    initialBounds: RecipeBounds,
    blocks: List<Block>,
    image: Uri?,
    scannedWidth: Int,
    scannedHeight: Int
) {
    val mContext = LocalContext.current
    var bounds by remember { mutableStateOf(initialBounds) }
    var uiUpdate by remember { mutableIntStateOf(0) }
    var imageWidth: Float by remember { mutableFloatStateOf(0f) }
    var imageHeight: Float by remember { mutableFloatStateOf(0f) }
    var scale: Float by remember { mutableFloatStateOf(0f) }
    var dx: Float by remember { mutableFloatStateOf(0f) }
    var dy: Float by remember { mutableFloatStateOf(0f) }
    val displayMetrics = resources?.displayMetrics ?: return
    val textMeasurer = rememberTextMeasurer()

    //show popup about how to use box selection
    var showPopup by remember { mutableStateOf(true) }
    if (showPopup){
        ExplanationPopup(stringResource(id = R.string.box_selection_explination), "Creation.Image Loading.Box Selection Tooltip") {
            showPopup = false
        }
    }

    Scaffold(modifier = Modifier.navigationBarsPadding(), bottomBar = {
        //buttons to create new boxes
        Column {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
            ) {
                for (type in ScanBoxType.entries) {
                    Button(
                        modifier = Modifier.padding(5.dp),
                        onClick = {
                            val centerX = scannedWidth / 2
                            val centerY = scannedHeight / 2
                            val sizeX = scannedWidth / 8
                            val sizeY = scannedHeight / 8
                            initialBounds.add(
                                ScanBox(
                                    Point(centerX - sizeX, centerY - sizeY),
                                    Point(centerX + sizeX, centerY + sizeY),
                                    type, lastUpdate = ++uiUpdate
                                )

                            )

                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = type.color,
                            contentColor = Color.White,

                            )

                    ) {
                        Column {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "todo",//todo stringResource(id = R.string.search_icon_content_description),
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)

                                    .size(24.dp)
                            )
                            Text(type.name, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }
            }
            Spacer(Modifier.size(5.dp))
            Button(onClick = {
                val recipe = ImageToRecipe.boundsToRecipe(
                    bounds, blocks, settings = SettingsActivity.loadSettings(
                        mContext.getSharedPreferences(
                            "olim.android.rezepte.settings",
                            Context.MODE_PRIVATE
                        )
                    )
                )
                //if the recipe is still empty don't start create just give error
                if (recipe == getEmptyRecipe()) {
                    Toast.makeText(mContext, R.string.no_recipe_found_toast, Toast.LENGTH_SHORT)
                        .show()
                    return@Button //todo go home
                }
                //when loaded send the recipe to the create menu
                val intent = Intent(mContext, CreateActivity::class.java)

                intent.putExtra("data", parseData(recipe))

                mContext.startActivity(intent)

            }, modifier = Modifier.fillMaxWidth()) {
                Row {
                    Text(stringResource(id = R.string.box_selection_extract_text), modifier = Modifier.align(Alignment.CenterVertically))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowRightAlt,
                        contentDescription = "todo",//todo stringResource(id = R.string.search_icon_content_description),
                        modifier = Modifier
                            .size(24.dp)
                    )
                }
            }
        }
    }) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image)
                    .build(),
                contentDescription = "",
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .animateContentSize()
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary,
                        (RoundedCornerShape(5.dp))
                    ),
                contentScale = ContentScale.Fit,
                onState = {
                    it.painter?.intrinsicSize?.width?.let { width ->
                        imageWidth = width
                    }
                    it.painter?.intrinsicSize?.height?.let { height ->
                        imageHeight = height
                    }
                }
            )
            // Overlay canvas
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pos = (down.position - Offset(dx, dy)) / scale
                            var transformed = false
                            val xdpiScaled = displayMetrics.xdpi / scale
                            val ydpiScaled = displayMetrics.ydpi / scale
                            val modifyingEdge: MutableList<Edge> =
                                mutableListOf()  //list to allow corner dragging
                            var selectedBound: ScanBox? = null
                            do {
                                val event = awaitPointerEvent()
                                val touchSlop = viewConfiguration.touchSlop

                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - zoomChange) * centroidSize

                                //check if expanding box by dragging edge

                                //dragging finger
                                if (panChange.getDistance() > touchSlop || zoomMotion > touchSlop) {
                                    transformed = true
                                }
                                if (transformed && (zoomChange != 1f || panChange != Offset.Zero)) {

                                    if (selectedBound == null) {
                                        //find which bound is being modified
                                        for (bound in bounds.scanBoxesByLastUpdateDescending()) {
                                            //check if expanding box by dragging edge
                                            for (edge in Edge.entries) {
                                                if (bound.edgeContains(
                                                        pos.x,
                                                        pos.y,
                                                        edge,
                                                        xdpiScaled,
                                                        ydpiScaled
                                                    )
                                                ) {
                                                    modifyingEdge.add(edge)
                                                    selectedBound = bound
                                                }
                                            }

                                            //check if inside of box
                                            if (bound.contains(pos.x, pos.y)
                                            ) {
                                                selectedBound = bound
                                                break
                                            }
                                        }

                                    } else {
                                        //there is going to be updated ui
                                        uiUpdate++
                                        //modify select bounds instead of changing between different in same action
                                        if (modifyingEdge.isNotEmpty()) {
                                            for (edge in modifyingEdge) {
                                                if (edge == Edge.Top || edge == Edge.Bottom) {
                                                    selectedBound.expand(
                                                        edge,
                                                        panChange.y / (scale),
                                                        uiUpdate,
                                                        xdpiScaled,
                                                        ydpiScaled
                                                    )
                                                } else {
                                                    selectedBound.expand(
                                                        edge,
                                                        panChange.x / (scale),
                                                        uiUpdate,
                                                        xdpiScaled,
                                                        ydpiScaled
                                                    )
                                                }
                                            }

                                        }
                                        //else move
                                        else {
                                            selectedBound.scale(
                                                zoomChange,
                                                uiUpdate,
                                                xdpiScaled,
                                                ydpiScaled
                                            )
                                            selectedBound += panChange / scale
                                            selectedBound.lastUpdate = uiUpdate
                                        }
                                    }
                                }
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (!transformed) {
                                // Tap
                                val removed =
                                    bounds.removeDeleted(pos.x, pos.y, xdpiScaled, ydpiScaled)

                                if (removed) {
                                    uiUpdate++
                                }
                                //select box tapped to be on top
                                for (bound in bounds.scanBoxesByLastUpdateDescending()) {
                                    //check if inside of box
                                    if (bound.contains(pos.x, pos.y)
                                    ) {
                                        bound.lastUpdate = ++uiUpdate
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (uiUpdate > 0) { //force update on moving etc
                }
                val imageWidth = imageWidth
                val imageHeight = imageHeight

                scale = min(
                    imageWidth / scannedWidth,
                    imageHeight / scannedHeight
                )

                dx = (size.width - imageWidth) / 2f
                dy = (size.height - imageHeight) / 2f


                for (bound in bounds.scanBoxesByLastUpdateAscending()) {
                    drawBox(bound, bound.type.color, textMeasurer, scale, dx, dy)
                    continue
                }
            }
        }
    }
}

fun DrawScope.drawBox(
    rect: ScanBox,
    color: Color,
    textMeasurer: TextMeasurer,
    scale: Float,
    dx: Float,
    dy: Float
) {
    val topLeft = Offset(
        rect.start.x.toFloat() * scale + dx,
        rect.start.y.toFloat() * scale + dy
    )

    val size = Size(
        rect.width().toFloat() * scale,
        rect.height().toFloat() * scale
    )

    // Draw outline
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 4f)
    )

    // Measure text
    val textLayout = textMeasurer.measure(
        AnnotatedString(rect.type.name),
        style = TextStyle(
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    )
    // Label text
    drawText(
        textLayoutResult = textLayout,
        color = color,
        shadow = Shadow(
            color = Color.Black,
            offset = Offset(1f, 1f),
            blurRadius = 6f
        ),
        topLeft = Offset(
            topLeft.x,
            topLeft.y - textLayout.size.height
        )
    )

    // ---- CROSS ICON ----
    val padding = 8f
    val crossSize = 16f

    val topRight = Offset(
        topLeft.x + size.width - padding,
        topLeft.y + padding
    )

    val half = crossSize / 2f

    drawCircle(
        color = color,
        radius = 16f,
        center = topRight
    )
    drawLine(
        color = Color.White,
        start = Offset(topRight.x - half, topRight.y - half),
        end = Offset(topRight.x + half, topRight.y + half),
        strokeWidth = 3f
    )

    drawLine(
        color = Color.White,
        start = Offset(topRight.x - half, topRight.y + half),
        end = Offset(topRight.x + half, topRight.y - half),
        strokeWidth = 3f
    )

}

/**
 * Shows a popup with text and a dissmis button. if settings provided also shows button to not show again
 */
@Composable
fun ExplanationPopup(message: String, setting  : String? = null,onDismiss: () -> Unit){
    val mContext = LocalContext.current
    val preferences = mContext.getSharedPreferences(
        "olim.android.rezepte.settings",
        MODE_PRIVATE
    )
    val settings = SettingsActivity.loadSettings(
        preferences
    )
    var state by remember { mutableStateOf(settings[setting] == "false") }

    //hide if popup already disabled
    if (settings[setting] == "false"){
        onDismiss()
        return
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        // Draw a rectangle shape with rounded corners inside the dialog
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column( modifier = Modifier
                .fillMaxWidth()
                ) {
                Text(message, modifier = Modifier.padding(10.dp).align(Alignment.CenterHorizontally), textAlign = TextAlign.Center,)

                Button(onClick = {onDismiss()}, modifier = Modifier.padding(top = 10.dp).align(Alignment.CenterHorizontally)){
                    Text(stringResource(id = R.string.got_it))
                }
                if (setting != null){
                    Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Checkbox(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            checked = state,
                            onCheckedChange = { state = !state
                                SettingsActivity.saveSetting(preferences, setting, if (state) "false" else "true")
                            })
                        Text(stringResource(id = R.string.dont_show_agin),modifier = Modifier.align(Alignment.CenterVertically))
                    }

                }
            }


        }
    }
}