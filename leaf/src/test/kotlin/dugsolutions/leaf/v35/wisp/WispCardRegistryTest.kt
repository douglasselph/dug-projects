package dugsolutions.leaf.v35.wisp

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WispCardRegistryTest {

    private lateinit var registry: WispCardRegistry

    @BeforeEach
    fun setup() {
        registry = WispCardRegistry(GameEffectConverter())
    }

    @Test
    fun loadFromCsv_loadsAllWispDefinitions() {
        // Act
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        // Assert
        assertEquals(13, registry.getAllCards().size)
    }

    @Test
    fun loadFromCsv_parsesWispCardFieldsAndEffect() {
        // Act
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        // Assert
        val card = assertNotNull(registry.getCard("Wisp_Award_VP"))
        assertEquals(2, card.quantity)
        assertEquals("Wisp of Honor", card.title)
        assertEquals(2, card.count)
        assertEquals(GameEffect.GAIN_ONE_VP, card.effect)
        assertNull(card.lineIcons)
        assertEquals(80, card.lineIconsHeight)
        assertEquals("{{ images.victory_victory.url }}", card.vpIcon)
        assertEquals("{{ images.cloud_honor.url }}", card.mainBackdrop)
        assertEquals(2, card.endGameVp)
    }

    @Test
    fun loadFromCsv_parsesAllWispEndGameVpValues() {
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        assertEquals(3, assertNotNull(registry.getCard("Wisp_Award_VP2")).endGameVp)
        assertEquals(1, assertNotNull(registry.getCard("Wisp_Gain_Critters")).endGameVp)
        assertEquals(0, assertNotNull(registry.getCard("Wisp_Quake")).endGameVp)
        assertEquals(0, assertNotNull(registry.getCard("Wisp_Reckoning")).endGameVp)
    }

    @Test
    fun getCard_whenCaseAndWhitespaceDiffer_returnsCard() {
        // Arrange
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        // Act
        val result = registry.getCard("  WISP_AWARD_VP  ")

        // Assert
        assertEquals("Wisp_Award_VP", result?.name)
    }

    @Test
    fun getCard_whenNameDoesNotExist_returnsNull() {
        // Arrange
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        // Act
        val result = registry.getCard("Not_A_Wisp")

        // Assert
        assertNull(result)
    }

    @Test
    fun loadFromCsv_whenSameFileLoadedTwice_rejectsDuplicateNames() {
        // Arrange
        val file = dataPath(CardDataFiles.WISP_LIST)
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
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        // Act
        registry.clear()

        // Assert
        assertEquals(emptyList(), registry.getAllCards())
    }

    @Test
    fun loadFromCsv_parsesCurrentWispTimingMetadata() {
        registry.loadFromCsv(dataPath(CardDataFiles.WISP_LIST))

        val quake = assertNotNull(registry.getCard("Wisp_Quake"))
        val reckoning = assertNotNull(registry.getCard("Wisp_Reckoning"))
        val pollenTheft = assertNotNull(registry.getCard("Wisp_Swap_Die"))

        assertEquals(true, quake.playImmediately)
        assertEquals(false, reckoning.playImmediately)
        assertEquals(false, reckoning.battleOnly)
        assertEquals(true, pollenTheft.battleOnly)
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()
}
