package dugsolutions.leaf.main.ui


import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun CardImageDisplay(
    imageName: String,
    onError: (String) -> Unit = {},
    onSelected: () -> Unit = {},
    highlight: HighlightInfo = HighlightInfo.NONE
) {
    val cardWidth: Dp = 200.dp
    val borderColor = when (highlight) {
        HighlightInfo.SELECTABLE -> Colors.SelectableColor
        HighlightInfo.SELECTED -> Colors.SelectedColor
        else -> MaterialTheme.colors.primary
    }
    val borderShape: Shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .border(width = 2.dp, color = borderColor, shape = borderShape)
            .clickable { onSelected() }
    ) {
        ImageDisplay(
            imageName = imageName,
            cardWidth = cardWidth,
            onError = onError
        )
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
