package dugsolutions.leaf.components.die

abstract class Die(
    final override val sides: Int
) : DieBase {

    init {
        require(sides > 0) { "Die must have at least 1 side" }
    }

    protected var _value: Int = 1

    override val value: Int get() = _value

    val copy: DieValue
        get() {
            return DieValue(sides, value)
        }

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

    fun adjustToMax(): Int {
        val amount = sides - _value
        _value = sides
        return amount
    }

    override fun toString(): String {
        return "Die(sides=$sides, value=$value)"
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
