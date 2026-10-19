package dugsolutions.leaf.v30.random.die

data class MissingDieException(
    override val message: String
) : Exception(message)
