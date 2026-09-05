package dugsolutions.leaf.integration.v35.sanity.battle

import dugsolutions.leaf.integration.v35.support.ChronicleAssertions
import dugsolutions.leaf.integration.v35.support.ChronicleQueries
import dugsolutions.leaf.integration.v35.support.random.ScriptedRandomizer
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompleteBattleRoundSanityTest {

    @Test
    fun `complete deterministic battle round runs draw place actions strikes doom cleanup in order`() {
        val randomizer = ScriptedRandomizer().rolls(
            // Opening Draw: P1 then P2, three D4s each.
            4, 3, 3,
            3, 3, 3,
            // First Main Draw: P1 then P2.
            6, 5,
            // Final Main Draw: P1 then P2.
            6, 5
        )

        battleHarness(randomizer = randomizer).use { harness ->
            val execution = checkNotNull(harness.runNextRound())
            val snapshot = harness.snapshot()
            val entries = harness.chronicleEntries()

            assertEquals(1, execution.roundNumber)
            assertEquals("Battle_Bloom_Burrow", execution.card.name)
            assertEquals(2, snapshot.player(1).vp)
            assertEquals(0, snapshot.player(2).vp)
            assertTrue(snapshot.player(1).hand.isEmpty())
            assertTrue(snapshot.player(2).hand.isEmpty())
            assertEquals(listOf(DieSides.D6), snapshot.player(1).supply.map { it.dieSides })
            assertEquals(listOf(DieSides.D6), snapshot.player(2).supply.map { it.dieSides })
            assertEquals(listOf(4, 6, 6), snapshot.player(1).discard.map { it.value })
            assertEquals(listOf(5, 5), snapshot.player(2).discard.map { it.value })

            assertEquals(listOf(PlayerId(1), PlayerId(2)), ChronicleQueries.battleOrders(entries).single().order)
            assertEquals(4, entries.filterIsInstance<GameEntry.MainAction>()
                .count { it.phase == ChroniclePhase.BATTLE })
            assertEquals(
                listOf(StrikeRow.TOP, StrikeRow.MIDDLE, StrikeRow.BOTTOM),
                ChronicleQueries.strikes(entries).map { it.row }
            )
            assertEquals(5, ChronicleQueries.doom(entries).single().count)
            assertEquals(listOf(3), ChronicleQueries.doom(entries).single().valuesTrashed)
            assertEquals(2, entries.filterIsInstance<GameEntry.Cleanup>()
                .count { it.phase == ChroniclePhase.BATTLE })

            val revealIndex = entries.indexOfFirst { it is GameEntry.RoundRevealed }
            val drawIndex = entries.indexOfFirst {
                it is GameEntry.OpeningDrawCompleted && it.phase == ChroniclePhase.BATTLE
            }
            val placeIndex = entries.indexOfFirst { it is GameEntry.BattleOrder }
            val actionIndex = entries.indexOfFirst {
                it is GameEntry.MainAction && it.phase == ChroniclePhase.BATTLE
            }
            val strikeIndex = entries.indexOfFirst { it is GameEntry.StrikeResolved }
            val doomIndex = entries.indexOfFirst { it is GameEntry.Doom }
            val cleanupIndex = entries.indexOfFirst {
                it is GameEntry.Cleanup && it.phase == ChroniclePhase.BATTLE
            }
            val completeIndex = entries.indexOfFirst { it is GameEntry.RoundCompleted }
            assertTrue(revealIndex < drawIndex)
            assertTrue(drawIndex < placeIndex)
            assertTrue(placeIndex < actionIndex)
            assertTrue(actionIndex < strikeIndex)
            assertTrue(strikeIndex < doomIndex)
            assertTrue(doomIndex < cleanupIndex)
            assertTrue(cleanupIndex < completeIndex)

            ChronicleAssertions.assertRoundLifecycle(entries, 1, "Battle_Bloom_Burrow", RoundCardType.BATTLE)
            ChronicleAssertions.assertNoMarkers(entries)
            ChronicleAssertions.assertSequenceContinuous(entries)
            randomizer.assertExhausted()
        }
    }
}
