package dugsolutions.leaf.v30.random.die

import dugsolutions.leaf.v30.random.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class OneOfEachFaceBagTest {

    private lateinit var randomizer: Randomizer
    private lateinit var d4: OneOfEachFaceBag
    private lateinit var d6: OneOfEachFaceBag
    private lateinit var d8: OneOfEachFaceBag
    private lateinit var d10: OneOfEachFaceBag
    private lateinit var d12: OneOfEachFaceBag
    private lateinit var d20: OneOfEachFaceBag

    @BeforeEach
    fun setup() {
        randomizer = Randomizer.create(seed = 12345L)
        d4 = OneOfEachFaceBag(4, randomizer)
        d6 = OneOfEachFaceBag(6, randomizer)
        d8 = OneOfEachFaceBag(8, randomizer)
        d10 = OneOfEachFaceBag(10, randomizer)
        d12 = OneOfEachFaceBag(12, randomizer)
        d20 = OneOfEachFaceBag(20, randomizer)
    }

    @Test
    fun roll_d4_containsAllFacesInOneCycle() {
        assertOneCompleteCycle(d4, 4)
    }

    @Test
    fun roll_d6_containsAllFacesInOneCycle() {
        assertOneCompleteCycle(d6, 6)
    }

    @Test
    fun roll_d8_containsAllFacesInOneCycle() {
        assertOneCompleteCycle(d8, 8)
    }

    @Test
    fun roll_d10_containsAllFacesInOneCycle() {
        assertOneCompleteCycle(d10, 10)
    }

    @Test
    fun roll_d12_containsAllFacesInOneCycle() {
        assertOneCompleteCycle(d12, 12)
    }

    @Test
    fun roll_d20_containsAllFacesInOneCycle() {
        assertOneCompleteCycle(d20, 20)
    }

    @Test
    fun roll_d6_resetsAfterAllFacesUsed() {
        // Act
        val firstCycle = rollDie(d6, 6)
        val secondCycle = rollDie(d6, 6)

        // Assert
        assertEquals((1..6).toSet(), firstCycle.toSet())
        assertEquals((1..6).toSet(), secondCycle.toSet())
        assertEachFaceAppearsExactlyOnce(firstCycle, 6)
        assertEachFaceAppearsExactlyOnce(secondCycle, 6)
    }

    @Test
    fun roll_distributionAcrossDieTypesAndCounts_hasOneOfEachFacePerCycle() {
        // Arrange
        val countList = listOf(1, 2, 3, 5, 10, 20)
        val dice = listOf(
            DieSides.D4 to d4,
            DieSides.D6 to d6,
            DieSides.D8 to d8,
            DieSides.D10 to d10,
            DieSides.D12 to d12,
            DieSides.D20 to d20
        )

        for (count in countList) {
            for ((sides, die) in dice) {
                val results = rollDie(die, sides.value * count)
                assertEachFaceAppearsExactly(results, sides.value, count)
            }
        }
    }

    @Test
    fun roll_returnsSameDieInstance() {
        // Act
        val result = d6.roll()

        // Assert
        assertSame(d6, result)
    }

    private fun assertOneCompleteCycle(die: OneOfEachFaceBag, sides: Int) {
        val results = rollDie(die, sides)

        assertEquals((1..sides).toSet(), results.toSet())
        assertEachFaceAppearsExactlyOnce(results, sides)
    }

    private fun rollDie(die: OneOfEachFaceBag, count: Int): List<Int> {
        return List(count) { die.roll().value }
    }

    private fun assertEachFaceAppearsExactlyOnce(results: List<Int>, sides: Int) {
        assertEachFaceAppearsExactly(results, sides, expectedCount = 1)
    }

    private fun assertEachFaceAppearsExactly(results: List<Int>, sides: Int, expectedCount: Int) {
        val counts = results.groupingBy { it }.eachCount()

        assertEquals(sides, counts.size)
        for (value in 1..sides) {
            assertEquals(
                expectedCount,
                counts[value],
                "Face $value should appear exactly $expectedCount times"
            )
        }
    }

}
