package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard

enum class BattleEnablingSupportGate {
    PASSED,
    TARGET_IS_NOT_FACE_DOWN,
    NO_FACE_DOWN_PLANT,
    NO_BETTER_FINAL_MAIN,
    WATER_REQUIRES_IMPROVEMENT_STEPS
}

/** One-step evaluation of Worm Flip or Water Refresh. */
data class BattleEnablingSupportAnalysis(
    val action: BattleSupportAction.Shared,
    val enabledOpportunity: BattleEnabledPlantOpportunity?,
    val currentBestFinalMainScore: Int,
    val incrementalValue: Int,
    val gate: BattleEnablingSupportGate,
    val refreshedButterflyCount: Int,
    val equivalentRecoveryPreference: Int,
    val priority: PriorityScore
) {
    val passesSpendingGate: Boolean
        get() = gate == BattleEnablingSupportGate.PASSED

    val individuallyWorthwhile: Boolean
        get() = passesSpendingGate && incrementalValue > 0
}

/**
 * Performs the approved one-step enabling reasoning:
 *
 * `Support -> refreshed Plant -> best visible immediate Final Main`.
 *
 * It compares the unlocked Plant opportunity with the best Final Main already
 * legal. Water's refreshed Butterflies are recorded only as a secondary bonus;
 * they never satisfy the improvement-step gate by themselves.
 */
