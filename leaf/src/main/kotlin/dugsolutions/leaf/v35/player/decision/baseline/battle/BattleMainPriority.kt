package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.DrawPriority
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard

object BattleMainPriority {
    fun intrinsicScore(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): PriorityScore =
        when (action) {
            BattleMainAction.Draw -> DrawPriority.score(context)
            is BattleMainAction.ActivatePlant -> {
                val intrinsic = cardScorers.forPlant(action.card.card).playScore(
                    context = context,
                    phase = CardPhase.BATTLE,
                    cardName = action.card.card.name
                )
                val effectiveEffect = context.self.board.creature.firstOrNull { it.id == action.card.id }?.effect
                    ?: action.card.card.effect
                val sapping = BattleSappingActivationPriority()
                val collateral = BattleCollateralActivationPriority()
                if (sapping.isSapping(effectiveEffect)) {
                    // Top-level action and downstream target selection use the
                    // same complete deterministic projector, not a second
                    // initial-Raise/row-need bonus from CardScoringHelpers.
                    sapping.score(context, effectiveEffect, intrinsic.base) ?: intrinsic
                 } else if (collateral.supports(effectiveEffect)) {
                    collateral.score(context, effectiveEffect, intrinsic.base) ?: intrinsic
                } else if (effectiveEffect in setOf(
                        GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                    )) {
                    BattleOEdelweissAnalyzer(cardScorers)
                        .topLevelPriority(context, action.card.id)
                        ?: intrinsic
                } else {
                    intrinsic
                }
            }
            BattleMainAction.RoundEffect1 -> scoreRoundEffect(roundCard.firstEffect.effect)
            BattleMainAction.RoundEffect2 -> scoreRoundEffect(roundCard.secondEffect.effect)
        }

    /**
     * All ordinary Battle Main call sites (First Main, Final Main, Sunlight-funded
     * Main, enabling-Support preview) must use the same consequence-aware scorer.
     * In particular, Final Main Draw must not get an unconditional +10 unrelated
     * to legal placement or projected Strike consequences.
     */
    fun score(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): PriorityScore = BattleFirstMainPriority(cardScorers)(context, roundCard, action)

    private fun scoreRoundEffect(effect: GameEffect): PriorityScore =
        when (effect) {
            GameEffect.GAIN_ONE_VP -> PriorityScore(60)
            GameEffect.GAIN_D20_TO_DISCARD -> PriorityScore(65)
            GameEffect.GAIN_D12_TO_DISCARD -> PriorityScore(58)
            GameEffect.GAIN_D10_TO_DISCARD -> PriorityScore(54)
            GameEffect.GAIN_ONE_WISP -> PriorityScore(50)
            GameEffect.GAIN_ANY_DIE_TO_DISCARD -> PriorityScore(60)
            GameEffect.REFRESH_CREATURE -> PriorityScore(55)
            GameEffect.GAIN_TWO_WORMS -> PriorityScore(55)
            GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT -> PriorityScore(58)
            GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS -> PriorityScore(70)
            else -> PriorityScore(45)
        }
}
