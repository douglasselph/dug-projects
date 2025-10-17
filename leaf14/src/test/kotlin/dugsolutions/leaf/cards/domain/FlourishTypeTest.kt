package dugsolutions.leaf.cards.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FlourishTypeTest {

    companion object {
        private const val EXPECTED_NONE = "NONE"
        private const val EXPECTED_SEEDLING = "SEEDLING"
        private const val EXPECTED_ROOT = "ROOT"
        private const val EXPECTED_CANOPY = "CANOPY"
        private const val EXPECTED_VINE = "VINE"
        private const val EXPECTED_FLOWER = "FLOWER"
        private const val EXPECTED_BLOOM = "BLOOM"
    }

    @Test
    fun values_whenCalled_returnsAllFlourishTypes() {
        // Arrange
        val expectedTypes = listOf(
            FlourishType.NONE,
            FlourishType.SEEDLING,
            FlourishType.ROOT,
            FlourishType.CANOPY,
            FlourishType.VINE,
            FlourishType.FLOWER,
            FlourishType.BLOOM
        )

        // Act
        val actualTypes = FlourishType.values().toList()

        // Assert
        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun name_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(EXPECTED_NONE, FlourishType.NONE.name)
        assertEquals(EXPECTED_SEEDLING, FlourishType.SEEDLING.name)
        assertEquals(EXPECTED_ROOT, FlourishType.ROOT.name)
        assertEquals(EXPECTED_CANOPY, FlourishType.CANOPY.name)
        assertEquals(EXPECTED_VINE, FlourishType.VINE.name)
        assertEquals(EXPECTED_FLOWER, FlourishType.FLOWER.name)
        assertEquals(EXPECTED_BLOOM, FlourishType.BLOOM.name)
    }

    @Test
    fun valueOf_whenValidName_returnsCorrectFlourishType() {
        // Arrange & Act & Assert
        assertEquals(FlourishType.NONE, FlourishType.valueOf(EXPECTED_NONE))
        assertEquals(FlourishType.SEEDLING, FlourishType.valueOf(EXPECTED_SEEDLING))
        assertEquals(FlourishType.ROOT, FlourishType.valueOf(EXPECTED_ROOT))
        assertEquals(FlourishType.CANOPY, FlourishType.valueOf(EXPECTED_CANOPY))
        assertEquals(FlourishType.VINE, FlourishType.valueOf(EXPECTED_VINE))
        assertEquals(FlourishType.FLOWER, FlourishType.valueOf(EXPECTED_FLOWER))
        assertEquals(FlourishType.BLOOM, FlourishType.valueOf(EXPECTED_BLOOM))
    }
}


