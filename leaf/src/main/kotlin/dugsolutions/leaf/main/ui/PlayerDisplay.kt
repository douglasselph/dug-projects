package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.GatherCardInfo

@Composable
fun PlayerDisplay(player: PlayerInfo) {
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player name
            Text(
                text = player.name,
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Hand section
            Column {
                Text(
                    text = "Hand",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cards in hand
                    Box(modifier = Modifier.weight(3f)) {
                        CardRowDisplay(player.handCards)
                    }
                    // Dice in hand
                    Box(modifier = Modifier.weight(1f)) {
                        DiceDisplay(player.handDice)
                    }
                }
            }

            // Floral array (only if not empty)
            if (player.floralArray.isNotEmpty()) {
                Column {
                    Text(
                        text = "Floral Array",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CardRowDisplay(player.floralArray)
                }
            }

            // Supply and Compost sections
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Supply section
                Box(modifier = Modifier.weight(1f)) {
                    SectionDisplay(
                        title = "Supply",
                        cardCount = player.supplyCardCount,
                        dice = player.supplyDice
                    )
                }

                // Compost section
                Box(modifier = Modifier.weight(1f)) {
                    SectionDisplay(
                        title = "Compost",
                        cardCount = player.compostCardCount,
                        dice = player.compostDice
                    )
                }
            }
        }
    }
}

// region Preview

// Preview window for testing player display
fun playerDisplayMain() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Player Display Preview",
        state = WindowState(
            width = 1200.dp,
            height = 800.dp
        )
    ) {
        val gatherCardInfo = GatherCardInfo()
        
        // Sample player data
        val samplePlayer = PlayerInfo(
            name = "Player 1",
            handCards = listOf(
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
                        matchEffect = null,
                        matchValue = 0,
                        trashEffect = null,
                        trashValue = 0,
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
                        trashEffect = null,
                        trashValue = 0,
                        thorn = 0
                    )
                )
            ),
            handDice = DiceInfo(listOf("D6", "D8", "D10")),
            supplyDice = DiceInfo(listOf("D4", "D6", "D8")),
            floralArray = listOf(
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
            ),
            supplyCardCount = 42,
            compostCardCount = 7,
            compostDice = DiceInfo(listOf("D4", "D4"))
        )

        PlayerDisplay(samplePlayer)
    }
}

// endregion Preview
