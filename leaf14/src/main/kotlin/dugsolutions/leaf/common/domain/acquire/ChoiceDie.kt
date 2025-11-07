package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.random.die.DieSides
import dugsolutions.leaf.random.die.DieValues

data class ChoiceDie(
    val dieSides: DieSides,
    val usingDice: DieValues
)
