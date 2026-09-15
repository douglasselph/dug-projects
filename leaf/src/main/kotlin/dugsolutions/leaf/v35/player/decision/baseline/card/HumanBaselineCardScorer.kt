package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Human-readable, card-local valuation used by the Reasonable Human Baseline.
 *
 * Priority is a decision preference, not a measurement of objective card
 * strength. Card strength is intentionally left for later simulation work.
 */
interface HumanBaselineCardScorer {
    val cardNames: Set<String>
    val effect: GameEffect
    val cultivationPlayBase: Int
    val battlePlayBase: Int

    fun playScore(
        context: DecisionContext,
        phase: CardPhase,
        cardName: String
    ): PriorityScore =
        CardScoringHelpers.playScore(
            context = context,
            phase = phase,
            effect = effect,
            cardName = cardName,
            base = if (phase == CardPhase.BATTLE) battlePlayBase else cultivationPlayBase
        )

    /** Small tie-break style value layered on top of Buy's dominant cost-tier score. */
    fun acquireScore(
        context: DecisionContext,
        card: PlantCard
    ): PriorityScore =
        CardScoringHelpers.acquireScore(
            context = context,
            card = card,
            neutralPlayBase = maxOf(cultivationPlayBase, battlePlayBase)
        )

    /** Value lost by flipping/snipping this card; larger means preserve it. */
    fun lossValue(
        context: DecisionContext,
        card: CreatureCardView
    ): Int =
        CardScoringHelpers.lossValue(
            context = context,
            card = card,
            neutralPlayBase = maxOf(cultivationPlayBase, battlePlayBase)
        )

    fun wispPlayScore(
        context: DecisionContext,
        card: WispCard
    ): PriorityScore =
        CardScoringHelpers.wispPlayScore(
            context = context,
            card = card,
            base = if (CardPhase.from(context.phase) == CardPhase.BATTLE) battlePlayBase else cultivationPlayBase
        )
}

/** Compact implementation used by the one-file-per-card scorer objects. */
abstract class ConfiguredCardScorer(
    final override val cardNames: Set<String>,
    final override val effect: GameEffect,
    final override val cultivationPlayBase: Int,
    final override val battlePlayBase: Int = cultivationPlayBase
) : HumanBaselineCardScorer
