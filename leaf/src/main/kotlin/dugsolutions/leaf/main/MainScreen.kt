package dugsolutions.leaf.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.domain.PlayerInfo
import dugsolutions.leaf.main.top.MainOutput
import dugsolutions.leaf.main.top.MainPlayerSection
import dugsolutions.leaf.main.top.MainTitle
import dugsolutions.leaf.main.ui.DraggableDivider
import kotlinx.coroutines.flow.StateFlow

data class MainScreenArgs(
    val state: StateFlow<MainDomain>,
    val onDrawCountChosen: (value: Int) -> Unit = {},
    val onActionButtonPressed: (action: ActionButton) -> Unit = {},
    val onStepEnabledToggled: (value: Boolean) -> Unit = {},
    val onGroveCardSelected: (card: CardInfo) -> Unit = {},
    val onHandCardSelected: (player: PlayerInfo, card: CardInfo) -> Unit = { _, _ -> },
    val onFloralCardSelected: (player: PlayerInfo, card: CardInfo) -> Unit = { _, _ -> },
    val onDieSelected: (player: PlayerInfo, die: DieInfo) -> Unit = { _, _ -> }
)

@Composable
fun MainScreen(args: MainScreenArgs) {
    val state by args.state.collectAsState()
    var dividerPosition by remember { mutableStateOf(0.7f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title bar (fixed at top)
            MainTitle(
                state,
                args,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colors.onSurface)
            )

            // Scrollable content area with dynamic height
            MainPlayerSection(
                state,
                args,
                modifier = Modifier
                    .weight(dividerPosition)
                    .fillMaxWidth()
            )

            // Draggable divider
            DraggableDivider(
                onPositionChange = { newPosition ->
                    dividerPosition = newPosition
                }
            )

            // Output area with dynamic height
            MainOutput(
                state = state,
                modifier = Modifier
                    .weight(1f - dividerPosition)
                    .fillMaxWidth()
            )
        }
    }
} 
