package dugsolutions.leaf.v35.plant.domain

import dugsolutions.leaf.v35.effect.GameEffect

data class PlantCard(
    val quantity: Int,
    val name: String,
    val title: String,
    val type: PlantType,
    val cost: Int,
    val lineIcon: String?,
    val vpIcon: String,
    val typeIcon: String,
    val fgColor: String,
    val textColor: String,
    val fullImage: String,
    val backgroundImage: String,
    val cardBackgroundImage: String,
    val effect: GameEffect
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlantCard

        if (quantity != other.quantity) return false
        if (cost != other.cost) return false
        if (name != other.name) return false
        if (title != other.title) return false
        if (type != other.type) return false
        if (lineIcon != other.lineIcon) return false
        if (vpIcon != other.vpIcon) return false
        if (typeIcon != other.typeIcon) return false
        if (fgColor != other.fgColor) return false
        if (textColor != other.textColor) return false
        if (fullImage != other.fullImage) return false
        if (backgroundImage != other.backgroundImage) return false
        if (cardBackgroundImage != other.cardBackgroundImage) return false
        if (effect != other.effect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quantity
        result = 31 * result + cost
        result = 31 * result + name.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + lineIcon.hashCode()
        result = 31 * result + vpIcon.hashCode()
        result = 31 * result + typeIcon.hashCode()
        result = 31 * result + fgColor.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + fullImage.hashCode()
        result = 31 * result + backgroundImage.hashCode()
        result = 31 * result + cardBackgroundImage.hashCode()
        result = 31 * result + effect.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlantCard(quantity=$quantity, name='$name', title='$title', type=$type, cost=$cost, lineIcon=$lineIcon, vpIcon='$vpIcon', typeIcon='$typeIcon', fgColor='$fgColor', textColor='$textColor', fullImage='$fullImage', backgroundImage='$backgroundImage', cardBackgroundImage='$cardBackgroundImage', effect=$effect)"
    }

}
