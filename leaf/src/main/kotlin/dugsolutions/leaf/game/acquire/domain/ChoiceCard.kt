package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.components.GameCard

data class ChoiceCard(
    val card: GameCard,
    val combination: Combination
)
