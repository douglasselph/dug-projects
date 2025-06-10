package dugsolutions.leaf.game.acquire.domain

import dugsolutions.leaf.random.die.Die

data class ChoiceDie(
    val die: Die,
    val combination: Combination
)
