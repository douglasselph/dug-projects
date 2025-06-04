package dugsolutions.leaf.main.ui


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo

@Composable
fun HandDisplay(
    player: PlayerInfo,
    listeners: PlayerDisplayClickListeners = PlayerDisplayClickListeners()
) {

    Surface(
        border = BorderStroke(1.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Hand",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Cards in hand
                Box {
                    CardRowDisplay(player.handCards) { cardInfo ->
                        listeners.onHandCardSelected(cardInfo)
                    }
                }
                // Dice in hand
                Box {
                    DiceDisplay(player.handDice) { dieValue ->
                        listeners.onDieSelected(dieValue)
                    }
                }
            }
        }
    }
}

// region Preview

// Preview window for testing hand display
fun main() = application {
    val gatherDiceInfo = GatherDiceInfo()
    val sampleDie = SampleDie()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Hand Display Preview",
        state = WindowState(
            width = 700.dp,
            height = 600.dp
        )
    ) {
        val gatherCardInfo = GatherCardInfo()
        val infoLine = PlayerScore(1, scoreDice = 10, scoreCards = 15).toString()
        // Sample player data focused on hand content
        val samplePlayer = PlayerInfo(
            name = "Player 1",
            infoLine = infoLine,
            handCards = listOf(
                gatherCardInfo(
                    incoming = GameCard(
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
                    incoming = GameCard(
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
                gatherCardInfo(
                    incoming = GameCard(
                        id = 3,
                        name = "Vibrant Bloom",
                        type = FlourishType.BLOOM,
                        resilience = 1,
                        cost = Cost(listOf(CostElement.SingleDieMinimum(1))),
                        primaryEffect = CardEffect.GAIN_FREE_ROOT,
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
            handDice = gatherDiceInfo(Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10, sampleDie.d12)), true),
            supplyDice = gatherDiceInfo(Dice(emptyList()), false),
            floralArray = emptyList(),
            supplyCardCount = 0,
            compostCardCount = 0,
            compostDice = gatherDiceInfo(Dice(emptyList()), false)
        )
        HandDisplay(samplePlayer)
    }
}

// endregion Preview
