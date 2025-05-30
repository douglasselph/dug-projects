package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.main.domain.DiceInfo

@Composable
fun DiceDisplay(dice: DiceInfo) {
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Process dice in groups of 3
            dice.values.chunked(3).forEach { rowDice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create up to 3 dice boxes in this row
                    rowDice.forEach { dieValue ->
                        Surface(
                            border = BorderStroke(1.dp, Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = dieValue,
                                style = MaterialTheme.typography.h6,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                    // Fill remaining space with empty boxes if needed
                    repeat(3 - rowDice.size) {
                        Surface(
                            border = BorderStroke(1.dp, Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// region Preview

// Preview window for testing dice display
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dice Display Preview",
        state = WindowState(
            width = 400.dp,
            height = 600.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // First example - Standard dice
            val standardDice = DiceInfo(listOf(
                "2D4", "D6", "2D8",
                "D10", "D12",
                "D20", "D20", "D20",
                "D4"
            ))
            DiceDisplay(standardDice)

            // Second example - Custom dice
            val customDice = DiceInfo(listOf(
                "D6", "D6", "D6",
                "D8", "D8",
                "D10"
            ))
            DiceDisplay(customDice)
        }
    }
}

// endregion Preview
