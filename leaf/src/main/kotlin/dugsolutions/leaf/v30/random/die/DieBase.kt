package dugsolutions.leaf.v30.random.die

interface DieBase {

    val sides: Int
    val value: Int
    override fun equals(other: Any?): Boolean

}
