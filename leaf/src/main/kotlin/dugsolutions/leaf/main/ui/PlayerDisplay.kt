package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.cost.CostElement
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.MainActionDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo

data class PlayerDisplayClickListeners(
    val onDrawCountChosen: (value: Int) -> Unit = {},
    val onHandCardSelected: (value: CardInfo) -> Unit = {},
    val onFloralCardSelected: (value: CardInfo) -> Unit = {},
    val onDieSelected: (value: DieInfo) -> Unit = {},
    val onNutrientsClicked: () -> Unit = {}
)

@Composable
fun PlayerDisplay(
    player: PlayerInfo,
    actionDomain: MainActionDomain,
    listeners: PlayerDisplayClickListeners = PlayerDisplayClickListeners()
) {
    val showDrawCount = remember(actionDomain) {
        player.name == actionDomain.drawCountForPlayerName
    }
    Box {
        Surface(
            border = BorderStroke(2.dp, MaterialTheme.colors.primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Player name and score
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.h5
                    )
                    Text(
                        text = player.infoLine,
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(start = 16.dp),
                        maxLines = 2
                    )
                }

                if (showDrawCount) {
                    DrawCountDecisionDisplay { value -> listeners.onDrawCountChosen(value) }
                } else {
                    HandDisplay(player, listeners)
                }

                // Floral array (only if not empty)
                if (player.floralArray.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Budding Stack",
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CardRowDisplay(player.floralArray) { cardInfo ->
                            listeners.onFloralCardSelected(cardInfo)
                        }
                    }
                }

                // Supply and Bed sections
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Supply section
                    Box {
                        SectionDisplay(
                            title = "Supply",
                            cardCount = player.supplyCardCount,
                            dice = player.supplyDice
                        )
                    }

                    // Compost section
                    Box {
                        SectionDisplay(
                            title = "Discard Patch",
                            cardCount = player.discardCardCount,
                            dice = player.discardDice
                        )
                    }
                }
            }
        }
        
        // Nutrients button positioned in the upper right corner
        if (player.nutrients > 0) {
            Button(
                onClick = { listeners.onNutrientsClicked() },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text(player.nutrients.toString())
            }
        }
    }
}

// region Preview

// Preview window for testing player display
fun main() = application {
    val gatherDiceInfo = GatherDiceInfo()
    val sampleDie = SampleDie()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Player Display Preview",
        state = WindowState(
            width = 800.dp,
            height = 1000.dp
        )
    ) {
        val gatherCardInfo = GatherCardInfo.previewVariation()
        val infoLine = PlayerScore(1, scoreDice = 10, scoreCards = 15).toString()
        // Sample player data
        val samplePlayer = PlayerInfo(
            name = "Player 1",
            infoLine = infoLine,
            nutrients = 5,
            handCards = listOf(
                gatherCardInfo(
                    card = GameCard(
                        id = 1,
                        name = "Sprouting Seed",
                        type = FlourishType.SEEDLING,
                        resilience = 0,
                        nutrient = 0,
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
                    card = GameCard(
                        id = 2,
                        name = "Nourishing Root",
                        type = FlourishType.ROOT,
                        resilience = 8,
                        nutrient = 1,
                        cost = Cost.from(listOf(CostElement.SingleDieMinimum(2))),
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
            handDice = gatherDiceInfo(Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10)), true),
            supplyDice = gatherDiceInfo(Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d12)), false),
            floralArray = listOf(
                gatherCardInfo(
                    card = GameCard(
                        id = 3,
                        name = "Sheltering Canopy",
                        type = FlourishType.CANOPY,
                        resilience = 20,
                        nutrient = 3,
                        cost = Cost.from(listOf(CostElement.FlourishTypePresent(FlourishType.ROOT))),
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
            discardCardCount = 7,
            discardDice = gatherDiceInfo(Dice(listOf(sampleDie.d4, sampleDie.d4)), false)
        )
        val actionDomain = MainActionDomain()
        PlayerDisplay(samplePlayer, actionDomain)
    }
}

// endregion Preview
