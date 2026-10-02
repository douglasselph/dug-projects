package dugsolutions.leaf.v35.tokens

class Butterflies(
    butterflies: List<Butterfly> = emptyList(),
    private val supplyObserver: SharedTokenSupplyObserver = SharedTokenSupplyObserver.NONE
) : Iterable<Butterfly> {
    private val butterflies = butterflies.toMutableList()
    private val faceUp = butterflies.associateWith { true }.toMutableMap()

    override fun iterator(): Iterator<Butterfly> = all.iterator()

    val all: List<Butterfly>
        get() = butterflies.toList()

    val size: Int
        get() = butterflies.size

    val isEmpty: Boolean
        get() = butterflies.isEmpty()

    val isNotEmpty: Boolean
        get() = butterflies.isNotEmpty()

    fun add(butterfly: Butterfly): Butterflies {
        supplyObserver.onReturn(SharedTokenResource.BUTTERFLY)
        butterflies.add(butterfly)
        faceUp[butterfly] = true
        return this
    }

    fun remove(butterfly: Butterfly): Boolean {
        val removed = butterflies.remove(butterfly)
        supplyObserver.onGainAttempt(SharedTokenResource.BUTTERFLY, removed)
        if (removed && !butterflies.contains(butterfly)) {
            faceUp.remove(butterfly)
        }
        return removed
    }

    fun clear() {
        butterflies.clear()
        faceUp.clear()
    }

    fun isFaceUp(butterfly: Butterfly): Boolean {
        return butterflies.contains(butterfly) && faceUp[butterfly] == true
    }

    fun isFaceDown(butterfly: Butterfly): Boolean {
        return butterflies.contains(butterfly) && faceUp[butterfly] == false
    }

    fun faceUp(butterfly: Butterfly): Boolean {
        if (!butterflies.contains(butterfly)) return false
        faceUp[butterfly] = true
        return true
    }

    fun faceDown(butterfly: Butterfly): Boolean {
        if (!butterflies.contains(butterfly)) return false
        faceUp[butterfly] = false
        return true
    }
}
