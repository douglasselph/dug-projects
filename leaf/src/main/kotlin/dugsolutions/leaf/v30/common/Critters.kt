package dugsolutions.leaf.v30.common

class Critters(
    critters: List<Critter> = emptyList()
) : Iterable<Critter> {
    private val critters = critters.toMutableList()

    override fun iterator(): Iterator<Critter> = all.iterator()

    val all: List<Critter>
        get() = critters.toList()

    val size: Int
        get() = critters.size

    val isEmpty: Boolean
        get() = critters.isEmpty()

    val isNotEmpty: Boolean
        get() = critters.isNotEmpty()

    fun add(critter: Critter): Critters {
        critters.add(critter)
        return this
    }

    fun count(critter: Critter): Int {
        return critters.count { it == critter }
    }

    fun set(critter: Critter, amount: Int): Critters {
        require(amount >= 0) { "Critter count cannot be negative: $amount" }
        critters.removeAll { it == critter }
        repeat(amount) {
            critters.add(critter)
        }
        return this
    }

    fun remove(critter: Critter): Boolean {
        return critters.remove(critter)
    }

    fun clear() {
        critters.clear()
    }
}
