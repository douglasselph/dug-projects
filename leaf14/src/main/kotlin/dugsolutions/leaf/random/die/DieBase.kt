package dugsolutions.leaf.random.die

interface DieBase {

    val sides: Int
    val value: Int
    override fun equals(other: Any?): Boolean

}
