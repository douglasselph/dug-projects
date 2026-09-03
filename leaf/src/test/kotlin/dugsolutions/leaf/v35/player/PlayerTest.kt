package dugsolutions.leaf.v35.player

import dugsolutions.leaf.v35.player.creature.Creature
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
    fun newPlayer_hasExplicitIdAndEmptyOwnedState() {
        val player = Player(
            id = PlayerId(3)
        )

        assertEquals(PlayerId(3), player.id)
        assertTrue(player.creature.isEmpty)
        assertTrue(player.dice.supply.isEmpty())
        assertTrue(player.dice.hand.isEmpty())
        assertTrue(player.dice.discard.isEmpty())
        assertTrue(player.critters.isEmpty)
        assertEquals(0, player.tokens.waterCount)
        assertEquals(0, player.tokens.mulchCount)
        assertEquals(0, player.tokens.pendingMulchCount)
        assertTrue(player.butterflies.isEmpty)
        assertTrue(player.wisps.isEmpty)
        assertEquals(0, player.vp)
    }

    @Test
    fun constructor_usesSuppliedOwnedStateObjects() {
        val creature = Creature()
        val dice = PlayerDice()
        val critters = Critters()
        val tokens = Tokens()
        val butterflies = Butterflies()
        val wisps = WispHand()

        val player = Player(
            id = PlayerId(1),
            creature = creature,
            dice = dice,
            critters = critters,
            tokens = tokens,
            butterflies = butterflies,
            wisps = wisps
        )

        assertTrue(player.creature === creature)
        assertTrue(player.dice === dice)
        assertTrue(player.critters === critters)
        assertTrue(player.tokens === tokens)
        assertTrue(player.butterflies === butterflies)
        assertTrue(player.wisps === wisps)
    }

    @Test
    fun defaultOwnedState_isNotSharedBetweenPlayers() {
        val first = Player(
            id = PlayerId(1)
        )
        val second = Player(
            id = PlayerId(2)
        )

        first.critters.add(Critter.BEE)
        first.tokens.add(Token.WATER)
        first.butterflies.add(Butterfly.GREEN)

        assertEquals(listOf(Critter.BEE), first.critters.all)
        assertTrue(second.critters.isEmpty)

        assertEquals(1, first.tokens.waterCount)
        assertEquals(0, second.tokens.waterCount)

        assertEquals(listOf(Butterfly.GREEN), first.butterflies.all)
        assertTrue(second.butterflies.isEmpty)
    }

    @Test
    fun addVp_increasesVp() {
        val player = Player(
            id = PlayerId(1)
        )

        player.addVp(1)
        player.addVp(3)

        assertEquals(4, player.vp)
    }

    @Test
    fun addVp_withZero_throws() {
        val player = Player(
            id = PlayerId(1)
        )

        assertFailsWith<IllegalArgumentException> {
            player.addVp(0)
        }

        assertEquals(0, player.vp)
    }

    @Test
    fun addVp_withNegativeAmount_throws() {
        val player = Player(
            id = PlayerId(1)
        )

        assertFailsWith<IllegalArgumentException> {
            player.addVp(-1)
        }

        assertEquals(0, player.vp)
    }

    @Test
    fun resetVp_setsVpBackToZero() {
        val player = Player(
            id = PlayerId(1)
        )
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
}
