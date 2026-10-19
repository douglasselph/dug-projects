package dugsolutions.leaf.v30.game.domain

data class MainActionException(
    override val message: String
) : Exception(message)
