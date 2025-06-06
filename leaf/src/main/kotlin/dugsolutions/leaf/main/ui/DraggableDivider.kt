package dugsolutions.leaf.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun DraggableDivider(
    modifier: Modifier = Modifier,
    onPositionChange: (Float) -> Unit,
    initialPosition: Float = 0.7f
) {
    var dividerPosition by remember { mutableStateOf(initialPosition) }
    var isDragging by remember { mutableStateOf(false) }
    var lastY by remember { mutableStateOf(0f) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colors.onSurface)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press -> {
                                isDragging = true
                                lastY = event.changes.first().position.y
                                println("Press: lastY = $lastY")
                            }
                            PointerEventType.Move -> {
                                if (isDragging) {
                                    val currentY = event.changes.first().position.y
                                    val deltaY = currentY - lastY
                                    println("Move: currentY = $currentY, lastY = $lastY, deltaY = $deltaY")
                                    
                                    // Calculate the position change as a fraction of the total height
                                    val positionDelta = deltaY / size.height
                                    val newPosition = dividerPosition - positionDelta
                                    
                                    println("Position calculation: positionDelta = $positionDelta, newPosition = $newPosition")
                                    
                                    // Update position with constraints
                                    dividerPosition = newPosition.coerceIn(0.2f, 0.8f)
                                    onPositionChange(dividerPosition)
                                    
                                    // Update lastY for next move
                                    lastY = currentY
                                }
                            }
                            PointerEventType.Release -> {
                                isDragging = false
                                println("Release: final position = $dividerPosition")
                            }
                        }
                    }
                }
            }
    ) {
        // Grip handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
        ) {
            // Add grip handle visual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colors.surface)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
} 
