package dugsolutions.leaf.main.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import dugsolutions.leaf.common.Commons
import org.jetbrains.skia.Image
import java.io.File
import java.nio.file.Paths


@Composable
fun ImageDisplay(
    imageName: String,
    cardWidth: Dp,
    onError: (String) -> Unit = {}
) {
    val imagePath = remember(imageName) {
        val currentDir = System.getProperty("user.dir")
        Paths.get(currentDir, Commons.IMAGES_DIR, imageName).toString()
    }
    val imageFile = remember(imagePath) { File(imagePath) }

    if (!imageFile.exists()) {
        onError("Image file not found: $imagePath")
        return
    }
    val bitmap = remember(imagePath) { loadImageBitmapFromFile(imageFile) }
    if (bitmap == null) {
        onError("Failed to decode image: $imagePath")
        return
    }
    Image(
        bitmap = bitmap,
        contentDescription = "Card image: ${imageFile.name}",
        modifier = Modifier
            .width(cardWidth),
        contentScale = ContentScale.Fit
    )
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
