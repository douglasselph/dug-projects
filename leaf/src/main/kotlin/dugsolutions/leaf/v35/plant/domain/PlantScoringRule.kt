package dugsolutions.leaf.v35.plant.domain

/**
 * Structured end-game VP rule for one grafted Plant card.
 *
 * The authored CSV still retains vpIcon for presentation. This rule is the
 * gameplay interpretation used by final scoring.
 */
sealed interface PlantScoringRule {
    data class Fixed(
        val points: Int
    ) : PlantScoringRule {
        init {
            require(points >= 0) {
                "Plant fixed VP must be non-negative: $points"
            }
        }
    }

    /** 1 VP for each Vine grafted to this player's Plant Creature. */
    data object PerGraftedVine : PlantScoringRule

    /** 1 VP for each Butterfly this player controls. */
    data object PerButterfly : PlantScoringRule
}