class BattleEnablingSupportAnalyzer(
    private val cardScorers: HumanBaselineCardScorerRegistry =
        HumanBaselineCardScorerRegistry(),
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val enabledPlantAnalyzer: BattleEnabledPlantAnalyzer =
        BattleEnabledPlantAnalyzer(cardScorers, policy)
) {
    operator fun invoke(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleSupportAction.Shared,
        currentFinalMains: Collection<BattleMainAction>,
        legalSupports: Collection<BattleSupportAction>
    ): BattleEnablingSupportAnalysis? {
        val currentBest = currentFinalMains.maxOfOrNull { main ->
            currentFinalMainScore(context, roundCard, main)
        } ?: return null

        return when (val shared = action.action) {
            is SupportAction.UseWormFlip -> analyzeWorm(
                context,
                action,
                shared,
                currentBest,
                legalSupports
            )

            SupportAction.UseWaterRefresh -> analyzeWater(
                context,
                action,
                currentBest,
                legalSupports
            )

            else -> null
        }
    }

    private fun analyzeWorm(
        context: DecisionContext,
        action: BattleSupportAction.Shared,
        worm: SupportAction.UseWormFlip,
        currentBest: Int,
        legalSupports: Collection<BattleSupportAction>
    ): BattleEnablingSupportAnalysis {
        val card = context.self.board.creature.firstOrNull { it.id == worm.cardId }
        if (card == null || !card.isFaceDown) {
            return disqualified(
                action = action,
                currentBest = currentBest,
                gate = BattleEnablingSupportGate.TARGET_IS_NOT_FACE_DOWN
            )
        }
        val opportunity = enabledPlantAnalyzer(context, card)
        val incremental = opportunity.priority.total - currentBest
        val gate = if (incremental > 0) {
            BattleEnablingSupportGate.PASSED
        } else {
            BattleEnablingSupportGate.NO_BETTER_FINAL_MAIN
        }
        val preference = equivalentRecoveryPreference(
            context = context,
            cardId = card.id,
            action = action,
            legalSupports = legalSupports
        )
        return result(
            action = action,
            opportunity = opportunity,
            currentBest = currentBest,
            incremental = incremental,
            gate = gate,
            refreshedButterflies = 0,
            preference = preference
        )
    }

    private fun analyzeWater(
        context: DecisionContext,
        action: BattleSupportAction.Shared,
        currentBest: Int,
        legalSupports: Collection<BattleSupportAction>
    ): BattleEnablingSupportAnalysis {
        val faceDown = context.self.board.creature.filter { it.isFaceDown }
        if (faceDown.isEmpty()) {
            return disqualified(
                action = action,
                currentBest = currentBest,
                gate = BattleEnablingSupportGate.NO_FACE_DOWN_PLANT,
                refreshedButterflies = faceDownButterflyCount(context)
            )
        }
        val opportunity = faceDown
            .map { enabledPlantAnalyzer(context, it) }
            .maxWith(
                compareBy<BattleEnabledPlantOpportunity> { it.priority.total }
                    .thenBy { it.improvementStepCount }
            )
        val incremental = opportunity.priority.total - currentBest
        val minimumSteps = policy.battleWaterRefreshMinImprovementSteps(context)
        val gate = when {
            incremental <= 0 -> BattleEnablingSupportGate.NO_BETTER_FINAL_MAIN
            opportunity.improvementStepCount < minimumSteps ->
                BattleEnablingSupportGate.WATER_REQUIRES_IMPROVEMENT_STEPS
            else -> BattleEnablingSupportGate.PASSED
        }
        val refreshedButterflies = faceDownButterflyCount(context)
        val preference = equivalentRecoveryPreference(
            context = context,
            cardId = opportunity.card.id,
            action = action,
            legalSupports = legalSupports
        )
        return result(
            action = action,
            opportunity = opportunity,
            currentBest = currentBest,
            incremental = incremental,
            gate = gate,
            refreshedButterflies = refreshedButterflies,
            preference = preference
        )
    }

    private fun result(
        action: BattleSupportAction.Shared,
        opportunity: BattleEnabledPlantOpportunity,
        currentBest: Int,
        incremental: Int,
        gate: BattleEnablingSupportGate,
        refreshedButterflies: Int,
        preference: Int
    ): BattleEnablingSupportAnalysis {
        val priority = if (gate != BattleEnablingSupportGate.PASSED) {
            PriorityScore(DISQUALIFIED_PREMIUM_SCORE)
                .adjusted(incremental, "Incremental enabled Final Main value")
                .adjusted(0, gateReason(gate))
        } else {
            PriorityScore(incremental)
                .adjusted(
                    refreshedButterflies * REFRESHED_BUTTERFLY_BONUS,
                    "Refreshed Butterflies are secondary future Support"
                )
                .adjusted(preference, preferenceReason(preference))
        }
        return BattleEnablingSupportAnalysis(
            action = action,
            enabledOpportunity = opportunity,
            currentBestFinalMainScore = currentBest,
            incrementalValue = incremental,
            gate = gate,
            refreshedButterflyCount = refreshedButterflies,
            equivalentRecoveryPreference = preference,
            priority = priority
        )
    }

    private fun disqualified(
        action: BattleSupportAction.Shared,
        currentBest: Int,
        gate: BattleEnablingSupportGate,
        refreshedButterflies: Int = 0
    ): BattleEnablingSupportAnalysis =
        BattleEnablingSupportAnalysis(
            action = action,
            enabledOpportunity = null,
            currentBestFinalMainScore = currentBest,
            incrementalValue = 0,
            gate = gate,
            refreshedButterflyCount = refreshedButterflies,
            equivalentRecoveryPreference = 0,
            priority = PriorityScore(DISQUALIFIED_PREMIUM_SCORE)
                .adjusted(0, gateReason(gate))
        )

    private fun equivalentRecoveryPreference(
        context: DecisionContext,
        cardId: CreatureCardId,
        action: BattleSupportAction.Shared,
        legalSupports: Collection<BattleSupportAction>
    ): Int {
        if (context.self.board.creature.count { it.isFaceDown } != 1) return 0
        if (faceDownButterflyCount(context) != 0) return 0
        val hasWorm = legalSupports.any { candidate ->
            val shared = (candidate as? BattleSupportAction.Shared)?.action
            shared is SupportAction.UseWormFlip && shared.cardId == cardId
        }
        val hasWater = legalSupports.any { candidate ->
            (candidate as? BattleSupportAction.Shared)?.action == SupportAction.UseWaterRefresh
        }
        if (!hasWorm || !hasWater) return 0
        return when (action.action) {
            is SupportAction.UseWormFlip -> EQUIVALENT_WORM_PREFERENCE
            SupportAction.UseWaterRefresh -> -EQUIVALENT_WORM_PREFERENCE
            else -> 0
        }
    }

    private fun faceDownButterflyCount(context: DecisionContext): Int =
        context.self.board.butterflies.count { !it.isFaceUp }

    private fun currentFinalMainScore(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction
    ): Int {
        if (action is BattleMainAction.ActivatePlant) {
            val card = context.self.board.creature.firstOrNull { it.id == action.card.id }
            if (card != null) return enabledPlantAnalyzer(context, card).priority.total
        }
        return BattleMainPriority.score(context, roundCard, action, cardScorers).total
    }

    private fun gateReason(gate: BattleEnablingSupportGate): String =
        when (gate) {
            BattleEnablingSupportGate.PASSED -> "Enabling Support gate passed"
            BattleEnablingSupportGate.TARGET_IS_NOT_FACE_DOWN ->
                "Worm Flip requires a face-down Plant"
            BattleEnablingSupportGate.NO_FACE_DOWN_PLANT ->
                "Water Refresh has no face-down Plant to enable"
            BattleEnablingSupportGate.NO_BETTER_FINAL_MAIN ->
                "Refresh does not improve the best available Final Main"
            BattleEnablingSupportGate.WATER_REQUIRES_IMPROVEMENT_STEPS ->
                "Water Refresh requires the policy minimum improvement steps"
        }

    private fun preferenceReason(preference: Int): String =
        when {
            preference > 0 -> "Prefer Worm for equivalent single-Plant recovery"
            preference < 0 -> "Preserve Water when Worm gives equivalent recovery"
            else -> "No equivalent-resource preference"
        }

    private companion object {
        const val DISQUALIFIED_PREMIUM_SCORE = -10_000
        const val REFRESHED_BUTTERFLY_BONUS = 5
        const val EQUIVALENT_WORM_PREFERENCE = 1
    }
}
