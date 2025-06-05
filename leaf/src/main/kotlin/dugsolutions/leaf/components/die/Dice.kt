package dugsolutions.leaf.components.die

class Dice(dice: List<Die> = emptyList()) {

    private val _dice = dice.toMutableList()
    private val lock = Any()

    init {
        sort()
    }

    val dice: List<Die> 
        get() = synchronized(lock) { _dice.toList() }
    val size: Int 
        get() = synchronized(lock) { _dice.size }

    val copy: List<DieValue>
        get() = synchronized(lock) { dice.map { it.copy } }

    val totalSides: Int
        get() = synchronized(lock) { _dice.sumOf { it.sides } }

    fun set(dice: List<Die>) {
        synchronized(lock) {
            clear()
            addAll(dice)
        }
    }

    fun isEmpty(): Boolean = synchronized(lock) { _dice.isEmpty() }
    fun isNotEmpty(): Boolean = synchronized(lock) { !isEmpty() }

    // Sort dice by number of sides
    fun sort() {
        synchronized(lock) {
            _dice.sortBy { it.sides }
        }
    }

    // Draw a die from the collection (lowest sides first)
    fun draw(): Die? {
        synchronized(lock) {
            if (_dice.isEmpty()) return null
            val index = _dice.indexOf(_dice.minByOrNull { it.sides })
            return _dice.removeAt(index)
        }
    }

    // Draw the highest sided die from the collection
    fun drawHighest(): Die? {
        synchronized(lock) {
            if (_dice.isEmpty()) return null
            val index = _dice.indexOf(_dice.maxByOrNull { it.sides })
            return _dice.removeAt(index)
        }
    }

    // Draw the lowest sided die from the collection
    fun drawLowest(): Die? {
        synchronized(lock) {
            if (_dice.isEmpty()) return null
            val index = _dice.indexOf(_dice.minByOrNull { it.sides })
            return _dice.removeAt(index)
        }
    }

    // Add one or more dice to the collection and sort
    fun add(die: Die): Dice {
        synchronized(lock) {
            _dice.add(die)
        }
        return this
    }

    fun addAll(dice: List<Die>): Dice {
        synchronized(lock) {
            _dice.addAll(dice)
        }
        return this
    }

    fun clear() {
        synchronized(lock) {
            _dice.clear()
        }
    }

    fun reroll() {
        synchronized(lock) {
            _dice.forEach { die -> die.roll() }
        }
    }

    fun hasDie(die: Die): Boolean {
        return synchronized(lock) { _dice.contains(die) }
    }

    fun hasDie(die: DieValue): Boolean {
        return synchronized(lock) { _dice.any { it.equals(die) } }
    }

    /**
     * Returns true if any die in the collection satisfies the given predicate.
     *
     * @param predicate The predicate to test against each die
     * @return true if any die satisfies the predicate, false otherwise
     */
    fun any(predicate: (Die) -> Boolean): Boolean {
        return synchronized(lock) { _dice.any(predicate) }
    }

    // Add two Dice collections together
    operator fun plus(other: Dice): Dice = synchronized(lock) { Dice(_dice + other.dice) }

    // Remove the first matching die from the collection
    fun remove(die: Die): Boolean {
        synchronized(lock) {
            val index = _dice.indexOfFirst { it == die }
            if (index >= 0) {
                _dice.removeAt(index)
                return true
            }
            return false
        }
    }

    fun remove(die: DieValue): Boolean {
        synchronized(lock) {
            val index = _dice.indexOfFirst { it.equals(die) }
            if (index >= 0) {
                _dice.removeAt(index)
                return true
            }
            return false
        }
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
        synchronized(lock) {
            val index = _dice.indexOfFirst { it == die }
            if (index < 0) return false
            val targetDie = _dice[index]
            targetDie.adjustBy(amount)
            return true
        }
    }

    fun adjust(die: DieValue, amount: Int): Boolean {
        synchronized(lock) {
            val found = _dice.firstOrNull() { it.equals(die) } ?: return false
            return adjust(found, amount)
        }
    }

    fun adjustToMax(die: Die): Boolean {
        synchronized(lock) {
            val index = _dice.indexOfFirst { it == die }
            if (index < 0) return false
            val targetDie = _dice[index]
            targetDie.adjustToMax()
            return true
        }
    }

    fun adjustToMax(die: DieValue): Boolean {
        synchronized(lock) {
            val found = _dice.firstOrNull() { it.equals(die) } ?: return false
            return adjustToMax(found)
        }
    }

    override fun toString(): String {
        synchronized(lock) {
            if (dice.isEmpty()) return ""

            // Group dice by number of sides
            val diceBySides = _dice.groupBy { it.sides }

            // Format each group as "countDsides" (e.g., "2D4")
            return diceBySides.entries
                .sortedBy { it.key } // Sort by number of sides
                .filter { it.value.isNotEmpty() } // Ensure we don't include empty groups
                .map { (sides, diceList) -> "${diceList.size}D$sides" }
                .joinToString(",")
        }
    }

    fun values(): String {
        synchronized(lock) {
            if (dice.isEmpty()) return ""

            // Sort the dice by sides for consistent output
            val sortedDice = dice.sortedBy { it.sides }

            // Format each die as "Dn(value)"
            return sortedDice
                .map { die -> "D${die.sides}(${die.value})" }
                .joinToString(",")
        }
    }

    /**
     * Operator function to access dice by index using square bracket notation.
     * Returns the die at the specified index or null if the index is out of bounds.
     *
     * @param index The index of the die to retrieve
     * @return The die at the specified index, or null if the index is out of bounds
     */
    operator fun get(index: Int): Die? {
        synchronized(lock) {
            return if (index in _dice.indices) _dice[index] else null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Dice

        return _dice == other._dice
    }

    override fun hashCode(): Int {
        return _dice.hashCode()
    }


}
