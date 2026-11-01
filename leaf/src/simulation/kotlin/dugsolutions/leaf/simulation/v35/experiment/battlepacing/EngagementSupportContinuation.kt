package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSupportCapacity
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSupportCapacityAssessor
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSupportReachability
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleTurnOrchestrator
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Task 3: adapt the *production* B10-B14 continuation gate to an Engagement.
 *
 * Ordinary v37 may chain any number of Supports before Final Main. Candidate B
 * may take only [supportSlotsRemaining] Supports in this Engagement, and a
 * deferred normal Main must NOT be offered to the v37 chooser as a Final Main.
 * The original scorers and choice policy remain authoritative for which action
 * to take; this class decides only which Supports have a credible continuation.
 */
internal class EngagementSupportContinuation(
    private val orchestrator: BattleTurnOrchestrator = BattleTurnOrchestrator(),
    private val capacityAssessor: BattleSupportCapacityAssessor = BattleSupportCapacityAssessor(),
    private val reachability: BattleSupportReachability = BattleSupportReachability()
) {
    fun worthwhile(
        context: DecisionContext,
        roundCard: RoundCard,
        legalSupports: List<BattleSupportAction>,
        normalMainsOffered: List<BattleMainAction>,
        supportSlotsRemaining: Int
    ): List<BattleSupportAction> {
        require(supportSlotsRemaining in 0..2) { "Invalid Engagement Support budget: $supportSlotsRemaining" }
        if (supportSlotsRemaining == 0 || legalSupports.isEmpty()) return emptyList()

        val choices = normalMainsOffered.map { BattleTurnAction.FinalMain(it) } +
            legalSupports.map { BattleTurnAction.Support(it) }
        val assessment = orchestrator(context, roundCard, choices)
        if (!assessment.continuation.shouldContinue) return emptyList()
        if (assessment.continuation.individuallyWorthwhileSupportExists) {
            // Production B11/B12 direct/enabling gates, Wisp scorer and Sunlight
            // value already decided these. No invented positive-score threshold.
            return assessment.worthwhileSupports
        }
        if (!assessment.continuation.hasMeaningfulCumulativePath) return emptyList()

        // v37's optimistic reachability uses *all* currently held Bees and
        // Butterflies. In B, a path requiring 3+ Supports cannot be completed in
        // a single Engagement. Re-evaluate the SAME production reachability
        // logic for only combinations that fit the remaining Support budget.
        val capacity = capacityAssessor(context, choices).own
        val possible = feasibleCumulativePairs(
            supportSlotsRemaining,
            capacity.beeMoves,
            capacity.butterflyMoves
        ) { bees, butterflies ->
            reachability(
                context,
                BattleSupportCapacity(beeMoves = bees, butterflyMoves = butterflies)
            ).hasMeaningfulPath
        }
        if (possible.isEmpty()) return emptyList()
        val includesBee = possible.any { it.first > 0 }
        val includesButterfly = possible.any { it.second > 0 }
        return legalSupports.filter { candidate ->
            when (candidate) {
                is BattleSupportAction.PlaceCritter -> includesBee && candidate.critter == Critter.BEE
                is BattleSupportAction.Shared -> includesButterfly && candidate.action is SupportAction.UseButterfly
                is BattleSupportAction.UseSunlight -> false
            }
        }
    }

    /** Exhaustive two-slot budget; never expands branches with dice rolls or RNG. */
    internal fun feasibleCumulativePairs(
        slots: Int,
        availableBees: Int,
        availableButterflies: Int,
        meaningful: (bees: Int, butterflies: Int) -> Boolean
    ): List<Pair<Int, Int>> {
        require(slots in 0..2 && availableBees >= 0 && availableButterflies >= 0)
        return (0..minOf(slots, availableBees)).flatMap { bee ->
            (0..minOf(slots - bee, availableButterflies)).mapNotNull { butterfly ->
                if (bee + butterfly > 0 && meaningful(bee, butterfly)) bee to butterfly else null
            }
        }
    }
}
