package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.gather.GatherDiceInfo

@Composable
fun DiceDisplay(
    dice: DiceInfo,
    elementsPerRow: Int = 3,
    onDieSelected: (die: DieInfo) -> Unit = {}
) {
    Surface(
        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Process dice in groups of elementsPerRow
            dice.values.chunked(elementsPerRow).forEach { rowDice ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Create up to 3 dice boxes in this row
                    rowDice.forEach { dieValue ->
                        Surface(
                            border = BorderStroke(1.dp, Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .width(100.dp)
                                .shadow(2.dp, RoundedCornerShape(4.dp))
                                .then(
                                    if (dieValue.highlight != HighlightInfo.NONE) {
                                        Modifier.clickable { onDieSelected(dieValue) }
                                    } else Modifier
                                ),
                            color = when (dieValue.highlight) {
                                HighlightInfo.SELECTABLE -> Colors.SelectableColor
                                HighlightInfo.SELECTED -> Colors.SelectedColor
                                else -> MaterialTheme.colors.surface
                            }
                        ) {
                            Text(
                                text = dieValue.value,
                                style = MaterialTheme.typography.h6,
                                textAlign = TextAlign.Center,
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
    val gatherDiceInfo = GatherDiceInfo()
    val sampleDie = SampleDie()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Dice Display Preview",
        state = WindowState(
            width = 400.dp,
            height = 1200.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // First example - Standard dice
            val standardDice = Dice(
                listOf(
                    sampleDie.d4, sampleDie.d4, sampleDie.d6,
                    sampleDie.d8, sampleDie.d8, sampleDie.d10,
                    sampleDie.d12, sampleDie.d20, sampleDie.d20,
                    sampleDie.d20.adjustTo(19)
                )
            )
            DiceDisplay(gatherDiceInfo(standardDice, values = true))
            DiceDisplay(gatherDiceInfo(standardDice, values = false))
            DiceDisplay(gatherDiceInfo(Dice(listOf(sampleDie.d6)), values = false))
            val info = gatherDiceInfo(standardDice, values = false)
            val withHighlights = info.copy(
                values = info.values.mapIndexed() { index, die ->
                    if (index == 0) {
                        die.copy(highlight = HighlightInfo.SELECTED)
                    } else {
                        die.copy(highlight = HighlightInfo.SELECTABLE)
                    }
                }
            )
            DiceDisplay(withHighlights)
            DiceDisplay(
                gatherDiceInfo(
                    Dice(
                        listOf(
                            sampleDie.d4,
                            sampleDie.d6,
                            sampleDie.d8,
                            sampleDie.d10,
                            sampleDie.d12,
                            sampleDie.d20
                        )
                    ), values = false
                ),
                elementsPerRow = 1
            )

        }
    }
}

// endregion Preview
