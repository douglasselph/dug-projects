package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.Moment
import dugsolutions.leaf.random.die.DieValue
import dugsolutions.leaf.player.PlayerTD
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HandleDeliverDamageTest {

    private lateinit var player1: PlayerTD
    private lateinit var player2: PlayerTD
    private lateinit var player3: PlayerTD
    private lateinit var player4: PlayerTD
    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var mockHandleAbsorbDamage: HandleAbsorbDamage

    private lateinit var SUT: HandleDeliverDamage

    @BeforeEach
    fun setup() {
        // Create PlayerTD instances instead of mocks
        player1 = PlayerTD("Player 1", 1)
        player2 = PlayerTD("Player 2", 2)
        player3 = PlayerTD("Player 3", 3)
        player4 = PlayerTD("Player 4", 4)

        mockGameChronicle = mockk(relaxed = true)
        mockHandleAbsorbDamage = mockk(relaxed = true)
        SUT = HandleDeliverDamage(mockHandleAbsorbDamage, mockGameChronicle)

        // Reset player values before each test
        player1.incomingDamage = 0
        player2.incomingDamage = 0
        player3.incomingDamage = 0
        player4.incomingDamage = 0
        player1.pipModifier = 0
        player2.pipModifier = 0
        player3.pipModifier = 0
        player4.pipModifier = 0

        // Clear dice from previous tests
        player1.diceInHand.clear()
        player2.diceInHand.clear()
        player3.diceInHand.clear()
        player4.diceInHand.clear()

        // Configure chronicle mock
        coEvery { mockHandleAbsorbDamage(any()) } returns 0

    }

    @Test
    fun invoke_whenThreePlayersWithDifferentPips_deliversDamageInChain() = runBlocking {
        // Arrange
        // Setup pip totals with dice and modifiers
        player1.addDieToHand(DieValue(6, 3))  // 3 pips
        player2.addDieToHand(DieValue(6, 5))  // 5 pips
        player3.addDieToHand(DieValue(8, 7))  // 7 pips

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        assertEquals(2, player1.incomingDamage)  // 5 - 3
        assertEquals(2, player2.incomingDamage)  // 7 - 5
        assertEquals(0, player3.incomingDamage)  // No one above

        // Verify the chronicle is called with the correct groupings
        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE && it.defender.id == player1.id
            })
        }

        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE && it.defender.id == player2.id
            })
        }
    }

    @Test
    fun invoke_whenThreePlayersWithThornDamage_deliversThornDamage() = runBlocking {
        // Arrange
        // Setup pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips
        player2.addDieToHand(DieValue(6, 5))  // 5 pips
        player3.addDieToHand(DieValue(8, 7))  // 7 pips

        coEvery { mockHandleAbsorbDamage(player1) } returns 1
        coEvery { mockHandleAbsorbDamage(player2) } returns 2

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        assertEquals(2, player1.incomingDamage)  // 5 - 3
        assertEquals(3, player2.incomingDamage)  // (7 - 5) + 1 (Thorn from player1)
        assertEquals(2, player3.incomingDamage)  // Thorn from player2

        // Verify chronicle includes thorn damage
        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.damageToDefender == 2 // 5 - 3
            })
            mockGameChronicle(match {
                it is Moment.THORN_DAMAGE &&
                        it.thornDamage == 1    // Thorn
            })
        }

        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.damageToDefender == 2 // 7 - 5
            })
            mockGameChronicle(match {
                it is Moment.THORN_DAMAGE &&
                        it.thornDamage == 2    // Thorn
            })
        }
    }

    @Test
    fun invoke_whenTwoPlayersWithEqualPips_noDamageDelivered() = runBlocking {
        // Arrange
        // Equal pip totals with dice
        player1.addDieToHand(DieValue(6, 5))  // 5 pips
        player2.addDieToHand(DieValue(6, 5))  // 5 pips

        val players = listOf(player1, player2)

        // Act
        SUT(players)

        // Assert
        assertEquals(0, player1.incomingDamage)
        assertEquals(0, player2.incomingDamage)
        verify(exactly = 1) { mockGameChronicle(any()) }
    }

    @Test
    fun invoke_whenTwoTiedPlayersDefending_defenderClosestInChainGetsHit() = runBlocking {
        // Arrange
        // Set up pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips (tied lowest)
        player2.addDieToHand(DieValue(8, 3))  // 3 pips (tied lowest)
        player3.addDieToHand(DieValue(8, 7))  // 7 pips (tied highest)

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        assertEquals(4, player1.incomingDamage)
        assertEquals(0, player2.incomingDamage)
        assertEquals(0, player3.incomingDamage)

        // Chronicle should record the group damage
        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE && it.defender.id == player1.id
            })
        }
    }

    @Test
    fun invoke_whenTwoTiedDefendersWithThorn_attackerGetsWithCorrectThornDamage() = runBlocking {
        // Arrange
        // Set up pip totals with combination of dice and modifiers
        player1.addDieToHand(DieValue(6, 6))  // 6 pips
        player1.pipModifier = 4  // Total: 10 pips (highest)

        player2.addDieToHand(DieValue(6, 5))  // 5 pips (tied middle)
        player3.addDieToHand(DieValue(6, 5))  // 5 pips (tied middle)

        player4.addDieToHand(DieValue(6, 2))  // 2 pips (lowest)

        // Each tied defender has thorn damage
        coEvery { mockHandleAbsorbDamage(player2) } returns 1
        coEvery { mockHandleAbsorbDamage(player3) } returns 2

        val players = listOf(player1, player2, player3, player4)

        // Act
        SUT(players)

        // Assert
        // Player1 gets hit with combined thorn damage from first in line tied player
        assertEquals(1, player1.incomingDamage)  // Thorn: 1

        // First in line tied player gets hit with the same damage from player1
        assertEquals(5, player2.incomingDamage)  // 10-5
        assertEquals(0, player3.incomingDamage)  // 10-5

        // Lowest player gets hit by the tied group
        assertEquals(3, player4.incomingDamage)  // 5-2

        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.defender.id == player2.id &&
                        it.damageToDefender == 5  // 10-5
            })
            mockGameChronicle(match {
                it is Moment.THORN_DAMAGE &&
                        it.player.id == player1.id &&
                        it.thornDamage == 1    // Thorn: 1
            })
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.defender.id == player4.id &&
                        it.damageToDefender == 3     // 5-2
            })
        }

    }

    @Test
    fun invoke_whenAllPlayersTied_noDamageDelivered() = runBlocking {
        // Arrange
        // Equal pip totals using dice
        player1.addDieToHand(DieValue(6, 5))  // 5 pips
        player2.addDieToHand(DieValue(6, 5))  // 5 pips
        player3.addDieToHand(DieValue(6, 5))  // 5 pips

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        assertEquals(0, player1.incomingDamage)
        assertEquals(0, player2.incomingDamage)
        assertEquals(0, player3.incomingDamage)
        verify { mockGameChronicle(any()) }
    }

    @Test
    fun invoke_whenTiedGroupsWithGap_damageCalculatedCorrectly() = runBlocking {
        // Arrange
        // Set up pip totals with mixed dice and modifiers
        player1.addDieToHand(DieValue(6, 6))  // 6 pips
        player1.pipModifier = 4  // Total: 10 pips (highest)

        player2.addDieToHand(DieValue(6, 4))  // 4 pips
        player2.pipModifier = 5  // Total: 9 pips (next highest)

        player3.addDieToHand(DieValue(6, 4))  // 4 pips (tied lowest)
        player4.addDieToHand(DieValue(6, 4))  // 4 pips (tied lowest)

        val players = listOf(player1, player2, player3, player4)

        // Act
        SUT(players)

        // Assert
        assertEquals(0, player1.incomingDamage)
        assertEquals(1, player2.incomingDamage)
        assertEquals(5, player3.incomingDamage)
        assertEquals(0, player4.incomingDamage)

        // Verify chronicle
        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.defender.id == player3.id &&
                        it.damageToDefender == 5
            })
        }
    }

    @Test
    fun invoke_whenMixedDiceAndModifiers_calculatesPipTotalCorrectly() = runBlocking {
        // Arrange
        // Setup mixed pip total with both dice and modifiers
        player1.addDieToHand(DieValue(6, 2))  // 2 pips from die
        player1.pipModifier = 1               // +1 from modifier = 3 total

        player2.addDieToHand(DieValue(4, 3))  // 3 pips from die
        player2.pipModifier = 2               // +2 from modifier = 5 total

        player3.addDieToHand(DieValue(8, 4))  // 4 pips from die
        player3.addDieToHand(DieValue(6, 3))  // +3 pips from second die
        player3.pipModifier = 0               // +0 from modifier = 7 total

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        assertEquals(2, player1.incomingDamage)  // 5 - 3
        assertEquals(2, player2.incomingDamage)  // 7 - 5
        assertEquals(0, player3.incomingDamage)  // No one above

        // Verify chronicle
        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.defender.id == player1.id
            })
        }

        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.defender.id == player2.id
            })
        }
    }

    @Test
    fun invoke_whenHighestPipPlayerGetsThornDamage_handlesAbsorbDamage() = runBlocking {
        // Arrange
        // Setup pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips (lowest)
        player2.addDieToHand(DieValue(8, 7))  // 7 pips (highest)

        // Player1 has thorn damage that will hit player2
        coEvery { mockHandleAbsorbDamage(player1) } returns 2

        val players = listOf(player1, player2)

        // Act
        SUT(players)

        // Assert
        assertEquals(4, player1.incomingDamage)  // No damage from above
        assertEquals(2, player2.incomingDamage)  // 7-3 = 4 damage from player1

        // Verify chronicle for the damage
        verify {
            mockGameChronicle(match {
                it is Moment.DELIVER_DAMAGE &&
                        it.defender.id == player1.id &&
                        it.damageToDefender == 4  // 7-3
            })
            mockGameChronicle(match {
                it is Moment.THORN_DAMAGE &&
                        it.player.id == player2.id &&
                        it.thornDamage == 2     // Thorn: 2
            })
        }

        // Verify that handleAbsorbDamage was called for player2
        coVerify { mockHandleAbsorbDamage(player2) }
    }
} 
