package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.SimpleEffectMagnitude
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.context.DieView
import kotlin.math.roundToInt

/**
 * Shared bounded two-choice O Edelweiss estimate for Cultivation and Battle.
 * Facing transitions are exact and side-effect free; nested effects reuse the
 * existing phase-specific card scorers (not a mechanical engine execution).
 * The second choice is scored on the projected facing state, then execution
 * independently recomputes legal choices against actual state.
 */
class BattleOEdelweissAnalyzer(
    private val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    private val enabledPlantAnalyzer: BattleEnabledPlantAnalyzer =
        BattleEnabledPlantAnalyzer(cardScorers)
) {
    fun scoreChoice(
        context: DecisionContext,
        choice: OEdelweissChoice
    ): PriorityScore? {
        if (context.phase == null) return null

        return when (choice) {
            OEdelweissChoice.Done -> PriorityScore(DONE_SCORE)

            is OEdelweissChoice.Play -> {
                val card = currentCard(context, choice.card) ?: return null
                if (card.isFaceUp != choice.card.isFaceUp || !card.isFaceUp) return null

                plantValue(context, card).adjusted(
                    0,
                    "Play face-up Plant for its current projected value"
                )
            }

            is OEdelweissChoice.Flip -> {
                val card = currentCard(context, choice.card) ?: return null
                if (card.isFaceUp != choice.card.isFaceUp) return null

                val opportunity = plantValue(context, card)
                if (card.isFaceDown) {
                    PriorityScore(DONE_SCORE).adjusted(
                        maxOf(0, (opportunity.total - DONE_SCORE) / 4),
                        "Future option created by flipping spent Plant face up"
                    )
                } else {
                    val lostValue = maxOf(0, opportunity.total - DONE_SCORE)
                    PriorityScore(DONE_SCORE - FACE_UP_FLIP_PENALTY)
                        .adjusted(
                            -lostValue,
                            "Flipping face-up Plant down disables its current Battle Main"
                        )
                }
            }
        }
    }

    /** Bounded two-choice plan using immutable projected Plant facings. */
    fun topLevelPriority(context: DecisionContext, sourceCardId: CreatureCardId): PriorityScore? {
        if (context.phase == null) return null
        return bestSequence(context, 2, sourceCardId = sourceCardId).second
    }

    /** Shared at decision time: rank first choices by their best legal continuation. */
    fun bestChoicePriority(
        context: DecisionContext,
        choice: OEdelweissChoice,
        remainingChoices: Int,
        legalChoices: List<OEdelweissChoice>
    ): PriorityScore? {
        if (context.phase == null) return null
        return sequenceValue(context, choice, remainingChoices, legalChoices, emptySet())
    }

    private fun bestSequence(
        context: DecisionContext,
        remainingChoices: Int,
        sourceCardId: CreatureCardId? = null,
        excluded: Set<CreatureCardId> = emptySet()
    ): Pair<OEdelweissChoice, PriorityScore> {
        val candidates = choices(context, sourceCardId, excluded)
        return candidates.mapNotNull { choice ->
            sequenceValue(context, choice, remainingChoices, candidates, excluded)?.let { choice to it }
        }.maxByOrNull { it.second.total } ?: (OEdelweissChoice.Done to PriorityScore(DONE_SCORE))
    }

    private fun sequenceValue(
        context: DecisionContext,
        choice: OEdelweissChoice,
        remainingChoices: Int,
        legalChoices: List<OEdelweissChoice>,
        excluded: Set<CreatureCardId>
    ): PriorityScore? {
        if (choice == OEdelweissChoice.Done) return PriorityScore(DONE_SCORE)
        val first = scoreChoice(context, choice) ?: return null
        if (remainingChoices <= 1) return first
        val projected = projectedContext(context, choice) ?: return null
        val next = projected.context
        val allowedIds = legalChoices.mapNotNull {
            when (it) {
                is OEdelweissChoice.Play -> it.card.cardId
                is OEdelweissChoice.Flip -> it.card.cardId
                OEdelweissChoice.Done -> null
            }
        }.toSet()
        val nextCandidates = choices(next, excluded = excluded)
            .filter { it == OEdelweissChoice.Done || when (it) {
                is OEdelweissChoice.Play -> it.card.cardId in allowedIds
                is OEdelweissChoice.Flip -> it.card.cardId in allowedIds
                else -> false
            } }
        val bestSecond = nextCandidates.mapNotNull { candidate ->
            projectedChoiceScore(projected, candidate)?.let { candidate to it }
        }.maxByOrNull { it.second.total }
        val second = bestSecond?.second ?: PriorityScore(DONE_SCORE)
        // A Flip-up immediately followed by Play of that same card does not
        // retain the earlier future-option value; that option was consumed.
        val consumedSetup = if (choice is OEdelweissChoice.Flip &&
            !choice.card.isFaceUp &&
            bestSecond?.first is OEdelweissChoice.Play &&
            (bestSecond.first as OEdelweissChoice.Play).card.cardId == choice.card.cardId
        ) first.total - DONE_SCORE else 0
        // Each ordinary card priority includes the common Done baseline.
        // Only the incremental value of the second choice is added.
        return first.adjusted(
            second.total - DONE_SCORE - consumedSetup,
            "Best projected second O Edelweiss choice (no duplicate base or consumed Flip setup)"
        )
    }

    /** A prediction-only overlay: expected rolls remain doubles, never physical DieViews. */
    private data class Prediction(
        val context: DecisionContext,
        val expectedDice: List<ExpectedDie> = emptyList()
    )

    private data class ExpectedDie(val sides: Int, val mean: Double)

    /**
     * Evaluate the next effect against the updated deterministic state. For unrolled dice,
     * add only the marginal value missed by the legacy integer-valued card scorer.
     */
    private fun projectedChoiceScore(prediction: Prediction, choice: OEdelweissChoice): PriorityScore? {
        val ordinary = scoreChoice(prediction.context, choice) ?: return null
        if (choice !is OEdelweissChoice.Play || prediction.expectedDice.isEmpty()) return ordinary
        val card = currentCard(prediction.context, choice.card) ?: return ordinary
        val raiseAmount = when (card.effect) {
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.RESEARCH_RAISE_DIE_PLUS_20 -> requireNotNull(SimpleEffectMagnitude.raiseAmount(card.effect))
            GameEffect.RAISE_ANY_DIE_PLUS_1 -> 1
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW -> 2
            else -> null
        } ?: return ordinary
        val realGain = prediction.context.self.board.hand.maxOfOrNull {
            DieValueHeuristics.actualRaiseGain(it, raiseAmount).toDouble()
        } ?: 0.0
        val predictedGain = prediction.expectedDice.maxOfOrNull {
            minOf(it.sides.toDouble(), it.mean + raiseAmount) - it.mean
        } ?: 0.0
        val extra = (predictedGain - realGain).coerceAtLeast(0.0)
        if (extra == 0.0) return ordinary
        val powerBefore = PurchaseThresholdHeuristics.purchasingPower(prediction.context.self.board).toDouble() +
            prediction.expectedDice.sumOf { it.mean }
        val costs = PurchaseThresholdHeuristics.availableCostTiers(prediction.context.grove)
        val tiers = if (prediction.context.phase == RoundCardType.CULTIVATION) {
            costs.count { it > powerBefore + realGain && it <= powerBefore + predictedGain } * 10
        } else 0
        return ordinary.adjusted((extra * 3).roundToInt() + tiers,
            "Projected mean-valued dice available to second Plant (without simulated rolls)")
    }

    private fun projectedContext(context: DecisionContext, choice: OEdelweissChoice): Prediction? {
        val id = when (choice) {
            is OEdelweissChoice.Play -> choice.card.cardId
            is OEdelweissChoice.Flip -> choice.card.cardId
            OEdelweissChoice.Done -> return Prediction(context)
        }
        if (context.self.board.creature.none { it.id == id }) return null
        val updated = context.self.board.creature.map { card ->
            if (card.id != id) card else when (choice) {
                is OEdelweissChoice.Play -> card.copy(facing = CreatureCard.Facing.FACE_DOWN)
                is OEdelweissChoice.Flip -> card.copy(facing = if (card.isFaceUp) CreatureCard.Facing.FACE_DOWN else CreatureCard.Facing.FACE_UP)
                else -> card
            }
        }
        val facingContext = context.copy(self = context.self.copy(board = context.self.board.copy(creature = updated)))
        if (choice !is OEdelweissChoice.Play) return Prediction(facingContext)
        val source = currentCard(context, choice.card) ?: return null
        return projectFirstEffect(facingContext, source.effect)
    }

    /**
     * Project deterministic outcomes that can be determined without resolving choices or RNG.
     * The unknown Draw result is recorded only as a fractional expectation.
     * Unsupported targeted effects retain the prior state rather than guessing a target.
     */
    private fun projectFirstEffect(context: DecisionContext, effect: GameEffect): Prediction {
        val board = context.self.board
        fun withBoard(updated: dugsolutions.leaf.v35.player.decision.context.PlayerBoardView) =
            Prediction(context.copy(self = context.self.copy(board = updated)))
        return when (effect) {
            GameEffect.GAIN_WATER_TOKEN ->
                if (context.grove.water > 0) withBoard(board.copy(water = board.water + 1)) else Prediction(context)
            GameEffect.GAIN_ANY_TWO_CRITTERS -> {
                val bees = minOf(2, context.grove.bees)
                val worms = minOf(2 - bees, context.grove.worms)
                withBoard(board.copy(bees = board.bees + bees, worms = board.worms + worms))
            }
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.RESEARCH_RAISE_DIE_PLUS_20,
            GameEffect.RAISE_ANY_DIE_PLUS_1 -> {
                val amount = SimpleEffectMagnitude.raiseAmount(effect) ?: 1
                val target = board.hand.maxByOrNull { DieValueHeuristics.actualRaiseGain(it, amount) }
                    ?: return Prediction(context)
                val gain = DieValueHeuristics.actualRaiseGain(target, amount)
                if (gain <= 0) return Prediction(context)
                val raised = board.hand.map { if (it.index == target.index) it.copy(value = it.value + gain) else it }
                val updated = board.copy(hand = raised)
                val battle = context.battle?.copy(rows = context.battle.rows.map { row ->
                    row.copy(players = row.players.map { player ->
                        if (player.playerId != context.self.id || player.dice.none { it.handIndex == target.index }) player
                        else player.copy(
                            dice = player.dice.map { if (it.handIndex == target.index) it.copy(value = it.value + gain) else it },
                            dieTotal = player.dieTotal + gain,
                            total = player.total + gain
                        )
                    })
                })
                Prediction(context.copy(self = context.self.copy(board = updated), battle = battle))
            }
            GameEffect.DRAW_TWO_DICE,
            GameEffect.RESEARCH_DRAW_FOUR_DICE -> {
                var supply = board.supply
                var discard = board.discard
                val expected = mutableListOf<ExpectedDie>()
                repeat(requireNotNull(SimpleEffectMagnitude.drawCount(effect))) {
                    if (supply.isEmpty()) { supply = discard; discard = emptyList() }
                    val next = supply.minByOrNull { it.sides } ?: return@repeat
                    expected += ExpectedDie(next.sides, DieValueHeuristics.expectedRoll(next.sides))
                    val selectedIndex = supply.indexOf(next)
                    supply = supply.filterIndexed { index, _ -> index != selectedIndex }
                }
                val updated = board.copy(supply = supply, discard = discard)
                Prediction(context.copy(self = context.self.copy(board = updated)), expected)
            }
            else -> Prediction(context)
        }
    }

    private fun choices(
        context: DecisionContext,
        sourceCardId: CreatureCardId? = null,
        excluded: Set<CreatureCardId> = emptySet()
    ): List<OEdelweissChoice> = buildList {
        context.self.board.creature.filter { it.id != sourceCardId && it.id !in excluded }
            .forEach { card ->
                if (card.isFaceUp) {
                    add(OEdelweissChoice.Play(card.toChoice()))
                }
                add(OEdelweissChoice.Flip(card.toChoice()))
            }
        add(OEdelweissChoice.Done)
    }

    private fun plantValue(context: DecisionContext, card: CreatureCardView): PriorityScore =
        if (context.phase == RoundCardType.BATTLE) enabledPlantAnalyzer(context, card).priority
        else cardScorers.forPlant(card).playScore(
            context, CardPhase.CULTIVATION, card.name
        )

    private fun currentCard(
        context: DecisionContext,
        choice: EffectPlantChoice
    ): CreatureCardView? =
        context.self.board.creature.firstOrNull {
            it.id == choice.cardId && it.name == choice.cardName
        }

    private fun CreatureCardView.toChoice(): EffectPlantChoice =
        EffectPlantChoice(
            cardId = id,
            cardName = name,
            isFaceUp = isFaceUp
        )

    private companion object {
        const val DONE_SCORE = 35
        const val FACE_UP_FLIP_PENALTY = 1
    }
}
