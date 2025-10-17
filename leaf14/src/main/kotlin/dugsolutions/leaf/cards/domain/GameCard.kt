package dugsolutions.leaf.cards.domain

data class GameCard(
    val id: CardID,
    val name: String,
    val type: FlourishType,
    val resilience: Int,
    val nutrient: Int,
    val cost: Cost,
    val phase: Phase,
    val primaryEffect: CardEffect?,
    val primaryValue: Int,
    val matchWith: MatchWith,
    val matchEffect: CardEffect?,
    val matchValue: Int,
    val image: String? = null,
    val count: Int,
    val notes: String? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameCard

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (resilience != other.resilience) return false
        if (nutrient != other.nutrient) return false
        if (cost != other.cost) return false
        if (phase != other.phase) return false
        if (primaryEffect != other.primaryEffect) return false
        if (primaryValue != other.primaryValue) return false
        if (matchWith != other.matchWith) return false
        if (matchEffect != other.matchEffect) return false
        if (matchValue != other.matchValue) return false
        if (image != other.image) return false
        if (count != other.count) return false
        if (notes != other.notes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + resilience
        result = 31 * result + nutrient
        result = 31 * result + cost.hashCode()
        result = 31 * result + phase.hashCode()
        result = 31 * result + (primaryEffect?.hashCode() ?: 0)
        result = 31 * result + primaryValue
        result = 31 * result + matchWith.hashCode()
        result = 31 * result + (matchEffect?.hashCode() ?: 0)
        result = 31 * result + matchValue
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + count
        result = 31 * result + (notes?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "GameCard(cost=$cost, id=$id, name='$name', type=$type, resilience=$resilience, nutrient=$nutrient, phase=$phase, primaryEffect=$primaryEffect, primaryValue=$primaryValue, matchWith=$matchWith, matchEffect=$matchEffect, matchValue=$matchValue, image=$image, count=$count, notes=$notes)"
    }


}
