package dugsolutions.leaf.game.purchase

import dugsolutions.leaf.components.FlourishType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManagePurchasedFloralTypesTest {

    private lateinit var SUT: ManagePurchasedFloralTypes
    
    @BeforeEach
    fun setup() {
        SUT = ManagePurchasedFloralTypes()
    }
    
    @Test
    fun list_whenInitialState_returnsEmptyList() {
        // Arrange - handled by setup
        
        // Act
        val result = SUT.list
        
        // Assert
        assertTrue(result.isEmpty(), "Initial list should be empty")
    }
    
    @Test
    fun add_withSingleType_addsToList() {
        // Arrange
        val type = FlourishType.ROOT
        
        // Act
        SUT.add(type)
        
        // Assert
        assertEquals(1, SUT.list.size, "List should contain one item")
        assertTrue(SUT.list.contains(type), "List should contain the added type")
    }
    
    @Test
    fun add_withDuplicateType_addsOnlyOnce() {
        // Arrange
        val type = FlourishType.ROOT
        
        // Act
        SUT.add(type)
        SUT.add(type) // Adding same type twice
        
        // Assert
        assertEquals(1, SUT.list.size, "List should contain only one instance of the type")
    }
    
    @Test
    fun add_withMultipleTypes_addsAllToList() {
        // Arrange
        val types = listOf(FlourishType.ROOT, FlourishType.CANOPY, FlourishType.VINE)
        
        // Act
        types.forEach { SUT.add(it) }
        
        // Assert
        assertEquals(types.size, SUT.list.size, "List should contain all added types")
        types.forEach { type ->
            assertTrue(SUT.list.contains(type), "List should contain $type")
        }
    }
    
    @Test
    fun has_withExistingType_returnsTrue() {
        // Arrange
        val type = FlourishType.BLOOM
        SUT.add(type)
        
        // Act
        val result = SUT.has(type)
        
        // Assert
        assertTrue(result, "Should return true for existing type")
    }
    
    @Test
    fun has_withNonExistingType_returnsFalse() {
        // Arrange
        SUT.add(FlourishType.ROOT)
        
        // Act
        val result = SUT.has(FlourishType.BLOOM)
        
        // Assert
        assertFalse(result, "Should return false for non-existing type")
    }
    
    @Test
    fun clear_afterAddingTypes_removesAllTypes() {
        // Arrange
        val types = listOf(FlourishType.ROOT, FlourishType.CANOPY, FlourishType.VINE)
        types.forEach { SUT.add(it) }
        
        // Act
        SUT.clear()
        
        // Assert
        assertTrue(SUT.list.isEmpty(), "List should be empty after clear")
        types.forEach { type ->
            assertFalse(SUT.has(type), "Should not have any previously added types")
        }
    }
    
    @Test
    fun list_alwaysReturnsNewList() {
        // Arrange
        SUT.add(FlourishType.ROOT)
        
        // Act
        val list1 = SUT.list
        val list2 = SUT.list
        
        // Assert
        assertTrue(list1 !== list2, "Should return a new list instance each time")
    }
} 