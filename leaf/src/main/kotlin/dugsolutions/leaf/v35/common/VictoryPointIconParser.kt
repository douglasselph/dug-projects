package dugsolutions.leaf.v35.common

/**
 * Converts the current Component Studio presentation tokens for fixed VP into
 * a gameplay number at the CSV ingestion boundary.
 *
 * Variable Plant VP icons are intentionally not handled here because they are
 * represented by PlantScoringRule instead of a fixed number.
 */
object VictoryPointIconParser {
    fun fixedPoints(icon: String?): Int? =
        when (icon?.trim().orEmpty()) {
            "" -> 0
            "{{ images.victory.url }}" -> 1
            "{{ images.victory_victory.url }}" -> 2
            "{{ images.victory_victory_victory.url }}" -> 3
            else -> null
        }
}
