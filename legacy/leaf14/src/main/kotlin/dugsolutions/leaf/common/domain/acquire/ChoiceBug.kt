package dugsolutions.leaf.common.domain.acquire

import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.random.die.DieValue

data class ChoiceBug(
    val bug: Token,
    val usingDie: DieValue
)
