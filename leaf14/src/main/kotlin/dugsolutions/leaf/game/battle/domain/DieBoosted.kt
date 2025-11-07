package dugsolutions.leaf.game.battle.domain

import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.die.DieValue

data class DieBoosted(
    val dieValue: DieValue,
    val insects: List<Token> = emptyList()
) {

    companion object {

        fun from(player: Player): List<DieBoosted> {
            return player.diceInHand.dice.map { DieValue(it.sides, it.value) }.map { DieBoosted(it) }
        }

        fun empty(): DieBoosted {
            return DieBoosted(DieValue(0, 0))
        }
    }

    val attack: Int
        get() = dieValue.value + insects.sumOf { it.attack }

}
