package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.player.Player
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertTrue

class ReportDamageTest {

    private lateinit var SUT: ReportDamage

    companion object {
        private const val PLAYER_A_ID = 1
        private const val PLAYER_B_ID = 2
        private const val DAMAGE_TO_DEFENDER = 5
        private const val DAMAGE_TO_ATTACKER = 3
    }

    private val defender = mockk<Player>(relaxed = true)
    private val attacker = mockk<Player>(relaxed = true)

    @BeforeEach
    fun setup() {
        SUT = ReportDamage()
        every { defender.id } returns PLAYER_A_ID
        every { attacker.id } returns PLAYER_B_ID
    }

    @Test
    fun invoke_whenDamageToDefenderOnly_returnsCorrectReport() {
        // Arrange
        val moment = mockk<Moment.DELIVER_DAMAGE>(relaxed = true)
        every { moment.damageToDefender } returns DAMAGE_TO_DEFENDER
        every { moment.damageToAttacker } returns 0
        every { moment.defender } returns defender
        every { moment.attacker } returns attacker
        // Act
        val report = SUT(moment)

        // Assert
        assertTrue(report.contains(PLAYER_A_ID.toStr()))
        assertTrue(report.contains(DAMAGE_TO_DEFENDER.toString()))
    }

    @Test
    fun invoke_whenDamageToAttackerOnly_returnsCorrectReport() {
        // Arrange
        val moment = mockk<Moment.DELIVER_DAMAGE>(relaxed = true)
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
        val moment = mockk<Moment.DELIVER_DAMAGE>(relaxed = true)
        every { moment.damageToDefender } returns DAMAGE_TO_DEFENDER
        every { moment.damageToAttacker } returns DAMAGE_TO_ATTACKER
        every { moment.defender } returns defender
        every { moment.attacker } returns attacker

        // Act
        val report = SUT(moment)

        // Assert
        assertTrue(report.contains(PLAYER_A_ID.toString()))
        assertTrue(report.contains(PLAYER_B_ID.toString()))
        assertTrue(report.contains(DAMAGE_TO_DEFENDER.toString()))
        assertTrue(report.contains(DAMAGE_TO_ATTACKER.toString()))
    }

} 
