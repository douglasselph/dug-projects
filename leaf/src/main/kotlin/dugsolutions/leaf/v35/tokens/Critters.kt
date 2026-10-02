package dugsolutions.leaf.v35.tokens

class Critters(
    critters: List<Critter> = emptyList(),
    private val supplyObserver: SharedTokenSupplyObserver = SharedTokenSupplyObserver.NONE
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
        supplyObserver.onReturn(critter.sharedResource())
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
        val removed = critters.remove(critter)
        supplyObserver.onGainAttempt(critter.sharedResource(), removed)
        return removed
    }

    fun replace(
        from: Critter,
        to: Critter
    ): Int {
        var replaced = 0
        critters.indices.forEach { index ->
            if (critters[index] == from) {
                critters[index] = to
                replaced++
            }
        }
        return replaced
    }

    fun clear() {
        critters.clear()
    }
}

private fun Critter.sharedResource(): SharedTokenResource = when (this) {
    Critter.BEE -> SharedTokenResource.BEE
    Critter.WORM -> SharedTokenResource.WORM
}
