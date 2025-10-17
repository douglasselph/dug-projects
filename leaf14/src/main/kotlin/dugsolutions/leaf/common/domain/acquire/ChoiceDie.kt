package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.random.die.Die

data class ChoiceDie(
    val die: Die,
    val usingDice: UsingDice
)
