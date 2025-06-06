package dugsolutions.leaf.main.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.MainScreenArgs
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.MainDomain

@Composable
fun MainTitle(
    state: MainDomain,
    args: MainScreenArgs,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
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
            state.actionInstruction?.let { instruction ->
                Surface(
                    color = Colors.SelectableColor,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            state.actionButton.text?.let { actionText ->
                Button(
                    onClick = { args.onActionButtonPressed(state.actionButton) }
                ) {
                    Text(actionText)
                }
            }
        }
    }

}
