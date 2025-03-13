package dugsolutions.leaf.components.die

class Dice(dice: List<Die> = emptyList()) {

    private val _dice = dice.toMutableList()

    init {
        sort()
    }

    val dice: List<Die> get() = _dice.toList()
    val size: Int get() = _dice.size

    val copy: List<DieValue>
        get() = dice.map { it.copy }

    val totalSides: Int
        get() = _dice.sumOf { it.sides }

    fun set(dice: List<Die>) {
        clear()
        addAll(dice)
    }

    fun isEmpty(): Boolean = _dice.isEmpty()
    fun isNotEmpty(): Boolean = !isEmpty()

    // Sort dice by number of sides
    fun sort() {
        _dice.sortBy { it.sides }
    }

    // Draw a die from the collection (lowest sides first)
    fun draw(): Die? {
        if (_dice.isEmpty()) return null
        val index = _dice.indexOf(_dice.minByOrNull { it.sides })
        return _dice.removeAt(index)
    }

    // Draw the highest sided die from the collection
    fun drawHighest(): Die? {
        if (_dice.isEmpty()) return null
        val index = _dice.indexOf(_dice.maxByOrNull { it.sides })
        return _dice.removeAt(index)
    }

    // Draw the lowest sided die from the collection
    fun drawLowest(): Die? {
        if (_dice.isEmpty()) return null
        val index = _dice.indexOf(_dice.minByOrNull { it.sides })
        return _dice.removeAt(index)
    }

    // Add one or more dice to the collection and sort
    fun add(die: Die): Dice {
        _dice.add(die)
        return this
    }

    fun addAll(dice: List<Die>): Dice {
        _dice.addAll(dice)
        return this
    }

    fun clear() {
        _dice.clear()
    }

    fun reroll() {
        _dice.forEach { die -> die.roll() }
    }

    fun hasDie(die: Die): Boolean {
        return _dice.contains(die)
    }

    fun hasDie(die: DieValue): Boolean {
        return _dice.any { it.equals(die) }
    }

    // Add two Dice collections together
    operator fun plus(other: Dice): Dice = Dice(_dice + other.dice)

    // Remove the first matching die from the collection
    fun remove(die: Die): Boolean {
        val index = _dice.indexOfFirst { it == die }
        if (index >= 0) {
            _dice.removeAt(index)
            return true
        }
        return false
    }

    fun remove(die: DieValue): Boolean {
        val index = _dice.indexOfFirst { it.equals(die) }
        if (index >= 0) {
            _dice.removeAt(index)
            return true
        }
        return false
    }

    /**
     * Adjusts the value of a die that matches the provided die.
     * The adjusted value will be constrained between 1 and the die's sides.
     *
     * @param die The die to match and adjust
     * @param amount The amount to adjust the die's value by (can be positive or negative)
     * @return Whether the adjustment was successful
     */
    fun adjust(die: Die, amount: Int): Boolean {
        val index = _dice.indexOfFirst { it == die }
        if (index < 0) return false
        val targetDie = _dice[index]
        targetDie.adjustBy(amount)
        return true
    }

    fun adjust(die: DieValue, amount: Int): Boolean {
        val found = _dice.firstOrNull() { it.equals(die) } ?: return false
        return adjust(found, amount)
    }

    fun adjustToMax(die: Die): Boolean {
        val index = _dice.indexOfFirst { it == die }
        if (index < 0) return false
        val targetDie = _dice[index]
        targetDie.adjustToMax()
        return true
    }

    fun adjustToMax(die: DieValue): Boolean {
        val found = _dice.firstOrNull() { it.equals(die) } ?: return false
        return adjustToMax(found)
    }

    override fun toString(): String {
        if (dice.isEmpty()) return ""

        // Group dice by number of sides
        val diceBySides = _dice.groupBy { it.sides }

        // Format each group as "countDsides" (e.g., "2D4")
        return diceBySides.entries
            .sortedBy { it.key } // Sort by number of sides
            .filter { it.value.isNotEmpty() } // Ensure we don't include empty groups
            .map { (sides, diceList) -> "${diceList.size}D$sides" }
            .joinToString(", ")
    }

    fun values(): String {
        if (dice.isEmpty()) return ""

        // Sort the dice by sides for consistent output
        val sortedDice = dice.sortedBy { it.sides }

        // Format each die as "Dn(value)"
        return sortedDice
            .map { die -> "D${die.sides}(${die.value})" }
            .joinToString(",")
    }

    /**
     * Operator function to access dice by index using square bracket notation.
     * Returns the die at the specified index or null if the index is out of bounds.
     *
     * @param index The index of the die to retrieve
     * @return The die at the specified index, or null if the index is out of bounds
     */
    operator fun get(index: Int): Die? {
        return if (index in _dice.indices) _dice[index] else null
    }

}
