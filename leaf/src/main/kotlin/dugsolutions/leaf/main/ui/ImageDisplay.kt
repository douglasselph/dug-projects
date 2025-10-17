package dugsolutions.leaf.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import java.io.File

@Composable
fun ImageDisplay(
    imagePath: String,
    displayWidth: Dp? = null,
    displayHeight: Dp? = null,
    bgColor: Color = Color.Transparent,
    onError: (String) -> Unit = {},
    onSelected: (() -> Unit)? = null,
) {
    val imageFile = remember(imagePath) { File(imagePath) }
    val bitmap = remember(imagePath) { loadImageBitmapFromFile(imageFile) }
    
    if (bitmap == null) {
        onError("Image file not found or failed to decode: $imagePath")
        return
    }
    // Calculate dimensions if not provided
    val finalWidth = displayWidth ?: run {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        (displayHeight ?: 24.dp) * aspectRatio
    }
    val finalHeight = displayHeight ?: run {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height
        (displayWidth ?: 24.dp) / aspectRatio
    }

    if (onSelected != null) {
        // With selection support
        Box(
            modifier = Modifier
                .size(width = finalWidth, height = finalHeight)
                .background(bgColor)
                .clickable { onSelected() }
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = imagePath,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }
    } else {
        // Simple image without selection
        Image(
            bitmap = bitmap,
            contentDescription = imagePath,
            modifier = Modifier.size(width = finalWidth, height = finalHeight),
            contentScale = ContentScale.Fit
        )
    }
}

private fun loadImageBitmapFromFile(imageFile: File): ImageBitmap? {
    return try {
        val bytes = imageFile.readBytes()
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
