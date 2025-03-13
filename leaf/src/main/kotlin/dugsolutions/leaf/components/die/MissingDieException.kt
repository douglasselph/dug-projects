package dugsolutions.leaf.components.die

data class MissingDieException(
    override val message: String
) : Exception(message)
