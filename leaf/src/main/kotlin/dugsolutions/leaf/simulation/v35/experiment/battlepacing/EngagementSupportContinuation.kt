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
class EngagementSupportContinuation(
    private val orchestrator: BattleTurnOrchestrator = BattleTurnOrchestrator(),
    private val capacityAssessor: BattleSupportCapacityAssessor = BattleSupportCapacityAssessor(),
    private val reachability: BattleSupportReachability = BattleSupportReachability()
) {
    fun worthwhile(
        context: DecisionContext,
        roundCard: RoundCard,
        legalSupports: List<BattleSupportAction>,
        normalMainsOffered: List<BattleMainAction>,
        supportSlotsRemaining: Int,
        freeSupports: List<BattleSupportAction> = emptyList()
    ): List<BattleSupportAction> {
        require(supportSlotsRemaining in 0..2) { "Invalid Engagement Support budget: $supportSlotsRemaining" }
        val availableFree = freeSupports.filter { it in legalSupports }
        if (legalSupports.isEmpty()) return emptyList()
        if (supportSlotsRemaining == 0) return availableFree

        val choices = normalMainsOffered.map { BattleTurnAction.FinalMain(it) } +
            legalSupports.map { BattleTurnAction.Support(it) }
        val assessment = orchestrator(context, roundCard, choices)
        if (!assessment.continuation.shouldContinue) return availableFree
        val individual = if (assessment.continuation.individuallyWorthwhileSupportExists) {
            // Do not lose a genuinely worthwhile single action when evaluating
            // a *different* cumulative path under the Engagement cap.
            assessment.worthwhileSupports
        } else emptyList()
        if (!assessment.continuation.hasMeaningfulCumulativePath) {
            return (individual + availableFree).distinct()
        }

        // The production reachability estimate sees all physical Bees and
        // Butterflies, but those moves only spend paid slots when the matching
        // Plant has NOT granted a free Support permission. In particular, an
        // Engagement with zero paid slots may still have a tactical Bee/Butterfly
        // path; with one slot remaining a free Bee may combine with a paid
        // Butterfly. Keep policy reachability consistent with the scheduler.
        val capacity = capacityAssessor(context, choices).own
        val freeBeeMoves = if (availableFree.any { it is BattleSupportAction.PlaceCritter &&
                it.critter == Critter.BEE }) capacity.beeMoves else 0
        val freeButterflyMoves = if (availableFree.any { it is BattleSupportAction.Shared &&
                it.action is SupportAction.UseButterfly }) capacity.butterflyMoves else 0
        val possible = feasibleCumulativePairs(
            slots = supportSlotsRemaining,
            availableBees = capacity.beeMoves,
            availableButterflies = capacity.butterflyMoves,
            freeBees = freeBeeMoves,
            freeButterflies = freeButterflyMoves
        ) { bees, butterflies ->
            reachability(
                context,
                BattleSupportCapacity(beeMoves = bees, butterflyMoves = butterflies)
            ).hasMeaningfulPath
        }
        val includesBee = possible.any { it.first > 0 }
        val includesButterfly = possible.any { it.second > 0 }
        val cumulative = legalSupports.filter { candidate ->
            when (candidate) {
                is BattleSupportAction.PlaceCritter -> includesBee && candidate.critter == Critter.BEE
                is BattleSupportAction.Shared -> includesButterfly && candidate.action is SupportAction.UseButterfly
                is BattleSupportAction.UseSunlight -> false
            }
        }
        return (individual + cumulative + availableFree).distinct()
    }

    /** Exhaustive two-slot budget; never expands branches with dice rolls or RNG. */
    fun feasibleCumulativePairs(
        slots: Int,
        availableBees: Int,
        availableButterflies: Int,
        freeBees: Int = 0,
        freeButterflies: Int = 0,
        meaningful: (bees: Int, butterflies: Int) -> Boolean
    ): List<Pair<Int, Int>> {
        require(slots in 0..2 && availableBees >= 0 && availableButterflies >= 0)
        require(freeBees in 0..availableBees && freeButterflies in 0..availableButterflies)
        // Each *physical* move costs either zero or one paid slot. The free
        // counts cannot exceed the actual legal resources supplied above.
        // Do not count a single Butterfly multiple times merely because it has
        // several legal reroll targets.
        return (0..minOf(availableBees, slots + freeBees)).flatMap { bee ->
            (0..minOf(availableButterflies, slots + freeButterflies)).mapNotNull { butterfly ->
                val paid = (bee - freeBees).coerceAtLeast(0) +
                    (butterfly - freeButterflies).coerceAtLeast(0)
                if (bee + butterfly > 0 && paid <= slots && meaningful(bee, butterfly)) {
                    bee to butterfly
                } else null
            }
        }
    }
}
