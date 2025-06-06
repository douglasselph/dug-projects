package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.gather.GatherCardInfo


@Composable
fun CardRowDisplay(cards: List<CardInfo>) {
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cards.forEach { cardInfo ->
                CardDisplay(cardInfo)
            }
        }
    }
}

// Preview window for testing card rows
fun main() = application {
    val gatherCardInfo = GatherCardInfo()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Card Row Display Preview",
        state = WindowState(
            width = 800.dp,
            height = 500.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First row of cards
            CardRowDisplay(
                listOf(
                    gatherCardInfo(
                        GameCard(
                            id = 1,
                            name = "Sprouting Seed",
                            type = FlourishType.SEEDLING,
                            resilience = 2,
                            cost = Cost(emptyList()),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = CardEffect.REDUCE_COST_ROOT,
                            matchValue = 2,
                            trashEffect = CardEffect.GAIN_FREE_ROOT,
                            trashValue = 1,
                            thorn = 0
                        )
                    ),
                    gatherCardInfo(
                        GameCard(
                            id = 2,
                            name = "Nourishing Root",
                            type = FlourishType.ROOT,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.SingleDieMinimum(2))),
                            primaryEffect = CardEffect.DRAW_DIE,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = CardEffect.RETAIN_DIE,
                            trashValue = 1,
                            thorn = 0
                        )
                    ),
                    gatherCardInfo(
                        GameCard(
                            id = 3,
                            name = "Sheltering Canopy",
                            type = FlourishType.CANOPY,
                            resilience = 4,
                            cost = Cost(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT))),
                            primaryEffect = CardEffect.DEFLECT,
                            primaryValue = 2,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    )
                )
            )

            // Second row of cards
            CardRowDisplay(
                listOf(
                    gatherCardInfo(
                        GameCard(
                            id = 4,
                            name = "Thorny Vine",
                            type = FlourishType.VINE,
                            resilience = 2,
                            cost = Cost(emptyList()),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = CardEffect.REUSE_DIE,
                            trashValue = 1,
                            thorn = 2
                        )
                    ),
                    gatherCardInfo(
                        GameCard(
                            id = 5,
                            name = "Spring Flower",
                            type = FlourishType.FLOWER,
                            resilience = 1,
                            cost = Cost(emptyList()),
                            primaryEffect = CardEffect.ADORN,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = CardEffect.RESILIENCE_BOOST,
                            trashValue = 5,
                            thorn = 0
                        )
                    ),
                    gatherCardInfo(
                        GameCard(
                            id = 6,
                            name = "Spring Bloom",
                            type = FlourishType.BLOOM,
                            resilience = 1,
                            cost = Cost(emptyList()),
                            primaryEffect = CardEffect.ADD_TO_TOTAL,
                            primaryValue = 3,
                            matchWith = MatchWith.Flower(5),
                            matchEffect = CardEffect.ADD_TO_TOTAL,
                            matchValue = 1,
                            trashEffect = CardEffect.DEFLECT,
                            trashValue = 2,
                            thorn = 0
                        )
                    )
                )
            )
        }
    }
}
