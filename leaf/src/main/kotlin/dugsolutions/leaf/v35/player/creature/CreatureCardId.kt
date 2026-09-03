package dugsolutions.leaf.v35.player.creature

/**
 * Runtime identity for one physical Plant card grafted to a Creature.
 *
 * This is intentionally different from PlantCard identity. PlantCard is the
 * shared card definition; CreatureCardId distinguishes two physical copies of
 * the same PlantCard grafted to one Creature.
 */
@JvmInline
value class CreatureCardId(
    val value: Int
)
