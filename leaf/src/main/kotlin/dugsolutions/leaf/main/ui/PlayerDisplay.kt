package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
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
import androidx.compose.material.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.domain.ImagePath
import dugsolutions.leaf.chronicle.domain.PlayerScore
import dugsolutions.leaf.common.AppStrings
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
    val onNutrientsClicked: () -> Unit = {},
    val onDecidingToggled: () -> Unit = {},
    val onError: (msg: String) -> Unit = {}
)

@Composable
fun PlayerDisplay(
    player: PlayerInfo,
    actionDomain: MainActionDomain,
    showExtended: Boolean = false,
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
                // Player name and info
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
                        maxLines = 5
                    )
                }
                if (showDrawCount) {
                    DrawCountDecisionDisplay { value -> listeners.onDrawCountChosen(value) }
                } else if (showExtended) {
                    HandDisplay(
                        player,
                        okayToShowImages = true,
                        listeners
                    )
                }
                // Floral array (only if not empty)
                if (showExtended && player.floralArray.isNotEmpty()) {
                    Column {
                        Text(
                            text = AppStrings.FLORAL_ARRAY,
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CardRowDisplay(player.floralArray) { cardInfo ->
                            listeners.onFloralCardSelected(cardInfo)
                        }
                    }
                }
            }
        }

        // Control buttons positioned in the upper right corner
        Row(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Human/Robot toggle control
            val iconName = if (player.decidingPlayer) {
                "ic_human.png"
            } else {
                "ic_robot.png"
            }
            val imagePath = remember(iconName) { ImagePath.icon(iconName) }
            val imageSize = 48.dp
            val iconSize = 55.dp
            IconButton(
                onClick = { listeners.onDecidingToggled() },
                modifier = Modifier.size(iconSize)
            ) {
                Surface(
                    color = MaterialTheme.colors.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    ImageDisplay(
                        imagePath = imagePath,
                        displayWidth = imageSize,
                        displayHeight = imageSize,
                        onError = { error -> listeners.onError(error) }
                    )
                }
            }

            // Nutrients button (only if nutrients > 0)
            if (player.nutrients > 0) {
                Button(
                    onClick = { listeners.onNutrientsClicked() }
                ) {
                    Text(player.nutrients.toString())
                }
            }
        }
    }
}

// region Preview

// Preview window for testing player display
fun main() = application {
    val gatherDiceInfo = GatherDiceInfo()
    val sampleDie = SampleDie()
    var errorMessage by remember { mutableStateOf("") }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Player Display Preview",
        state = WindowState(
            width = 1100.dp,
            height = 1100.dp
        )
    ) {
        val gatherCardInfo = GatherCardInfo.previewVariation()
        val infoLine = PlayerScore(1, scoreDice = 10, scoreCards = 15).toString()
        // Sample player data
        var samplePlayer by remember {
            mutableStateOf(
                PlayerInfo(
                    name = "Player 1",
                    infoLine = infoLine,
                    nutrients = 5,
                    handCards = listOf(
                        gatherCardInfo(
                            card = FakeCards.seedlingCard
                        ),
                        gatherCardInfo(
                            card = FakeCards.seedlingCard3
                        )
                    ),
                    handDice = gatherDiceInfo(Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10)), true),
                    supplyDice = gatherDiceInfo(Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d12)), false),
                    floralArray = listOf(
                        gatherCardInfo(
                            card = FakeCards.flowerCard
                        )
                    ),
                    supplyCardCount = 42,
                    discardCardCount = 7,
                    discardDice = gatherDiceInfo(Dice(listOf(sampleDie.d4, sampleDie.d4)), false),
                    decidingPlayer = true
                )
            )
        }
        val actionDomain = MainActionDomain()
        val listeners = PlayerDisplayClickListeners(
            onError = { error -> errorMessage = error },
            onDecidingToggled = { samplePlayer = samplePlayer.copy(decidingPlayer = !samplePlayer.decidingPlayer) }
        )

        var samplePlayer2 by remember {
            mutableStateOf(
                PlayerInfo(
                    name = "Player 1",
                    infoLine = infoLine,
                    nutrients = 5,
                    handCards = listOf(
                        gatherCardInfo(
                            card = FakeCards.seedlingCard
                        ),
                        gatherCardInfo(
                            card = FakeCards.seedlingCard3
                        )
                    ),
                    handDice = gatherDiceInfo(Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d10)), true),
                    supplyDice = gatherDiceInfo(Dice(listOf(sampleDie.d4, sampleDie.d6, sampleDie.d12)), false),
                    floralArray = listOf(
                        gatherCardInfo(
                            card = FakeCards.flowerCard
                        )
                    ),
                    supplyCardCount = 42,
                    discardCardCount = 7,
                    discardDice = gatherDiceInfo(Dice(listOf(sampleDie.d4, sampleDie.d4)), false),
                    decidingPlayer = true
                )
            )
        }
        val listeners2 = PlayerDisplayClickListeners(
            onError = { error -> errorMessage = error },
            onDecidingToggled = { samplePlayer2 = samplePlayer2.copy(decidingPlayer = !samplePlayer2.decidingPlayer) }
        )

        Row {
            PlayerDisplay(
                samplePlayer,
                actionDomain,
                showExtended = true,
                listeners = listeners
            )
            PlayerDisplay(
                samplePlayer2,
                actionDomain,
                listeners = listeners2
            )
        }

        // Error dialog
        if (errorMessage.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { errorMessage = "" },
                title = { Text("Image Loading Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(onClick = { errorMessage = "" }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// endregion Preview
