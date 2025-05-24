package dugsolutions.leaf.components

data class GameCard(
    val id: CardID,
    val name: String,
    val type: FlourishType,
    val resilience: Int,
    val thorn: Int,
    val cost: Cost,
    val primaryEffect: CardEffect?,
    val primaryValue: Int,
    val matchWith: MatchWith,
    val matchEffect: CardEffect?,
    val matchValue: Int,
    val trashEffect: CardEffect?,
    val trashValue: Int
) {
    override fun toString(): String {
        return "GameCard(id=$id, name='$name', type=$type, resilience=$resilience, thorn=$thorn, " +
               "cost=[$cost], " +
               "primaryEffect=$primaryEffect, primaryValue=$primaryValue, " +
               "matchWith=$matchWith, matchEffect=$matchEffect, matchValue=$matchValue, " +
               "trashEffect=$trashEffect, trashValue=$trashValue)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameCard

        if (name != other.name) return false
        if (type != other.type) return false
        if (resilience != other.resilience) return false
        if (thorn != other.thorn) return false
        if (cost.elements != other.cost.elements) return false
        if (primaryEffect != other.primaryEffect) return false
        if (primaryValue != other.primaryValue) return false
        if (matchWith != other.matchWith) return false
        if (matchEffect != other.matchEffect) return false
        if (matchValue != other.matchValue) return false
        if (trashEffect != other.trashEffect) return false
        if (trashValue != other.trashValue) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + resilience
        result = 31 * result + thorn
        result = 31 * result + cost.elements.hashCode()
        result = 31 * result + (primaryEffect?.hashCode() ?: 0)
        result = 31 * result + primaryValue
        result = 31 * result + matchWith.hashCode()
        result = 31 * result + (matchEffect?.hashCode() ?: 0)
        result = 31 * result + matchValue
        result = 31 * result + (trashEffect?.hashCode() ?: 0)
        result = 31 * result + trashValue
        return result
    }

    val flowerCardId: CardID?
        get() = if (matchWith is MatchWith.Flower) matchWith.flowerCardId else null

}
