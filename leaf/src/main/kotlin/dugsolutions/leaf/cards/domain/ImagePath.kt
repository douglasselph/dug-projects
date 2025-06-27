package dugsolutions.leaf.cards.domain

import dugsolutions.leaf.common.Commons
import java.nio.file.Paths

object ImagePath {

    fun card(imageName: String): String {
        val currentDir = System.getProperty("user.dir")
        return Paths.get(currentDir, Commons.IMAGES_DIR, imageName).toString()
    }

    fun icon(iconName: String): String {
        val currentDir = System.getProperty("user.dir")
        return Paths.get(currentDir, Commons.ICON_DIR, iconName).toString()
    }
}
