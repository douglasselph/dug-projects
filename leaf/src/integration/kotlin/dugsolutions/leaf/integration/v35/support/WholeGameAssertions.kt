package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.round.domain.RoundCardType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * High-level invariants for complete GameRunner integration scenarios.
 *
 * These assertions intentionally avoid pinning a seeded simulation to every
 * intermediate die/card choice. The smaller sanity suites cover exact local
 * behavior; whole-game tests instead prove that the complete production graph
 * reaches a coherent end state and Chronicle lifecycle.
 */
object WholeGameAssertions {

    fun assertCompletedGame(
        harness: IntegrationGameHarness,
        result: GameRunResult,
        expectedCultivationRounds: Int,
        expectedBattleRounds: Int
    ) {
        val expectedRounds = expectedCultivationRounds + expectedBattleRounds
        val entries = harness.chronicleEntries()
        val reveals = entries.filterIsInstance<GameEntry.RoundRevealed>()
        val completions = entries.filterIsInstance<GameEntry.RoundCompleted>()
        val finalScores = entries.filterIsInstance<GameEntry.FinalScore>()
        val finalWinners = entries.filterIsInstance<GameEntry.FinalWinners>()
        val gameCompleted = entries.filterIsInstance<GameEntry.GameCompleted>()

        assertEquals(expectedRounds, result.roundsCompleted, "GameRunResult round count")
        assertEquals(expectedRounds, harness.game.roundNumber, "Game round number")
        assertEquals(GameStatus.COMPLETE, harness.game.status)
        assertTrue(harness.game.isComplete)
        assertTrue(harness.game.roundDeck.isEmpty, "Round deck should be exhausted")

        assertEquals(expectedRounds, reveals.size, "RoundRevealed count")
        assertEquals(expectedRounds, completions.size, "RoundCompleted count")
        assertEquals(
            expectedCultivationRounds,
            reveals.count { it.cardType == RoundCardType.CULTIVATION },
            "Cultivation reveal count"
        )
        assertEquals(
            expectedBattleRounds,
            reveals.count { it.cardType == RoundCardType.BATTLE },
            "Battle reveal count"
        )

        assertEquals(
            (1..expectedRounds).toList(),
            reveals.map { it.roundNumber },
            "revealed round numbers"
        )
        assertEquals(
            (1..expectedRounds).toList(),
            completions.map { it.roundNumber },
            "completed round numbers"
        )

        reveals.forEach { reveal ->
            val completion = completions.single { it.roundNumber == reveal.roundNumber }
            assertEquals(reveal.cardName, completion.cardName, "Round ${reveal.roundNumber} card")
            assertEquals(reveal.cardType, completion.cardType, "Round ${reveal.roundNumber} type")
            ChronicleAssertions.assertBefore(reveal, completion)
        }

        val playerIds = harness.game.players.map { it.id }
        assertEquals(playerIds.size, result.finalScoring.scores.size, "final score count")
        assertEquals(
            playerIds.toSet(),
            result.finalScoring.scores.map { it.playerId }.toSet(),
            "players with final scores"
        )
        assertTrue(result.finalScoring.winnerIds.isNotEmpty(), "Game must have at least one winner")
        assertTrue(
            result.finalScoring.winnerIds.all { it in playerIds },
            "Every winner must be a player in this game"
        )

        assertEquals(playerIds.size, finalScores.size, "Chronicle FinalScore count")
        assertEquals(playerIds.toSet(), finalScores.map { it.playerId }.toSet())
        assertEquals(1, finalWinners.size, "Chronicle FinalWinners count")
        assertEquals(result.finalScoring.winnerIds, finalWinners.single().winnerIds)
        assertEquals(1, gameCompleted.size, "Chronicle GameCompleted count")
        assertEquals(expectedRounds, gameCompleted.single().roundsCompleted)

        val lastRound = completions.maxBy { it.roundNumber }
        finalScores.forEach { ChronicleAssertions.assertBefore(lastRound, it) }
        finalScores.forEach { ChronicleAssertions.assertBefore(it, finalWinners.single()) }
        ChronicleAssertions.assertBefore(finalWinners.single(), gameCompleted.single())
        assertEquals(gameCompleted.single(), entries.last(), "GameCompleted should close the Chronicle")

        ChronicleAssertions.assertNoMarkers(entries)
        ChronicleAssertions.assertSequenceContinuous(entries)
    }
}
