package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.CardStackInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun CardStackDisplay(
    stack: CardStackInfo,
    onSelected: (card: CardInfo) -> Unit = {}
) {
    val emptyStackWidth: Dp = 160.dp
    val emptyStackHeight: Dp = 100.dp

    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stack name
            Text(
                text = stack.name,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Card and count display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top card if it exists
                if (stack.topCard == null || stack.numCards == 0) {
                    // Grey box representing empty stack
                    Box(
                        modifier = Modifier
                            .width(emptyStackWidth)
                            .height(emptyStackHeight)
                            .background(
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                } else {
                    Box {
                        CardDisplay(stack.topCard) {
                            onSelected(stack.topCard)
                        }
                    }
                }

                // Card count
                Surface(
                    border = BorderStroke(1.dp, MaterialTheme.colors.primary),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stack.numCards.toString(),
                            style = MaterialTheme.typography.h4
                        )
                        Text(
                            text = "Cards",
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            }
        }
    }
}

// region Preview

// Preview window for testing stack info display
fun main() = application {
    val gatherCardInfo = GatherCardInfo()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Stack Info Display Preview",
        state = WindowState(
            width = 800.dp,
            height = 600.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // First example - Stack with card
            val stackWithCard = CardStackInfo(
                stack = MarketStackID.ROOT_1,
                topCard = gatherCardInfo(card = FakeCards.fakeRoot2),
                numCards = 42
            )
            CardStackDisplay(stackWithCard)

            // Second example - Stack with card
            val stackWithCard2 = CardStackInfo(
                stack = MarketStackID.ROOT_1,
                topCard = gatherCardInfo(
                    card = FakeCards.fakeRoot,
                    highlight = HighlightInfo.SELECTABLE
                ),
                numCards = 12
            )
            CardStackDisplay(stackWithCard2)

            // First example - Stack with card
            val stackEmpty = CardStackInfo(
                stack = MarketStackID.ROOT_1,
                topCard = gatherCardInfo(card = FakeCards.fakeRoot),
                numCards = 0
            )
            CardStackDisplay(stackEmpty)

        }
    }
}

// endregion Preview 
