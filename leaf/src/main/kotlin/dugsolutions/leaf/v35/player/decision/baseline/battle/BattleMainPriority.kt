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
            is BattleMainAction.ActivatePlant ->
                cardScorers.forPlant(action.card.card).playScore(
                    context = context,
                    phase = CardPhase.BATTLE,
                    cardName = action.card.card.name
                )
            BattleMainAction.RoundEffect1 -> scoreRoundEffect(roundCard.firstEffect.effect)
            BattleMainAction.RoundEffect2 -> scoreRoundEffect(roundCard.secondEffect.effect)
        }

    /**
     * Legacy Step-5 Main score retained until B14 replaces the flat Support/Final-Main
     * orchestration. First Main uses [BattleFirstMainPriority] from B7 instead.
     */
    fun score(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleMainAction,
        cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
    ): PriorityScore {
        val intrinsic = intrinsicScore(context, roundCard, action, cardScorers)
        return if (action == BattleMainAction.Draw) {
            intrinsic.adjusted(10, "An extra Battle die can reinforce a needy row")
        } else {
            intrinsic
        }
    }

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
