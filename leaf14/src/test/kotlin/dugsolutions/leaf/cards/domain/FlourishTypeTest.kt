package dugsolutions.leaf.cards.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FlourishTypeTest {

    companion object {
        private const val EXPECTED_NONE = "NONE"
        private const val EXPECTED_CANOPY = "CANOPY"
        private const val EXPECTED_RESOURCE = "RESOURCE"
        private const val EXPECTED_ROOT = "ROOT"
        private const val EXPECTED_WILD_VINE = "WildVINE"
        private const val EXPECTED_VINE = "Vine"
        private const val EXPECTED_FLOWER = "FLOWER"
        private const val EXPECTED_BUTTERFLY = "BUTTERFLY"
        private const val EXPECTED_WISP = "WISP"
        
        private const val MATCH_NONE = ""
        private const val MATCH_CANOPY = "Canopy"
        private const val MATCH_RESOURCE = "Resource"
        private const val MATCH_ROOT = "Root"
        private const val MATCH_WILD_VINE = "WildVine"
        private const val MATCH_VINE = "Vine"
        private const val MATCH_FLOWER = "Flower"
        private const val MATCH_BUTTERFLY = "Butterfly"
        private const val MATCH_WISP = "Wisp"
    }

    @Test
    fun values_whenCalled_returnsAllFlourishTypes() {
        // Arrange
        val expectedTypes = listOf(
            FlourishType.NONE,
            FlourishType.CANOPY,
            FlourishType.RESOURCE,
            FlourishType.ROOT,
            FlourishType.WildVINE,
            FlourishType.Vine,
            FlourishType.FLOWER,
            FlourishType.BUTTERFLY,
            FlourishType.WISP
        )

        // Act
        val actualTypes = FlourishType.entries

        // Assert
        assertEquals(expectedTypes, actualTypes)
    }

    @Test
    fun name_whenCalled_returnsCorrectString() {
        // Arrange & Act & Assert
        assertEquals(EXPECTED_NONE, FlourishType.NONE.name)
        assertEquals(EXPECTED_CANOPY, FlourishType.CANOPY.name)
        assertEquals(EXPECTED_RESOURCE, FlourishType.RESOURCE.name)
        assertEquals(EXPECTED_ROOT, FlourishType.ROOT.name)
        assertEquals(EXPECTED_WILD_VINE, FlourishType.WildVINE.name)
        assertEquals(EXPECTED_VINE, FlourishType.Vine.name)
        assertEquals(EXPECTED_FLOWER, FlourishType.FLOWER.name)
        assertEquals(EXPECTED_BUTTERFLY, FlourishType.BUTTERFLY.name)
        assertEquals(EXPECTED_WISP, FlourishType.WISP.name)
    }

    @Test
    fun valueOf_whenValidName_returnsCorrectFlourishType() {
        // Arrange & Act & Assert
        assertEquals(FlourishType.NONE, FlourishType.valueOf(EXPECTED_NONE))
        assertEquals(FlourishType.CANOPY, FlourishType.valueOf(EXPECTED_CANOPY))
        assertEquals(FlourishType.RESOURCE, FlourishType.valueOf(EXPECTED_RESOURCE))
        assertEquals(FlourishType.ROOT, FlourishType.valueOf(EXPECTED_ROOT))
        assertEquals(FlourishType.WildVINE, FlourishType.valueOf(EXPECTED_WILD_VINE))
        assertEquals(FlourishType.Vine, FlourishType.valueOf(EXPECTED_VINE))
        assertEquals(FlourishType.FLOWER, FlourishType.valueOf(EXPECTED_FLOWER))
        assertEquals(FlourishType.BUTTERFLY, FlourishType.valueOf(EXPECTED_BUTTERFLY))
        assertEquals(FlourishType.WISP, FlourishType.valueOf(EXPECTED_WISP))
    }

    @Test
    fun from_whenValidMatch_returnsCorrectFlourishType() {
        // Arrange & Act & Assert
        assertEquals(FlourishType.NONE, FlourishType.from(MATCH_NONE))
        assertEquals(FlourishType.CANOPY, FlourishType.from(MATCH_CANOPY))
        assertEquals(FlourishType.RESOURCE, FlourishType.from(MATCH_RESOURCE))
        assertEquals(FlourishType.ROOT, FlourishType.from(MATCH_ROOT))
        assertEquals(FlourishType.WildVINE, FlourishType.from(MATCH_WILD_VINE))
        assertEquals(FlourishType.Vine, FlourishType.from(MATCH_VINE))
        assertEquals(FlourishType.FLOWER, FlourishType.from(MATCH_FLOWER))
        assertEquals(FlourishType.BUTTERFLY, FlourishType.from(MATCH_BUTTERFLY))
        assertEquals(FlourishType.WISP, FlourishType.from(MATCH_WISP))
    }

    @Test
    fun from_whenCaseInsensitive_returnsCorrectFlourishType() {
        // Arrange & Act & Assert
        assertEquals(FlourishType.CANOPY, FlourishType.from("canopy"))
        assertEquals(FlourishType.CANOPY, FlourishType.from("CANOPY"))
        assertEquals(FlourishType.CANOPY, FlourishType.from("Canopy"))
        assertEquals(FlourishType.RESOURCE, FlourishType.from("resource"))
        assertEquals(FlourishType.RESOURCE, FlourishType.from("RESOURCE"))
        assertEquals(FlourishType.ROOT, FlourishType.from("root"))
        assertEquals(FlourishType.ROOT, FlourishType.from("ROOT"))
    }

    @Test
    fun from_whenStartsWithMatch_returnsCorrectFlourishType() {
        // Arrange & Act & Assert
        assertEquals(FlourishType.CANOPY, FlourishType.from("Canopy Tree"))
        assertEquals(FlourishType.RESOURCE, FlourishType.from("Resource Card"))
        assertEquals(FlourishType.ROOT, FlourishType.from("Root System"))
        assertEquals(FlourishType.FLOWER, FlourishType.from("Flower Garden"))
        assertEquals(FlourishType.BUTTERFLY, FlourishType.from("Butterfly Effect"))
        assertEquals(FlourishType.WISP, FlourishType.from("Wisp Spirit"))
    }

    @Test
    fun from_whenNoMatch_throwsIllegalArgumentException() {
        // Arrange
        val invalidInput = "InvalidType"

        // Act & Assert
        val exception = assertThrows(IllegalArgumentException::class.java) {
            FlourishType.from(invalidInput)
        }
        assertEquals("No matching FlourishType found for: $invalidInput", exception.message)
    }

    @Test
    fun from_whenEmptyString_returnsNone() {
        // Arrange & Act
        val result = FlourishType.from("")

        // Assert
        assertEquals(FlourishType.NONE, result)
    }
}
