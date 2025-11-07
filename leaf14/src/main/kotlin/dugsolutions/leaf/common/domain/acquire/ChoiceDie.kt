package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValues

data class ChoiceDie(
    val die: Die,
    val usingDice: DieValues
)
