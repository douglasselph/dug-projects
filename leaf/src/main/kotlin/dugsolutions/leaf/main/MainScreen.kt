package dugsolutions.leaf.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.ItemInfo
import dugsolutions.leaf.main.domain.MainActionDomain
import dugsolutions.leaf.main.domain.MainGameDomain
import dugsolutions.leaf.main.domain.MainOutputDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.top.DraggableDivider
import dugsolutions.leaf.main.top.MainOutput
import dugsolutions.leaf.main.top.MainPlayerSection
import dugsolutions.leaf.main.top.MainTitle
import dugsolutions.leaf.main.top.MainTitleListeners
import kotlinx.coroutines.flow.StateFlow

data class MainListeners(
    val onDrawCountChosen: (playerInfo: PlayerInfo, value: Int) -> Unit = { _, _ -> },
    val onActionButtonPressed: (action: ActionButton) -> Unit = {},
    val onBooleanInstructionChosen: (value: Boolean) -> Unit = {},
    val onStepEnabledToggled: (value: Boolean) -> Unit = {},
    val onAskTrashToggled: (value: Boolean) -> Unit = {},
    val onGroveItemSelected: (card: ItemInfo) -> Unit = { },
    val onHandCardSelected: (player: PlayerInfo, card: CardInfo) -> Unit = { _, _ -> },
    val onFloralCardSelected: (player: PlayerInfo, card: CardInfo) -> Unit = { _, _ -> },
    val onDieSelected: (player: PlayerInfo, die: DieInfo) -> Unit = { _, _ -> },
    val onNutrientsClicked: (player: PlayerInfo) -> Unit = {}
)
data class MainScreenArgs(
    val gameState: StateFlow<MainGameDomain>,
    val outputState: StateFlow<MainOutputDomain>,
    val actionState: StateFlow<MainActionDomain>,
    val listeners: MainListeners
)

@Composable
fun MainScreen(args: MainScreenArgs) {
    val gameState by args.gameState.collectAsState()
    val outputState by args.outputState.collectAsState()
    val actionState by args.actionState.collectAsState()
    val listeners = args.listeners
    val initialValue = 300.dp
    var outputHeight by remember { mutableStateOf(initialValue) }
    var adjustBy by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title bar (fixed at top)
        MainTitle(
            gameState = gameState,
            actionState = actionState,
            listeners = MainTitleListeners(
                onStepEnabledToggled = listeners.onStepEnabledToggled,
                onAskTrashToggled = listeners.onAskTrashToggled,
                onActionButtonPressed = listeners.onActionButtonPressed,
                onBooleanInstructionChosen = listeners.onBooleanInstructionChosen,
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Player section (expands to fill available space)
        MainPlayerSection(
            gameState = gameState,
            actionState = actionState,
            listeners= listeners,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colors.onSurface)
        ) {
            DraggableDivider(
                onValueChange = { newValue ->
                    adjustBy = newValue
                    outputHeight = initialValue - adjustBy.dp
                }
            )
        }

        // Output section (fixed height controlled by divider)
        MainOutput(
            state = outputState,
            modifier = Modifier
                .fillMaxWidth()
                .height(outputHeight)
        )
    }
} 
