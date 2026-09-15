package dugsolutions.leaf.v35.player.decision.baseline.scoring

/**
 * Deterministically enumerates complete legal subsets for decisions that choose
 * more than one item at once.
 *
 * Input order is preserved inside every combination and combinations are
 * emitted in increasing size, then source-list order. Strategies can therefore
 * score the whole action+target set instead of greedily choosing members one at
 * a time.
 */
object LegalChoiceCombinations {
    fun <T> exactly(
        choices: List<T>,
        size: Int
    ): List<List<T>> = between(choices, size, size)

    fun <T> between(
        choices: List<T>,
        minSize: Int,
        maxSize: Int
    ): List<List<T>> {
        require(minSize >= 0) { "Minimum combination size cannot be negative: $minSize" }
        require(maxSize >= minSize) {
            "Maximum combination size must be >= minimum: min=$minSize max=$maxSize"
        }
        require(maxSize <= choices.size) {
            "Maximum combination size cannot exceed available choices: max=$maxSize size=${choices.size}"
        }

        val result = mutableListOf<List<T>>()
        for (targetSize in minSize..maxSize) {
            collect(
                choices = choices,
                targetSize = targetSize,
                startIndex = 0,
                selected = mutableListOf(),
                result = result
            )
        }
        return result
    }

    private fun <T> collect(
        choices: List<T>,
        targetSize: Int,
        startIndex: Int,
        selected: MutableList<T>,
        result: MutableList<List<T>>
    ) {
        if (selected.size == targetSize) {
            result += selected.toList()
            return
        }

        val remainingNeeded = targetSize - selected.size
        val lastStart = choices.size - remainingNeeded
        for (index in startIndex..lastStart) {
            selected += choices[index]
            collect(choices, targetSize, index + 1, selected, result)
            selected.removeAt(selected.lastIndex)
        }
    }
}
