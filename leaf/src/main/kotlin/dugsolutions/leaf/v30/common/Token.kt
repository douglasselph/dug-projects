package dugsolutions.leaf.v30.common

import dugsolutions.leaf.v30.random.die.DieSides

sealed class Token {
    object WATER : Token()
    data class MULCH(val sides: DieSides? = null) : Token()
}
