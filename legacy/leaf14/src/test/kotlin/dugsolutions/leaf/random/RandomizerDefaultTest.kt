package dugsolutions.leaf.random

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RandomizerDefaultTest {

    companion object {
        private const val TEST_SEED = 12345L
        private const val DIFFERENT_SEED = 67890L
        private const val NEXT_INT_UPPER_BOUND = 100
        private const val NEXT_INT_FROM = 10
        private const val NEXT_INT_UNTIL = 50
        private const val TEST_LIST_ITEM_A = "A"
        private const val TEST_LIST_ITEM_B = "B"
        private const val TEST_LIST_ITEM_C = "C"
    }

    @Test
    fun nextBoolean_withSameSeed_returnsSameSequence() {
        // Arrange
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = TEST_SEED
        randomizer2.seed = TEST_SEED

        // Act
        val sequence1 = (1..10).map { randomizer1.nextBoolean() }
        val sequence2 = (1..10).map { randomizer2.nextBoolean() }

        // Assert
        assertEquals(sequence1, sequence2)
    }

    @Test
    fun nextInt_withSameSeedAndUpperBound_returnsSameSequence() {
        // Arrange
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = TEST_SEED
        randomizer2.seed = TEST_SEED

        // Act
        val sequence1 = (1..10).map { randomizer1.nextInt(NEXT_INT_UPPER_BOUND) }
        val sequence2 = (1..10).map { randomizer2.nextInt(NEXT_INT_UPPER_BOUND) }

        // Assert
        assertEquals(sequence1, sequence2)
    }

    @Test
    fun nextInt_withSameSeedAndRange_returnsSameSequence() {
        // Arrange
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = TEST_SEED
        randomizer2.seed = TEST_SEED

        // Act
        val sequence1 = (1..10).map { randomizer1.nextInt(NEXT_INT_FROM, NEXT_INT_UNTIL) }
        val sequence2 = (1..10).map { randomizer2.nextInt(NEXT_INT_FROM, NEXT_INT_UNTIL) }

        // Assert
        assertEquals(sequence1, sequence2)
    }

    @Test
    fun randomOrNull_withSameSeed_returnsSameSequence() {
        // Arrange
        val testList = listOf(TEST_LIST_ITEM_A, TEST_LIST_ITEM_B, TEST_LIST_ITEM_C)
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = TEST_SEED
        randomizer2.seed = TEST_SEED

        // Act
        val sequence1 = (1..10).map { randomizer1.randomOrNull(testList) }
        val sequence2 = (1..10).map { randomizer2.randomOrNull(testList) }

        // Assert
        assertEquals(sequence1, sequence2)
    }

    @Test
    fun shuffled_withSameSeed_returnsSameSequence() {
        // Arrange
        val testList = listOf(TEST_LIST_ITEM_A, TEST_LIST_ITEM_B, TEST_LIST_ITEM_C)
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = TEST_SEED
        randomizer2.seed = TEST_SEED

        // Act
        val result1 = randomizer1.shuffled(testList)
        val result2 = randomizer2.shuffled(testList)

        // Assert
        assertEquals(result1, result2)
    }

    @Test
    fun mixedCalls_withSameSeed_returnsSameSequence() {
        // Arrange
        val testList = listOf(TEST_LIST_ITEM_A, TEST_LIST_ITEM_B, TEST_LIST_ITEM_C)
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = TEST_SEED
        randomizer2.seed = TEST_SEED

        // Act
        val sequence1 = listOf(
            randomizer1.nextBoolean(),
            randomizer1.nextInt(NEXT_INT_UPPER_BOUND),
            randomizer1.nextInt(NEXT_INT_FROM, NEXT_INT_UNTIL),
            randomizer1.randomOrNull(testList),
            randomizer1.nextBoolean()
        )

        val sequence2 = listOf(
            randomizer2.nextBoolean(),
            randomizer2.nextInt(NEXT_INT_UPPER_BOUND),
            randomizer2.nextInt(NEXT_INT_FROM, NEXT_INT_UNTIL),
            randomizer2.randomOrNull(testList),
            randomizer2.nextBoolean()
        )

        // Assert
        assertEquals(sequence1, sequence2)
    }

    @Test
    fun nextBoolean_withDifferentSeeds_returnsDifferentSequences() {
        // Arrange
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = TEST_SEED
        randomizer2.seed = DIFFERENT_SEED

        // Act
        val sequence1 = (1..10).map { randomizer1.nextBoolean() }
        val sequence2 = (1..10).map { randomizer2.nextBoolean() }

        // Assert
        assertNotEquals(sequence1, sequence2)
    }

    @Test
    fun seed_whenSetToNull_usesRandomDefault() {
        // Arrange
        val randomizer1 = RandomizerDefault()
        val randomizer2 = RandomizerDefault()

        randomizer1.seed = null
        randomizer2.seed = null

        // Act
        val sequence1 = (1..10).map { randomizer1.nextInt(NEXT_INT_UPPER_BOUND) }
        val sequence2 = (1..10).map { randomizer2.nextInt(NEXT_INT_UPPER_BOUND) }

        // Assert - With Random.Default, sequences should likely be different
        // Note: There's a tiny chance they could be the same, but extremely unlikely
        assertNotEquals(sequence1, sequence2)
    }

    @Test
    fun seed_getterReturnsSetValue() {
        // Arrange
        val randomizer = RandomizerDefault()

        // Act
        randomizer.seed = TEST_SEED

        // Assert
        assertEquals(TEST_SEED, randomizer.seed)
    }

    @Test
    fun seed_initiallyNull() {
        // Arrange & Act
        val randomizer = RandomizerDefault()

        // Assert
        assertEquals(null, randomizer.seed)
    }

} 
