package dugsolutions.leaf.v30.common

class Butterflies(
    butterflies: List<Butterfly> = emptyList()
) : Iterable<Butterfly> {
    private val butterflies = butterflies.toMutableList()

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
        butterflies.add(butterfly)
        return this
    }

    fun remove(butterfly: Butterfly): Boolean {
        return butterflies.remove(butterfly)
    }

    fun clear() {
        butterflies.clear()
    }
}
