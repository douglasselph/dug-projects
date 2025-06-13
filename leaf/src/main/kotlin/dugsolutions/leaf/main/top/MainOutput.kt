package dugsolutions.leaf.main.top

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.domain.MainGameDomain
import dugsolutions.leaf.main.domain.MainOutputDomain

@Composable
fun MainOutput(
    state: MainOutputDomain,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
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
