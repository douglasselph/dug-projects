package dugsolutions.leaf.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.ItemInfo
import dugsolutions.leaf.main.domain.StackInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun GroveCards(grove: GroveInfo, onSelected: (item: ItemInfo) -> Unit = {}) {
    val gridData = createGridFromStacks(grove.stacks)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridData.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { stack ->
                    Box {
                        if (stack != null) {
                            StackInfoDisplay(stack) { card ->
                                onSelected(ItemInfo.Card(card))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Creates a 2D grid structure from the list of stacks based on their order property.
 * Order format: 10s digit = column (1-3), 1s digit = row (1-4)
 * Returns a list of rows, where each row contains nullable StackInfo for each column
 */
private fun createGridFromStacks(stacks: List<StackInfo>): List<List<StackInfo?>> {
    // Find max row and column from order values
    val maxRow = stacks.maxOfOrNull { it.order % 10 } ?: 0
    val maxCol = stacks.maxOfOrNull { it.order / 10 } ?: 0

    // Create empty grid
    val grid = Array(maxRow) { Array<StackInfo?>(maxCol) { null } }

    // Fill grid based on order values
    stacks.forEach { stack ->
        val column = (stack.order / 10) - 1  // Convert to 0-based index
        val row = (stack.order % 10) - 1     // Convert to 0-based index
        if (row in 0..<maxRow && column >= 0 && column < maxCol) {
            grid[row][column] = stack
        }
    }

    // Convert to list of lists for easier iteration
    return grid.map { it.toList() }
}

// region Preview

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Grove Cards Grid Preview",
        state = WindowState(
            width = 1200.dp,
            height = 800.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Test with sample grove data
            GroveCards(createSampleGroveInfo())
        }
    }
}

@Composable
private fun createSampleGroveInfo(): GroveInfo {
    val gatherCardInfo = GatherCardInfo()

    // Create sample cards for different types
    val rootCard = GameCard(
        id = 1,
        name = "Ancient Root",
        type = FlourishType.ROOT,
        resilience = 3,
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

    val vineCard = GameCard(
        id = 2,
        name = "Climbing Vine",
        type = FlourishType.VINE,
        resilience = 2,
        cost = Cost(emptyList()),
        primaryEffect = CardEffect.GAIN_FREE_ROOT,
        primaryValue = 1,
        matchWith = MatchWith.None,
        matchEffect = null,
        matchValue = 0,
        trashEffect = null,
        trashValue = 0,
        thorn = 1
    )

    val canopyCard = GameCard(
        id = 3,
        name = "Broad Canopy",
        type = FlourishType.CANOPY,
        resilience = 4,
        cost = Cost(emptyList()),
        primaryEffect = CardEffect.ADD_TO_TOTAL,
        primaryValue = 2,
        matchWith = MatchWith.None,
        matchEffect = null,
        matchValue = 0,
        trashEffect = null,
        trashValue = 0,
        thorn = 0
    )

    val flowerCard = GameCard(
        id = 4,
        name = "Bright Blossom",
        type = FlourishType.FLOWER,
        resilience = 1,
        cost = Cost(emptyList()),
        primaryEffect = CardEffect.ADD_TO_TOTAL,
        primaryValue = 3,
        matchWith = MatchWith.None,
        matchEffect = null,
        matchValue = 0,
        trashEffect = null,
        trashValue = 0,
        thorn = 0
    )

    val wildCard = GameCard(
        id = 5,
        name = "Wild Growth",
        type = FlourishType.ROOT,
        resilience = 2,
        cost = Cost(emptyList()),
        primaryEffect = CardEffect.ADD_TO_DIE,
        primaryValue = 2,
        matchWith = MatchWith.None,
        matchEffect = null,
        matchValue = 0,
        trashEffect = null,
        trashValue = 0,
        thorn = 0
    )

    // Create stack infos with proper order values matching the grid layout
    val stacks = listOf(
        StackInfo(MarketStackID.ROOT_1, gatherCardInfo(card = rootCard), 5),
        StackInfo(MarketStackID.ROOT_2, gatherCardInfo(card = rootCard), 3),
        StackInfo(MarketStackID.VINE_1, gatherCardInfo(card = vineCard), 4),
        StackInfo(MarketStackID.VINE_2, gatherCardInfo(card = vineCard), 2),
        StackInfo(MarketStackID.CANOPY_1, gatherCardInfo(card = canopyCard), 6),
        StackInfo(MarketStackID.CANOPY_2, gatherCardInfo(card = canopyCard), 1),
        StackInfo(MarketStackID.WILD_1, gatherCardInfo(card = wildCard), 3),
        StackInfo(MarketStackID.WILD_2, gatherCardInfo(card = wildCard), 2),
        StackInfo(MarketStackID.FLOWER_1, gatherCardInfo(card = flowerCard), 0),
        StackInfo(MarketStackID.FLOWER_2, gatherCardInfo(card = flowerCard), 3),
        StackInfo(MarketStackID.FLOWER_3, gatherCardInfo(card = flowerCard), 2)
    )

    return GroveInfo(
        stacks = stacks,
        instruction = "Select a card to acquire",
        quantities = "Cards: 35, Dice: 12"
    )
}
