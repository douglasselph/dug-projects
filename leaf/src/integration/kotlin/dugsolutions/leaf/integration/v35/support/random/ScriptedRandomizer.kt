package dugsolutions.leaf.integration.v35.support.random

import dugsolutions.leaf.v35.random.Randomizer
import java.util.ArrayDeque

/**
 * Fail-fast deterministic Randomizer for scenario tests.
 *
 * It is intentionally independent of a seed. Tests enqueue the exact random
 * answers they expect production code to request. IntegrationGameHarness can
 * inject it through GameFactory, so deck setup and dice rolls can be made
 * independent from unrelated random calls.
 */
class ScriptedRandomizer : Randomizer {
    private val booleans = ArrayDeque<Boolean>()
    private val ints = ArrayDeque<Int>()
    private val randomIndexes = ArrayDeque<Int>()
    private val shufflePermutations = ArrayDeque<List<Int>>()

    fun booleans(vararg values: Boolean): ScriptedRandomizer = apply {
        values.forEach(booleans::addLast)
    }

    fun ints(vararg values: Int): ScriptedRandomizer = apply {
        values.forEach(ints::addLast)
    }

    /**
     * Convenience alias for scripted die/D20 results. Random rolls in v35
     * ultimately use Randomizer.nextInt, so these values are consumed by the
     * next integer random requests in exact order.
     */
    fun rolls(vararg values: Int): ScriptedRandomizer =
        ints(*values)

    /** Queue zero-based indexes for [randomOrNull]. */
    fun randomIndexes(vararg values: Int): ScriptedRandomizer = apply {
        values.forEach(randomIndexes::addLast)
    }

    /**
     * Queue an exact output ordering for the next [shuffled] call.
     * Example: permutation(2, 0, 1) turns [A,B,C] into [C,A,B].
     */
    fun permutation(vararg indexes: Int): ScriptedRandomizer = apply {
        shufflePermutations.addLast(indexes.toList())
    }

    override fun nextBoolean(): Boolean =
        removeRequired(booleans, "nextBoolean")

    override fun nextInt(from: Int, until: Int): Int {
        require(from < until) { "Invalid random range: [$from, $until)" }
        val value = removeRequired(ints, "nextInt($from, $until)")
        require(value in from until until) {
            "Scripted random value $value is outside [$from, $until)"
        }
        return value
    }

    override fun nextInt(until: Int): Int {
        require(until > 0) { "Random upper bound must be positive: $until" }
        val value = removeRequired(ints, "nextInt($until)")
        require(value in 0 until until) {
            "Scripted random value $value is outside [0, $until)"
        }
        return value
    }

    override fun <T> randomOrNull(list: List<T>): T? {
        if (list.isEmpty()) return null
        val index = removeRequired(randomIndexes, "randomOrNull(size=${list.size})")
        require(index in list.indices) {
            "Scripted random index $index is outside ${list.indices}"
        }
        return list[index]
    }

    override fun <T> shuffled(list: List<T>): List<T> {
        val permutation = removeRequired(
            shufflePermutations,
            "shuffled(size=${list.size})"
        )
        require(permutation.size == list.size) {
            "Scripted shuffle has ${permutation.size} indexes for list size ${list.size}"
        }
        require(permutation.toSet() == list.indices.toSet()) {
            "Scripted shuffle must be a permutation of ${list.indices}: $permutation"
        }
        return permutation.map(list::get)
    }

    fun assertExhausted() {
        check(booleans.isEmpty()) { "${booleans.size} scripted booleans remain" }
        check(ints.isEmpty()) { "${ints.size} scripted ints remain" }
        check(randomIndexes.isEmpty()) { "${randomIndexes.size} scripted random indexes remain" }
        check(shufflePermutations.isEmpty()) {
            "${shufflePermutations.size} scripted shuffle permutations remain"
        }
    }

    private fun <T> removeRequired(queue: ArrayDeque<T>, call: String): T =
        checkNotNull(queue.pollFirst()) {
            "Unexpected random call $call: no scripted value remains"
        }
}
