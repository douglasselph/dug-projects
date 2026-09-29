package dugsolutions.leaf.v35.player.decision.baseline.context

import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdChange
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.round.domain.RoundCardType

/** Public, shallow phase look-ahead shared by Human Baseline decisions. */
object PhaseProximity {
    fun nextPhase(context: DecisionContext): RoundCardType? =
        context.progress.upcomingRoundTypes.firstOrNull()

    fun battleIsNext(context: DecisionContext): Boolean =
        nextPhase(context) == RoundCardType.BATTLE

    fun roundsUntilBattle(context: DecisionContext): Int? {
        val index = context.progress.upcomingRoundTypes.indexOf(RoundCardType.BATTLE)
        return if (index < 0) null else index + 1
    }
}

data class CreatureRefreshObservation(
    val faceUpCount: Int,
    val faceDownCount: Int,
    val immediateRefresh: Boolean,
    val twoActionRefreshOpportunity: Boolean,
    val battleIsNext: Boolean,
    val restoredCards: List<String>,
    val restoredCultivationBaseTotal: Int,
    val restoredBattleBaseTotal: Int
)

/**
 * Describes the shallow refresh opportunity created by consuming one face-up Plant.
 * It intentionally assigns no calibrated priority bonus yet.
 */
class CreatureRefreshValue(
    private val scorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
) {
    /**
     * Provisional shared priority adjustment.  This is deliberately shallow:
     * it values cards restored by the refresh rather than predicting their future targets.
     */
    fun priorityAdjustment(observation: CreatureRefreshObservation): Int = when {
        observation.immediateRefresh -> {
            val restored = if (observation.battleIsNext)
                observation.restoredBattleBaseTotal else observation.restoredCultivationBaseTotal
            12 + (restored / 25).coerceAtMost(20)
        }
        observation.twoActionRefreshOpportunity -> {
            val restored = if (observation.battleIsNext)
                observation.restoredBattleBaseTotal else observation.restoredCultivationBaseTotal
            4 + (restored / 50).coerceAtMost(10)
        }
        else -> 0
    }

    fun observe(context: DecisionContext, selected: CreatureCardView): CreatureRefreshObservation {
        val creature = context.self.board.creature
        require(selected in creature) { "Selected Plant is not in the acting player's Creature" }
        require(selected.isFaceUp) { "Refresh observation requires a face-up selected Plant" }
        val faceUp = creature.count { it.isFaceUp }
        val faceDown = creature.filter { it.isFaceDown }
        val immediate = faceUp == 1
        val twoAction = faceUp == 2
        // An immediate refresh restores the selected card too.  For a two-action
        // opportunity, both currently face-up cards are potentially restored.
        val restored = if (immediate || twoAction) creature else faceDown
        return CreatureRefreshObservation(
            faceUpCount = faceUp,
            faceDownCount = faceDown.size,
            immediateRefresh = immediate,
            twoActionRefreshOpportunity = twoAction,
            battleIsNext = PhaseProximity.battleIsNext(context),
            restoredCards = restored.map { it.name },
            restoredCultivationBaseTotal = restored.sumOf { scorers.forPlant(it).cultivationPlayBase },
            restoredBattleBaseTotal = restored.sumOf { scorers.forPlant(it).battlePlayBase }
        )
    }
}

data class PhasePreservationObservation(
    val battleIsNext: Boolean,
    val cultivationBase: Int,
    val battleBase: Int,
    val battleValuePremium: Int,
    val immediateRefresh: Boolean,
    val twoActionRefreshOpportunity: Boolean,
    val preservationRelevant: Boolean
)

