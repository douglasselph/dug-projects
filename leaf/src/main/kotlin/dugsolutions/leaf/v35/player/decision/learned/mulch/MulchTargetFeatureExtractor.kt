package dugsolutions.leaf.v35.player.decision.learned.mulch

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType

object MulchTargetFeatureExtractor {
    fun extract(request: ChooseEffectDieRequest, candidate: EffectDieChoice, humanReference: EffectDieChoice): DoubleArray {
        require(candidate in request.legalChoices)
        val c = request.context
        val b = c.self.board
        val legal = request.legalChoices
        val beforePower = PurchaseThresholdHeuristics.purchasingPower(b)
        val afterPower = (beforePower - candidate.value).coerceAtLeast(0)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(c.grove)
        val change = PurchaseThresholdHeuristics.change(beforePower, afterPower, tiers)
        val beforeTier = change.beforeTier ?: 0
        val afterTier = change.afterTier ?: 0
        val storedSides = (b.mulch + b.pendingMulch).mapNotNull { it.storedDieSides?.value }
        val battleNext = c.progress.upcomingRoundTypes.firstOrNull() == RoundCardType.BATTLE
        val values = DoubleArray(MulchTargetFeature.entries.size)
        fun set(f: MulchTargetFeature, v: Double) { values[f.ordinal] = v }
        set(MulchTargetFeature.BIAS, 1.0)
        set(MulchTargetFeature.DIE_FACE, candidate.value / 20.0)
        set(MulchTargetFeature.DIE_SIDES, candidate.sides / 20.0)
        set(MulchTargetFeature.DIE_FACE_FRACTION, candidate.value.toDouble() / candidate.sides)
        val expected = (candidate.sides + 1) / 2.0
        set(MulchTargetFeature.DIE_EXPECTED_REROLL, expected / 20.0)
        set(MulchTargetFeature.DIE_REROLL_GAIN, (expected - candidate.value) / 20.0)
        set(MulchTargetFeature.DIE_HEADROOM, (candidate.sides - candidate.value).toDouble() / candidate.sides)
        set(MulchTargetFeature.IS_LOWEST_FACE, if (candidate.value == legal.minOf { it.value }) 1.0 else 0.0)
        set(MulchTargetFeature.IS_HIGHEST_FACE, if (candidate.value == legal.maxOf { it.value }) 1.0 else 0.0)
        set(MulchTargetFeature.IS_LOWEST_SIDED, if (candidate.sides == legal.minOf { it.sides }) 1.0 else 0.0)
        set(MulchTargetFeature.IS_HIGHEST_SIDED, if (candidate.sides == legal.maxOf { it.sides }) 1.0 else 0.0)
        set(MulchTargetFeature.HAND_COUNT, b.hand.size / 10.0)
        set(MulchTargetFeature.HAND_FACE_TOTAL, b.hand.sumOf { it.value } / 100.0)
        set(MulchTargetFeature.HAND_FACE_TOTAL_AFTER, (b.hand.sumOf { it.value } - candidate.value).coerceAtLeast(0) / 100.0)
        set(MulchTargetFeature.HAND_SIDES_TOTAL, b.hand.sumOf { it.sides } / 100.0)
        set(MulchTargetFeature.PURCHASE_TIER_STEPS_LOST, change.crossedDown.size / 4.0)
        set(MulchTargetFeature.PURCHASE_BEST_TIER_LOSS, (beforeTier - afterTier).coerceAtLeast(0) / 20.0)
        set(MulchTargetFeature.BATTLE_NEXT, if (battleNext) 1.0 else 0.0)
        set(MulchTargetFeature.CULTIVATION_ROUNDS_REMAINING, (c.progress.cultivationRoundsRemaining ?: 0) / 7.0)
        set(MulchTargetFeature.BATTLES_REMAINING, (c.progress.battleRoundsRemaining ?: 0) / 3.0)
        set(MulchTargetFeature.STORED_MULCH_COUNT, storedSides.size / 6.0)
        set(MulchTargetFeature.STORED_MULCH_SIDES_TOTAL, storedSides.sum() / 120.0)
        set(MulchTargetFeature.STORED_MULCH_MAX_SIDES, (storedSides.maxOrNull() ?: 0) / 20.0)
        set(MulchTargetFeature.HUMAN_REFERENCE_MATCH, if (candidate == humanReference) 1.0 else 0.0)
        return values
    }
}
