package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.gather.GatherCardInfo


@Composable
fun CardRowDisplay(
    cards: List<CardInfo>,
    onSelected: (card: CardInfo) -> Unit = {}
) {
    val overlapOffset: Dp = 80.dp // How much cards overlap when names match
    val normalSpacing: Dp = 8.dp // Normal spacing between different cards
    val cardWidth: Dp = 200.dp // Width of each card (from CardDisplay)

    // Calculate the total width needed
    val totalWidth = if (cards.isEmpty()) {
        cardWidth
    } else {
        var width = cardWidth // First card
        cards.drop(1).forEachIndexed { index, cardInfo ->
            val previousCard = cards[index] // Note: index is offset by drop(1)
            val shouldOverlap = cardInfo.name == previousCard.name
            width += if (shouldOverlap) overlapOffset else (cardWidth + normalSpacing)
        }
        width
    }

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
                // Check if this card should overlap with the previous one
                val shouldOverlap = index > 0 && cardInfo.name == cards[index - 1].name

                // Calculate the offset for this card
                if (index > 0) {
                    currentOffset += if (shouldOverlap) overlapOffset else (cardWidth + normalSpacing)
                }
                Box(
                    modifier = Modifier.offset(x = currentOffset)
                ) {
                    CardDisplay(cardInfo) { onSelected(cardInfo) }
                }
            }
        }
    }
}

// Preview window for testing card rows
fun main() = application {
    val gatherCardInfo = GatherCardInfo.previewVariation()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Card Row Display Preview",
        state = WindowState(
            width = 800.dp,
            height = 800.dp
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
                    gatherCardInfo(card = FakeCards.rootCard),
                    gatherCardInfo(card = FakeCards.canopyCard)
                )
            )

            // Second row of cards
            CardRowDisplay(
                listOf(
                    gatherCardInfo(card = FakeCards.vineCard),
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
