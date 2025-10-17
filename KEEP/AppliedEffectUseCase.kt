package dugsolutions.leaf.player.effect

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect
import dugsolutions.leaf.player.domain.CardOrDie
import dugsolutions.leaf.random.die.DieSides

class AppliedEffectUseCase(
    private val chronicle: GameChronicle
) {

    operator fun invoke(
        player: Player,
        effect: CardEffect,
        value: Int
    ): AppliedEffect? {
        return when (effect) {

            CardEffect.ADD_TO_DIE -> AppliedEffect.AdjustDieRoll(value)

            CardEffect.ADD_TO_TOTAL -> AppliedEffect.AddToTotal(value)

            CardEffect.ADJUST_BY -> AppliedEffect.AdjustDieRoll(value, canTargetPlayer = false)

            CardEffect.ADJUST_TO_MAX -> AppliedEffect.AdjustDieToMax()

            CardEffect.ADJUST_TO_MIN_OR_MAX -> AppliedEffect.AdjustDieToMax(minOrMax = true)

            CardEffect.ADORN -> {
                processAdornEffect(player)
                null
            }

            CardEffect.DEFLECT -> {
                player.deflectDamage += value
                chronicle(Moment.DEFLECT_DAMAGE(player, value))
                null
            }

            CardEffect.DISCARD -> AppliedEffect.Discard(value)

            CardEffect.DISCARD_CARD -> AppliedEffect.Discard(value, cardsOnly = true)

            CardEffect.DISCARD_DIE -> AppliedEffect.Discard(value, diceOnly = true)

            CardEffect.DRAW_CARD -> AppliedEffect.DrawCards(value)

            CardEffect.DRAW_CARD_COMPOST -> AppliedEffect.DrawCards(value, fromCompost = true)

            CardEffect.DRAW_DIE -> AppliedEffect.DrawDice(value)

            CardEffect.DRAW_DIE_ANY -> AppliedEffect.DrawDice(value, drawHighest = true)

            CardEffect.DRAW_DIE_COMPOST -> AppliedEffect.DrawDice(value, fromCompost = true)

            CardEffect.DRAW_THEN_DISCARD -> AppliedEffect.DrawThenDiscard(
                drawCount = value,
                discardCount = value - 1
            )

            CardEffect.FLOURISH_OVERRIDE -> AppliedEffect.FlourishOverride

            CardEffect.GAIN_FREE_ROOT -> AppliedEffect.MarketBenefit(
                type = FlourishType.ROOT,
                costReduction = 0,
                isFree = true,
            )

            CardEffect.GAIN_FREE_CANOPY -> AppliedEffect.MarketBenefit(
                type = FlourishType.CANOPY,
                costReduction = 0,
                isFree = true,
            )

            CardEffect.GAIN_FREE_VINE -> AppliedEffect.MarketBenefit(
                type = FlourishType.VINE,
                costReduction = 0,
                isFree = true
            )

            CardEffect.REDUCE_COST_ROOT -> AppliedEffect.MarketBenefit(
                type = FlourishType.ROOT,
                costReduction = value
            )

            CardEffect.REDUCE_COST_CANOPY -> AppliedEffect.MarketBenefit(
                type = FlourishType.CANOPY,
                costReduction = value
            )

            CardEffect.REDUCE_COST_VINE -> AppliedEffect.MarketBenefit(
                type = FlourishType.VINE,
                costReduction = value
            )

            CardEffect.REROLL_ACCEPT_2ND -> AppliedEffect.RerollDie(value, mustAcceptSecond = true)

            CardEffect.REROLL_ALL_MAX -> AppliedEffect.RerollDie(value, forceMax = true)

            CardEffect.REROLL_TAKE_BETTER -> AppliedEffect.RerollDie(value, takeBetter = true)

            CardEffect.RETAIN_CARD -> AppliedEffect.RetainCard

            CardEffect.RETAIN_DIE -> AppliedEffect.RetainDie()

            CardEffect.RETAIN_DIE_REROLL -> AppliedEffect.RetainDie(withReroll = true)

            CardEffect.REUSE_CARD -> AppliedEffect.Reuse(cardOrDie = CardOrDie.Card)

            CardEffect.REUSE_DIE -> AppliedEffect.Reuse(cardOrDie = CardOrDie.Die)

            CardEffect.REUSE_ANY -> AppliedEffect.Reuse(cardOrDie = CardOrDie.Any)

            CardEffect.REPLAY_VINE -> AppliedEffect.Replay(flourishType = FlourishType.VINE)

            CardEffect.UPGRADE_ANY_RETAIN -> AppliedEffect.UpgradeDie()

            CardEffect.UPGRADE_ANY -> AppliedEffect.UpgradeDie(discardAfterUse = true)

            CardEffect.UPGRADE_D4 -> AppliedEffect.UpgradeDie(discardAfterUse = false, only = listOf(DieSides.D4))

            CardEffect.UPGRADE_D6 -> AppliedEffect.UpgradeDie(discardAfterUse = true, only = listOf(DieSides.D4, DieSides.D6))

            CardEffect.UPGRADE_D8 -> AppliedEffect.UpgradeDie(
                discardAfterUse = true,
                only = listOf(DieSides.D4, DieSides.D6, DieSides.D8)
            )

            CardEffect.UPGRADE_D10 -> AppliedEffect.UpgradeDie(
                discardAfterUse = true,
                only = listOf(DieSides.D4, DieSides.D6, DieSides.D8, DieSides.D10)
            )

            CardEffect.USE_OPPONENT_CARD -> AppliedEffect.UseOpponent(cardOrDie = CardOrDie.Card)

            CardEffect.USE_OPPONENT_DIE -> AppliedEffect.UseOpponent(cardOrDie = CardOrDie.Die)
        }
    }
}
