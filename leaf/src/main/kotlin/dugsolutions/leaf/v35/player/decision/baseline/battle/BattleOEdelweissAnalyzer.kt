package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Bounded one-step Battle valuation for O Edelweiss's current choice.
 *
 * Each real [OEdelweissChoice] is scored only from the current Battle snapshot:
 * replaying a spent Plant uses the same immediate enabled-Plant reasoning used
 * elsewhere in Human Baseline, while Flip values only the Main that toggling the
 * card immediately enables or disables. It deliberately does not search the
 * second O Edelweiss choice.
 *
 * Top-level activation uses the same scoring model over the current Creature,
 * excluding the active O Edelweiss source. Actual execution still rebuilds the
 * legal choices and DecisionContext after the first resolution; no second choice
 * is precommitted here.
 */
class BattleOEdelweissAnalyzer(
    cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    private val enabledPlantAnalyzer: BattleEnabledPlantAnalyzer =
        BattleEnabledPlantAnalyzer(cardScorers)
) {
    fun scoreChoice(
        context: DecisionContext,
        choice: OEdelweissChoice
    ): PriorityScore? {
        if (context.phase != RoundCardType.BATTLE) return null

        return when (choice) {
            OEdelweissChoice.Done -> PriorityScore(DONE_SCORE)

            is OEdelweissChoice.Play -> {
                val card = currentCard(context, choice.card) ?: return null
                if (!card.isFaceDown || choice.card.isFaceUp) return null

                enabledPlantAnalyzer(context, card).priority.adjusted(
                    0,
                    "Replay spent Plant for its best immediate Battle value"
                )
            }

            is OEdelweissChoice.Flip -> {
                val card = currentCard(context, choice.card) ?: return null
                if (card.isFaceUp != choice.card.isFaceUp) return null

                val opportunity = enabledPlantAnalyzer(context, card).priority
                if (card.isFaceDown) {
                    opportunity.adjusted(
                        -FLIP_ENABLE_DELAY,
                        "Flip spent Plant face up to enable its immediate Battle Main"
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

    /**
     * Values the top-level O Edelweiss activation from the best CURRENT first
     * choice only. Returning that same choice score, rather than adding it to
     * O Edelweiss's generic card base, keeps the action from looking attractive
     * when its only sensible current choice is Done and avoids double-counting
     * the replayed/enabled Plant's own value.
     */
    fun topLevelPriority(
        context: DecisionContext,
        sourceCardId: CreatureCardId
    ): PriorityScore? {
        if (context.phase != RoundCardType.BATTLE) return null

        return topLevelChoices(context, sourceCardId)
            .mapNotNull { scoreChoice(context, it) }
            .maxByOrNull { it.total }
            ?.adjusted(0, "Best current one-step O Edelweiss Battle choice")
            ?: PriorityScore(DONE_SCORE)
    }

    private fun topLevelChoices(
        context: DecisionContext,
        sourceCardId: CreatureCardId
    ): List<OEdelweissChoice> =
        buildList {
            context.self.board.creature
                .filter { it.id != sourceCardId }
                .forEach { card ->
                    val choice = card.toChoice()
                    if (card.isFaceDown) {
                        // Whether a nested effect is executable remains an engine
                        // legality question. This bounded projection deliberately
                        // does not execute or pre-resolve it.
                        add(OEdelweissChoice.Play(choice))
                    }
                    add(OEdelweissChoice.Flip(choice))
                }
            add(OEdelweissChoice.Done)
        }

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
        const val FLIP_ENABLE_DELAY = 1
        const val FACE_UP_FLIP_PENALTY = 1
    }
}
