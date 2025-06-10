package dugsolutions.leaf.grove.local

import dugsolutions.leaf.random.die.DieSides
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class aDieSupplyTest {
    private lateinit var diceSupply: DiceSupply

    @BeforeEach
    fun setup() {
        diceSupply = DiceSupply()
    }

    // region Initialization Tests

    @Test
    fun createDefault_createsSupplyWithDefaultQuantities() {
        // Act
        val supply = DiceSupply.createDefault()

        // Assert
        DiceSupply.VALID_DICE_SIDES.forEach { sides ->
            assertEquals(DiceSupply.DEFAULT_QUANTITY, supply.getQuantity(sides))
        }
    }

    @Test
    fun empty_createsSupplyWithZeroQuantities() {
        // Act
        val supply = DiceSupply.empty()

        // Assert
        DiceSupply.VALID_DICE_SIDES.forEach { sides ->
            assertEquals(0, supply.getQuantity(sides))
        }
    }

    @Test
    fun constructor_withInitialSupply_createsSupplyWithSpecifiedQuantities() {
        // Arrange
        val initialSupply = mapOf(4 to 2, 6 to 3, 8 to 1)

        // Act
        val supply = DiceSupply(initialSupply)

        // Assert
        assertEquals(2, supply.getQuantity(4))
        assertEquals(3, supply.getQuantity(6))
        assertEquals(1, supply.getQuantity(8))
        assertEquals(0, supply.getQuantity(10))
        assertEquals(0, supply.getQuantity(12))
        assertEquals(0, supply.getQuantity(20))
    }

    // endregion Initialization Tests

    // region Quantity Management Tests

    @Test
    fun getQuantity_whenDieExists_returnsCorrectQuantity() {
        // Arrange
        diceSupply.add(2, 6)

        // Act
        val quantity = diceSupply.getQuantity(6)

        // Assert
        assertEquals(2, quantity)
    }

    @Test
    fun getQuantity_whenDieDoesNotExist_returnsZero() {
        // Act
        val quantity = diceSupply.getQuantity(6)

        // Assert
        assertEquals(0, quantity)
    }

    @Test
    fun getQuantity_whenInvalidDie_returnsZero() {
        // Act
        val quantity = diceSupply.getQuantity(7)

        // Assert
        assertEquals(0, quantity)
    }

    @Test
    fun getAvailableSides_returnsOnlySidesWithPositiveQuantity() {
        // Arrange
        diceSupply.add(2, 6)
        diceSupply.add(1, 8)
        diceSupply.add(0, 4)

        // Act
        val availableSides = diceSupply.getAvailableSides()

        // Assert
        assertEquals(listOf(6, 8), availableSides)
    }

    // endregion Quantity Management Tests

    // region Die Management Tests

    @Test
    fun removeDie_whenDieExists_returnsTrueAndDecrementsQuantity() {
        // Arrange
        diceSupply.add(2, 6)

        // Act
        val result = diceSupply.removeDie(6)

        // Assert
        assertTrue(result)
        assertEquals(1, diceSupply.getQuantity(6))
    }

    @Test
    fun removeDie_whenDieDoesNotExist_returnsFalse() {
        // Act
        val result = diceSupply.removeDie(6)

        // Assert
        assertFalse(result)
        assertEquals(0, diceSupply.getQuantity(6))
    }

    @Test
    fun removeDie_whenInvalidDie_returnsFalse() {
        // Act
        val result = diceSupply.removeDie(7)

        // Assert
        assertFalse(result)
    }

    @Test
    fun removeDie_whenQuantityIsZero_returnsFalse() {
        // Arrange
        diceSupply.add(0, 6)

        // Act
        val result = diceSupply.removeDie(6)

        // Assert
        assertFalse(result)
        assertEquals(0, diceSupply.getQuantity(6))
    }

    @Test
    fun addDie_whenValidDie_returnsTrueAndIncrementsQuantity() {
        // Act
        val result = diceSupply.addDie(6)

        // Assert
        assertTrue(result)
        assertEquals(1, diceSupply.getQuantity(6))
    }

    @Test
    fun addDie_whenInvalidDie_returnsFalse() {
        // Act
        val result = diceSupply.addDie(7)

        // Assert
        assertFalse(result)
        assertEquals(0, diceSupply.getQuantity(7))
    }

    // region HasDie Tests

    @Test
    fun hasDie_whenDieExistsWithPositiveQuantity_returnsTrue() {
        // Arrange
        diceSupply.add(1, 6)
        
        // Act
        val result = diceSupply.hasDie(6)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun hasDie_whenDieExistsWithMultipleQuantity_returnsTrue() {
        // Arrange
        diceSupply.add(5, 8)
        
        // Act
        val result = diceSupply.hasDie(8)
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun hasDie_whenDieExistsWithZeroQuantity_returnsFalse() {
        // Arrange
        diceSupply.add(0, 10)
        
        // Act
        val result = diceSupply.hasDie(10)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun hasDie_whenDieDoesNotExist_returnsFalse() {
        // Act
        val result = diceSupply.hasDie(12)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun hasDie_whenInvalidDie_returnsFalse() {
        // Act
        val result = diceSupply.hasDie(7)
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun hasDie_afterAddAndRemove_reflectsCurrentState() {
        // Arrange
        diceSupply.add(1, 20)
        assertTrue(diceSupply.hasDie(20))
        
        // Act
        diceSupply.removeDie(20)
        
        // Assert
        assertFalse(diceSupply.hasDie(20))
    }

    // endregion HasDie Tests

    // endregion Die Management Tests

    // region Utility Tests

    @Test
    fun total_returnsSumOfAllQuantities() {
        // Arrange
        diceSupply.add(2, 6)
        diceSupply.add(1, 8)
        diceSupply.add(3, 4)

        // Act
        val total = diceSupply.total()

        // Assert
        assertEquals(6, total)
    }

    @Test
    fun getAffordableSides_returnsOnlySidesWithinBudget() {
        // Arrange
        diceSupply.add(2, 6)
        diceSupply.add(1, 8)
        diceSupply.add(3, 4)

        // Act
        val affordableSides = diceSupply.getAffordableSides(5)

        // Assert
        assertEquals(listOf(4), affordableSides)
    }

    @Test
    fun clear_removesAllDice() {
        // Arrange
        diceSupply.add(2, 6)
        diceSupply.add(1, 8)
        diceSupply.add(3, 4)

        // Act
        diceSupply.clear()

        // Assert
        assertEquals(0, diceSupply.total())
        assertTrue(diceSupply.getAvailableSides().isEmpty())
    }

    @Test
    fun add_withValidDie_addsSpecifiedQuantity() {
        // Act
        diceSupply.add(3, 6)

        // Assert
        assertEquals(3, diceSupply.getQuantity(6))
    }

    @Test
    fun add_withInvalidDie_doesNotAddQuantity() {
        // Act
        diceSupply.add(3, 7)

        // Assert
        assertEquals(0, diceSupply.getQuantity(7))
    }

    @Test
    fun addMany_setsAllValidDiceToSpecifiedQuantity() {
        // Act
        diceSupply.addMany(DieSides.D20, 2)

        // Assert
        assertEquals(2, diceSupply.getQuantity(20))
    }

    // endregion Utility Tests
} 
