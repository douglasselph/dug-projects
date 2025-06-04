package dugsolutions.leaf.main.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.main.MainScreenArgs
import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.MainDomain

data class MainTitleListeners(
    val onStepEnabledToggled: (value: Boolean) -> Unit = {},
    val onAskTrashToggled: (value: Boolean) -> Unit = {},
    val onActionButtonPressed: (action: ActionButton) -> Unit = {},
    val onBooleanInstructionChosen: (value: Boolean) -> Unit = {}
)

@Composable
fun MainTitle(
    state: MainDomain,
    listeners: MainTitleListeners,
    modifier: Modifier
) {
    Column(
        modifier = modifier
    ) {
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
                        checked = state.askTrashEnabled,
                        onCheckedChange = { listeners.onAskTrashToggled(it) }
                    )
                    Text("Ask Trash")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = state.stepModeEnabled,
                        onCheckedChange = { listeners.onStepEnabledToggled(it) }
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
                        onClick = { listeners.onActionButtonPressed(state.actionButton) }
                    ) {
                        Text(actionText)
                    }
                }
            }
        }
        
        // Boolean instruction section
        state.booleanInstruction?.let { instruction ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Colors.SelectableColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = instruction,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { listeners.onBooleanInstructionChosen(true) }
                    ) {
                        Text("Yes")
                    }
                    Button(
                        onClick = { listeners.onBooleanInstructionChosen(false) }
                    ) {
                        Text("No")
                    }
                }
            }
        }
    }
}

// region Preview

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Main Title Preview",
        state = WindowState(
            width = 1200.dp,
            height = 600.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Basic State", style = MaterialTheme.typography.h6)
            PreviewBasicState()
            
            Text("With Action Instruction", style = MaterialTheme.typography.h6)
            PreviewWithActionInstruction()
            
            Text("With Boolean Instruction", style = MaterialTheme.typography.h6)
            PreviewWithBooleanInstruction()
            
            Text("With Both Instructions", style = MaterialTheme.typography.h6)
            PreviewWithBothInstructions()
        }
    }
}

@Composable
private fun PreviewBasicState() {
    val state = MainDomain(
        turn = 5,
        stepModeEnabled = false,
        actionInstruction = null,
        actionButton = ActionButton.NONE,
        booleanInstruction = null
    )
    val listeners = MainTitleListeners(
        onStepEnabledToggled = { println("Step mode toggled: $it") },
        onActionButtonPressed = { println("Action button pressed: $it") },
        onBooleanInstructionChosen = { println("Boolean choice: $it") }
    )
    MainTitle(state, listeners, Modifier.fillMaxWidth())
}

@Composable
private fun PreviewWithActionInstruction() {
    val state = MainDomain(
        turn = 12,
        stepModeEnabled = true,
        actionInstruction = "Select cards and/or dice to absorb 5 damage.",
        actionButton = ActionButton.DONE,
        booleanInstruction = null
    )
    val listeners = MainTitleListeners(
        onStepEnabledToggled = { println("Step mode toggled: $it") },
        onActionButtonPressed = { println("Action button pressed: $it") },
        onBooleanInstructionChosen = { println("Boolean choice: $it") }
    )
    MainTitle(state, listeners, Modifier.fillMaxWidth())
}

@Composable
private fun PreviewWithBooleanInstruction() {
    val state = MainDomain(
        turn = 8,
        stepModeEnabled = false,
        actionInstruction = null,
        actionButton = ActionButton.NONE,
        booleanInstruction = "Trash card for effect?"
    )
    val listeners = MainTitleListeners(
        onStepEnabledToggled = { println("Step mode toggled: $it") },
        onActionButtonPressed = { println("Action button pressed: $it") },
        onBooleanInstructionChosen = { println("Boolean choice: $it") }
    )
    MainTitle(state, listeners, Modifier.fillMaxWidth())
}

@Composable
private fun PreviewWithBothInstructions() {
    val state = MainDomain(
        turn = 15,
        stepModeEnabled = true,
        actionInstruction = "Choose cards to purchase from grove",
        actionButton = ActionButton.NEXT,
        booleanInstruction = "Process additional effect?"
    )
    val listeners = MainTitleListeners(
        onStepEnabledToggled = { println("Step mode toggled: $it") },
        onActionButtonPressed = { println("Action button pressed: $it") },
        onBooleanInstructionChosen = { println("Boolean choice: $it") }
    )
    MainTitle(state, listeners, Modifier.fillMaxWidth())
}

// endregion Preview
