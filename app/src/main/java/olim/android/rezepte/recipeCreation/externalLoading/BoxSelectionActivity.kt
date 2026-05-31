package olim.android.rezepte.recipeCreation.externalLoading

import android.content.Context
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
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import coil.compose.AsyncImage
import coil.request.ImageRequest
import olim.android.rezepte.R
import olim.android.rezepte.SettingsActivity
import olim.android.rezepte.getEmptyRecipe
import olim.android.rezepte.recipeCreation.CreateActivity
import olim.android.rezepte.recipeCreation.parseData
import olim.android.rezepte.ui.theme.RezepteTheme
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

    Scaffold(modifier = Modifier.navigationBarsPadding(), bottomBar = {
        //buttons to create new boxes
        Column {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()).background(MaterialTheme.colorScheme.onTertiary)) {
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
                                    type, uiUpdate
                                )

                            )
                            uiUpdate++
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
                val recipe = ImageToRecipe.boundsToRecipe(bounds,blocks, settings = SettingsActivity.loadSettings(
                    mContext.getSharedPreferences(
                        "olim.android.rezepte.settings",
                        Context.MODE_PRIVATE
                    )))
                //if the recipe is still empty don't start create just give error
                if (recipe == getEmptyRecipe()) {
                    Toast.makeText(mContext, R.string.no_recipe_found_toast, Toast.LENGTH_SHORT)
                        .show()
                    return@Button //todo go home
                }
                //when loaded send the recipe to the create menu
                val intent = Intent(mContext, CreateActivity::class.java)

                intent.putExtra("data", parseData(recipe))
                //intent.putExtra("imageData",recipe.second) add image
                mContext.startActivity(intent)

            }, modifier = Modifier.fillMaxWidth()) {
                Row {
                    Text("Extract Text", modifier = Modifier.align(Alignment.CenterVertically))
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
                            var transformed = false
                            do {
                                val event = awaitPointerEvent()
                                val touchSlop = viewConfiguration.touchSlop

                                val zoomChange = event.calculateZoom()
                                val rotationChange = event.calculateRotation()
                                val panChange = event.calculatePan()
                                val center = event.calculateCentroid(useCurrent = false)


                                //dragging finger
                                if (panChange.getDistance() > touchSlop || zoomChange > touchSlop) {
                                    transformed = true
                                }
                                if (transformed && (zoomChange != 1f || panChange != Offset.Zero)) {

                                    for (bound in bounds.scanBoxesByLastUpdate()) {
                                        val relativeX = (center.x - dx) / (scale);
                                        val relativeY = (center.y - dy) / (scale)
                                        //check if expanding box by dragging edge
                                        val oldState = uiUpdate;
                                        if (bound.edgeContains(relativeX, relativeY, Edge.Top)) {
                                            bound.expand(Edge.Top, panChange.y / (scale),uiUpdate)
                                            uiUpdate++

                                        }
                                        if (bound.edgeContains(relativeX, relativeY, Edge.Bottom)) {
                                            bound.expand(Edge.Bottom, panChange.y / (scale),uiUpdate)
                                            uiUpdate++

                                        }
                                        if (bound.edgeContains(relativeX, relativeY, Edge.Left)) {
                                            bound.expand(Edge.Left, panChange.x / (scale),uiUpdate)
                                            uiUpdate++

                                        }
                                        if (bound.edgeContains(relativeX, relativeY, Edge.Right)) {
                                            bound.expand(Edge.Right, panChange.x / (scale),uiUpdate)
                                            uiUpdate++

                                        }
                                        //don't move box if expanding edges
                                        if (oldState != uiUpdate) {
                                            break
                                        }

                                        //check if inside of box
                                        if (bound.contains(
                                                relativeX,
                                                relativeY
                                            )
                                        ) {

                                            //todo zoom as well


                                            bound += panChange / scale
                                            bound.lastUpdate = uiUpdate
                                            //bound += pan / scale
                                            uiUpdate++


                                            break;
                                        }
                                    }


                                }


                                // Apply pan
                                //offset += pan


                                // Apply zoom
                                //scale *= zoom
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            } while (event.changes.fastAny { it.pressed })

                            if (!transformed) {
                                // Tap
                                val pos = (down.position - Offset(dx, dy)) / scale
                                println(pos)
                                val removed = bounds.removeDeleted(pos.x, pos.y)

                                if (removed) {
                                    uiUpdate++
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


                for (bound in bounds.scanBoxesByLastUpdate()) {
                    drawBox(bound, bound.type.color, scale, dx, dy)
                    continue


                }


            }
        }
    }


}

fun DrawScope.drawBox(
    rect: ScanBox,
    color: Color,
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