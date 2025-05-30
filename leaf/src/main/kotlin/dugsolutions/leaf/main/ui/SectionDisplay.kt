package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo

@Composable
fun SectionDisplay(
    title: String,
    cardCount: Int,
    dice: DiceInfo
) {
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Card count and dice display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Card count box
                Surface(
                    border = BorderStroke(1.dp, MaterialTheme.colors.primary),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cardCount.toString(),
                            style = MaterialTheme.typography.h5
                        )
                        Text(
                            text = "Cards",
                            style = MaterialTheme.typography.body1
                        )
                    }
                }

                // Dice display in a Box to prevent stretching
                Box {
                    DiceDisplay(dice)
                }
            }
        }
    }
}

// region Preview

// Preview window for testing section display
fun main() = application {
    val gatherDiceInfo = GatherDiceInfo()
    val sampleDie = SampleDie()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Section Display Preview",
        state = WindowState(
            width = 800.dp,
            height = 400.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // First example - Supply section
            SectionDisplay(
                title = "Supply",
                cardCount = 42,
                dice = gatherDiceInfo(Dice(listOf(sampleDie.d6, sampleDie.d8, sampleDie.d12)), values = false)
            )

            // Second example - Compost section
            SectionDisplay(
                title = "Compost",
                cardCount = 7,
                dice = gatherDiceInfo(Dice(listOf(sampleDie.d4, sampleDie.d4, sampleDie.d10)), values = false)
            )
        }
    }
}

// endregion Preview 
