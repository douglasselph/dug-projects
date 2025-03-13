package dugsolutions.leaf.components.die

interface DieBase {

    val sides: Int
    val value: Int
    override fun equals(other: Any?): Boolean

}
