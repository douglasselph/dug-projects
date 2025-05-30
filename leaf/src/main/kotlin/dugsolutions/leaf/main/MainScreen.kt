package dugsolutions.leaf.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.ui.DrawCountDecisionDisplay
import dugsolutions.leaf.main.ui.GroveDisplay
import dugsolutions.leaf.main.ui.PlayerDisplay
import kotlinx.coroutines.flow.StateFlow

data class MainScreenArgs(
    val state: StateFlow<MainDomain>,
    val onDrawCountChosen: (value: Int) -> Unit = {},
    val onRunButtonPressed: () -> Unit = {},
    val onStepEnabledToggled: (value: Boolean) -> Unit = {},
    val onNextButtonPressed: () -> Unit= {},
    val onGroveCardSelected: (card: CardInfo) -> Unit = {}
)

@Composable
fun MainScreen(args: MainScreenArgs) {
    val state by args.state.collectAsState()
    
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Turn ${state.turn}",
                    style = MaterialTheme.typography.h4
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = state.stepModeEnabled,
                            onCheckedChange = { args.onStepEnabledToggled(it) }
                        )
                        Text("Step Mode")
                    }
                    if (state.showNextButton) {
                        Button(
                            onClick = { args.onNextButtonPressed() }
                        ) {
                            Text("Next")
                        }
                    }
                    if (state.showRunButton) {
                        Button(
                            onClick = { args.onRunButtonPressed() }
                        ) {
                            Text("Run")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colors.onSurface)
            )

            // Scrollable content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Players row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        state.players.forEach { player ->
                            PlayerDisplay(player = player) { drawCount ->
                                args.onDrawCountChosen(drawCount)
                            }
                        }
                    }

                    // Grove display (if available)
                    state.groveInfo?.let { groveInfo ->
                        GroveDisplay(grove = groveInfo) { card ->
                            args.onGroveCardSelected(card)
                        }
                    }
                }
            }

            // Black line divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colors.onSurface)
            )

            // Output area (fixed at bottom)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                val height = MaterialTheme.typography.body1.fontSize.value.dp * 7
                val listState = rememberLazyListState()
                val lastIndex = remember(state.simulationOutput.size) { state.simulationOutput.size - 1 }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height * 3f)
                ) {
                    items(state.simulationOutput.size) { index ->
                        Text(
                            text = state.simulationOutput[index],
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Auto-scroll to bottom when output changes
                LaunchedEffect(state.simulationOutput.size) {
                    if (lastIndex >= 0) {
                        listState.animateScrollToItem(lastIndex)
                    }
                }
            }
        }
    }
} 
