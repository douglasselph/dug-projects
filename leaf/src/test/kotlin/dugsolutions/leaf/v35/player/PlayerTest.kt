package dugsolutions.leaf.v35.player

import dugsolutions.leaf.v35.player.creature.Creature
import dugsolutions.leaf.v35.player.critter.CritterValueState
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.player.wisp.WispHand
import dugsolutions.leaf.v35.tokens.Butterflies
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Critters
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.tokens.Tokens
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlayerTest {

    @Test
    fun newPlayer_hasExplicitIdDecisionsAndEmptyOwnedState() {
        val decisions = DecisionDirector.baseline()
        val player = Player(
            id = PlayerId(3),
            decisions = decisions
        )

        assertEquals(PlayerId(3), player.id)
        assertTrue(player.decisions === decisions)
        assertTrue(player.creature.isEmpty)
        assertTrue(player.dice.supply.isEmpty())
        assertTrue(player.dice.hand.isEmpty())
        assertTrue(player.dice.discard.isEmpty())
        assertTrue(player.critters.isEmpty)
        assertEquals(1, player.critterValues.valueOf(Critter.WORM))
        assertEquals(2, player.critterValues.valueOf(Critter.BEE))
        assertEquals(0, player.tokens.waterCount)
        assertEquals(0, player.tokens.mulchCount)
        assertEquals(0, player.tokens.pendingMulchCount)
        assertTrue(player.butterflies.isEmpty)
        assertTrue(player.wisps.isEmpty)
        assertEquals(0, player.vp)
    }

    @Test
    fun constructor_usesSuppliedOwnedStateObjects() {
        val decisions = DecisionDirector.baseline()
        val creature = Creature()
        val dice = PlayerDice()
        val critters = Critters()
        val critterValues = CritterValueState()
        val tokens = Tokens()
        val butterflies = Butterflies()
        val wisps = WispHand()

        val player = Player(
            id = PlayerId(1),
            decisions = decisions,
            creature = creature,
            dice = dice,
            critters = critters,
            critterValues = critterValues,
            tokens = tokens,
            butterflies = butterflies,
            wisps = wisps
        )

        assertTrue(player.decisions === decisions)
        assertTrue(player.creature === creature)
        assertTrue(player.dice === dice)
        assertTrue(player.critters === critters)
        assertTrue(player.critterValues === critterValues)
        assertTrue(player.tokens === tokens)
        assertTrue(player.butterflies === butterflies)
        assertTrue(player.wisps === wisps)
    }

    @Test
    fun differentPlayers_canHaveDifferentDecisionDirectors() {
        val firstDecisions = DecisionDirector.baseline()
        val secondDecisions = DecisionDirector.baseline()

        val first = Player(
            id = PlayerId(1),
            decisions = firstDecisions
        )
        val second = Player(
            id = PlayerId(2),
            decisions = secondDecisions
        )

        assertTrue(first.decisions === firstDecisions)
        assertTrue(second.decisions === secondDecisions)
        assertTrue(first.decisions !== second.decisions)
    }

    @Test
    fun defaultOwnedState_isNotSharedBetweenPlayers() {
        val first = Player(
            id = PlayerId(1),
            decisions = DecisionDirector.baseline()
        )
        val second = Player(
            id = PlayerId(2),
            decisions = DecisionDirector.baseline()
        )

        first.critters.add(Critter.BEE)
        first.critterValues.boostForRound(Critter.WORM, 2)
        first.tokens.add(Token.WATER)
        first.butterflies.add(Butterfly.GREEN)

        assertEquals(listOf(Critter.BEE), first.critters.all)
        assertTrue(second.critters.isEmpty)
        assertEquals(3, first.critterValues.valueOf(Critter.WORM))
        assertEquals(1, second.critterValues.valueOf(Critter.WORM))

        assertEquals(1, first.tokens.waterCount)
        assertEquals(0, second.tokens.waterCount)

        assertEquals(listOf(Butterfly.GREEN), first.butterflies.all)
        assertTrue(second.butterflies.isEmpty)
    }

    @Test
    fun addVp_increasesVp() {
        val player = newPlayer()

        player.addVp(1)
        player.addVp(3)

        assertEquals(4, player.vp)
    }

    @Test
    fun addVp_withZero_throws() {
        val player = newPlayer()

        assertFailsWith<IllegalArgumentException> {
            player.addVp(0)
        }

        assertEquals(0, player.vp)
    }

    @Test
    fun addVp_withNegativeAmount_throws() {
        val player = newPlayer()

        assertFailsWith<IllegalArgumentException> {
            player.addVp(-1)
        }

        assertEquals(0, player.vp)
    }

    @Test
    fun resetVp_setsVpBackToZero() {
        val player = newPlayer()
        player.addVp(5)

        player.resetVp()

        assertEquals(0, player.vp)
    }

    @Test
    fun playerId_usesValueEquality() {
        assertEquals(
            PlayerId(7),
            PlayerId(7)
        )
    }

    private fun newPlayer(
        id: Int = 1
    ): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline()
        )
}
