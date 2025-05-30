package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.CardOrDie
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.core.DecisionShouldProcessTrashEffect
import dugsolutions.leaf.player.domain.AppliedEffect

/**
 * Main processor for all effects in the game.
 * This class coordinates between different effect types and their application to players.
 */
class CardEffectProcessor(
    private val canProcessMatchEffect: CanProcessMatchEffect
) {

    private val effects = mutableListOf<AppliedEffect>()

    /**
     * Process a card effect and return the list of applied effects
     */
    operator fun invoke(
        card: GameCard,
        player: Player
    ): List<AppliedEffect> {
        effects.clear()

        // Process primary effect
        if (card.primaryEffect != null) {
            processEffect(card, card.primaryEffect, card.primaryValue)
        }
        // Process match effect if applicable
        if (card.matchEffect != null) {
            val result = canProcessMatchEffect(card, player)
            if (result.possible) {
                result.dieCost?.let { player.discard(result.dieCost) }
                processEffect(card, card.matchEffect, matchValue(player, card))
            }
        }
        // See if we should trash this card right now.
        // This is only taking into consideration non-battle affecting trash effects.
        card.trashEffect?.let {
            when (player.decisionDirector.shouldProcessTrashEffect(card)) {
                DecisionShouldProcessTrashEffect.Result.TRASH -> {
                    processEffect(card, card.trashEffect, card.trashValue)
                    // Remove card now
                    player.removeCardFromHand(card.id)
                }

                DecisionShouldProcessTrashEffect.Result.TRASH_IF_NEEDED -> {
                    player.effectsList.add(AppliedEffect.TrashIfNeeded(card))
                }

                else -> {}
            }
        }
        return effects
    }

    /**
     * Process a single effect and return the resulting applied effect
     */
    private fun processEffect(
        card: GameCard,
        effect: CardEffect,
        value: Int
    ) {
        val appliedEffect = when (effect) {

            CardEffect.ADD_TO_DIE -> AppliedEffect.AdjustDieRoll(value)

            CardEffect.ADD_TO_TOTAL -> AppliedEffect.AddToTotal(value)

            CardEffect.ADJUST_BY -> AppliedEffect.AdjustDieRoll(value, canTargetPlayer = true)

            CardEffect.ADJUST_TO_MAX -> AppliedEffect.AdjustDieToMax()

            CardEffect.ADJUST_TO_MIN_OR_MAX -> AppliedEffect.AdjustDieToMax(minOrMax = true)

            CardEffect.ADORN -> AppliedEffect.Adorn(card.id)

            CardEffect.DEFLECT -> AppliedEffect.DeflectDamage(value)

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

            CardEffect.RESILIENCE_BOOST -> AppliedEffect.ResilienceBoost(value)

            CardEffect.UPGRADE_ANY_RETAIN -> AppliedEffect.UpgradeDie()

            CardEffect.UPGRADE_ANY -> AppliedEffect.UpgradeDie(discardAfterUse = true)

            CardEffect.UPGRADE_D4 -> AppliedEffect.UpgradeDie(discardAfterUse = true, only = listOf(DieSides.D4))

            CardEffect.UPGRADE_D6 -> AppliedEffect.UpgradeDie(discardAfterUse = true, only = listOf(DieSides.D6))

            CardEffect.UPGRADE_D4_D6 -> AppliedEffect.UpgradeDie(discardAfterUse = true, only = listOf(DieSides.D4, DieSides.D6))

            CardEffect.USE_OPPONENT_CARD -> AppliedEffect.UseOpponent(cardOrDie = CardOrDie.Card)

            CardEffect.USE_OPPONENT_DIE -> AppliedEffect.UseOpponent(cardOrDie = CardOrDie.Die)

            else -> throw IllegalArgumentException("Unsupported effect: $effect")
        }
        effects.add(appliedEffect)
    }

    private fun matchValue(player: Player, card: GameCard): Int {
        return if (card.type == FlourishType.BLOOM) {
            player.flowerCount(card)
        } else card.matchValue
    }
} 
