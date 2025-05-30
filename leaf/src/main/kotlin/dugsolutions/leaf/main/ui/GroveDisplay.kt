package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.StackInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun GroveDisplay(grove: GroveInfo, onCardSelected: (cardInfo: CardInfo) -> Unit = {}) {
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
            // Grove title and selection instruction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grove",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                grove.selectText?.let { text ->
                    Surface(
                        color = Color(0xFFFFF9C4),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "Select card: $text",
                            style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Stacks in rows of 3
            reorder(grove.stacks).chunked(3).forEach { rowStacks ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowStacks.forEach { stack ->
                        Box {
                            StackInfoDisplay(stack) { card ->
                                onCardSelected(card)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun reorder(stacks: List<StackInfo>): List<StackInfo> {
    // First, separate stacks into their types
    val rootStacks = stacks.filter { it.stack == MarketStackID.ROOT_1 || it.stack == MarketStackID.ROOT_2 }
    val vineStacks = stacks.filter { it.stack == MarketStackID.VINE_1 || it.stack == MarketStackID.VINE_2 }
    val canopyStacks = stacks.filter { it.stack == MarketStackID.CANOPY_1 || it.stack == MarketStackID.CANOPY_2 }
    val otherStacks = stacks.filter {
        it.stack != MarketStackID.ROOT_1 &&
                it.stack != MarketStackID.ROOT_2 &&
                it.stack != MarketStackID.VINE_1 &&
                it.stack != MarketStackID.VINE_2 &&
                it.stack != MarketStackID.CANOPY_1 &&
                it.stack != MarketStackID.CANOPY_2
    }

    // Create triplets of ROOT, VINE, CANOPY
    val reorderedStacks = mutableListOf<StackInfo>()
    val tripletCount = minOf(rootStacks.size, vineStacks.size, canopyStacks.size)

    for (i in 0 until tripletCount) {
        reorderedStacks.add(rootStacks[i])
        reorderedStacks.add(vineStacks[i])
        reorderedStacks.add(canopyStacks[i])
    }

    // Add remaining stacks in their original order
    reorderedStacks.addAll(otherStacks)

    return reorderedStacks
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
            height = 1100.dp
        )
    ) {
        // Sample grove data
        val sampleGrove = GroveInfo(
            selectText = "Select for Player 1",
            stacks = listOf(
                StackInfo(
                    stack = MarketStackID.ROOT_1,
                    topCard = gatherCardInfo(
                        GameCard(
                            id = 2,
                            name = "Nourishing Root",
                            type = FlourishType.ROOT,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.TotalDiceMinimum(2))),
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
                    stack = MarketStackID.ROOT_2,
                    topCard = gatherCardInfo(
                        GameCard(
                            id = 2,
                            name = "Nourishing Root",
                            type = FlourishType.ROOT,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.TotalDiceMinimum(2))),
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
                    stack = MarketStackID.CANOPY_1,
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
                    stack = MarketStackID.CANOPY_2,
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
                    stack = MarketStackID.VINE_1,
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
                    stack = MarketStackID.VINE_2,
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
                    stack = MarketStackID.FLOWER_1,
                    highlight = true,
                    topCard = gatherCardInfo(
                        GameCard(
                            id = 4,
                            name = "Blooming Flower",
                            type = FlourishType.FLOWER,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.TotalDiceMinimum(1))),
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
                    numCards = 20
                ),
                StackInfo(
                    stack = MarketStackID.FLOWER_2,
                    topCard = gatherCardInfo(
                        GameCard(
                            id = 4,
                            name = "Blooming Flower",
                            type = FlourishType.FLOWER,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.SingleDieMinimum(1))),
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
                    numCards = 20
                ),
                StackInfo(
                    stack = MarketStackID.FLOWER_3,
                    topCard = gatherCardInfo(
                        GameCard(
                            id = 4,
                            name = "Blooming Flower",
                            type = FlourishType.FLOWER,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.SingleDieMinimum(1))),
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
                    numCards = 20
                ),
                StackInfo(
                    stack = MarketStackID.JOINT_RCV,
                    topCard = gatherCardInfo(
                        GameCard(
                            id = 5,
                            name = "Wild Growth",
                            type = FlourishType.ROOT,
                            resilience = 3,
                            cost = Cost(listOf(CostElement.SingleDieMinimum(3))),
                            primaryEffect = CardEffect.DRAW_CARD,
                            primaryValue = 2,
                            matchWith = MatchWith.None,
                            matchEffect = null,
                            matchValue = 0,
                            trashEffect = null,
                            trashValue = 0,
                            thorn = 0
                        )
                    ),
                    numCards = 10
                )
            )
        )
        GroveDisplay(sampleGrove)
    }
}

// endregion Preview 
