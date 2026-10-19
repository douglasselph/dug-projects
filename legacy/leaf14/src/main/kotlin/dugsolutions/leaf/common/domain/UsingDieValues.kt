package dugsolutions.leaf.common.domain

import dugsolutions.leaf.random.die.DieValues

data class UsingDieValues(
    val values: DieValues = DieValues(emptyList())
)
