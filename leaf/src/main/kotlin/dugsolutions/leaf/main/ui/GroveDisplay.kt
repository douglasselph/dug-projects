package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.CardStackInfo
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.ItemInfo
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.SampleDie


@Composable
fun GroveDisplay(grove: GroveInfo, onSelected: (item: ItemInfo) -> Unit = {}) {
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GroveTitle(grove)
            if (grove.dice.values.isNotEmpty()) {
                DiceDisplay(
                    dice = grove.dice,
                    elementsPerRow = grove.dice.values.size
                ) { die -> onSelected(ItemInfo.Die(die)) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GroveCards(grove, onSelected)
                if (grove.blooms.isNotEmpty()) {
                    CardsColumnDisplay(grove.blooms)
                }
            }
        }
    }
}

// region Preview

// Preview window for testing grove display
fun main() = application {
    val gatherCardInfo = GatherCardInfo.previewVariation()
    val gatherDiceInfo = GatherDiceInfo()
    val sampleDie = SampleDie()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Grove Display Preview",
        state = WindowState(
            width = 1200.dp,
            height = 1100.dp
        )
    ) {
        // Sample grove data
        val sampleGrove = GroveInfo(
            instruction = "Select for Player 1",
            quantities = "2D4 3D6 4D8 4D10 4D12 4D20",
            dice = gatherDiceInfo(
                Dice(
                    listOf(
                        sampleDie.d4,
                        sampleDie.d6,
                        sampleDie.d8,
                        sampleDie.d10,
                        sampleDie.d12,
                        sampleDie.d20
                    )
                ), values = false
            ),
            blooms = listOf(
                gatherCardInfo(card = FakeCards.bloomCard),
                gatherCardInfo(card = FakeCards.bloomCard2),
                gatherCardInfo(card = FakeCards.bloomCard3)
            ),
            stacks = listOf(
                CardStackInfo(
                    stack = MarketStackID.ROOT_1,
                    topCard = gatherCardInfo(card = FakeCards.rootCard),
                    numCards = 28
                ),
                CardStackInfo(
                    stack = MarketStackID.ROOT_2,
                    topCard = gatherCardInfo(card = FakeCards.rootCard2),
                    numCards = 28
                ),
                CardStackInfo(
                    stack = MarketStackID.CANOPY_1,
                    topCard = gatherCardInfo(card = FakeCards.canopyCard),
                    numCards = 15
                ),
                CardStackInfo(
                    stack = MarketStackID.CANOPY_2,
                    topCard = gatherCardInfo(card = FakeCards.canopyCard2),
                    numCards = 15
                ),
                CardStackInfo(
                    stack = MarketStackID.VINE_1,
                    topCard = gatherCardInfo(card = FakeCards.vineCard),
                    numCards = 42
                ),
                CardStackInfo(
                    stack = MarketStackID.VINE_2,
                    topCard = gatherCardInfo(card = FakeCards.vineCard2),
                    numCards = 42
                ),
                CardStackInfo(
                    stack = MarketStackID.FLOWER_1,
                    topCard = gatherCardInfo(
                        card = FakeCards.flowerCard,
                        highlight = HighlightInfo.SELECTABLE
                    ),
                    numCards = 20
                ),
                CardStackInfo(
                    stack = MarketStackID.FLOWER_2,
                    topCard = gatherCardInfo(
                        card = FakeCards.flowerCard2,
                        highlight = HighlightInfo.SELECTED
                    ),
                    numCards = 20
                ),
                CardStackInfo(
                    stack = MarketStackID.FLOWER_3,
                    topCard = gatherCardInfo(
                        card = FakeCards.flowerCard3
                    ),
                    numCards = 20
                ),
                CardStackInfo(
                    stack = MarketStackID.WILD_1,
                    topCard = gatherCardInfo(
                        card = FakeCards.rootCard
                    ),
                    numCards = 10
                )
            )
        )
        GroveDisplay(sampleGrove)
    }
}

// endregion Preview 
