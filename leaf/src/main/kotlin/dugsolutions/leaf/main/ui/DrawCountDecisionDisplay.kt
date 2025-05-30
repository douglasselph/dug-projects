package dugsolutions.leaf.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.runtime.LaunchedEffect

@Composable
fun DrawCountDecisionDisplay(onDrawCountChosen: (value: Int) -> Unit = {}) {

    var selectedCount by remember { mutableStateOf<Int?>(null) }
    val selectedFeedbackDelayMs = 500L

    LaunchedEffect(selectedCount) {
        if (selectedCount != null) {
            kotlinx.coroutines.delay(selectedFeedbackDelayMs)
            selectedCount = null
        }
    }

    Surface(
        modifier = Modifier
            .background(Color.Yellow)
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, Color.DarkGray),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Instruction text
            Text(
                text = "Draw Cards",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Selection boxes
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (count in 0..4) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (selectedCount == count)
                                    MaterialTheme.colors.primary
                                else
                                    MaterialTheme.colors.surface
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (selectedCount == count)
                                        MaterialTheme.colors.onPrimary
                                    else
                                        MaterialTheme.colors.primary
                                ),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                selectedCount = count
                                onDrawCountChosen(count)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = count.toString(),
                            color = if (selectedCount == count)
                                MaterialTheme.colors.onPrimary
                            else
                                MaterialTheme.colors.onSurface
                        )
                    }
                }
            }
        }
    }
}

// region Preview

@Composable
private fun Preview() {
    MaterialTheme {
        DrawCountDecisionDisplay()
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Draw Count Decision Display Preview",
        state = WindowState(
            width = 800.dp,
            height = 200.dp
        )
    ) {
        Preview()
    }
}
// endregion Preview
