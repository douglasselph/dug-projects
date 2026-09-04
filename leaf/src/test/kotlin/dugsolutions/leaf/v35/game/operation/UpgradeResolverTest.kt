package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpgradeResolverTest {

    private val resolver = UpgradeResolver()

    @Test
    fun nextNormalStep_followsNormalUpgradeLadder() {
        assertEquals(DieSides.D6, resolver.nextNormalStep(DieSides.D4))
        assertEquals(DieSides.D8, resolver.nextNormalStep(DieSides.D6))
        assertEquals(DieSides.D10, resolver.nextNormalStep(DieSides.D8))
        assertEquals(DieSides.D12, resolver.nextNormalStep(DieSides.D10))
        assertEquals(DieSides.D20, resolver.nextNormalStep(DieSides.D12))
        assertNull(resolver.nextNormalStep(DieSides.D20))
    }

    @Test
    fun canUpgradeNormalStep_requiresImmediateNextSizeInGraftBed() {
        val player = player(FixedDie(6, 5))
        val game = GameEngineTestFixture.game(players = listOf(player, player(2)))

        assertTrue(resolver.canUpgradeNormalStep(game, player.dice.hand.single()))

        repeat(9) {
            assertTrue(game.grove.graftBed.take(DieSides.D8))
        }

        assertFalse(resolver.canUpgradeNormalStep(game, player.dice.hand.single()))
    }

    @Test
    fun upgradeD4_movesReplacementToDiscardAndReturnsD4ToGraftBed() {
        val old = FixedDie(4, 4)
        val player = player(old)
        val game = GameEngineTestFixture.game(players = listOf(player, player(2)))

        val result = resolver.upgradeFromHandToDiscard(game, player, old)

        assertEquals(DieSides.D4, result.from)
        assertEquals(DieSides.D6, result.to)
        assertTrue(player.dice.hand.isEmpty())
        assertEquals(1, player.dice.discardSize)
        assertEquals(6, player.dice.discard.single().sides)
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
        assertEquals(8, game.grove.graftBed.count(DieSides.D6))
    }

    @Test
    fun upgradeD6_removesOldDieFromGameRatherThanReturningItToGraftBed() {
        val old = FixedDie(6, 6)
        val player = player(old)
        val game = GameEngineTestFixture.game(players = listOf(player, player(2)))

        resolver.upgradeFromHandToDiscard(game, player, old)

        assertTrue(player.dice.hand.isEmpty())
        assertEquals(8, player.dice.discard.single().sides)
        assertEquals(9, game.grove.graftBed.count(DieSides.D6))
        assertEquals(8, game.grove.graftBed.count(DieSides.D8))
    }

    private fun player(
        die: Die? = null,
        id: Int = 1
    ): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.baseline(),
        dice = PlayerDice(hand = die?.let(::listOf) ?: emptyList())
    )

    private fun player(id: Int): Player = player(null, id)

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }
}
