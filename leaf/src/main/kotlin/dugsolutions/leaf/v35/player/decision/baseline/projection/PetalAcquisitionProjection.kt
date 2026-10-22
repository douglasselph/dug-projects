package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.battle.domain.BattleSquare
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleAnalysisMode
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDiePlacementAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectOwnedDieZone
import dugsolutions.leaf.v35.player.decision.effect.PetalD4SourceChoice
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Decision-only projection for the four research versions of Petal To Die 4.
 * Never rolls, mutates gameplay state, or samples hidden dice. Unknown Grove D4
 * faces and subsequent Draws use arithmetic-mean predictions.
 *
 * All source choices must already have passed game-engine legality checks.
 * Scoring is additive only for distinct consequences: ownership, immediate
 * tactical/purchasing change, opponent denial, and subsequent D4 cycling.
 */
object PetalAcquisitionProjection {
    private const val END_GAME_D4_POINTS = 10
    private const val FUTURE_D4_PER_ROUND = 1
    private const val EXPECTED_VALUE_POINTS = 3
    private const val BUY_TIER_POINTS = 10

    fun isRedesigned(effect: GameEffect): Boolean = effect in setOf(
        GameEffect.GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW,
        GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S,
        GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S,
        GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S,
        GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S
    )

    fun toHand(effect: GameEffect): Boolean = when (effect) {
        GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S,
        GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S -> true
        else -> false
    }

    fun canStealDiscard(effect: GameEffect): Boolean = effect ==
        GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S ||
        effect == GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S

    /** Enumerate only sources permitted by the current effect and visible state. */
    fun legalSources(context: DecisionContext, effect: GameEffect): List<PetalD4SourceChoice> {
        if (!isRedesigned(effect)) return emptyList()
        val battle = context.battle
        if (toHand(effect) && context.phase == RoundCardType.BATTLE &&
            (battle == null || battle.rows.none { row ->
                !row.closed && row.forPlayer(context.self.id)?.let { !it.withdrawn && it.dice.size < BattleSquare.MAX_DICE } == true
            })) return emptyList()
        return buildList {
            if ((context.grove.graftBed[DieSides.D4] ?: 0) > 0) add(PetalD4SourceChoice.Grove)
            context.opponents.forEach { opponent ->
                opponent.board.hand.forEach { die ->
                    if (die.sides == 4) add(PetalD4SourceChoice.Opponent(
                        opponent.id, EffectOwnedDieZone.HAND,
                        dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice(die.index, die.sides, die.value)
                    ))
                }
                if (canStealDiscard(effect)) opponent.board.discard.forEach { die ->
                    if (die.sides == 4) add(PetalD4SourceChoice.Opponent(
                        opponent.id, EffectOwnedDieZone.DISCARD,
                        dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice(die.index, die.sides, die.value)
                    ))
                }
            }
        }
    }

