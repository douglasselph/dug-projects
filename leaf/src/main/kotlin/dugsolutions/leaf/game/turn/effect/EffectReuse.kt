package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.player.Player

class EffectReuse(
    private val effectReuseCard: EffectReuseCard,
    private val effectReuseDie: EffectReuseDie
) {

    operator fun invoke(player: Player) {
        val die = player.diceInHand.dice.maxByOrNull { it.value }
        if (die != null) {
            if (die.sides >= 10) {
                effectReuseDie(player, rerollOkay = false)
                return
            }
        }
        effectReuseCard(player)
    }
}
