package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.scoring.LegacyPreferenceBridge
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore

/**
 * A complete Strike-row projection must not inherit a card-specific legacy
 * priority. The same tactical outcome receives the same action preference,
 * regardless of the Plant providing it. This origin is still a compatibility
 * bridge until non-tactical Battle actions are calibrated.
 */
internal object BattleProjectedActionScore {
    private const val SHARED_BATTLE_ORIGIN = 35

    fun fromCompleteProjection(tacticalValue: Double, description: String): PriorityScore =
        PriorityScore(SHARED_BATTLE_ORIGIN).adjusted(
            LegacyPreferenceBridge.battleTacticalAdjustment(tacticalValue),
            description
        )
}
