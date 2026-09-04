package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.InvalidGameStateException
import dugsolutions.leaf.v35.player.PlayerId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class BattleStateTest {

    @Test
    fun createRanksPlayersAndUsesThatOrderForGridColumns() {
        val p1 =
            BattleTestFixture.player(
                1,
                BattleTestFixture.die(8, 5)
            )
        val p2 =
            BattleTestFixture.player(
                2,
                BattleTestFixture.die(12, 10)
            )
        val p3 =
            BattleTestFixture.player(
                3,
                BattleTestFixture.die(10, 7)
            )

        val state =
            BattleState.create(
                players = listOf(p1, p2, p3),
                randomizer = BattleTestFixture.FixedRandomizer()
            )

        assertEquals(
            listOf(2, 3, 1),
            state.playerIdsInBattleOrder.map { it.value }
        )
        assertEquals(
            state.playerIdsInBattleOrder,
            state.grid.playerIdsInGridOrder
        )
    }

    @Test
    fun placeInitialHandsPlacesEachPlayersCurrentHandInTheirOwnColumn() {
        val p1Top = BattleTestFixture.die(12, 9)
        val p1Mid = BattleTestFixture.die(6, 3)
        val p2Top = BattleTestFixture.die(8, 8)
        val p2Mid = BattleTestFixture.die(20, 7)
        val p2Bottom = BattleTestFixture.die(4, 1)
        val p1 = BattleTestFixture.player(1, p1Mid, p1Top)
        val p2 = BattleTestFixture.player(2, p2Bottom, p2Mid, p2Top)
        val state = BattleState(listOf(p1, p2))

        val placements = state.placeInitialHands()

        assertEquals(5, placements.size)
        assertSame(
            p1Top,
            state.grid.square(PlayerId(1), StrikeRow.TOP).dice.single()
        )
        assertSame(
            p1Mid,
            state.grid.square(PlayerId(1), StrikeRow.MIDDLE).dice.single()
        )
        assertSame(
            p2Top,
            state.grid.square(PlayerId(2), StrikeRow.TOP).dice.single()
        )
        assertSame(
            p2Mid,
            state.grid.square(PlayerId(2), StrikeRow.MIDDLE).dice.single()
        )
        assertSame(
            p2Bottom,
            state.grid.square(PlayerId(2), StrikeRow.BOTTOM).dice.single()
        )
    }

    @Test
    fun playerLookupReturnsLiveParticipatingPlayer() {
        val player = BattleTestFixture.player(1)
        val state = BattleState(listOf(player, BattleTestFixture.player(2)))

        assertSame(
            player,
            state.player(PlayerId(1))
        )
    }

    @Test
    fun playerLookupRejectsNonParticipantWithTypedStateException() {
        val state =
            BattleState(
                listOf(
                    BattleTestFixture.player(1),
                    BattleTestFixture.player(2)
                )
            )

        assertFailsWith<InvalidGameStateException> {
            state.player(PlayerId(99))
        }
    }
}