/** Describes, but does not yet score, the cost of spending a Plant before Battle. */
class PhasePreservationValue(
    private val scorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    private val refreshValue: CreatureRefreshValue = CreatureRefreshValue(scorers)
) {
    /** Negative means preserve the Plant for the imminent Battle. */
    fun priorityAdjustment(observation: PhasePreservationObservation): Int {
        if (!observation.preservationRelevant) return 0
        val premium = observation.battleValuePremium.coerceAtLeast(0)
        // Even equal-base cards have some option value in Battle; cards calibrated
        // with a larger Battle premium are preserved more strongly.
        var penalty = 8 + premium / 2
        if (observation.twoActionRefreshOpportunity) penalty /= 2
        return -penalty.coerceAtMost(30)
    }

    fun observe(context: DecisionContext, card: CreatureCardView): PhasePreservationObservation {
        val scorer = scorers.forPlant(card)
        val refresh = refreshValue.observe(context, card)
        val battleNext = PhaseProximity.battleIsNext(context)
        return PhasePreservationObservation(
            battleIsNext = battleNext,
            cultivationBase = scorer.cultivationPlayBase,
            battleBase = scorer.battlePlayBase,
            battleValuePremium = scorer.battlePlayBase - scorer.cultivationPlayBase,
            immediateRefresh = refresh.immediateRefresh,
            twoActionRefreshOpportunity = refresh.twoActionRefreshOpportunity,
            preservationRelevant = battleNext && !refresh.immediateRefresh
        )
    }
}

data class FutureDieAvailabilityObservation(
    val dieSides: Int,
    val supplyDiceBeforeRecycle: Int,
    val lowerSidedDiscardDiceAhead: Int,
    val approximateDrawsUntilAvailable: Int,
    val roundsUntilBattle: Int?,
    val battleIsNext: Boolean,
    val expectedUpcomingSupplySides: List<Int>
)

/** Visible, approximate dice-cycle reasoning; no hidden RNG or future rolls. */
object FutureDiceAvailability {
    fun forDiscardDie(context: DecisionContext, die: DieView): FutureDieAvailabilityObservation {
        require(die in context.self.board.discard) { "Die must be in Discard" }
        val board = context.self.board
        val lowerAhead = board.discard.count { candidate ->
            candidate.index != die.index && (candidate.sides < die.sides ||
                (candidate.sides == die.sides && candidate.index < die.index))
        }
        return FutureDieAvailabilityObservation(
            dieSides = die.sides,
            supplyDiceBeforeRecycle = board.supply.size,
            lowerSidedDiscardDiceAhead = lowerAhead,
            approximateDrawsUntilAvailable = board.supply.size + lowerAhead + 1,
            roundsUntilBattle = PhaseProximity.roundsUntilBattle(context),
            battleIsNext = PhaseProximity.battleIsNext(context),
            expectedUpcomingSupplySides = board.supply.sortedWith(compareBy<DieView> { it.sides }.thenBy { it.index }).map { it.sides }
        )
    }
}

data class DicePoolQualityObservation(
    val ownedDiceCount: Int,
    val d4Count: Int,
    val d6Count: Int,
    val weakDieFraction: Double,
    val preparedMulchCount: Int,
    val nextSupplySides: List<Int>
)

/** Visible pool-quality facts. Deliberate cleanup randomness belongs in strategy code. */
object DicePoolQuality {
    fun observe(context: DecisionContext): DicePoolQualityObservation {
        val board = context.self.board
        val cycling = board.supply + board.hand + board.discard
        val stored = board.mulch.count { it.storedDieSides != null } + board.pendingMulch.count { it.storedDieSides != null }
        val owned = cycling.size + stored
        val d4 = cycling.count { it.sides == 4 }
        val d6 = cycling.count { it.sides == 6 }
        return DicePoolQualityObservation(
            ownedDiceCount = owned,
            d4Count = d4,
            d6Count = d6,
            weakDieFraction = if (cycling.isEmpty()) 0.0 else (d4 + d6).toDouble() / cycling.size,
            preparedMulchCount = stored,
            nextSupplySides = board.supply.sortedWith(compareBy<DieView> { it.sides }.thenBy { it.index }).map { it.sides }
        )
    }
}

data class PurchaseOpportunityObservation(
    val beforePower: Int,
    val afterPower: Int,
    val thresholdChange: PurchaseThresholdChange
)

/** Reuses the canonical Buy-threshold model instead of inventing another one. */
object PurchaseOpportunityCost {
    fun afterPowerChange(context: DecisionContext, valueChange: Int): PurchaseOpportunityObservation {
        val before = PurchaseThresholdHeuristics.purchasingPower(context.self.board)
        val after = (before + valueChange).coerceAtLeast(0)
        val change = PurchaseThresholdHeuristics.change(
            beforePower = before,
            afterPower = after,
            costs = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        )
        return PurchaseOpportunityObservation(before, after, change)
    }
}
