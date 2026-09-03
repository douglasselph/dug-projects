package dugsolutions.leaf.v35.grove

import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Shared Graft Bed die inventory for one game.
 *
 * The Grove does not own actual rollable Die objects. It only tracks how many
 * dice of each size are physically available in the shared Graft Bed.
 *
 * Initial v35 setup:
 * - D4 = 0 (return space)
 * - D6/D8/D10/D12/D20 = 9 each
 *
 * Returned upgraded-away D4s may re-enter the D4 space. Replaced D6+ dice
 * leave the game and therefore have no generic return operation here.
 */
class GraftBed {

    companion object {
        const val STANDARD_DICE_PER_STACK = 9
    }

    private val countsBySides =
        linkedMapOf<DieSides, Int>()

    init {
        reset()
    }

    val counts: Map<DieSides, Int>
        get() = countsBySides.toMap()

    fun count(sides: DieSides): Int =
        countsBySides.getValue(sides)

    fun has(sides: DieSides): Boolean =
        count(sides) > 0

    /**
     * Takes one die of [sides] from the Graft Bed.
     *
     * Returns false without mutation when none are available.
     */
    fun take(sides: DieSides): Boolean {
        val current = count(sides)
        if (current <= 0) return false

        countsBySides[sides] = current - 1
        return true
    }

    /**
     * Returns one upgraded-away D4 to its dedicated Graft Bed return space.
     */
    fun returnD4() {
        countsBySides[DieSides.D4] =
            count(DieSides.D4) + 1
    }

    fun reset() {
        countsBySides.clear()

        DieSides.entries.forEach { sides ->
            countsBySides[sides] =
                if (sides == DieSides.D4) {
                    0
                } else {
                    STANDARD_DICE_PER_STACK
                }
        }
    }
}
