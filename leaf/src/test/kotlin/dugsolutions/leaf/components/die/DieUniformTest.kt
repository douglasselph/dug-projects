package dugsolutions.leaf.components.die

import dugsolutions.leaf.tool.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DieUniformTest {

    companion object {
        private const val TEST_ITERATIONS = 1000 // Number of rolls to test distribution
    }

    private lateinit var randomizer: Randomizer
    private lateinit var d4: DieUniform
    private lateinit var d6: DieUniform
    private lateinit var d8: DieUniform
    private lateinit var d10: DieUniform
    private lateinit var d12: DieUniform
    private lateinit var d20: DieUniform

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create()
        d4 = DieUniform(4, randomizer)
        d6 = DieUniform(6, randomizer)
        d8 = DieUniform(8, randomizer)
        d10 = DieUniform(10, randomizer)
        d12 = DieUniform(12, randomizer)
        d20 = DieUniform(20, randomizer)
    }

    @Test
    fun roll_d4_distributesEvenly() {
        // Act
        val results = rollDie(d4, TEST_ITERATIONS)

        // Assert
        verifyDistribution(results, 4)
    }

    @Test
    fun roll_d6_distributesEvenly() {
        // Act
        val results = rollDie(d6, TEST_ITERATIONS)

        // Assert
        verifyDistribution(results, 6)
    }

    @Test
    fun roll_d8_distributesEvenly() {
        // Act
        val results = rollDie(d8, TEST_ITERATIONS)

        // Assert
        verifyDistribution(results, 8)
    }

    @Test
    fun roll_d10_distributesEvenly() {
        // Act
        val results = rollDie(d10, TEST_ITERATIONS)

        // Assert
        verifyDistribution(results, 10)
    }

    @Test
    fun roll_d12_distributesEvenly() {
        // Act
        val results = rollDie(d12, TEST_ITERATIONS)

        // Assert
        verifyDistribution(results, 12)
    }

    @Test
    fun roll_d20_distributesEvenly() {
        // Act
        val results = rollDie(d20, TEST_ITERATIONS)

        // Assert
        verifyDistribution(results, 20)
    }

    @Test
    fun roll_d6_resetsAfterAllNumbersUsed() {
        // Act
        val results = mutableListOf<Int>()
        repeat(6) { results.add(d6.roll().value) }
        repeat(6) { results.add(d6.roll().value) }

        // Assert
        assertEquals(12, results.size)
        verifyDistribution(results.take(6), 6)
        verifyDistribution(results.drop(6), 6)
    }

    @Test
    fun roll_d6_containsAllNumbersInFirstSet() {
        // Act
        val results = mutableListOf<Int>()
        repeat(6) { results.add(d6.roll().value) }

        // Assert
        assertEquals(setOf(1, 2, 3, 4, 5, 6), results.toSet())
    }

    @Test
    fun roll_d6_containsAllNumbersInSecondSet() {
        // Act
        repeat(6) { d6.roll() } // First set
        val results = mutableListOf<Int>()
        repeat(6) { results.add(d6.roll().value) }

        // Assert
        assertEquals(setOf(1, 2, 3, 4, 5, 6), results.toSet())
    }

    @Test
    fun roll_distributionAcrossDieTypesAndCounts_isUniform() {
        // Arrange
        val countList = listOf(1, 2, 3, 5, 10, 20)
        val dieTypes = listOf(
            DieSides.D4 to d4,
            DieSides.D6 to d6,
            DieSides.D8 to d8,
            DieSides.D10 to d10,
            DieSides.D12 to d12,
            DieSides.D20 to d20
        )
        
        // For each count, perform the test
        for (count in countList) {
            // Create a map to store results for each die type
            val resultsByDieType = mutableMapOf<DieSides, MutableMap<Int, Int>>()
            
            // Initialize the results map for each die type
            dieTypes.forEach { (sides, _) ->
                resultsByDieType[sides] = mutableMapOf()
            }
            
            // Perform the rolling pattern
            val maxSides = DieSides.D20.value
            val maxIterations = count * maxSides
            
            for (iteration in 0 until maxIterations) {
                // For each die type
                for ((sides, die) in dieTypes) {
                    // Skip if this die has already been rolled its number of sides * count times
                    val currentRolls = resultsByDieType[sides]?.values?.sum() ?: 0
                    if (currentRolls >= sides.value * count) {
                        continue
                    }
                    
                    // Roll the die and record the result
                    val value = die.roll().value
                    resultsByDieType[sides]?.merge(value, 1, Int::plus)
                }
            }
            
            // Verify the distribution for each die type
            for (sides in DieSides.values()) {
                val results = resultsByDieType[sides] ?: continue
                
                // Verify each possible value appears exactly 'count' times
                for (value in 1..sides.value) {
                    val occurrences = results[value] ?: 0
                    assertEquals(
                        count, 
                        occurrences, 
                        "For ${sides.name}, value $value should appear exactly $count times, but appeared $occurrences times"
                    )
                }
            }
        }
    }

    @Test
    fun roll_distributionAcrossDieTypesAndCounts_isUniformWithHelperClass() {
        // Arrange
        val countList = listOf(1, 2, 3, 5, 10, 20)
        val dieTypes = listOf(
            DieSides.D4 to d4,
            DieSides.D6 to d6,
            DieSides.D8 to d8,
            DieSides.D10 to d10,
            DieSides.D12 to d12,
            DieSides.D20 to d20
        )
        
        // For each count, perform the test
        for (count in countList) {
            val dieRoller = DieRoller(dieTypes)
            dieRoller.rollForCount(count)
            dieRoller.verifyDistribution(count)
        }
    }

    // Helper class to manage the rolling process
    private class DieRoller(private val dieTypes: List<Pair<DieSides, DieUniform>>) {
        private val resultsByDieType = mutableMapOf<DieSides, MutableMap<Int, Int>>()
        
        init {
            // Initialize the results map for each die type
            dieTypes.forEach { (sides, _) ->
                resultsByDieType[sides] = mutableMapOf()
            }
        }
        
        fun rollForCount(count: Int) {
            val maxSides = DieSides.D20.value
            val maxIterations = count * maxSides
            
            for (iteration in 0 until maxIterations) {
                // For each die type
                for ((sides, die) in dieTypes) {
                    // Skip if this die has already been rolled its number of sides * count times
                    val currentRolls = resultsByDieType[sides]?.values?.sum() ?: 0
                    if (currentRolls >= sides.value * count) {
                        continue
                    }
                    
                    // Roll the die and record the result
                    val value = die.roll().value
                    resultsByDieType[sides]?.merge(value, 1, Int::plus)
                }
            }
        }
        
        fun verifyDistribution(expectedCount: Int) {
            for ((sides, _) in dieTypes) {
                val results = resultsByDieType[sides] ?: continue
                
                // Verify each possible value appears exactly 'expectedCount' times
                for (value in 1..sides.value) {
                    val occurrences = results[value] ?: 0
                    assertEquals(
                        expectedCount, 
                        occurrences, 
                        "For ${sides.name}, value $value should appear exactly $expectedCount times, but appeared $occurrences times"
                    )
                }
            }
        }
    }

    // region Support

    private fun rollDie(die: DieUniform, iterations: Int): List<Int> {
        return List(iterations) { die.roll().value }
    }

    private fun verifyDistribution(results: List<Int>, sides: Int) {
        // Count occurrences of each number
        val counts = results.groupingBy { it }.eachCount()
        
        // Verify all numbers from 1 to sides are present
        assertEquals(sides, counts.size)
        assertTrue((1..sides).all { it in counts })
        
        // Calculate expected count (allowing for small variance)
        val expectedCount = results.size / sides
        val maxVariance = expectedCount * 0.1 // Allow 10% variance
        
        // Verify each number appears approximately the expected number of times
        counts.values.forEach { count ->
            assertTrue(
                count in (expectedCount - maxVariance).toInt()..(expectedCount + maxVariance).toInt(),
                "Count $count is outside expected range of ${expectedCount - maxVariance} to ${expectedCount + maxVariance}"
            )
        }
    }

    // endregion Support

} 