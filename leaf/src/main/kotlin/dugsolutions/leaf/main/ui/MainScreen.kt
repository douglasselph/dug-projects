package dugsolutions.leaf.main.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.domain.MainDomain
import kotlinx.coroutines.flow.StateFlow

data class MainScreenArgs(
    val onRunButtonClicked: () -> Unit,
    val state: StateFlow<MainDomain>,
    val onNumPlayersChanged: (Int) -> Unit
)

@Composable
fun MainScreen(args: MainScreenArgs) {
    val state by args.state.collectAsState()
    
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Select Number of Players")
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (players in 2..6) {
                OutlinedButton(
                    onClick = { args.onNumPlayersChanged(players) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (state.players.size == players) MaterialTheme.colors.primary else MaterialTheme.colors.surface
                    )
                ) {
                    Text(
                        text = players.toString(),
                        color = if (state.players.size == players) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
                    )
                }
            }
        }

        Button(
            onClick = { args.onRunButtonClicked() },
            modifier = Modifier.width(120.dp)
        ) {
            Text("Run")
        }
    }
} 