    fun scoreSource(context: DecisionContext, effect: GameEffect, source: PetalD4SourceChoice): PriorityScore {
        require(isRedesigned(effect))
        require(source in legalSources(context, effect)) { "Illegal Petal D4 acquisition candidate: $source" }
        var score = PriorityScore(50)
            .adjusted(END_GAME_D4_POINTS, "One additional owned D4: Petal end-game VP")
        val futureRounds = (context.progress.cultivationRoundsRemaining ?: 0) +
            (context.progress.battleRoundsRemaining ?: 0)
        score = score.adjusted(minOf(10, futureRounds * FUTURE_D4_PER_ROUND), "Future owned D4 availability")

        val acquiredValue = when (source) {
            PetalD4SourceChoice.Grove -> DieValueHeuristics.expectedRoll(4)
            is PetalD4SourceChoice.Opponent -> source.die.value.toDouble()
        }
        if (source is PetalD4SourceChoice.Opponent) {
            // Denial of persistent ownership is separate from immediate Hand removal.
            score = score.adjusted(END_GAME_D4_POINTS, "Opponent loses a D4 for Petal end-game scoring")
            if (source.zone == EffectOwnedDieZone.HAND && context.phase == RoundCardType.CULTIVATION) {
                val before = context.opponents.first { it.id == source.playerId }.board
                val oldPower = PurchaseThresholdHeuristics.purchasingPower(before)
                val afterPower = (oldPower - source.die.value).coerceAtLeast(0)
                val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
                val lost = tiers.count { it > afterPower && it <= oldPower }
                score = score.adjusted(lost * BUY_TIER_POINTS, "Opponent loses reachable purchase tiers")
            }
            if (source.zone == EffectOwnedDieZone.HAND && context.phase == RoundCardType.BATTLE) {
                val affectedRow = context.battle?.rows?.firstOrNull { row ->
                    row.forPlayer(source.playerId)?.dice?.any { it.handIndex == source.die.index } == true
                }
                // A known opposing die removed from a Strike is evaluated once as a
                // Battle-position delta, not also as a generic Hand-denial bonus.
                if (affectedRow != null) {
                    val own = affectedRow.forPlayer(context.self.id)
                    if (own != null && !own.withdrawn && !affectedRow.closed) {
                        val before = ProjectedStrike(
                            row = affectedRow.row, ownTotal = own.total.toDouble(),
                            opponentTotals = affectedRow.players.filter { it.playerId != context.self.id && !it.withdrawn }
                                .associate { it.playerId.value.toString() to it.total.toDouble() },
                            liveOpponentKeys = affectedRow.players.filter {
                                it.playerId != context.self.id && !it.withdrawn && context.battle?.isDone(it.playerId) == false
                            }.map { it.playerId.value.toString() }.toSet()
                        )
                        val key = source.playerId.value.toString()
                        if (key in before.opponentTotals) {
                            val baseline = ProjectedState(strikes = listOf(before))
                            val after = HumanStateProjection.apply(baseline,
                                ProjectionStep.OpponentStrikeDelta(affectedRow.row, key, -source.die.value.toDouble()))
                            val swing = HumanProjectionEvaluator().evaluate(context, baseline, after).battle
                            score = score.adjusted((swing?.totalValue ?: 0.0).roundToInt(), "Battle swing from removing opponent's D4")
                        }
                    }
                }
            }
        }
        if (toHand(effect)) {
            if (context.phase == RoundCardType.BATTLE) {
                val bestPlacement = BattleDiePlacementAnalyzer()(context, acquiredValue, BattleAnalysisMode.EXPECTED)
                    .maxOfOrNull { it.tacticalValue } ?: 0.0
                score = score.adjusted(bestPlacement.roundToInt(), "Best legal Battle placement of acquired D4")
            } else {
                val before = PurchaseThresholdHeuristics.purchasingPower(context.self.board).toDouble()
                val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
                score = score.adjusted((acquiredValue * EXPECTED_VALUE_POINTS).roundToInt(), "Acquired D4 contributes immediate purchasing power")
                score = score.adjusted(tiers.count { it > before && it <= before + acquiredValue } * BUY_TIER_POINTS,
                    "New reachable purchase tiers")
            }
        }
        return score
    }

    /**
     * Score one complete acquisition choice, including its best optional cycling
     * continuation. Activation and the actual source callback must rank the same
     * sources with this function. This method does not consume RNG or mutate state.
     */
    fun scoreAcquisitionSequence(
        context: DecisionContext,
        effect: GameEffect,
        source: PetalD4SourceChoice
    ): PriorityScore {
        val acquisition = scoreSource(context, effect, source)
        val handD4 = context.self.board.hand.filter { it.sides == 4 }
        val newD4Value = if (toHand(effect)) when (source) {
            PetalD4SourceChoice.Grove -> DieValueHeuristics.expectedRoll(4)
            is PetalD4SourceChoice.Opponent -> source.die.value.toDouble()
        } else null
        val nextValue = nextDrawMean(context)
        val ordinaryCycling = handD4.sumOf { max(0.0, nextValue - it.value) }
        val newDieCycling = if (newD4Value == null) 0.0 else {
            if (context.phase == RoundCardType.BATTLE) {
                val currentPlacement = BattleDiePlacementAnalyzer()(context, newD4Value, BattleAnalysisMode.EXPECTED)
                    .maxOfOrNull { it.tacticalValue } ?: 0.0
                val replacementPlacement = BattleDiePlacementAnalyzer()(context, nextValue, BattleAnalysisMode.EXPECTED)
                    .maxOfOrNull { it.tacticalValue } ?: 0.0
                max(0.0, replacementPlacement - currentPlacement) / EXPECTED_VALUE_POINTS
            } else max(0.0, nextValue - newD4Value)
        }
        return acquisition.adjusted(((ordinaryCycling + newDieCycling) * EXPECTED_VALUE_POINTS).roundToInt(),
            "Net optional D4 cycling (arithmetic-mean Draws)")
    }

    /** Top-level value of the same legal acquisition sequences used by source selection. */
    fun activationScore(context: DecisionContext, effect: GameEffect): PriorityScore =
        legalSources(context, effect).maxByOrNull { source ->
            scoreAcquisitionSequence(context, effect, source).total
        }?.let { source -> scoreAcquisitionSequence(context, effect, source) }
            ?: PriorityScore(10).adjusted(0, "No legal D4 acquisition")

    private fun nextDrawMean(context: DecisionContext): Double {
        val sides = context.self.board.supply.minOfOrNull { it.sides }
            ?: context.self.board.discard.minOfOrNull { it.sides }
            ?: 4
        return DieValueHeuristics.expectedRoll(sides)
    }
}
