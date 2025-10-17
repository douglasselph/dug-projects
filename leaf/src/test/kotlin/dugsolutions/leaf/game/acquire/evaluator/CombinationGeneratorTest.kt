package dugsolutions.leaf.game.acquire.evaluator

import dugsolutions.leaf.player.Player
import dugsolutions.leaf.random.RandomizerTD
import dugsolutions.leaf.random.die.Dice
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.random.die.SampleDie
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CombinationGeneratorTest {

    private val mockPlayer: Player = mockk(relaxed = true)
    private lateinit var sampleDie1: Die
    private lateinit var sampleDie2: Die
    private lateinit var sampleDie3: Die
    private lateinit var testDieValue1: DieValue
    private lateinit var testDieValue2: DieValue
    private lateinit var testDieValue3: DieValue
    private lateinit var randomizer: RandomizerTD
    private lateinit var sampleDie: SampleDie

    private lateinit var SUT: CombinationGenerator

    @BeforeEach
    fun setup() {
        randomizer = RandomizerTD()
        sampleDie = SampleDie(randomizer)

        // Create test dice
        sampleDie1 = sampleDie.d6.adjustTo(3)
        sampleDie2 = sampleDie.d8.adjustTo(5)
        sampleDie3 = sampleDie.d10.adjustTo(7)
        testDieValue1 = sampleDie1.copy
        testDieValue2 = sampleDie2.copy
        testDieValue3 = sampleDie3.copy

        SUT = CombinationGenerator()
    }

    @Test
    fun invoke_withNoDice_returnsEmptyCombination() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice(emptyList())
        every { mockPlayer.pipModifier } returns 0

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(0, result.list.size)
    }

    @Test
    fun invoke_withSingleDie_generatesOneCombination() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice(listOf(sampleDie1))
        every { mockPlayer.pipModifier } returns 0

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(1, result.list.size)
        val combination = result.list[0]
        assertEquals(1, combination.values.dice.size)
        assertEquals(testDieValue1, combination.values.dice[0])
        assertEquals(0, combination.addToTotal)
    }

    @Test
    fun invoke_withTwoDice_generatesThreeCombinations() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice(listOf(sampleDie1, sampleDie2))
        every { mockPlayer.pipModifier } returns 0

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(3, result.list.size)

        // Check individual dice combinations
        val hasDie1 = result.list.any { it.values.dice.size == 1 && it.values.dice[0] == testDieValue1 }
        val hasDie2 = result.list.any { it.values.dice.size == 1 && it.values.dice[0] == testDieValue2 }
        val hasBoth = result.list.any { it.values.dice.size == 2 && it.values.dice.contains(testDieValue1) && it.values.dice.contains(testDieValue2) }

        assertTrue(hasDie1, "Should have combination with only die1")
        assertTrue(hasDie2, "Should have combination with only die2")
        assertTrue(hasBoth, "Should have combination with both dice")
    }

    @Test
    fun invoke_withThreeDice_generatesSevenCombinations() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice(listOf(sampleDie1, sampleDie2, sampleDie3))
        every { mockPlayer.pipModifier } returns 0

        // Act
        val result = SUT(mockPlayer)

        // Assert
        assertEquals(7, result.list.size)

        // Check all possible combinations
        val hasDie1 = result.list.any { it.values.dice.size == 1 && it.values.dice[0] == testDieValue1 }
        val hasDie2 = result.list.any { it.values.dice.size == 1 && it.values.dice[0] == testDieValue2 }
        val hasDie3 = result.list.any { it.values.dice.size == 1 && it.values.dice[0] == testDieValue3 }
        val hasDie1And2 = result.list.any { it.values.dice.size == 2 && it.values.dice.contains(testDieValue1) && it.values.dice.contains(testDieValue2) }
        val hasDie1And3 = result.list.any { it.values.dice.size == 2 && it.values.dice.contains(testDieValue1) && it.values.dice.contains(testDieValue3) }
        val hasDie2And3 = result.list.any { it.values.dice.size == 2 && it.values.dice.contains(testDieValue2) && it.values.dice.contains(testDieValue3) }
        val hasAll = result.list.any { it.values.dice.size == 3 && it.values.dice.contains(testDieValue1) && it.values.dice.contains(testDieValue2) && it.values.dice.contains(testDieValue3) }

        assertTrue(hasDie1, "Should have combination with only die1")
        assertTrue(hasDie2, "Should have combination with only die2")
        assertTrue(hasDie3, "Should have combination with only die3")
        assertTrue(hasDie1And2, "Should have combination with die1 and die2")
        assertTrue(hasDie1And3, "Should have combination with die1 and die3")
        assertTrue(hasDie2And3, "Should have combination with die2 and die3")
        assertTrue(hasAll, "Should have combination with all dice")
    }

    @Test
    fun invoke_withPipModifier_extendsAllCombinations() {
        // Arrange
        val pipModifier = 5
        every { mockPlayer.diceInHand } returns Dice(listOf(sampleDie1, sampleDie2))
        every { mockPlayer.pipModifier } returns pipModifier

        // Act
        val result = SUT(mockPlayer)

        // Assert
        // Should have 3 base combinations + 3 extended combinations
        assertEquals(6, result.list.size)

        // Check base combinations
        val baseCombinations = result.list.filter { it.addToTotal == 0 }
        assertEquals(3, baseCombinations.size)

        // Check extended combinations
        val extendedCombinations = result.list.filter { it.addToTotal == pipModifier }
        assertEquals(3, extendedCombinations.size)

        // Verify each base combination has a corresponding extended version
        baseCombinations.forEach { base ->
            val hasExtended = extendedCombinations.any { extended ->
                extended.values.dice == base.values.dice
            }
            assertTrue(hasExtended, "Each base combination should have an extended version")
        }
    }

    @Test
    fun invoke_withZeroPipModifier_doesNotExtendCombinations() {
        // Arrange
        every { mockPlayer.diceInHand } returns Dice(listOf(sampleDie1, sampleDie2))
        every { mockPlayer.pipModifier } returns 0

        // Act
        val result = SUT(mockPlayer)

        // Assert
        // Should only have base combinations
        assertEquals(3, result.list.size)
        assertTrue(result.list.all { it.addToTotal == 0 })
    }
}
