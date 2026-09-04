package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.InvalidGameStateException
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleCleanupCoordinatorTest {

    @Test
    fun cleanupReclaimsGridDiceToDiscardAndReturnsCommittedCritters() {
        val d1 = die(6, 4)
        val d2 = die(8, 7)
        val p1 = player(1, d1)
        val p2 = player(2, d2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, d1)
        state.grid.placeDie(p2, StrikeRow.BOTTOM, d2)

        // Conserve the shared physical Bee while committing it to Battle.
        assertTrue(game.grove.critters.remove(Critter.BEE))
        p1.critters.add(Critter.BEE)
        state.grid.placeCritter(p1, StrikeRow.TOP, Critter.BEE)
        val groveBeesBeforeCleanup = game.grove.critters.count(Critter.BEE)

        val result = coordinator(game).execute(game, state)

        assertTrue(p1.dice.hand.isEmpty())
        assertTrue(p2.dice.hand.isEmpty())
        assertTrue(p1.dice.discard.any { it === d1 })
        assertTrue(p2.dice.discard.any { it === d2 })
        assertTrue(state.grid.diePlacements.isEmpty())
        assertTrue(state.grid.critterPlacements.isEmpty())
        assertEquals(groveBeesBeforeCleanup + 1, game.grove.critters.count(Critter.BEE))
        assertEquals(2, result.totalDiscardedDice)
        assertEquals(1, result.totalReturnedCritters)
    }

    @Test
    fun cleanupRefreshesOnlyReadyCreatureAndItsButterflies() {
        val d1 = die(6, 4)
        val d2 = die(6, 4)
        val p1 = player(1, d1)
        val p2 = player(2, d2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, d1)
        state.grid.placeDie(p2, StrikeRow.TOP, d2)

        val p1Card = graftRoot(game, p1)
        val p2Card = graftRoot(game, p2)
        p2.creature.faceUp(p2Card.id)

        p1.butterflies.add(Butterfly.GREEN)
        p1.butterflies.faceDown(Butterfly.GREEN)
        p2.butterflies.add(Butterfly.PURPLE)
        p2.butterflies.faceDown(Butterfly.PURPLE)

        val result = coordinator(game).execute(game, state)

        assertTrue(p1.creature.get(p1Card.id)!!.isFaceUp)
        assertTrue(p1.butterflies.isFaceUp(Butterfly.GREEN))
        assertTrue(p2.creature.get(p2Card.id)!!.isFaceUp) // remained face up; no refresh required
        assertTrue(p2.butterflies.isFaceDown(Butterfly.PURPLE))
        assertEquals(listOf(PlayerId(1)), result.refreshedPlayers)
    }

    @Test
    fun cleanupNormalizesPendingMulchAndClearsTemporaryCritterValues() {
        val d1 = die(6, 4)
        val d2 = die(6, 4)
        val p1 = player(1, d1)
        val p2 = player(2, d2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, d1)
        state.grid.placeDie(p2, StrikeRow.TOP, d2)

        p1.tokens.add(Token.PENDING_MULCH(DieSides.D8))
        p1.critterValues.boostForRound(Critter.WORM, 4)
        assertEquals(5, p1.critterValues.valueOf(Critter.WORM))

        coordinator(game).execute(game, state)

        assertEquals(0, p1.tokens.pendingMulchCount)
        assertEquals(1, p1.tokens.mulchCount)
        assertEquals(DieSides.D8, p1.tokens.mulchTokens.single().sides)
        assertEquals(1, p1.critterValues.valueOf(Critter.WORM))
    }

    @Test
    fun cleanupRejectsUnplacedHandDiceBecauseBattleHandMustBeOnGrid() {
        val placed = die(6, 4)
        val unplaced = die(8, 5)
        val p1 = player(1, placed, unplaced)
        val p2 = player(2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, placed)

        assertFailsWith<InvalidGameStateException> {
            coordinator(game).execute(game, state)
        }

        // The invariant failure makes the misplaced die obvious instead of
        // silently pretending an unlocated Battle Hand die was legal.
        assertTrue(p1.dice.hand.any { it === unplaced })
    }

    @Test
    fun cleanupMovesExactEquivalentDiceNotJustEqualValues() {
        val first = die(6, 4)
        val second = die(6, 4)
        val p1 = player(1, second, first)
        val p2 = player(2)
        val game = GameEngineTestFixture.game(players = listOf(p1, p2))
        val state = BattleState(listOf(p1, p2))
        state.grid.placeDie(p1, StrikeRow.TOP, first)
        state.grid.placeDie(p1, StrikeRow.BOTTOM, second)

        coordinator(game).execute(game, state)

        assertTrue(p1.dice.hand.isEmpty())
        assertEquals(2, p1.dice.discard.size)
        assertTrue(p1.dice.discard.any { it === first })
        assertTrue(p1.dice.discard.any { it === second })
    }

    private fun coordinator(game: dugsolutions.leaf.v35.game.Game) =
        BattleCleanupCoordinator(
            RefreshResolver(game.chronicle)
        )

    private fun graftRoot(
        game: dugsolutions.leaf.v35.game.Game,
        player: Player
    ) = run {
        val stack = game.grove.plantMarket.stacks.first {
            it.card.type == PlantType.ROOT && it.remaining > 0
        }
        val card = requireNotNull(game.grove.plantMarket.take(stack.card))
        player.creature.graft(
            card,
            player.creature.legalPlacements(card).first()
        )
    }

    private fun player(id: Int, vararg dice: Die): Player =
        Player(
            id = PlayerId(id),
            decisions = DecisionDirector.baseline(),
            dice = PlayerDice(hand = dice.toList())
        )

    private fun die(sides: Int, value: Int): Die =
        object : Die(sides) {
            init { adjustTo(value) }
            override fun roll(): Die = this
        }
}
