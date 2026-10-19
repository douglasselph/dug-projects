package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die

interface DecisionDamageAbsorption {

    data class Result(
        val handCards: List<GameCard> = emptyList(),
        val handDice: List<Die> = emptyList(),
        val creatureCards: List<GameCard> = emptyList(),
        val thorn: Int = 0,
        val damageAbsorbed: Int = 0,
        val damageStillLeftToAbsorb: Int = 0
    )

    suspend operator fun invoke(): Result

}
