package dugsolutions.leaf.random.die

data class MissingDieException(
    override val message: String
) : Exception(message)
