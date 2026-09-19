package dugsolutions.leaf.v14.game.turn.effect

import dugsolutions.leaf.v14.player.Player

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
