package dugsolutions.leaf.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.main.domain.CardStackInfo
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.ItemInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun GroveCards(grove: GroveInfo, onSelected: (item: ItemInfo) -> Unit = {}) {
    val gridData = createGridFromStacks(grove.stacks)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        gridData.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { stack ->
                    Box {
                        if (stack != null) {
                            CardStackDisplay(stack) { card ->
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
private fun createGridFromStacks(stacks: List<CardStackInfo>): List<List<CardStackInfo?>> {
    // Find max row and column from order values
    val maxRow = stacks.maxOfOrNull { it.order % 10 } ?: 0
    val maxCol = stacks.maxOfOrNull { it.order / 10 } ?: 0

    // Create empty grid
    val grid = Array(maxRow) { Array<CardStackInfo?>(maxCol) { null } }

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
    val rootCard = FakeCards.rootCard
    val vineCard = FakeCards.vineCard
    val canopyCard = FakeCards.canopyCard
    val flowerCard = FakeCards.flowerCard
    val wildCard = FakeCards.rootCard2

    // Create stack infos with proper order values matching the grid layout
    val stacks = listOf(
        CardStackInfo(MarketStackID.ROOT_1, gatherCardInfo(card = rootCard), 5),
        CardStackInfo(MarketStackID.ROOT_2, gatherCardInfo(card = rootCard), 3),
        CardStackInfo(MarketStackID.VINE_1, gatherCardInfo(card = vineCard), 4),
        CardStackInfo(MarketStackID.VINE_2, gatherCardInfo(card = vineCard), 2),
        CardStackInfo(MarketStackID.CANOPY_1, gatherCardInfo(card = canopyCard), 6),
        CardStackInfo(MarketStackID.CANOPY_2, gatherCardInfo(card = canopyCard), 1),
        CardStackInfo(MarketStackID.WILD_1, gatherCardInfo(card = wildCard), 3),
        CardStackInfo(MarketStackID.WILD_2, gatherCardInfo(card = wildCard), 2),
        CardStackInfo(MarketStackID.FLOWER_1, gatherCardInfo(card = flowerCard), 0),
        CardStackInfo(MarketStackID.FLOWER_2, gatherCardInfo(card = flowerCard), 3),
        CardStackInfo(MarketStackID.FLOWER_3, gatherCardInfo(card = flowerCard), 2)
    )

    return GroveInfo(
        stacks = stacks,
        instruction = "Select a card to acquire",
        quantities = "Cards: 35, Dice: 12"
    )
}
