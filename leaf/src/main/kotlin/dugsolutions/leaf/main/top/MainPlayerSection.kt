package dugsolutions.leaf.main.top


import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.MainScreenArgs
import dugsolutions.leaf.main.domain.MainDomain
import dugsolutions.leaf.main.ui.GroveDisplay
import dugsolutions.leaf.main.ui.PlayerDisplay
import dugsolutions.leaf.main.ui.PlayerDisplayClickListeners

@Composable
fun MainPlayerSection(
    state: MainDomain,
    args: MainScreenArgs,
    modifier: Modifier
) {
    Box(
        modifier = modifier
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
                    PlayerDisplay(
                        player = player,
                        listeners = PlayerDisplayClickListeners(
                            onDrawCountChosen = { args.onDrawCountChosen(player, it) },
                            onHandCardSelected = { args.onHandCardSelected(player, it) },
                            onFloralCardSelected = { args.onFloralCardSelected(player, it) },
                            onDieSelected = { args.onDieSelected(player, it) }
                        )
                    )
                }
            }

            // Grove display (if available)
            state.groveInfo?.let { groveInfo ->
                GroveDisplay(grove = groveInfo) { item ->
                    args.onGroveItemSelected(item)
                }
            }
        }
    }
}
