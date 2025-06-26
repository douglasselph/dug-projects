package dugsolutions.leaf.main.ui


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.common.Commons
import java.io.File
import java.nio.file.Paths

@Composable
fun CardImageDisplay(
    imageName: String,
    onError: (String) -> Unit = {},
    onSelected: () -> Unit = {}
) {
    val cardWidth: Dp = 200.dp

    // Construct the path to the image file
    val imagePath = remember(imageName) {
        val currentDir = System.getProperty("user.dir")
        Paths.get(currentDir, Commons.IMAGES_DIR, imageName).toString()
    }
    val imageFile = remember(imagePath) {
        File(imagePath)
    }

    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(4.dp)
            .clickable { onSelected() }
    ) {
        if (!imageFile.exists()) {
            onError("Image file not found: $imagePath")
        } else {
            // Load and display the actual image using native Compose approach
            // This will throw an exception if the image cannot be loaded
            Image(
                painter = painterResource(imagePath),
                contentDescription = "Card image: $imageName",
                modifier = Modifier
                    .width(cardWidth)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )
        }
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
            height = 600.dp
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
