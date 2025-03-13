package dugsolutions.leaf.main

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class MainScreenArgs(
    val onRunButtonClicked: () -> Unit,
    val domain: MainDomain
)

@Composable
fun MainScreen(args: MainScreenArgs) {
    val state by remember { derivedStateOf { args.domain.state } }
    
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
                    onClick = { args.domain.setNumPlayers(players) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = if (state.numPlayers == players) MaterialTheme.colors.primary else MaterialTheme.colors.surface
                    )
                ) {
                    Text(
                        text = players.toString(),
                        color = if (state.numPlayers == players) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface
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