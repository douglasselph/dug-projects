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
        private const val PLAYER_C_ID = 3
        private const val DAMAGE_TO_DEFENDER = 5
        private const val DAMAGE_TO_ATTACKER = 3
    }

    @BeforeEach
    fun setup() {
        SUT = ReportDamage()
    }

    @Test
    fun invoke_whenDamageToDefenderOnly_returnsCorrectReport() {
        // Arrange
        val defender = mockk<Player>()
        every { defender.id } returns PLAYER_A_ID

        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns DAMAGE_TO_DEFENDER
        every { moment.damageToAttacker } returns 0
        every { moment.defenders } returns listOf(defender)
        every { moment.attackers } returns emptyList()

        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("Player $PLAYER_A_ID took $DAMAGE_TO_DEFENDER", report)
    }

    @Test
    fun invoke_whenDamageToAttackerOnly_returnsCorrectReport() {
        // Arrange
        val attacker = mockk<Player>()
        every { attacker.id } returns PLAYER_B_ID

        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns 0
        every { moment.damageToAttacker } returns DAMAGE_TO_ATTACKER
        every { moment.defenders } returns emptyList()
        every { moment.attackers } returns listOf(attacker)

        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("Player $PLAYER_B_ID took back $DAMAGE_TO_ATTACKER", report)
    }

    @Test
    fun invoke_whenDamageToBoth_returnsCorrectReport() {
        // Arrange
        val defender = mockk<Player>()
        val attacker = mockk<Player>()
        every { defender.id } returns PLAYER_A_ID
        every { attacker.id } returns PLAYER_B_ID

        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns DAMAGE_TO_DEFENDER
        every { moment.damageToAttacker } returns DAMAGE_TO_ATTACKER
        every { moment.defenders } returns listOf(defender)
        every { moment.attackers } returns listOf(attacker)

        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("Player $PLAYER_A_ID took $DAMAGE_TO_DEFENDER,Player $PLAYER_B_ID took back $DAMAGE_TO_ATTACKER", report)
    }

    @Test
    fun invoke_whenMultipleDefenders_returnsCorrectReport() {
        // Arrange
        val defender1 = mockk<Player>()
        val defender2 = mockk<Player>()
        every { defender1.id } returns PLAYER_A_ID
        every { defender2.id } returns PLAYER_C_ID

        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns DAMAGE_TO_DEFENDER
        every { moment.damageToAttacker } returns 0
        every { moment.defenders } returns listOf(defender1, defender2)
        every { moment.attackers } returns emptyList()

        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("Player $PLAYER_A_ID took $DAMAGE_TO_DEFENDER,Player $PLAYER_C_ID took $DAMAGE_TO_DEFENDER", report)
    }

    @Test
    fun invoke_whenNoDamage_returnsEmptyString() {
        // Arrange
        val moment = mockk<GameChronicle.Moment.DELIVER_DAMAGE>()
        every { moment.damageToDefender } returns 0
        every { moment.damageToAttacker } returns 0
        every { moment.defenders } returns emptyList()
        every { moment.attackers } returns emptyList()

        // Act
        val report = SUT(moment)

        // Assert
        assertEquals("", report)
    }
} 
