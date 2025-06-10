package dugsolutions.leaf.game.turn.effect

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.game.turn.handle.HandleDieUpgrade
import dugsolutions.leaf.game.turn.handle.HandleLimitedDieUpgrade
import dugsolutions.leaf.player.PlayerTD
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EffectUpgradeDieTest {
    private lateinit var player: PlayerTD
    private val handleLimitedDieUpgrade: HandleLimitedDieUpgrade = mockk()
    private val handleDieUpgrade: HandleDieUpgrade = mockk()
    private val chronicle: GameChronicle = mockk(relaxed = true)
    private lateinit var SUT: EffectUpgradeDie
    private val die: Die = mockk()

    @BeforeEach
    fun setup() {
        player = PlayerTD(1)
        SUT = EffectUpgradeDie(handleLimitedDieUpgrade, handleDieUpgrade, chronicle)
    }

    @Test
    fun invoke_withOnlyList_callsHandleLimitedDieUpgrade_andChronicle() {
        // Arrange
        val only = listOf(DieSides.D6, DieSides.D8)
        every { handleLimitedDieUpgrade(player, only, true) } returns die

        // Act
        SUT(player, only, true)

        // Assert
        verify { handleLimitedDieUpgrade(player, only, true) }
        verify { chronicle(Moment.UPGRADE_DIE(player, die)) }
    }

    @Test
    fun invoke_withEmptyOnly_callsHandleDieUpgrade_andChronicle() {
        // Arrange
        every { handleDieUpgrade(player, false) } returns die

        // Act
        SUT(player)

        // Assert
        verify { handleDieUpgrade(player, false) }
        verify { chronicle(Moment.UPGRADE_DIE(player, die)) }
    }

    @Test
    fun invoke_whenNoDieReturned_doesNotCallChronicle() {
        // Arrange
        every { handleDieUpgrade(player, false) } returns null

        // Act
        SUT(player)

        // Assert
        verify(exactly = 0) { chronicle(any()) }
    }
} 