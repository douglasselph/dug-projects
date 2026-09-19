package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
        registry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Assert
        assertEquals(12, registry.getAllCards().size)
    }

    @Test
    fun loadFromCsv_parsesBothEffectsAndPresentationFields() {
        // Act
        registry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Assert
        val card = assertNotNull(registry.getCard("Battle_Whisper_Burst"))
        assertEquals(1, card.quantity)
        assertEquals(RoundCardType.BATTLE, card.type)
        assertEquals("{{ images.battle_transition_back.url }}", card.backImage)

        assertEquals("Whisper", card.firstEffect.title)
        assertEquals("b80000", card.firstEffect.backgroundColor)
        assertEquals("f8f5f2", card.firstEffect.textColor)
        assertEquals("{{ images.turn_beauty.url }}", card.firstEffect.image)
        assertEquals("{{ images.wisp_bg2.url }}", card.firstEffect.icon)
        assertEquals(GameEffect.GAIN_ONE_WISP, card.firstEffect.effect)

        assertEquals("Burst", card.secondEffect.title)
        assertEquals("7a0000", card.secondEffect.backgroundColor)
        assertEquals("f8f5f2", card.secondEffect.textColor)
        assertEquals("{{ images.turn_surge_D20.url }}", card.secondEffect.image)
        assertEquals("{{ images.D20.url }}", card.secondEffect.icon)
        assertEquals(GameEffect.GAIN_D20_TO_DISCARD, card.secondEffect.effect)
    }

    @Test
    fun loadFromCsv_parsesCultivationTypeAndQuantity() {
        // Act
        registry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Assert
        val card = assertNotNull(registry.getCard("Resource_Water_Mulch"))
        assertEquals(2, card.quantity)
        assertEquals(RoundCardType.CULTIVATION, card.type)
        assertEquals(GameEffect.GAIN_WATER_TOKEN, card.firstEffect.effect)
        assertEquals(GameEffect.MULCH_DIE_FROM_HAND, card.secondEffect.effect)
    }

    @Test
    fun loadFromCsv_parsesCurrentSimpleBattleRoundEffects() {
        registry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))

        val surge = assertNotNull(registry.getCard("Battle_Bloom_Surge"))
        assertEquals(GameEffect.GAIN_D12_TO_DISCARD, surge.secondEffect.effect)

        val swell = assertNotNull(registry.getCard("Battle_Scatter_Swell"))
        assertEquals(GameEffect.GAIN_D10_TO_DISCARD, swell.secondEffect.effect)

        val bath = assertNotNull(registry.getCard("Battle_Sprawl_Bath"))
        assertEquals(GameEffect.REFRESH_CREATURE, bath.secondEffect.effect)

        val flow = assertNotNull(registry.getCard("Battle_Barkskin_Flow"))
        assertEquals(GameEffect.GAIN_ANY_DIE_TO_DISCARD, flow.secondEffect.effect)
    }

    @Test
    fun getCard_whenCaseAndWhitespaceDiffer_returnsCard() {
        // Arrange
        registry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Act
        val result = registry.getCard("  BATTLE_WHISPER_BURST  ")

        // Assert
        assertEquals("Battle_Whisper_Burst", result?.name)
    }

    @Test
    fun getCard_whenNameDoesNotExist_returnsNull() {
        // Arrange
        registry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Act
        val result = registry.getCard("Not_A_Round_Card")

        // Assert
        assertNull(result)
    }

    @Test
    fun loadFromCsv_whenSameFileLoadedTwice_rejectsDuplicateNames() {
        // Arrange
        val file = CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST)
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
        registry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))

        // Act
        registry.clear()

        // Assert
        assertEquals(emptyList(), registry.getAllCards())
    }

}
