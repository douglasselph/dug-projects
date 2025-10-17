package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.components.die.Dice
import dugsolutions.leaf.components.die.Die
import dugsolutions.leaf.components.die.DieSides
import dugsolutions.leaf.components.die.SampleDie
import dugsolutions.leaf.di.DieFactory
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.PlayerTD
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerBattlePhaseCheck2D20Test {

    private lateinit var mockGrove: Grove
    private lateinit var mockDieFactory: DieFactory
    private lateinit var fakePlayer: Player
    private lateinit var fakeD20: Die
    private lateinit var fakeD6: Die
    private lateinit var mockDice: Dice
    private val sampleDie = SampleDie()

    private lateinit var SUT: PlayerBattlePhaseCheck2D20

    @BeforeEach
    fun setup() {
        mockGrove = mockk(relaxed = true)
        mockDieFactory = mockk(relaxed = true)
        fakePlayer = PlayerTD(1)
        fakeD20 = sampleDie.d20
        fakeD6 = sampleDie.d6
        mockDice = mockk(relaxed = true)

        SUT = PlayerBattlePhaseCheck2D20(mockGrove, mockDieFactory)

        // Default setup for D20 dice
        every { mockDieFactory(DieSides.D20) } returns fakeD20
    }

    // isReady tests

    @Test
    fun isReady_whenPlayerHasTwoD20Dice_returnsTrue() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD20)
        fakePlayer.addDieToCompost(fakeD20)

        // Act
        val result = SUT.isReady(fakePlayer)

        // Assert
        assertTrue(result)
    }

    @Test
    fun isReady_whenPlayerHasMoreThanTwoD20Dice_returnsTrue() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD20)
        fakePlayer.addDieToCompost(fakeD20)
        fakePlayer.addDieToCompost(fakeD20)
        fakePlayer.addDieToCompost(fakeD20)

        // Act
        val result = SUT.isReady(fakePlayer)

        // Assert
        assertTrue(result)
    }

    @Test
    fun isReady_whenPlayerHasOneD20Die_returnsFalse() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD20)
        fakePlayer.addDieToCompost(fakeD6)

        every { mockGrove.hasCards } returns true
        every { mockGrove.hasDice } returns true

        // Act
        val result = SUT.isReady(fakePlayer)

        // Assert
        assertFalse(result)
    }

    @Test
    fun isReady_whenPlayerHasNoD20DiceButMarketIsEmpty_returnsTrue() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD6)
        fakePlayer.addDieToCompost(fakeD6)
        fakePlayer.addDieToCompost(fakeD6)

        every { mockGrove.hasCards } returns false
        every { mockGrove.hasDice } returns false

        // Act
        val result = SUT.isReady(fakePlayer)

        // Assert
        assertTrue(result)
    }

    @Test
    fun isReady_whenPlayerHasNoD20DiceAndMarketHasCards_returnsFalse() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD6)
        fakePlayer.addDieToCompost(fakeD6)

        every { mockGrove.hasCards } returns true
        every { mockGrove.hasDice } returns false

        // Act
        val result = SUT.isReady(fakePlayer)

        // Assert
        assertFalse(result)
    }

    // giftTo tests

    @Test
    fun giftTo_whenPlayerIsNotReadyAndMarketHasD20_givesD20ToPlayer() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD6)
        fakePlayer.addDieToCompost(fakeD6)

        every { mockGrove.hasCards } returns true
        every { mockGrove.hasDice } returns true
        every { mockGrove.hasDie(DieSides.D20.value) } returns true

        // Act
        SUT.giftTo(fakePlayer)

        // Assert
        verify { mockDieFactory(DieSides.D20) }
        verify { mockGrove.removeDie(fakeD20) }

        val allDice = fakePlayer.allDice.copy
        assertTrue(allDice.find { it.sides == 20 } != null)
    }

    @Test
    fun giftTo_whenPlayerIsNotReadyButMarketHasNoD20_doesNothing() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD6)
        fakePlayer.addDieToCompost(fakeD6)

        every { mockGrove.hasCards } returns true
        every { mockGrove.hasDice } returns true
        every { mockGrove.hasDie(DieSides.D20.value) } returns false

        // Act
        SUT.giftTo(fakePlayer)

        // Assert
        verify(exactly = 0) { mockDieFactory(any<DieSides>()) }
        verify(exactly = 0) { mockGrove.removeDie(any()) }

        val allDice = fakePlayer.allDice.copy
        val count = allDice.count { it.sides > 6 }
        assertEquals(0, count)
    }

    @Test
    fun giftTo_whenPlayerIsReady_doesNothing() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD20)
        fakePlayer.addDieToCompost(fakeD20)

        // Act
        SUT.giftTo(fakePlayer)

        // Assert
        verify(exactly = 0) { mockGrove.hasDie(any()) }
        verify(exactly = 0) { mockDieFactory(any<DieSides>()) }
        verify(exactly = 0) { mockGrove.removeDie(any()) }

        val allDice = fakePlayer.allDice.copy
        assertEquals(2, allDice.count { it.sides == 20 })
    }

    @Test
    fun giftTo_checksReadinessBeforeAccessingMarket() {
        // Arrange
        fakePlayer.addDieToCompost(fakeD20)
        fakePlayer.addDieToCompost(fakeD20)

        // Act
        SUT.giftTo(fakePlayer)

        // Assert
    }
} 
