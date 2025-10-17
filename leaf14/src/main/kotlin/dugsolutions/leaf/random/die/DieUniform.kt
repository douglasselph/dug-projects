package dugsolutions.leaf.random.die

import dugsolutions.leaf.random.Randomizer

class DieUniform(
    sides: Int,
    private val randomizer: Randomizer
) : Die(sides) {
    private var availableNumbers: MutableList<Int> = mutableListOf()

    init {
        resetAvailableNumbers()
    }

    private fun resetAvailableNumbers() {
        availableNumbers = (1..sides).toMutableList()
    }

    override fun roll(): Die {
        if (availableNumbers.isEmpty()) {
            resetAvailableNumbers()
        }

        // Get a random index from the available numbers
        val randomIndex = randomizer.nextInt(0, availableNumbers.size)
        
        // Get the number at that index and remove it
        _value = availableNumbers.removeAt(randomIndex)
        
        return this
    }
}
