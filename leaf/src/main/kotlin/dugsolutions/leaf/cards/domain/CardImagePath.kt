package dugsolutions.leaf.cards.domain

import dugsolutions.leaf.common.Commons
import java.nio.file.Paths

object CardImagePath {

    operator fun invoke(imageName: String): String {
        val currentDir = System.getProperty("user.dir")
        return Paths.get(currentDir, Commons.IMAGES_DIR, imageName).toString()
    }
}
