package dugsolutions.leaf.components.die

import dugsolutions.leaf.di.factory.DieFactory
import kotlin.math.max
import kotlin.math.min

class DieValue(
    override val sides: Int,
    incoming: Int
) : DieBase {

    override var value: Int = limit(incoming)
        private set

    fun dieFrom(dieFactory: DieFactory): Die {
        return dieFactory(sides).adjustTo(value)
    }

    fun adjustTo(amount: Int): DieValue {
        value = limit(amount)
        return this
    }

    fun adjustBy(amount: Int): DieValue {
        value = limit(value + amount)
        return this
    }

    fun adjustToMax(): Int {
        value = sides
        return value
    }

    private fun limit(incoming: Int): Int {
        return min(sides, max(1, incoming))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is DieValue) {
            if (sides != other.sides) return false
            if (value != other.value) return false
            return true
        }
        if (other is Die) {
            if (sides != other.sides) return false
            if (value != other.value) return false
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        var result = sides
        result = 31 * result + value
        return result
    }

    override fun toString(): String {
        return "DieValue(sides=$sides, value=$value)"
    }


}
