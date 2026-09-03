package dugsolutions.leaf.v35.random.die

interface DieBase {

    val sides: Int
    val value: Int
    override fun equals(other: Any?): Boolean

}
