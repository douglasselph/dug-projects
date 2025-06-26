package dugsolutions.leaf.main.ui


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.gather.GatherCardInfo
import org.jetbrains.skia.Image
import java.io.File
import java.nio.file.Paths


@Composable
fun CardImageDisplay(
    imageName: String,
    displayWidth: Dp = 350.dp,
    highlight: HighlightInfo = HighlightInfo.NONE,
    onError: (String) -> Unit = {},
    onSelected: () -> Unit = {},
) {
    val bgColor = when (highlight) {
        HighlightInfo.SELECTABLE -> Colors.SelectableColor
        HighlightInfo.SELECTED -> Colors.SelectedColor
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val imagePath = remember(imageName) {
        val currentDir = System.getProperty("user.dir")
        Paths.get(currentDir, Commons.IMAGES_DIR, imageName).toString()
    }
    val imageFile = remember(imagePath) { File(imagePath) }
    val bitmap = remember(imagePath) { loadImageBitmapFromFile(imageFile) }
    val aspectRatio = bitmap?.width?.toFloat()?.div(bitmap.height) ?: (258f / 356f)
    val displayHeight = displayWidth / aspectRatio

    if (bitmap == null) {
        onError("Image file not found or failed to decode: $imagePath")
        return
    }

    Box(
        modifier = Modifier
            .size(width = displayWidth, height = displayHeight)
            .background(bgColor)
            .clickable { onSelected() }
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Card image: $imageName",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

private fun loadImageBitmapFromFile(imageFile: File): ImageBitmap? {
    return try {
        val bytes = imageFile.readBytes()
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}

// Preview window for testing card image display
fun main() = application {
    val gatherCardInfo = GatherCardInfo.previewVariation()
    var errorMessage by remember { mutableStateOf("") }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Card Image Display Preview",
        state = WindowState(
            width = 800.dp,
            height = 1200.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Test with FakeCards.seedlingCard (should work if image exists)
            Text(
                text = "Testing with FakeCards.seedlingCard (should work):",
                style = MaterialTheme.typography.h6
            )
            CardImageDisplay(
                imageName = gatherCardInfo(card = FakeCards.seedlingCard).image ?: "seedling_cheap_sprout.png",
                onError = { error ->
                    errorMessage = error
                },
                onSelected = { println("Selected: FakeCards.seedlingCard") }
            )
            CardImageDisplay(
                imageName = gatherCardInfo(card = FakeCards.seedlingCard).image ?: "seedling_cheap_sprout.png",
                onError = { error ->
                    errorMessage = error
                },
                highlight = HighlightInfo.SELECTABLE,
                onSelected = { println("Selected: FakeCards.seedlingCard") }
            )

            // Test with FakeCards.seedlingCard2 (should fail - no image file)
            Text(
                text = "Testing with FakeCards.seedlingCard2 (should fail):",
                style = MaterialTheme.typography.h6
            )
            CardImageDisplay(
                imageName = gatherCardInfo(card = FakeCards.seedlingCard2).image ?: "nonexistent_image.png",
                onError = { error ->
                    errorMessage = error
                },
                onSelected = { println("Selected: FakeCards.seedlingCard2") }
            )
        }

        // Error dialog
        if (errorMessage.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                title = { Text("Image Loading Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(onClick = { errorMessage = "" }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
