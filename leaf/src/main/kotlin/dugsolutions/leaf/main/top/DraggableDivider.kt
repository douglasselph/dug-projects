package dugsolutions.leaf.main.top

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.input.pointer.PointerInputChange

@Composable
fun DraggableDivider(
    modifier: Modifier = Modifier,
    onValueChange: (Int) -> Unit,
    initialValue: Int = 0
) {
    var dividerPosition by remember { mutableStateOf(initialValue) }
    var isDragging by remember { mutableStateOf(false) }
    var lastY by remember { mutableStateOf(0f) }

    // Track layout coordinates to convert local offset to window offset
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                layoutCoordinates = coordinates
            }
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colors.onSurface)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.first()
                        val localPosition = change.position
                        val absolutePosition = layoutCoordinates?.localToWindow(localPosition)

                        when (event.type) {
                            PointerEventType.Press -> {
                                isDragging = true
                                lastY = absolutePosition?.y ?: localPosition.y
                            }

                            PointerEventType.Move -> {
                                if (isDragging) {
                                    val currentY = absolutePosition?.y ?: localPosition.y
                                    val deltaY = currentY - lastY

                                    dividerPosition += deltaY.toInt()

                                    onValueChange(dividerPosition)

                                    lastY = currentY
                                }
                            }

                            PointerEventType.Release -> {
                                isDragging = false
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
