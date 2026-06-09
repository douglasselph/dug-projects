package dugsolutions.leaf.v30.player.components

import dugsolutions.leaf.v30.cards.domain.GameCard

data class CreatureCard(
    val card: GameCard,
    val facing: Facing = Facing.FACE_DOWN
) {
    enum class Facing {
        FACE_UP,
        FACE_DOWN
    }

    val isFaceUp: Boolean
        get() = facing == Facing.FACE_UP

    val isFaceDown: Boolean
        get() = facing == Facing.FACE_DOWN

    fun faceUp(): CreatureCard {
        return copy(facing = Facing.FACE_UP)
    }

    fun faceDown(): CreatureCard {
        return copy(facing = Facing.FACE_DOWN)
    }
}
