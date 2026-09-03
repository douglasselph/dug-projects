package dugsolutions.leaf.v35.random.die

data class MissingDieException(
    override val message: String
) : Exception(message)
