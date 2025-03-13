package dugsolutions.leaf.game.battle

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.die.DieValue
import dugsolutions.leaf.player.PlayerTD
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HandleDeliverDamageTest {

    private lateinit var player1: PlayerTD
    private lateinit var player2: PlayerTD
    private lateinit var player3: PlayerTD
    private lateinit var player4: PlayerTD
    private lateinit var mockGameChronicle: GameChronicle
    private lateinit var SUT: HandleDeliverDamage

    @BeforeEach
    fun setup() {
        // Create PlayerTD instances instead of mocks
        player1 = PlayerTD("Player 1", 1)
        player2 = PlayerTD("Player 2", 2)
        player3 = PlayerTD("Player 3", 3)
        player4 = PlayerTD("Player 4", 4)
        
        mockGameChronicle = mockk()
        SUT = HandleDeliverDamage(mockGameChronicle)

        // Reset player values before each test
        player1.incomingDamage = 0
        player2.incomingDamage = 0
        player3.incomingDamage = 0
        player4.incomingDamage = 0
        player1.thornDamage = 0
        player2.thornDamage = 0
        player3.thornDamage = 0
        player4.thornDamage = 0
        player1.isDormant = false
        player2.isDormant = false
        player3.isDormant = false
        player4.isDormant = false
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
        every { mockGameChronicle(any()) } just Runs
    }

    @Test
    fun invoke_whenThreePlayersWithDifferentPips_deliversDamageInChain() {
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
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.contains(player1) && 
            it.attackers.contains(player2)
        }) }
        
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.contains(player2) && 
            it.attackers.contains(player3)
        }) }
    }

    @Test
    fun invoke_whenThreePlayersWithThornDamage_deliversThornDamage() {
        // Arrange
        // Setup pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips
        player2.addDieToHand(DieValue(6, 5))  // 5 pips
        player3.addDieToHand(DieValue(8, 7))  // 7 pips
        
        player1.thornDamage = 1
        player2.thornDamage = 2

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        assertEquals(2, player1.incomingDamage)  // 5 - 3
        assertEquals(3, player2.incomingDamage)  // (7 - 5) + 1 (Thorn from player1)
        assertEquals(2, player3.incomingDamage)  // Thorn from player2
        
        // Verify chronicle includes thorn damage
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.damageToDefender == 2 && // 5 - 3
            it.damageToAttacker == 1    // Thorn
        }) }
        
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.damageToDefender == 2 && // 7 - 5
            it.damageToAttacker == 2    // Thorn
        }) }
    }

    @Test
    fun invoke_whenTwoPlayersWithEqualPips_noDamageDelivered() {
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
        verify(exactly = 0) { mockGameChronicle(any()) }
    }
    
    @Test
    fun invoke_whenTwoTiedPlayersAttackingOne_eachDefenderGetsHitOnce() {
        // Arrange
        // Set up pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips (lowest)
        player2.addDieToHand(DieValue(8, 7))  // 7 pips (tied highest)
        player3.addDieToHand(DieValue(8, 7))  // 7 pips (tied highest)

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        // Player1 should only get hit once with 4 damage (7-3)
        assertEquals(4, player1.incomingDamage)
        
        // Tied players don't damage each other
        assertEquals(0, player2.incomingDamage)
        assertEquals(0, player3.incomingDamage)
        
        // Chronicle should record the group damage
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.size == 1 &&
            it.defenders.contains(player1) && 
            it.attackers.size == 2 &&
            it.attackers.containsAll(listOf(player2, player3))
        }) }
    }
    
    @Test
    fun invoke_whenTwoTiedDefendersWithThorn_eachAttackerGetsThornDamage() {
        // Arrange
        // Set up pip totals with combination of dice and modifiers
        player1.addDieToHand(DieValue(6, 6))  // 6 pips
        player1.pipModifier = 4  // Total: 10 pips (highest)
        
        player2.addDieToHand(DieValue(6, 5))  // 5 pips (tied middle)
        player3.addDieToHand(DieValue(6, 5))  // 5 pips (tied middle)
        
        player4.addDieToHand(DieValue(6, 2))  // 2 pips (lowest)
        
        // Each tied defender has thorn damage
        player2.thornDamage = 1
        player3.thornDamage = 2

        val players = listOf(player1, player2, player3, player4)

        // Act
        SUT(players)

        // Assert
        // Player1 gets hit with combined thorn damage from both tied players
        assertEquals(3, player1.incomingDamage)  // Thorn: 1+2
        
        // Each tied player gets hit with the same damage from player1
        assertEquals(5, player2.incomingDamage)  // 10-5
        assertEquals(5, player3.incomingDamage)  // 10-5
        
        // Lowest player gets hit by the tied group
        assertEquals(3, player4.incomingDamage)  // 5-2
        
        // Verify chronicle for high->mid damage
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.size == 2 &&
            it.defenders.containsAll(listOf(player2, player3)) && 
            it.attackers.contains(player1) &&
            it.damageToDefender == 5 &&  // 10-5
            it.damageToAttacker == 3     // Thorn: 1+2
        }) }
        
        // Verify chronicle for mid->low damage
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.contains(player4) && 
            it.attackers.containsAll(listOf(player2, player3)) &&
            it.damageToDefender == 3     // 5-2
        }) }
    }
    
    @Test
    fun invoke_whenAllPlayersTied_noDamageDelivered() {
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
        verify(exactly = 0) { mockGameChronicle(any()) }
    }
    
    @Test
    fun invoke_whenTiedGroupsWithGap_damageCalculatedCorrectly() {
        // Arrange
        // Set up pip totals with mixed dice and modifiers
        player1.addDieToHand(DieValue(6, 6))  // 6 pips
        player1.pipModifier = 4  // Total: 10 pips (tied highest)
        
        player2.addDieToHand(DieValue(6, 4))  // 4 pips 
        player2.pipModifier = 6  // Total: 10 pips (tied highest)
        
        player3.addDieToHand(DieValue(6, 4))  // 4 pips (tied lowest)
        player4.addDieToHand(DieValue(6, 4))  // 4 pips (tied lowest)
        
        val players = listOf(player1, player2, player3, player4)

        // Act
        SUT(players)

        // Assert
        // Each player in low group gets the same damage
        assertEquals(6, player3.incomingDamage)  // 10-4
        assertEquals(6, player4.incomingDamage)  // 10-4
        
        // High group gets no damage
        assertEquals(0, player1.incomingDamage)
        assertEquals(0, player2.incomingDamage)
        
        // Verify chronicle
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.size == 2 &&
            it.defenders.containsAll(listOf(player3, player4)) && 
            it.attackers.size == 2 &&
            it.attackers.containsAll(listOf(player1, player2)) &&
            it.damageToDefender == 6  // 10-4
        }) }
    }
    
    @Test
    fun invoke_whenDormantPlayersPresent_ignoresDormantPlayers() {
        // Arrange
        // Set up pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips (lowest)
        player2.addDieToHand(DieValue(6, 5))  // 5 pips (middle, but dormant)
        player3.addDieToHand(DieValue(8, 7))  // 7 pips (highest)
        
        // Set player2 as dormant - it should be ignored
        player2.isDormant = true

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        // Player1 should get hit by player3 with 4 damage (7-3)
        assertEquals(4, player1.incomingDamage)  // 7 - 3
        
        // Dormant player2 should be excluded and receive no damage
        assertEquals(0, player2.incomingDamage)
        
        // Player3 should receive no damage as it's highest
        assertEquals(0, player3.incomingDamage)
        
        // Chronicle should only show player3 attacking player1
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.contains(player1) && 
            it.attackers.contains(player3) &&
            !it.defenders.contains(player2) &&
            !it.attackers.contains(player2)
        }) }
        
        // And no other damage moments
        verify(exactly = 1) { mockGameChronicle(any()) }
    }
    
    @Test
    fun invoke_whenAllPlayersDormant_noDamageDelivered() {
        // Arrange
        // Set up pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips
        player2.addDieToHand(DieValue(6, 5))  // 5 pips
        
        // Set all players as dormant
        player1.isDormant = true
        player2.isDormant = true
        
        val players = listOf(player1, player2)

        // Act
        SUT(players)

        // Assert
        assertEquals(0, player1.incomingDamage)
        assertEquals(0, player2.incomingDamage)
        verify(exactly = 0) { mockGameChronicle(any()) }
    }
    
    @Test
    fun invoke_whenHighestPlayerDormant_correctChainIsCalculated() {
        // Arrange
        // Set up pip totals with dice
        player1.addDieToHand(DieValue(6, 3))  // 3 pips (lowest)
        player2.addDieToHand(DieValue(6, 5))  // 5 pips (middle)
        player3.addDieToHand(DieValue(6, 7))  // 7 pips (highest, but dormant)
        
        player3.isDormant = true

        val players = listOf(player1, player2, player3)

        // Act
        SUT(players)

        // Assert
        // Player1 should get hit by player2 with 2 damage (5-3)
        assertEquals(2, player1.incomingDamage)  // 5 - 3
        
        // Player2 should get no damage (player3 is dormant)
        assertEquals(0, player2.incomingDamage)
        
        // Dormant player3 should receive no damage
        assertEquals(0, player3.incomingDamage)
        
        // Chronicle should only show player2 attacking player1
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.contains(player1) && 
            it.attackers.contains(player2) &&
            !it.defenders.contains(player3) &&
            !it.attackers.contains(player3)
        }) }
        
        // And no other damage moments
        verify(exactly = 1) { mockGameChronicle(any()) }
    }
    
    @Test
    fun invoke_whenMixedDiceAndModifiers_calculatesPipTotalCorrectly() {
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
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.contains(player1) && 
            it.attackers.contains(player2)
        }) }
        
        verify { mockGameChronicle(match { 
            it is GameChronicle.Moment.DELIVER_DAMAGE && 
            it.defenders.contains(player2) && 
            it.attackers.contains(player3)
        }) }
    }
} 
