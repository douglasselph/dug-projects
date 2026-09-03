package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoundCardRegistryTest {

    private lateinit var registry: RoundCardRegistry

    @BeforeEach
    fun setup() {
        registry = RoundCardRegistry(GameEffectConverter())
    }

    @Test
    fun loadFromCsv_loadsAllRoundDefinitions() {
        // Act
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Assert
        assertEquals(12, registry.getAllCards().size)
    }

    @Test
    fun loadFromCsv_parsesBothEffectsAndPresentationFields() {
        // Act
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Assert
        val card = assertNotNull(registry.getCard("Battle_Beckon_Swell"))
        assertEquals(1, card.quantity)
        assertEquals(RoundCardType.BATTLE, card.type)
        assertEquals("{{ images.battle_transition_back.url }}", card.backImage)

        assertEquals("Beckon", card.firstEffect.title)
        assertEquals("a30000", card.firstEffect.backgroundColor)
        assertEquals("f8f5f2", card.firstEffect.textColor)
        assertEquals("{{ images.turn_surge_root.url }}", card.firstEffect.image)
        assertEquals("{{ images.ic_root.url }}", card.firstEffect.icon)
        assertEquals(GameEffect.GAIN_ONE_WISP, card.firstEffect.effect)

        assertEquals("Swell", card.secondEffect.title)
        assertEquals("7a0000", card.secondEffect.backgroundColor)
        assertEquals("#f8f5f2", card.secondEffect.textColor)
        assertEquals("{{ images.turn_surge_D12.url }}", card.secondEffect.image)
        assertEquals("{{ images.D10.url }}", card.secondEffect.icon)
        assertEquals(GameEffect.GAIN_D10_TO_DISCARD, card.secondEffect.effect)
    }

    @Test
    fun loadFromCsv_parsesCultivationTypeAndQuantity() {
        // Act
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Assert
        val card = assertNotNull(registry.getCard("Resource_Water_Mulch"))
        assertEquals(2, card.quantity)
        assertEquals(RoundCardType.CULTIVATION, card.type)
        assertEquals(GameEffect.GAIN_WATER_TOKEN, card.firstEffect.effect)
        assertEquals(GameEffect.MULCH_DIE_FROM_HAND, card.secondEffect.effect)
    }

    @Test
    fun getCard_whenCaseAndWhitespaceDiffer_returnsCard() {
        // Arrange
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Act
        val result = registry.getCard("  BATTLE_BECKON_SWELL  ")

        // Assert
        assertEquals("Battle_Beckon_Swell", result?.name)
    }

    @Test
    fun getCard_whenNameDoesNotExist_returnsNull() {
        // Arrange
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Act
        val result = registry.getCard("Not_A_Round_Card")

        // Assert
        assertNull(result)
    }

    @Test
    fun loadFromCsv_whenSameFileLoadedTwice_rejectsDuplicateNames() {
        // Arrange
        val file = dataPath(CardDataFiles.ROUND_CARD_LIST)
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
        registry.loadFromCsv(dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Act
        registry.clear()

        // Assert
        assertEquals(emptyList(), registry.getAllCards())
    }

    private fun dataPath(fileName: String): String =
        Path.of("data", "v35", fileName).toString()
}
