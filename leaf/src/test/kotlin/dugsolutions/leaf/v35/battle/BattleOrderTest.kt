package dugsolutions.leaf.v35.battle

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BattleOrderTest {

    @Test
    fun determine_ranksEveryPlayerByHighestThenNextHighestValues() {
        val players = listOf(
            BattleTestFixture.player(
                1,
                BattleTestFixture.die(20, 8),
                BattleTestFixture.die(6, 6),
                BattleTestFixture.die(4, 4)
            ),
            BattleTestFixture.player(
                2,
                BattleTestFixture.die(12, 12),
                BattleTestFixture.die(4, 2),
                BattleTestFixture.die(4, 1)
            ),
            BattleTestFixture.player(
                3,
                BattleTestFixture.die(20, 8),
                BattleTestFixture.die(10, 7),
                BattleTestFixture.die(4, 1)
            ),
            BattleTestFixture.player(
                4,
                BattleTestFixture.die(20, 8),
                BattleTestFixture.die(10, 7),
                BattleTestFixture.die(6, 5)
            )
        )
        val randomizer = BattleTestFixture.FixedRandomizer()

        val result = BattleOrder.determine(players, randomizer)

        assertEquals(
            listOf(2, 4, 3, 1),
            result.map { it.id.value }
        )
        assertEquals(0, randomizer.calls)
    }

    @Test
    fun determine_missingDiceCompareAsZero() {
        val players = listOf(
            BattleTestFixture.player(
                1,
                BattleTestFixture.die(8, 8)
            ),
            BattleTestFixture.player(
                2,
                BattleTestFixture.die(8, 8),
                BattleTestFixture.die(4, 1)
            ),
            BattleTestFixture.player(3)
        )

        val result =
            BattleOrder.determine(
                players,
                BattleTestFixture.FixedRandomizer()
            )

        assertEquals(
            listOf(2, 1, 3),
            result.map { it.id.value }
        )
    }

    @Test
    fun determine_completeHandTieUsesD20ToRankEntireTiedGroup() {
        val players = listOf(
            playerWithValues(1, 9, 5, 2),
            playerWithValues(2, 9, 5, 2),
            playerWithValues(3, 8, 8, 8),
            playerWithValues(4, 9, 5, 2)
        )
        /*
         * First tie rolls: P1=10, P2=10, P4=5.
         * P1/P2 reroll: P1=12, P2=19.
         */
        val randomizer =
            BattleTestFixture.FixedRandomizer(
                10, 10, 5,
                12, 19
            )

        val result =
            BattleOrder.determine(
                players,
                randomizer
            )

        assertEquals(
            listOf(2, 1, 4, 3),
            result.map { it.id.value }
        )
        assertEquals(5, randomizer.calls)
    }

    @Test
    fun determine_d20TiebreakDoesNotMutateHands() {
        val players = listOf(
            playerWithValues(1, 8, 4),
            playerWithValues(2, 8, 4)
        )
        val before =
            players.map { player ->
                player.dice.hand.map { it.value }
            }

        BattleOrder.determine(
            players,
            BattleTestFixture.FixedRandomizer(20, 2)
        )

        assertEquals(
            before,
            players.map { player ->
                player.dice.hand.map { it.value }
            }
        )
    }

    @Test
    fun determine_doesNotMutateInputPlayerList() {
        val players = mutableListOf(
            playerWithValues(1, 4),
            playerWithValues(2, 12),
            playerWithValues(3, 7)
        )
        val before = players.toList()

        BattleOrder.determine(
            players,
            BattleTestFixture.FixedRandomizer()
        )

        assertEquals(before, players)
    }

    private fun playerWithValues(
        id: Int,
        vararg values: Int
    ) =
        BattleTestFixture.player(
            id,
            *values.map { value ->
                BattleTestFixture.die(20, value)
            }.toTypedArray()
        )
}
