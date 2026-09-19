package dugsolutions.leaf.v30.player.domain

data class OutOfDiceException(
    override val message: String = "Player has no dice available to draw"
) : Exception(message)
