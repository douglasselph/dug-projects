package dugsolutions.leaf.main

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
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
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.ui.decision.DrawCountDecisionDisplay
import dugsolutions.leaf.main.ui.parts.GroveDisplay
import dugsolutions.leaf.main.ui.parts.PlayerDisplay
import kotlinx.coroutines.flow.StateFlow

data class MainScreenArgs(
    val state: StateFlow<MainDomain>,
    val onDrawCountChosen: (value: Int) -> Unit = {},
    val onRunButtonPressed: () -> Unit = {}
)

@Composable
fun MainScreen(args: MainScreenArgs) {
    val state by args.state.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Turn number display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Turn ${state.turn}",
                style = MaterialTheme.typography.h4
            )
            
            if (state.showRunButton) {
                Button(
                    onClick = { args.onRunButtonPressed() }
                ) {
                    Text("Run")
                }
            }
        }

        // Players row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.players.forEach { player ->
                PlayerDisplay(player = player)
            }
        }

        // Grove display (if available)
        state.groveInfo?.let { groveInfo ->
            GroveDisplay(grove = groveInfo)
        }

        if (state.showDrawCount) {
            DrawCountDecisionDisplay(args.onDrawCountChosen)
        }

        // Simulation output
        val height = MaterialTheme.typography.body1.fontSize.value.dp * 7
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(height * 1.5f)
                .background(MaterialTheme.colors.surface),
            elevation = 4.dp
        ) {
            val listState = rememberLazyListState()
            val lastIndex = remember(state.simulationOutput.size) { state.simulationOutput.size - 1 }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
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
