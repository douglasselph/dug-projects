package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.player.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReportDamageTest {

    private lateinit var SUT: ReportDamage

    companion object {
        private const val PLAYER_A_ID = 1
        private const val PLAYER_B_ID = 2
        private const val DAMAGE_TO_DEFENDER = 5
        private const val DAMAGE_TO_ATTACKER = 3
    }

    private val defender = mockk<Player>()
    private val attacker = mockk<Player>()

    @BeforeEach
    fun setup() {
        SUT = ReportDamage()
        every { defender.id } returns PLAYER_A_ID
        every { attacker.id } returns PLAYER_B_ID
    }

    @Test
    fun invoke_whenDamageToDefenderOnly_returnsCorrectReport() {
        // Arrange
        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns DAMAGE_TO_DEFENDER
        every { moment.damageToAttacker } returns 0
        every { moment.defender } returns defender
        every { moment.attacker } returns attacker
        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("Player $PLAYER_A_ID took $DAMAGE_TO_DEFENDER", report)
    }

    @Test
    fun invoke_whenDamageToAttackerOnly_returnsCorrectReport() {
        // Arrange
        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns 0
        every { moment.damageToAttacker } returns DAMAGE_TO_ATTACKER
        every { moment.defender } returns defender
        every { moment.attacker } returns attacker

        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("Player $PLAYER_B_ID took back $DAMAGE_TO_ATTACKER", report)
    }

    @Test
    fun invoke_whenDamageToBoth_returnsCorrectReport() {
        // Arrange
        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns DAMAGE_TO_DEFENDER
        every { moment.damageToAttacker } returns DAMAGE_TO_ATTACKER
        every { moment.defender } returns defender
        every { moment.attacker } returns attacker

        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("Player $PLAYER_A_ID took $DAMAGE_TO_DEFENDER,Player $PLAYER_B_ID took back $DAMAGE_TO_ATTACKER", report)
    }

} 
