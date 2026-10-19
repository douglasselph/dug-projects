package dugsolutions.leaf.v30.cards.domain

import androidx.compose.ui.graphics.Color

data class GameCard(
    val id: GameCardID,
    val quantity: Int,
    val name: String,
    val title: String,
    val type: CardType,
    val cost: Int,
    val lineIcon: String?,
    val fgColor: Color,
    val textColor: Color,
    val fullImage: String?,
    val bgImage2: String?,
    val bgCardImage2: String?,
    val effect: CardEffect
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameCard

        if (id != other.id) return false
        if (quantity != other.quantity) return false
        if (cost != other.cost) return false
        if (name != other.name) return false
        if (title != other.title) return false
        if (type != other.type) return false
        if (lineIcon != other.lineIcon) return false
        if (fgColor != other.fgColor) return false
        if (textColor != other.textColor) return false
        if (fullImage != other.fullImage) return false
        if (bgImage2 != other.bgImage2) return false
        if (bgCardImage2 != other.bgCardImage2) return false
        if (effect != other.effect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + quantity
        result = 31 * result + cost
        result = 31 * result + name.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (lineIcon?.hashCode() ?: 0)
        result = 31 * result + fgColor.hashCode()
        result = 31 * result + textColor.hashCode()
        result = 31 * result + (fullImage?.hashCode() ?: 0)
        result = 31 * result + (bgImage2?.hashCode() ?: 0)
        result = 31 * result + (bgCardImage2?.hashCode() ?: 0)
        result = 31 * result + effect.hashCode()
        return result
    }

    override fun toString(): String {
        return "GameCard(id=$id, quantity=$quantity, name='$name', title='$title', type=$type, cost=$cost, lineIcon=$lineIcon, fgColor=$fgColor, textColor=$textColor, fullImage=$fullImage, bgImage2=$bgImage2, bgCardImage2=$bgCardImage2, effect=$effect)"
    }

}
