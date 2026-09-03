package dugsolutions.leaf.v35.tokens

import dugsolutions.leaf.v35.random.die.DieSides

sealed class Token {
    object WATER : Token()
    data class MULCH(val sides: DieSides? = null) : Token()
    data class PENDING_MULCH(val sides: DieSides? = null) : Token()
}
