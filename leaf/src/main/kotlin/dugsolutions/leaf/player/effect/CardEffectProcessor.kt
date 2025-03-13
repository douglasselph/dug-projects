package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CardOrDie
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.player.Player

/**
 * Main processor for all effects in the game.
 * This class coordinates between different effect types and their application to players.
 */
class CardEffectProcessor(
    private val shouldProcessMatchEffect: ShouldProcessMatchEffect
) {
    /**
     * Process a card effect and return the list of applied effects
     */
    fun processCardEffect(
        card: GameCard,
        player: Player
    ): List<AppliedEffect> {
        val effects = mutableListOf<AppliedEffect>()

        // Process primary effect
        if (card.primaryEffect != null) {
            effects.add(processEffect(card.primaryEffect, card.primaryValue))
        }
        // Process match effect if applicable
        if (card.matchEffect != null) {
            if (shouldProcessMatchEffect(card, player)) {
                effects.add(processEffect(card.matchEffect, card.matchValue))
            }
        }
        // Process trash effect if applicable
        card.trashEffect?.let {
            if (player.decisionDirector.shouldProcessTrashEffect(card)) {
                effects.add(processEffect(card.trashEffect, card.trashValue, card.id))
                // Remove card now
                player.removeCardFromHand(card.id)
            }
        }
        return effects
    }

    /**
     * Process a single effect and return the resulting applied effect
     */
    private fun processEffect(
        effect: CardEffect,
        value: Int,
        trashAfterUse: CardID? = null
    ): AppliedEffect {
        return when (effect) {

            CardEffect.ADD_TO_DIE -> AppliedEffect.AdjustDieRoll(value, trashAfterUse = trashAfterUse)

            CardEffect.ADD_TO_TOTAL -> AppliedEffect.AddToTotal(value, trashAfterUse = trashAfterUse)

            CardEffect.ADJUST_BY -> AppliedEffect.AdjustDieRoll(value, canTargetPlayer = true, trashAfterUse = trashAfterUse)

            CardEffect.ADJUST_TO_MAX -> AppliedEffect.AdjustDieToMax(trashAfterUse = trashAfterUse)

            CardEffect.ADJUST_TO_MIN_OR_MAX -> AppliedEffect.AdjustDieToMax(trashAfterUse = trashAfterUse, minOrMax = true)

            CardEffect.DEFLECT -> AppliedEffect.DeflectDamage(value, trashAfterUse = trashAfterUse)

            CardEffect.DISCARD -> AppliedEffect.Discard(value, trashAfterUse = trashAfterUse)

            CardEffect.DISCARD_CARD -> AppliedEffect.Discard(value, cardsOnly = true, trashAfterUse = trashAfterUse)

            CardEffect.DISCARD_DIE -> AppliedEffect.Discard(value, diceOnly = true, trashAfterUse = trashAfterUse)

            CardEffect.DRAW_CARD -> AppliedEffect.DrawCards(value, trashAfterUse = trashAfterUse)

            CardEffect.DRAW_CARD_COMPOST -> AppliedEffect.DrawCards(value, fromCompost = true, trashAfterUse = trashAfterUse)

            CardEffect.DRAW_DIE -> AppliedEffect.DrawDice(value, trashAfterUse = trashAfterUse)

            CardEffect.DRAW_DIE_ANY -> AppliedEffect.DrawDice(value, drawHighest = true, trashAfterUse = trashAfterUse)

            CardEffect.DRAW_DIE_COMPOST -> AppliedEffect.DrawDice(value, fromCompost = true, trashAfterUse = trashAfterUse)

            CardEffect.DRAW_THEN_DISCARD -> AppliedEffect.DrawThenDiscard(
                drawCount = value,
                discardCount = value - 1,
                trashAfterUse = trashAfterUse
            )

            CardEffect.FLOURISH_OVERRIDE -> AppliedEffect.FlourishOverride(trashAfterUse = trashAfterUse)

            CardEffect.GAIN_FREE_ROOT -> AppliedEffect.MarketBenefit(
                type = FlourishType.ROOT,
                costReduction = 0,
                isFree = true,
                trashAfterUse = trashAfterUse
            )

            CardEffect.GAIN_FREE_CANOPY -> AppliedEffect.MarketBenefit(
                type = FlourishType.CANOPY,
                costReduction = 0,
                isFree = true,
                trashAfterUse = trashAfterUse
            )

            CardEffect.GAIN_FREE_VINE -> AppliedEffect.MarketBenefit(
                type = FlourishType.VINE,
                costReduction = 0,
                isFree = true,
                trashAfterUse = trashAfterUse
            )

            CardEffect.REDUCE_COST_ROOT -> AppliedEffect.MarketBenefit(
                type = FlourishType.ROOT,
                costReduction = value,
                trashAfterUse = trashAfterUse
            )

            CardEffect.REDUCE_COST_CANOPY -> AppliedEffect.MarketBenefit(
                type = FlourishType.CANOPY,
                costReduction = value,
                trashAfterUse = trashAfterUse
            )

            CardEffect.REDUCE_COST_VINE -> AppliedEffect.MarketBenefit(
                type = FlourishType.VINE,
                costReduction = value,
                trashAfterUse = trashAfterUse
            )

            CardEffect.REROLL_ACCEPT_2ND -> AppliedEffect.RerollDie(value, mustAcceptSecond = true, trashAfterUse = trashAfterUse)

            CardEffect.REROLL_ALL_MAX -> AppliedEffect.RerollDie(value, forceMax = true, trashAfterUse = trashAfterUse)

            CardEffect.REROLL_TAKE_BETTER -> AppliedEffect.RerollDie(value, takeBetter = true, trashAfterUse = trashAfterUse)

            CardEffect.RETAIN_CARD -> AppliedEffect.RetainCard(trashAfterUse = trashAfterUse)

            CardEffect.RETAIN_DIE -> AppliedEffect.RetainDie(trashAfterUse = trashAfterUse)

            CardEffect.RETAIN_DIE_REROLL -> AppliedEffect.RetainDie(withReroll = true, trashAfterUse = trashAfterUse)

            CardEffect.REUSE_CARD -> AppliedEffect.Reuse(trashAfterUse = trashAfterUse, cardOrDie = CardOrDie.Card)

            CardEffect.REUSE_DIE -> AppliedEffect.Reuse(trashAfterUse = trashAfterUse, cardOrDie = CardOrDie.Die)

            CardEffect.REPLAY_VINE -> AppliedEffect.Replay(flourishType = FlourishType.VINE, trashAfterUse = trashAfterUse)

            CardEffect.THORN -> AppliedEffect.ThornEffect(value, trashAfterUse = trashAfterUse)

            CardEffect.UPGRADE_ANY_RETAIN -> AppliedEffect.UpgradeDie(trashAfterUse = trashAfterUse)

            CardEffect.UPGRADE_ANY -> AppliedEffect.UpgradeDie(discardAfterUse = true, trashAfterUse = trashAfterUse)

            CardEffect.UPGRADE_D4 -> AppliedEffect.UpgradeDie(discardAfterUse = true, only = listOf(DieSides.D4), trashAfterUse = trashAfterUse)

            CardEffect.UPGRADE_D6 -> AppliedEffect.UpgradeDie(discardAfterUse = true, only = listOf(DieSides.D6), trashAfterUse = trashAfterUse)

            CardEffect.UPGRADE_D4_D6 -> AppliedEffect.UpgradeDie(discardAfterUse = true, only = listOf(DieSides.D4, DieSides.D6), trashAfterUse = trashAfterUse)

            CardEffect.USE_OPPONENT_CARD -> AppliedEffect.UseOpponent(trashAfterUse = trashAfterUse, cardOrDie = CardOrDie.Card)

            CardEffect.USE_OPPONENT_DIE -> AppliedEffect.UseOpponent(trashAfterUse = trashAfterUse, cardOrDie = CardOrDie.Die)

            else -> throw IllegalArgumentException("Unsupported effect: $effect")
        }
    }
} 
