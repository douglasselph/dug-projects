package dugsolutions.leaf.v14.random.die

data class MissingDieException(
    override val message: String
) : Exception(message)
