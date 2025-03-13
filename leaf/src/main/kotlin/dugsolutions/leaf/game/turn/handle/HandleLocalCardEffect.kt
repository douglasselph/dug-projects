package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.game.turn.select.SelectCardToRetain
import dugsolutions.leaf.game.turn.select.SelectDieToReroll
import dugsolutions.leaf.game.turn.select.SelectDieToRetain
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.AppliedEffect

class HandleLocalCardEffect(
    private val selectDieToReroll: SelectDieToReroll,
    private val selectCardToRetain: SelectCardToRetain,
    private val selectDieToRetain: SelectDieToRetain,
    private val handleDieUpgrade: HandleDieUpgrade,
    private val handleLimitedDieUpgrade: HandleLimitedDieUpgrade,
    private val chronicle: GameChronicle
) {

    operator fun invoke(player: Player) {
        for (effect in player.effectsList.copy()) {
            if (handleEffect(effect, player)) {
                player.effectsList.remove(effect)
            }
        }
    }

    private fun handleEffect(effect: AppliedEffect, player: Player): Boolean {
        return when (effect) {

            // TODO: Not sure where to put this.
            is AppliedEffect.RerollDie -> {
                selectDieToReroll(player.diceInHand)?.let { die ->
                    die.roll()
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
                die?.let { chronicle(GameChronicle.Moment.UPGRADE_DIE(player, die)) }
                true
            }

            is AppliedEffect.ThornEffect -> {
                player.thornDamage += effect.damage
                chronicle(GameChronicle.Moment.ADD_TO_THORN(player, effect.damage))
                true
            }

            is AppliedEffect.DeflectDamage -> {
                player.deflectDamage += effect.amount
                chronicle(GameChronicle.Moment.DEFLECT_DAMAGE(player, effect.amount))
                true
            }

            else -> false
        }
    }

} 
