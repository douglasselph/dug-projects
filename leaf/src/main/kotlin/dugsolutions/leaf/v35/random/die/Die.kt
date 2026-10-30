package dugsolutions.leaf.v35.random.die

abstract class Die(
    final override val sides: Int
) : DieBase {

    init {
        require(sides > 0) { "Die must have at least 1 side" }
    }

    /** Locked Strike dice cannot ever be changed by later Battle actions. */
    private var resolvedStrikeLocked: Boolean = false

    fun lockResolvedStrike() { resolvedStrikeLocked = true }

    /** Only Battle post-resolution reclaim/Doom may unlock a die for future rounds. */
    fun unlockAfterBattle() { resolvedStrikeLocked = false }

    protected fun requireMutableDie() {
        check(!resolvedStrikeLocked) { "Cannot modify die in resolved Strike: $this" }
    }

    protected var _value: Int = 1
        set(value) {
            requireMutableDie()
            field = value
        }

    override val value: Int get() = _value

    val copy: DieValue
        get() {
            return DieValue(sides, value).boost(value)
        }

    /** Mathematical mean of a fair die: (1 + sides) / 2. */
    val average: Float
        get() = (sides + 1) / 2f

    abstract fun roll(): Die

    fun adjustBy(amount: Int): Die {
        val newValue = value + amount
        val constrainedValue = newValue.coerceIn(1, sides)
        _value = constrainedValue
        return this
    }

    fun adjustTo(value: Int): Die {
        val newValue = value
        val constrainedValue = newValue.coerceIn(1, sides)
        _value = constrainedValue
        return this
    }

    // Supports the MAX token by allowing temporary values above the die's normal side limit.
    fun boost(value: Int): Die {
        _value = value
        return this
    }

    fun adjustToMax(): Int {
        val amount = sides - _value
        _value = sides
        return amount
    }

    fun flip(): Die {
        if (sides > 4) {
            _value = (sides + 1) - value
        }
        return this
    }

    override fun toString(): String {
        return "Die(sides=$sides, value=$value)"
    }

    fun toValue(): String {
        return "D$sides=$value"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is Die) {
            if (sides != other.sides) return false
            if (value != other.value) return false
            return true
        }
        if (other is DieValue) {
            if (sides != other.sides) return false
            if (value != other.value) return false
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        var result = sides
        result = 31 * result + _value
        return result
    }

} 
