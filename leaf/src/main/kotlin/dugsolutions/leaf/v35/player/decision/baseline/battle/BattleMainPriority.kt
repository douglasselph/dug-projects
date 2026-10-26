package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
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
            BattleMainAction.Draw -> BattleDrawUtility.score(context)
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
                val directDie = BattleDirectDieActivationPriority()
                val reroll = BattleRerollActivationPriority()
                if (sapping.isSapping(effectiveEffect)) {
                    // Top-level action and downstream target selection use the
                    // same complete deterministic projector, not a second
                    // initial-Raise/row-need bonus from CardScoringHelpers.
                    sapping.score(context, effectiveEffect, intrinsic.base) ?: BattlePlantFallbackScore.normalize(intrinsic)
                 } else if (collateral.supports(effectiveEffect)) {
                    collateral.score(context, effectiveEffect, intrinsic.base) ?: BattlePlantFallbackScore.normalize(intrinsic)
                } else if (effectiveEffect == GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS) {
                    reroll.score(context, effectiveEffect, intrinsic) ?: BattlePlantFallbackScore.normalize(intrinsic)
                } else if (effectiveEffect in BattleDirectDieActivationPriority.DIRECT_EFFECTS) {
                    directDie.score(context, effectiveEffect, intrinsic) ?: BattlePlantFallbackScore.normalize(intrinsic)
                } else if (effectiveEffect in setOf(
                        GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                    )) {
                    BattleOEdelweissAnalyzer(cardScorers)
                        .topLevelPriority(context, action.card.id)
                        ?: BattlePlantFallbackScore.normalize(intrinsic)
                } else {
                    BattlePlantFallbackScore.normalize(intrinsic)
                }
            }
            BattleMainAction.RoundEffect1 -> BattleRoundEffectPriority.score(context, roundCard.firstEffect.effect)
            BattleMainAction.RoundEffect2 -> BattleRoundEffectPriority.score(context, roundCard.secondEffect.effect)
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

}
