package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlantCardRegistryTest {

    private lateinit var registry: PlantCardRegistry

    @BeforeEach
    fun setup() {
        registry = PlantCardRegistry()
    }

    @Test
    fun loadFromCsv_whenRootFileLoaded_parsesRootCards() {
        // Act
        registry.loadFromCsv(dataPath(CardDataFiles.ROOT_CARD_LIST))

        // Assert
        assertEquals(12, registry.getAllCards().size)

        val card = assertNotNull(registry.getCard("Root_05_01"))
        assertEquals(6, card.quantity)
        assertEquals("Root Double Down", card.title)
        assertEquals(PlantType.ROOT, card.type)
        assertEquals(5, card.cost)
        assertNull(card.lineIcon)
        assertEquals("{{ images.victory.url }}", card.vpIcon)
        assertEquals("{{ images.icon_root.url }}", card.typeIcon)
        assertEquals("8f754f", card.fgColor)
        assertEquals("fcf5d9", card.textColor)
        assertEquals("{{ images.root_reinforced_full.url }}", card.fullImage)
        assertEquals("{{ images.back_root2.url }}", card.backgroundImage)
        assertEquals("{{ images.cc_root_double_down.url }}", card.cardBackgroundImage)
        assertEquals(GameEffect.DOUBLE_ONE_DIE, card.effect)
    }

    @Test
    fun loadFromCsv_whenVineFlowerFileLoaded_parsesAlternateBackgroundColumn() {
        // Act
        registry.loadFromCsv(dataPath(CardDataFiles.VF_CARD_LIST))

        // Assert
        assertEquals(24, registry.getAllCards().size)

        val card = assertNotNull(registry.getCard("Flower_11_01"))
        assertEquals(PlantType.FLOWER, card.type)
        assertEquals(11, card.cost)
        assertEquals("{{ images.back_flower.url }}", card.backgroundImage)
        assertEquals(
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            card.effect
        )
    }

    @Test
    fun loadFromCsv_whenBothPlantFilesLoaded_loadsAllCards() {
        // Act
        registry.loadFromCsv(
            dataPath(CardDataFiles.ROOT_CARD_LIST),
            dataPath(CardDataFiles.VF_CARD_LIST)
        )

        // Assert
        assertEquals(36, registry.getAllCards().size)
    }

    @Test
    fun getCard_whenCaseAndWhitespaceDiffer_returnsCard() {
        // Arrange
        registry.loadFromCsv(dataPath(CardDataFiles.ROOT_CARD_LIST))

        // Act
        val result = registry.getCard("  ROOT_05_01  ")

        // Assert
        assertEquals("Root_05_01", result?.name)
    }

    @Test
    fun getCard_whenNameDoesNotExist_returnsNull() {
        // Arrange
        registry.loadFromCsv(dataPath(CardDataFiles.ROOT_CARD_LIST))

        // Act
        val result = registry.getCard("Root_99_99")

        // Assert
        assertNull(result)
    }

    @Test
    fun loadFromCsv_whenSameFileLoadedTwice_rejectsDuplicateNames() {
        // Arrange
        val file = dataPath(CardDataFiles.ROOT_CARD_LIST)
        registry.loadFromCsv(file)

        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            registry.loadFromCsv(file)
        }
    }

    @Test
    fun loadFromCsv_whenFileDoesNotExist_throws() {
        // Act / Assert
        assertFailsWith<IllegalArgumentException> {
            registry.loadFromCsv("data/v35/does-not-exist.csv")
        }
    }

    @Test
    fun clear_removesAllCards() {
        // Arrange
        registry.loadFromCsv(
            dataPath(CardDataFiles.ROOT_CARD_LIST),
            dataPath(CardDataFiles.VF_CARD_LIST)
        )

        // Act
        registry.clear()

        // Assert
        assertEquals(emptyList(), registry.getAllCards())
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()
}
