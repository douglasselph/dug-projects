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
        val next = projectedContext(context, choice) ?: return null
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
            scoreChoice(next, candidate)?.let { candidate to it }
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

    private fun projectedContext(context: DecisionContext, choice: OEdelweissChoice): DecisionContext? {
        val id = when (choice) {
            is OEdelweissChoice.Play -> choice.card.cardId
            is OEdelweissChoice.Flip -> choice.card.cardId
            OEdelweissChoice.Done -> return context
        }
        if (context.self.board.creature.none { it.id == id }) return null
        val updated = context.self.board.creature.map { card ->
            if (card.id != id) card else when (choice) {
                is OEdelweissChoice.Play -> card.copy(facing = CreatureCard.Facing.FACE_DOWN)
                is OEdelweissChoice.Flip -> card.copy(facing = if (card.isFaceUp) CreatureCard.Facing.FACE_DOWN else CreatureCard.Facing.FACE_UP)
                else -> card
            }
        }
        return context.copy(self = context.self.copy(board = context.self.board.copy(creature = updated)))
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
