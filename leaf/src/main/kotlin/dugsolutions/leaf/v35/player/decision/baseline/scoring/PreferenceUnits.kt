package dugsolutions.leaf.v35.player.decision.baseline.scoring

import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import kotlin.math.roundToInt

/** C05: a number has no ranking meaning without its phase and unit. */
enum class PreferenceUnit {
    LEGACY_PRIORITY_POINTS,
    CULTIVATION_EXPECTED_PIPS,
    BATTLE_TACTICAL_UTILITY
}

data class PhasePreference(
    val phase: CardPhase,
    val unit: PreferenceUnit,
    val amount: Double
) : Comparable<PhasePreference> {
    init {
        require(amount.isFinite()) { "Preference must be finite" }
        require(unit == PreferenceUnit.LEGACY_PRIORITY_POINTS ||
            (phase == CardPhase.CULTIVATION && unit == PreferenceUnit.CULTIVATION_EXPECTED_PIPS) ||
            (phase == CardPhase.BATTLE && unit == PreferenceUnit.BATTLE_TACTICAL_UTILITY)) {
            "Incompatible phase $phase and unit $unit"
        }
    }

    override fun compareTo(other: PhasePreference): Int {
        require(phase == other.phase && unit == other.unit) {
            "Cannot compare $phase/$unit against ${other.phase}/${other.unit}; convert explicitly"
        }
        return amount.compareTo(other.amount)
    }
}

/**
 * Temporary compatibility boundary. This maps new projections to the existing
 * *legacy* preference scale until all candidates in that phase are calibrated.
 * It must not be interpreted as converting tactical utility into spending pips.
 */
object LegacyPreferenceBridge {
    private const val CULTIVATION_DRAW_ORIGIN = 35
    private const val POINTS_PER_EXPECTED_PIP = 4

    fun cultivationDraw(expectedPips: Double, dieSides: Int? = null): PriorityScore {
        require(expectedPips.isFinite() && expectedPips >= 0.0)
        require(dieSides == null || dieSides > 0) { "Die sides must be positive" }
        val dieLabel = dieSides?.let { "D$it: " } ?: ""
        return PriorityScore(CULTIVATION_DRAW_ORIGIN).adjusted(
            (expectedPips * POINTS_PER_EXPECTED_PIP).roundToInt(),
            "${dieLabel}expected draw pips converted to legacy Cultivation priority points"
        )
    }

    /** Tactical swing is an incremental adjustment, never a raw competing action priority. */
    fun battleTacticalAdjustment(tacticalUtility: Double): Int {
        require(tacticalUtility.isFinite())
        return tacticalUtility.roundToInt()
    }
}
