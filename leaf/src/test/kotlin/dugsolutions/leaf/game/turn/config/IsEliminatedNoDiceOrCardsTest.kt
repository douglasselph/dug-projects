package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.di.DieFactoryRandom
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.tool.RandomizerTD
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IsEliminatedNoDiceOrCardsTest {

    companion object {
        private const val PLAYER_ID = 1
        private const val PLAYER_NAME = "Test Player"
    }

    private lateinit var isEliminated: IsEliminatedNoDiceOrCards
    private lateinit var player: PlayerTD
    private lateinit var dieFactory: DieFactory
    private lateinit var randomizer: RandomizerTD

    @BeforeEach
    fun setup() {
        // Arrange
        randomizer = RandomizerTD()
        dieFactory = DieFactoryRandom(randomizer)
        
        // Create test player
        player = PlayerTD(PLAYER_NAME, PLAYER_ID)
        
        // Create the handler
        isEliminated = IsEliminatedNoDiceOrCards()
    }
    
    private fun setupPlayerWithCardsAndDice(cardCount: Int, diceCount: Int) {
        // Add cards if needed
        if (cardCount > 0) {
            val cards = when {
                cardCount >= 3 -> listOf(FakeCards.fakeSeedling, FakeCards.fakeBloom, FakeCards.fakeVine)
                cardCount == 2 -> listOf(FakeCards.fakeSeedling, FakeCards.fakeBloom)
                else -> listOf(FakeCards.fakeSeedling)
            }
            player.addCardsToSupply(cards.take(cardCount))
        }
        
        // Add dice if needed
        if (diceCount > 0) {
            val dice = listOf(
                dieFactory(DieSides.D6),
                dieFactory(DieSides.D8),
                dieFactory(DieSides.D10),
                dieFactory(DieSides.D12),
                dieFactory(DieSides.D20)
            )
            player.diceInSupply.addAll(dice.take(diceCount))
        }
    }

    @Test
    fun invoke_whenPlayerHasNoCardsAndNoDice_returnsTrue() {
        // Arrange
        setupPlayerWithCardsAndDice(0, 0)
        
        // Act
        val result = isEliminated(player)
        
        // Assert
        assertTrue(result, "Player with no cards and no dice should be eliminated")
    }
    
    @Test
    fun invoke_whenPlayerHasNoCardsButHasDice_returnsFalse() {
        // Arrange
        setupPlayerWithCardsAndDice(0, 3)
        
        // Act
        val result = isEliminated(player)
        
        // Assert
        assertFalse(result, "Player with no cards but with dice should NOT be eliminated")
    }
    
    @Test
    fun invoke_whenPlayerHasNoDiceButHasCards_returnsFalse() {
        // Arrange
        setupPlayerWithCardsAndDice(2, 0)
        
        // Act
        val result = isEliminated(player)
        
        // Assert
        assertFalse(result, "Player with no dice but with cards should NOT be eliminated")
    }
    
    @Test
    fun invoke_whenPlayerHasCardsAndDice_returnsFalse() {
        // Arrange
        setupPlayerWithCardsAndDice(2, 3)
        
        // Act
        val result = isEliminated(player)
        
        // Assert
        assertFalse(result, "Player with both cards and dice should not be eliminated")
    }
    
    @Test
    fun invoke_whenPlayerHasManyCardsAndManyDice_returnsFalse() {
        // Arrange
        setupPlayerWithCardsAndDice(3, 5)
        
        // Act
        val result = isEliminated(player)
        
        // Assert
        assertFalse(result, "Player with many cards and dice should not be eliminated")
    }
} 
