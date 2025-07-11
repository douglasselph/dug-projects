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
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.SampleDie
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.gather.GatherCardInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo

@Composable
fun HandDisplay(
    player: PlayerInfo,
    listeners: PlayerDisplayClickListeners = PlayerDisplayClickListeners()
) {

    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
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
                gatherCardInfo(card = FakeCards.fakeSeedling),
                gatherCardInfo(card = FakeCards.fakeRoot),
                gatherCardInfo(card = FakeCards.fakeBloom)
            ),
            handDice = gatherDiceInfo(Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10, sampleDie.d12)), true),
            supplyDice = gatherDiceInfo(Dice(emptyList()), false),
            buddingStack = emptyList(),
            nutrients = 2,
            supplyCardCount = 0,
            bedCardCount = 0,
            bedDice = gatherDiceInfo(Dice(emptyList()), false)
        )
        HandDisplay(samplePlayer)
    }
}

// endregion Preview
