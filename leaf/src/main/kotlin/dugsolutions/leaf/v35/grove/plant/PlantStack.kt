package dugsolutions.leaf.v35.grove.plant

import dugsolutions.leaf.v35.plant.domain.PlantCard

/**
 * One selected face-up Plant-card stack in the Grove.
 *
 * PlantCard is the immutable card definition. Physical copies in the Grove
 * are represented by [remaining], beginning at the definition's authored
 * quantity.
 */
class PlantStack(
    val card: PlantCard
) {

    init {
        require(card.quantity >= 0) {
            "Plant card quantity cannot be negative: ${card.name}=${card.quantity}"
        }
    }

    var remaining: Int = card.quantity
        private set

    val isEmpty: Boolean
        get() = remaining == 0

    val isNotEmpty: Boolean
        get() = remaining > 0

    /**
     * Takes one physical copy from this stack.
     *
     * Returns the shared immutable PlantCard definition, or null when empty.
     */
    fun take(): PlantCard? {
        if (isEmpty) return null

        remaining--
        return card
    }

    /**
     * Returns one physical copy to this stack, normally after a Snip.
     *
     * The returned card must match this stack's stable card name and the stack
     * may not exceed its authored physical quantity.
     */
    fun returnCard(returned: PlantCard): Boolean {
        if (!matches(returned)) return false
        if (remaining >= card.quantity) return false

        remaining++
        return true
    }

    fun reset() {
        remaining = card.quantity
    }

    fun matches(other: PlantCard): Boolean =
        card.name.trim().equals(
            other.name.trim(),
            ignoreCase = true
        )
}
