package dugsolutions.leaf.v35.battle.domain

import dugsolutions.leaf.v35.battle.BattleTestFixture
import dugsolutions.leaf.v35.error.InvalidGameStateException
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BattleGridTest {

    @Test
    fun columnsPreserveLeftToRightBattleOrder() {
        val grid = grid(3, 1, 4, 2)

        assertEquals(
            listOf(3, 1, 4, 2),
            grid.playerIdsInGridOrder.map { it.value }
        )
        assertEquals(
            listOf(0, 1, 2, 3),
            grid.columns.map { it.index }
        )
    }

    @Test
    fun placeDieAddsLocationWithoutRemovingDieFromPlayersHand() {
        val die = BattleTestFixture.die(8, 6)
        val player = BattleTestFixture.player(1, die)
        val grid = grid(1, 2)

        val placement =
            grid.placeDie(
                player,
                StrikeRow.TOP,
                die
            )

        assertEquals(
            BattleLocation(
                PlayerId(1),
                StrikeRow.TOP
            ),
            placement.location
        )
        assertTrue(
            player.dice.hand.any { it === die }
        )
        assertSame(
            die,
            grid.square(
                PlayerId(1),
                StrikeRow.TOP
            ).dice.single()
        )
    }

    @Test
    fun changingDieValueNeverChangesItsGridLocation() {
        val die = BattleTestFixture.die(12, 4)
        val player = BattleTestFixture.player(1, die)
        val grid = grid(1, 2)
        grid.placeDie(player, StrikeRow.MIDDLE, die)

        die.adjustTo(11)

        assertEquals(
            BattleLocation(
                PlayerId(1),
                StrikeRow.MIDDLE
            ),
            grid.locationOf(die)
        )
        assertSame(
            die,
            grid.square(
                PlayerId(1),
                StrikeRow.MIDDLE
            ).dice.single()
        )
        assertEquals(11, die.value)
    }

    @Test
    fun equivalentDiceAreTrackedByExactLiveIdentity() {
        val first = BattleTestFixture.die(6, 4)
        val second = BattleTestFixture.die(6, 4)
        val player =
            BattleTestFixture.player(
                1,
                first,
                second
            )
        val grid = grid(1, 2)

        grid.placeDie(player, StrikeRow.TOP, first)
        grid.placeDie(player, StrikeRow.BOTTOM, second)

        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.TOP),
            grid.locationOf(first)
        )
        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.BOTTOM),
            grid.locationOf(second)
        )

        first.adjustTo(5)

        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.TOP),
            grid.locationOf(first)
        )
        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.BOTTOM),
            grid.locationOf(second)
        )
    }

    @Test
    fun sameLiveDieCannotBePlacedTwice() {
        val die = BattleTestFixture.die(6, 4)
        val player = BattleTestFixture.player(1, die)
        val grid = grid(1, 2)
        grid.placeDie(player, StrikeRow.TOP, die)

        assertFailsWith<InvalidGameStateException> {
            grid.placeDie(
                player,
                StrikeRow.MIDDLE,
                die
            )
        }
    }

    @Test
    fun dieMustBeExactLiveMemberOfPlayersHand() {
        val owned = BattleTestFixture.die(6, 4)
        val merelyEquivalent = BattleTestFixture.die(6, 4)
        val player = BattleTestFixture.player(1, owned)
        val grid = grid(1, 2)

        assertFailsWith<InvalidGameStateException> {
            grid.placeDie(
                player,
                StrikeRow.TOP,
                merelyEquivalent
            )
        }

        assertNull(grid.locationOf(merelyEquivalent))
        assertTrue(grid.square(PlayerId(1), StrikeRow.TOP).isEmpty)
    }

    @Test
    fun strikeSquareHoldsAtMostThreeDiceButUnlimitedCritters() {
        val d1 = BattleTestFixture.die(4, 1)
        val d2 = BattleTestFixture.die(6, 2)
        val d3 = BattleTestFixture.die(8, 3)
        val d4 = BattleTestFixture.die(10, 4)
        val player =
            BattleTestFixture.player(
                1,
                d1, d2, d3, d4
            )
        repeat(5) {
            player.critters.add(Critter.WORM)
        }
        val grid = grid(1, 2)

        grid.placeDie(player, StrikeRow.TOP, d1)
        grid.placeDie(player, StrikeRow.TOP, d2)
        grid.placeDie(player, StrikeRow.TOP, d3)
        repeat(5) {
            grid.placeCritter(
                player,
                StrikeRow.TOP,
                Critter.WORM
            )
        }

        val square = grid.square(PlayerId(1), StrikeRow.TOP)
        assertEquals(3, square.dieCount)
        assertEquals(5, square.critterCount)
        assertTrue(square.isFull)
        assertEquals(0, player.critters.count(Critter.WORM))

        assertFailsWith<InvalidGameStateException> {
            grid.placeDie(player, StrikeRow.TOP, d4)
        }
    }

    @Test
    fun initialPlacementUsesHighestToLowestRowsAndKeepsAllDiceInHand() {
        val low = BattleTestFixture.die(8, 2)
        val high = BattleTestFixture.die(4, 4)
        val middle = BattleTestFixture.die(20, 3)
        val player =
            BattleTestFixture.player(
                1,
                low,
                high,
                middle
            )
        val grid = grid(1, 2)

        grid.placeInitialHand(player)

        assertSame(
            high,
            grid.square(PlayerId(1), StrikeRow.TOP).dice.single()
        )
        assertSame(
            middle,
            grid.square(PlayerId(1), StrikeRow.MIDDLE).dice.single()
        )
        assertSame(
            low,
            grid.square(PlayerId(1), StrikeRow.BOTTOM).dice.single()
        )
        assertEquals(3, player.dice.handSize)
    }

    @Test
    fun initialPlacementAllowsCallerToChooseOrderAmongEqualValues() {
        val d6 = BattleTestFixture.die(6, 4)
        val d20 = BattleTestFixture.die(20, 4)
        val player = BattleTestFixture.player(1, d6, d20)
        val grid = grid(1, 2)

        grid.placeInitialHand(
            player = player,
            topToBottom = listOf(d20, d6)
        )

        assertSame(
            d20,
            grid.square(PlayerId(1), StrikeRow.TOP).dice.single()
        )
        assertSame(
            d6,
            grid.square(PlayerId(1), StrikeRow.MIDDLE).dice.single()
        )
    }

    @Test
    fun initialPlacementRejectsValuesThatAreNotHighestToLowest() {
        val low = BattleTestFixture.die(6, 2)
        val high = BattleTestFixture.die(6, 5)
        val player = BattleTestFixture.player(1, low, high)
        val grid = grid(1, 2)

        assertFailsWith<InvalidGameStateException> {
            grid.placeInitialHand(
                player,
                listOf(low, high)
            )
        }

        assertTrue(grid.diePlacements.isEmpty())
    }

    @Test
    fun removeDieRemovesOnlyLocationAndLeavesPlayerHandUntouched() {
        val die = BattleTestFixture.die(10, 7)
        val player = BattleTestFixture.player(1, die)
        val grid = grid(1, 2)
        grid.placeDie(player, StrikeRow.TOP, die)

        val removed = grid.removeDie(die)

        assertEquals(StrikeRow.TOP, removed?.row)
        assertNull(grid.locationOf(die))
        assertTrue(player.dice.hand.any { it === die })
    }

    @Test
    fun moveDieChangesOnlyLocation() {
        val die = BattleTestFixture.die(10, 7)
        val player = BattleTestFixture.player(1, die)
        val grid = grid(1, 2)
        grid.placeDie(player, StrikeRow.TOP, die)

        grid.moveDie(die, StrikeRow.BOTTOM)

        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.BOTTOM),
            grid.locationOf(die)
        )
        assertTrue(player.dice.hand.any { it === die })
    }

    @Test
    fun replaceDiePreservesExactStrikeSquare() {
        val old = BattleTestFixture.die(6, 4)
        val replacement = BattleTestFixture.die(8, 7)
        val player = BattleTestFixture.player(1, old, replacement)
        val grid = grid(1, 2)

        grid.placeDie(player, StrikeRow.BOTTOM, old)

        val placed = grid.replaceDie(old, replacement)

        assertNull(grid.locationOf(old))
        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.BOTTOM),
            grid.locationOf(replacement)
        )
        assertSame(replacement, placed.die)
    }

    @Test
    fun swapDieLocationsExchangesExactRowsWithoutChangingValues() {
        val first = BattleTestFixture.die(6, 2)
        val second = BattleTestFixture.die(10, 9)
        val player = BattleTestFixture.player(1, first, second)
        val grid = grid(1, 2)

        grid.placeDie(player, StrikeRow.TOP, first)
        grid.placeDie(player, StrikeRow.BOTTOM, second)

        grid.swapDieLocations(first, second)

        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.BOTTOM),
            grid.locationOf(first)
        )
        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.TOP),
            grid.locationOf(second)
        )
        assertEquals(2, first.value)
        assertEquals(9, second.value)
    }

    @Test
    fun closedRowRejectsNewDiceCrittersAndMoves() {
        val placed = BattleTestFixture.die(8, 5)
        val unplaced = BattleTestFixture.die(6, 4)
        val player = BattleTestFixture.player(1, placed, unplaced)
        player.critters.add(Critter.BEE)
        val grid = grid(1, 2)
        grid.placeDie(player, StrikeRow.TOP, placed)
        grid.closeRow(StrikeRow.MIDDLE)

        assertTrue(grid.isRowClosed(StrikeRow.MIDDLE))
        assertFailsWith<InvalidGameStateException> {
            grid.placeDie(player, StrikeRow.MIDDLE, unplaced)
        }
        assertFailsWith<InvalidGameStateException> {
            grid.placeCritter(player, StrikeRow.MIDDLE, Critter.BEE)
        }
        assertFailsWith<InvalidGameStateException> {
            grid.moveDie(placed, StrikeRow.MIDDLE)
        }

        assertEquals(1, player.critters.count(Critter.BEE))
        assertEquals(
            BattleLocation(PlayerId(1), StrikeRow.TOP),
            grid.locationOf(placed)
        )
    }

    @Test
    fun playerWithdrawalIsDistinctFromGlobalRowClosureAndRejectsOnlyThatPlayersNewParticipation() {
        val first = BattleTestFixture.die(8, 5)
        val second = BattleTestFixture.die(6, 4)
        val otherDie = BattleTestFixture.die(10, 7)
        val player = BattleTestFixture.player(1, first, second)
        val other = BattleTestFixture.player(2, otherDie)
        player.critters.add(Critter.BEE)
        other.critters.add(Critter.WORM)
        val grid = grid(1, 2)

        grid.placeDie(player, StrikeRow.TOP, first)
        grid.withdrawPlayer(PlayerId(1), StrikeRow.MIDDLE)

        assertTrue(grid.isPlayerWithdrawn(PlayerId(1), StrikeRow.MIDDLE))
        assertFalse(grid.isPlayerWithdrawn(PlayerId(2), StrikeRow.MIDDLE))
        assertFalse(grid.isRowClosed(StrikeRow.MIDDLE))

        assertFailsWith<InvalidGameStateException> {
            grid.placeDie(player, StrikeRow.MIDDLE, second)
        }
        assertFailsWith<InvalidGameStateException> {
            grid.placeCritter(player, StrikeRow.MIDDLE, Critter.BEE)
        }
        assertFailsWith<InvalidGameStateException> {
            grid.moveDie(first, StrikeRow.MIDDLE)
        }

        // The Strike remains live for every other player.
        grid.placeDie(other, StrikeRow.MIDDLE, otherDie)
        grid.placeCritter(other, StrikeRow.MIDDLE, Critter.WORM)
        assertEquals(
            BattleLocation(PlayerId(2), StrikeRow.MIDDLE),
            grid.locationOf(otherDie)
        )
        assertEquals(
            listOf(Critter.WORM),
            grid.square(PlayerId(2), StrikeRow.MIDDLE).critters
        )
    }

    @Test
    fun placementViewsAreDefensiveStructuralLists() {
        val die = BattleTestFixture.die(8, 5)
        val player = BattleTestFixture.player(1, die)
        val grid = grid(1, 2)
        val squareBefore = grid.square(PlayerId(1), StrikeRow.TOP).dice
        val placementsBefore = grid.diePlacements

        grid.placeDie(player, StrikeRow.TOP, die)

        assertTrue(squareBefore.isEmpty())
        assertTrue(placementsBefore.isEmpty())
        assertEquals(1, grid.diePlacements.size)
    }

    private fun grid(vararg playerIds: Int): BattleGrid =
        BattleGrid(
            playerIds.map(::PlayerId)
        )
}
