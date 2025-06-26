package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun CardRowDisplay(
    cards: List<CardInfo>,
    okayToShowImages: Boolean = false,
    onSelected: (card: CardInfo) -> Unit = {}
) {
    val overlapOffset: Dp = 80.dp // How much cards overlap when names match
    val normalSpacing: Dp = 8.dp // Normal spacing between different cards
    val textCardWidth: Dp = 200.dp // Width of each text card
    val imageCardWidth: Dp = 350.dp // Width of each image card

    var errorMessage by remember { mutableStateOf("") }

    // Calculate the total width needed
    val totalWidth = computeTotalWidth(
        cards, textCardWidth, imageCardWidth, normalSpacing, overlapOffset, okayToShowImages
    )
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Box(
            modifier = Modifier.padding(8.dp)
                .width(totalWidth)
        ) {
            var currentOffset = 0.dp

            cards.forEachIndexed { index, cardInfo ->
                val cardKey = cardInfo.name + (cardInfo.image ?: "")
                var showImage by remember(cardKey) { mutableStateOf(true) }
                // Check if this card should overlap with the previous one
                val shouldOverlap = index > 0 && cardInfo.name == cards[index - 1].name

                // Calculate the offset for this card
                if (index > 0 && shouldOverlap) {
                    currentOffset -= overlapOffset
                }
                Box(
                    modifier = Modifier.offset(x = currentOffset)
                ) {
                    if (okayToShowImages && cardInfo.image != null && showImage) {
                        CardImageDisplay(
                            imageName = cardInfo.image,
                            displayWidth = imageCardWidth,
                            highlight = cardInfo.highlight,
                            onError = { error ->
                                errorMessage = error
                                showImage = false
                            },
                            onSelected = { onSelected(cardInfo) }
                        )
                    }
                    if (!okayToShowImages || cardInfo.image == null || !showImage) {
                        CardTextDisplay(
                            cardInfo,
                            displayWidth = textCardWidth
                        ) {
                            onSelected(cardInfo)
                        }
                        currentOffset += textCardWidth + normalSpacing
                    } else {
                        currentOffset += imageCardWidth + normalSpacing - overlapOffset
                    }
                }
            }
        }
    }
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

private fun computeTotalWidth(
    cards: List<CardInfo>,
    textCardWidth: Dp,
    imageCardWidth: Dp,
    normalSpacing: Dp,
    overlapOffset: Dp,
    okayToShowImages: Boolean
): Dp {
    val estimatedImagePadding = 0.dp
    val useImageWidth = imageCardWidth + estimatedImagePadding
    if (cards.isEmpty()) return textCardWidth
    var width = if (okayToShowImages && cards[0].image != null) useImageWidth else textCardWidth
    for (i in 1 until cards.size) {
        val prev = cards[i - 1]
        val curr = cards[i]
        val currWidth = if (okayToShowImages && curr.image != null) useImageWidth else textCardWidth
        val shouldOverlap = curr.name == prev.name
        width += if (shouldOverlap) overlapOffset else (currWidth + normalSpacing)
    }
    return width
}


// Preview window for testing card rows
fun main() = application {
    val gatherCardInfo = GatherCardInfo.previewVariation()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Card Row Display Preview",
        state = WindowState(
            width = 1000.dp,
            height = 1000.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First row of cards
            CardRowDisplay(
                listOf(
                    gatherCardInfo(card = FakeCards.seedlingCard),
                    gatherCardInfo(card = FakeCards.seedlingCard3).copy(highlight = HighlightInfo.SELECTABLE),
                    gatherCardInfo(card = FakeCards.bloomCard)
                ),
                okayToShowImages = true
            )

            // Second row of cards
            CardRowDisplay(
                listOf(
                    gatherCardInfo(card = FakeCards.vineCard).copy(highlight = HighlightInfo.SELECTABLE),
                    gatherCardInfo(card = FakeCards.flowerCard),
                    gatherCardInfo(card = FakeCards.bloomCard)
                )
            )

            // Third row of cards
            CardRowDisplay(
                listOf(
                    gatherCardInfo(card = FakeCards.flowerCard2),
                    gatherCardInfo(card = FakeCards.flowerCard3),
                    gatherCardInfo(card = FakeCards.flowerCard)
                )
            )
        }
    }
}
