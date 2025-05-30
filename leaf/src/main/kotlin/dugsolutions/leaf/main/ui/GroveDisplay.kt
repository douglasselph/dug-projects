package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.StackInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun GroveDisplay(grove: GroveInfo) {
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
            // Grove title
            Text(
                text = "Grove",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Stacks in rows of 3
            grove.stacks.chunked(3).forEach { rowStacks ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowStacks.forEach { stack ->
                        Box {
                            StackInfoDisplay(stack)
                        }
                    }
                }
            }
        }
    }
}

// region Preview

// Preview window for testing grove display
fun main() = application {
    val gatherCardInfo = GatherCardInfo()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Grove Display Preview",
        state = WindowState(
            width = 1200.dp,
            height = 800.dp
        )
    ) {
        // Sample grove data
        val sampleGrove = GroveInfo(
            stacks = listOf(
                StackInfo(
                    name = "Root Stack",
                    topCard = gatherCardInfo(
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
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 28
                ),
                StackInfo(
                    name = "Canopy Stack",
                    topCard = gatherCardInfo(
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
                    ),
                    numCards = 15
                ),
                StackInfo(
                    name = "Vine Stack",
                    topCard = gatherCardInfo(
                        GameCard(
                            id = 1,
                            name = "Long Vine",
                            type = FlourishType.VINE,
                            resilience = 2,
                            cost = Cost(emptyList()),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 1,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 42
                ),
                StackInfo(
                    name = "Empty Stack",
                    topCard = null,
                    numCards = 0
                )
            )
        )

        GroveDisplay(sampleGrove)
    }
}

// endregion Preview 
