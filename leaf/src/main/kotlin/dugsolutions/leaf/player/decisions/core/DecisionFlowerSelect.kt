package dugsolutions.leaf.player.decisions.core

import dugsolutions.leaf.cards.domain.GameCard

interface DecisionFlowerSelect {

    data class Result(
        val value: List<GameCard>
    ) {
        override fun toString(): String {
            return value.joinToString(",") { it.name }
        }
    }

    suspend operator fun invoke(): Result

}
