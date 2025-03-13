package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionDamageAbsorption
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.tool.Randomizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import dugsolutions.leaf.components.die.DieSides
import io.mockk.Runs
import io.mockk.just

class HandleAbsorbDamageTest {
    companion object {
        private const val MOCK_CARD_ID_1: CardID = 1
        private const val MOCK_CARD_ID_2: CardID = 2
    }

    private lateinit var handleAbsorbDamage: HandleAbsorbDamage
    private lateinit var mockPlayer: Player
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: Randomizer
    private lateinit var mockGameChronicle: GameChronicle

    @BeforeEach
    fun setup() {
        mockPlayer = mockk()
        mockGameChronicle = mockk()
        
        // Initialize random components
        randomizer = Randomizer.create()
        dieFactory = DieFactoryRandom(randomizer)

        handleAbsorbDamage = HandleAbsorbDamage(mockGameChronicle)

        every { mockPlayer.removeDieFromHand(any()) } returns true
        every { mockPlayer.removeCardFromHand(any()) } returns true
        every { mockGameChronicle(any()) } just Runs
    }

    @Test
    fun invoke_whenNoIncomingDamage_doesNothing() {
        // Arrange
        every { mockPlayer.hasIncomingDamage() } returns false

        // Act
        handleAbsorbDamage(mockPlayer)

        // Assert
        verify(exactly = 0) { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
    }

    @Test
    fun invoke_whenNoAbsorptionDecision_doesNothing() {
        // Arrange
        every { mockPlayer.hasIncomingDamage() } returns true
        every { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns null

        // Act
        handleAbsorbDamage(mockPlayer)

        // Assert
        verify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
    }

    @Test
    fun invoke_whenAbsorptionResultWithCards_removesCards() {
        // Arrange
        every { mockPlayer.hasIncomingDamage() } returns true
        every { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cardIds = listOf(MOCK_CARD_ID_1, MOCK_CARD_ID_2),
            dice = emptyList()
        )

        // Act
        handleAbsorbDamage(mockPlayer)

        // Assert
        verify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify { mockPlayer.removeCardFromHand(MOCK_CARD_ID_1) }
        verify { mockPlayer.removeCardFromHand(MOCK_CARD_ID_2) }
        verify(exactly = 0) { mockPlayer.removeDieFromHand(any()) }
    }

    @Test
    fun invoke_whenAbsorptionResultWithDice_removesDice() {
        // Arrange
        val d4 = dieFactory(DieSides.D4)
        val d6 = dieFactory(DieSides.D6)
        val d8 = dieFactory(DieSides.D8)
        val d10 = dieFactory(DieSides.D10)
        val d12 = dieFactory(DieSides.D12)
        val d20 = dieFactory(DieSides.D20)
        every { mockPlayer.hasIncomingDamage() } returns true
        every { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cardIds = emptyList(),
            dice = listOf(d4, d6, d8, d10, d12, d20)
        )

        // Act
        handleAbsorbDamage(mockPlayer)

        // Assert
        verify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify(exactly = 0) { mockPlayer.removeCardFromHand(any()) }
        verify { mockPlayer.removeDieFromHand(d4) }
        verify { mockPlayer.removeDieFromHand(d6) }
        verify { mockPlayer.removeDieFromHand(d8) }
        verify { mockPlayer.removeDieFromHand(d10) }
        verify { mockPlayer.removeDieFromHand(d12) }
    }

    @Test
    fun invoke_whenAbsorptionResultWithCardsAndDice_removesBoth() {
        // Arrange
        val d4 = dieFactory(DieSides.D4)
        val d6 = dieFactory(DieSides.D6)
        every { mockPlayer.hasIncomingDamage() } returns true
        every { mockPlayer.decisionDirector.damageAbsorptionDecision() } returns DecisionDamageAbsorption.Result(
            cardIds = listOf(MOCK_CARD_ID_1),
            dice = listOf(d4, d6)
        )

        // Act
        handleAbsorbDamage(mockPlayer)

        // Assert
        verify { mockPlayer.decisionDirector.damageAbsorptionDecision() }
        verify { mockPlayer.removeCardFromHand(MOCK_CARD_ID_1) }
        verify { mockPlayer.removeDieFromHand(d4) }
        verify { mockPlayer.removeDieFromHand(d6) }
    }
} 
