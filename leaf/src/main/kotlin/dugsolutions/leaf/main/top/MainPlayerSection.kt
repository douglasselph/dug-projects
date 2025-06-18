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
import dugsolutions.leaf.main.MainListeners
import dugsolutions.leaf.main.domain.MainActionDomain
import dugsolutions.leaf.main.domain.MainGameDomain
import dugsolutions.leaf.main.ui.GroveDisplay
import dugsolutions.leaf.main.ui.PlayerDisplay
import dugsolutions.leaf.main.ui.PlayerDisplayClickListeners

@Composable
fun MainPlayerSection(
    gameState: MainGameDomain,
    actionState: MainActionDomain,
    listeners: MainListeners,
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
                gameState.players.forEach { player ->
                    PlayerDisplay(
                        player = player,
                        actionDomain = actionState,
                        listeners = PlayerDisplayClickListeners(
                            onDrawCountChosen = { listeners.onDrawCountChosen(player, it) },
                            onHandCardSelected = { listeners.onHandCardSelected(player, it) },
                            onFloralCardSelected = { listeners.onFloralCardSelected(player, it) },
                            onDieSelected = { listeners.onDieSelected(player, it) },
                            onNutrientsClicked = { listeners.onNutrientsClicked(player) }
                        )
                    )
                }
            }

            // Grove display (if available)
            gameState.groveInfo?.let { groveInfo ->
                GroveDisplay(grove = groveInfo) { item ->
                    listeners.onGroveItemSelected(item)
                }
            }
        }
    }
}
