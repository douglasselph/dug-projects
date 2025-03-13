package dugsolutions.leaf.tool

class RandomizerTD : Randomizer {

    private val intStack = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    private var currentIndex = 0

    private val nextIndex: Int
        get() {
            return (currentIndex++ % intStack.size)
        }

    private val nextValue: Int
        get() {
            return if (intStack.isEmpty()) 0 else intStack[nextIndex]
        }

    fun setValues(values: List<Int>) {
        intStack.clear()
        intStack.addAll(values)
        currentIndex = 0
    }

    var randomOrNullIndex = 0
        set(value) {
            field = value % intStack.size
        }

    override fun nextBoolean(): Boolean {
        return nextValue != 0
    }

    // from : inclusive.
    // until : exclusive
    override fun nextInt(from: Int, until: Int): Int {
        val v1 = nextValue - from
        val v2 = v1 % (until - from)
        return v2 + from
    }

    override fun nextInt(until: Int): Int {
        return nextValue % until
    }

    override fun <T> randomOrNull(list: List<T>): T? {
        if (list.isEmpty()) return null
        return list[randomOrNullIndex % list.size]
    }

    override fun <T> shuffled(list: List<T>): List<T> {
        if (list.isEmpty()) return emptyList()

        // Create a new list to avoid modifying the input
        val result = list.toMutableList()

        // Get the shift amount from the next value
        val shift = nextValue % result.size
        if (shift > 0) {
            // Split the list at the shift point
            val firstPart = result.take(shift)
            val secondPart = result.drop(shift)

            // Join the parts in reverse order
            result.clear()
            result.addAll(secondPart)
            result.addAll(firstPart)
        }

        return result
    }
}