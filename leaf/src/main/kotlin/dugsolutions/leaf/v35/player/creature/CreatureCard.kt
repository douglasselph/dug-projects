package dugsolutions.leaf.v35.player.creature

import dugsolutions.leaf.v35.plant.domain.PlantCard

/**
 * One physical Plant card after it has been grafted to a player's Creature.
 *
 * PlantCard is the immutable shared definition. CreatureCard contains the
 * per-player/per-copy state acquired after grafting.
 */
data class CreatureCard(
    val id: CreatureCardId,
    val card: PlantCard,
    val side: CreatureSide,
    val position: CreaturePosition,
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

    fun faceUp(): CreatureCard =
        copy(facing = Facing.FACE_UP)

    fun faceDown(): CreatureCard =
        copy(facing = Facing.FACE_DOWN)

    fun flip(): CreatureCard =
        copy(
            facing = if (isFaceUp) {
                Facing.FACE_DOWN
            } else {
                Facing.FACE_UP
            }
        )
}
