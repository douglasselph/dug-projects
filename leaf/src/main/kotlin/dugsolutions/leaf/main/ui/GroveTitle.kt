package dugsolutions.leaf.main.ui


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dugsolutions.leaf.main.domain.Colors
import dugsolutions.leaf.main.domain.GroveInfo

@Composable
fun GroveTitle(grove: GroveInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Grove title and quantities grouped together
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Grove",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            grove.quantities?.let {
                Surface(
                    color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colors.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = grove.quantities,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        // Right side: instruction (unchanged)
        grove.instruction?.let { text ->
            Surface(
                color = Colors.SelectableColor,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
