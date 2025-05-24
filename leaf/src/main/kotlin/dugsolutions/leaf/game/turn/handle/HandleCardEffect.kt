package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.game.turn.select.SelectDieToAdjust
import dugsolutions.leaf.game.turn.select.SelectDieToMax
import dugsolutions.leaf.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.domain.AppliedEffect

class HandleCardEffect(
    private val selectDieToReroll: SelectDieToReroll,
    private val selectCardToRetain: SelectCardToRetain,
    private val selectDieToRetain: SelectDieToRetain,
    private val selectDieToAdjust: SelectDieToAdjust,
    private val selectDieToMax: SelectDieToMax,
    private val costScore: CostScore,
    private val handleDieUpgrade: HandleDieUpgrade,
    private val handleLimitedDieUpgrade: HandleLimitedDieUpgrade,
    private val cardManager: CardManager,
    private val chronicle: GameChronicle
) {

    data class EffectResult(
        val newCards: List<GameCard>,
        val diceAdjusted: Boolean = false
    ) {
        val hasMoreToProcess: Boolean
            get() = newCards.isNotEmpty() || diceAdjusted
    }

    private val newCards = mutableListOf<GameCard>()
    private var diceAdjusted = false

    operator fun invoke(player: Player, target: Player): EffectResult {
        newCards.clear()
        diceAdjusted = false
        for (effect in player.effectsList.copy()) {
            if (handleEffect(effect, player, target)) {
                player.effectsList.remove(effect)
            }
        }
        return EffectResult(newCards, diceAdjusted)
    }

    private fun handleEffect(effect: AppliedEffect, player: Player, target: Player): Boolean {
        return when (effect) {

            is AppliedEffect.AddToTotal -> {
                player.pipModifier += effect.amount
                chronicle(GameChronicle.Moment.ADD_TO_TOTAL(player, effect.amount))
                true
            }

            is AppliedEffect.Adorn -> {
                player.addCardToFloralArray(effect.flowerCard)
                chronicle(GameChronicle.Moment.ADORN(player, effect.flowerCard))
                true
            }

            is AppliedEffect.AdjustDieRoll -> {
                if (effect.canTargetPlayer && player.decisionDirector.shouldTargetPlayer(target, effect.adjustment)) {
                    selectDieToAdjust(target.diceInHand, -effect.adjustment)?.let { die ->
                        target.diceInHand.adjust(die, -effect.adjustment)
                        chronicle(GameChronicle.Moment.ADJUST_DIE(target, -effect.adjustment))
                    }
                } else {
                    selectDieToAdjust(player.diceInHand, effect.adjustment)?.let { selectedDie ->
                        if (player.diceInHand.adjust(selectedDie, effect.adjustment)) {
                            chronicle(
                                GameChronicle.Moment.ADJUST_DIE(player, effect.adjustment)
                            )
                        }
                    }
                }
                true
            }

            is AppliedEffect.AdjustDieToMax -> {
                // Choose die with with the greatest difference against MAX.
                selectDieToMax(player.diceInHand)?.let { die ->
                    val amount = die.adjustToMax()
                    chronicle(GameChronicle.Moment.ADJUST_DIE(player, amount))
                }
                true
            }

            is AppliedEffect.DeflectDamage -> {
                player.deflectDamage += effect.amount
                chronicle(GameChronicle.Moment.DEFLECT_DAMAGE(player, effect.amount))
                true
            }

            is AppliedEffect.DrawCards -> {
                repeat(effect.count) {
                    val cardId = if (effect.fromCompost) {
                        player.drawCardFromCompost()
                    } else {
                        player.drawCard()
                    }
                    cardId?.let {
                        chronicle(GameChronicle.Moment.DRAW_CARD(player, cardId))
                        cardManager.getCard(cardId)?.let { newCards.add(it) }
                    }
                }
                true
            }

            is AppliedEffect.DrawDice -> {
                repeat(effect.count) {
                    val die = if (effect.fromCompost) {
                        if (effect.drawHighest) {
                            player.drawBestDieFromCompost()
                        } else {
                            player.drawDieFromCompost()
                        }
                    } else {
                        if (effect.drawHighest) {
                            player.drawBestDie()
                        } else {
                            player.drawDie()
                        }
                    }
                    die?.let {
                        diceAdjusted = true
                        chronicle(GameChronicle.Moment.DRAW_DIE(player, die))
                    }
                }
                true
            }

            is AppliedEffect.DrawThenDiscard -> {
                // TODO: Draw then discard
                true
            }

            is AppliedEffect.Discard -> {
                if (effect.cardsOnly) {
                    // Choose card with the lowest cost.
                    val cardToDiscard = target.cardsInHand.minByOrNull { costScore(it.cost) }
                    if (cardToDiscard != null) {
                        target.discard(cardToDiscard.id)
                        chronicle(GameChronicle.Moment.DISCARD_CARD(target, cardToDiscard))
                    }
                } else if (effect.diceOnly) {
                    // Choose the die with the lowest rolled value
                    val dieToDiscard = target.diceInHand.dice.minByOrNull { it.value }
                    if (dieToDiscard != null) {
                        target.discard(dieToDiscard)
                        chronicle(GameChronicle.Moment.DISCARD_DIE(target, dieToDiscard))
                    }
                } else {
                    // If any die rolled a 1 or 2, choose one of them (the lowest).
                    // Otherwise just choose the card with the lowest cost.
                    val lowDice = target.diceInHand.dice.filter { it.value <= 2 }
                    if (lowDice.isNotEmpty()) {
                        val dieToDiscard = lowDice.minByOrNull { it.value }
                        if (dieToDiscard != null) {
                            target.discard(dieToDiscard)
                            chronicle(GameChronicle.Moment.DISCARD_DIE(target, dieToDiscard))
                        }
                    } else {
                        val cardToDiscard = target.cardsInHand.minByOrNull { costScore(it.cost) }
                        if (cardToDiscard != null) {
                            target.discard(cardToDiscard.id)
                            chronicle(GameChronicle.Moment.DISCARD_CARD(target, cardToDiscard))
                        }
                    }
                }
                true
            }

            is AppliedEffect.RerollDie -> {
                selectDieToReroll(player.diceInHand)?.let { die ->
                    die.roll()
                    diceAdjusted = true
                    chronicle(GameChronicle.Moment.REROLL(player, die))
                }
                true
            }

            is AppliedEffect.RetainCard -> {
                selectCardToRetain(player.cardsInHand, null)?.let { card ->
                    player.retainCard(card.id)
                    chronicle(GameChronicle.Moment.RETAIN_CARD(player, card))
                }
                // Choose BLOOM card, then VINE, then CANOPY, then ROOT
                // Choose card with the largest COST.
                true
            }

            is AppliedEffect.RetainDie -> {
                selectDieToRetain(player.diceInHand)?.let { die ->
                    player.retainDie(die)
                    chronicle(GameChronicle.Moment.RETAIN_DIE(player, die))
                }
                true
            }

            is AppliedEffect.Reuse -> {
                selectCardToRetain(player.cardsInHand, effect.flourishType)?.let { card ->
                    player.cardsReused.add(card)
                    chronicle(GameChronicle.Moment.REUSE_CARD(player, card))
                }
                true
            }

            is AppliedEffect.UpgradeDie -> {
                val die = if (effect.only.isNotEmpty()) {
                    handleLimitedDieUpgrade(player, effect.only, effect.discardAfterUse)
                } else {
                    handleDieUpgrade(player, effect.discardAfterUse)
                }
                die?.let {
                    chronicle(GameChronicle.Moment.UPGRADE_DIE(player, die))
                }
                true
            }


            else -> false
        }
    }

} 
