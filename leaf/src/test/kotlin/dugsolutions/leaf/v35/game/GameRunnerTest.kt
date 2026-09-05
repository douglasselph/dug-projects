package dugsolutions.leaf.v35.game

import dugsolutions.leaf.v35.error.GameLifecycleException
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRunnerTest {

    @Test
    fun run_transitionsReadyThroughRunningToComplete() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        val observedStatuses = mutableListOf<GameStatus>()
        val executor = RoundExecutor { receivedGame, _ ->
            observedStatuses.add(receivedGame.status)
        }

        // Act
        val result = runner(executor, executor).run(game)

        // Assert
        assertEquals(2, result.roundsCompleted)
        assertEquals(
            listOf(1, 2),
            result.finalScoring.winnerIds.map { it.value }
        )
        assertEquals(listOf(GameStatus.RUNNING, GameStatus.RUNNING), observedStatuses)
        assertEquals(GameStatus.COMPLETE, game.status)
        assertTrue(game.isComplete)
    }

    @Test
    fun run_executesEveryConfiguredCardExactlyOnceInDeckOrder() {
        // Arrange
        val game = GameEngineTestFixture.game(2, 2)
        val expected = game.roundDeck.cards.cards
        val cultivation = RecordingExecutor()
        val battle = RecordingExecutor()
        val executionOrder = mutableListOf<RoundCard>()
        cultivation.onExecute = { executionOrder.add(it) }
        battle.onExecute = { executionOrder.add(it) }

        // Act
        val result = runner(cultivation, battle).run(game)

        // Assert
        assertEquals(4, result.roundsCompleted)
        assertEquals(expected, executionOrder)
        assertEquals(2, cultivation.cards.size)
        assertEquals(2, battle.cards.size)
        assertTrue(cultivation.cards.all { it.type == RoundCardType.CULTIVATION })
        assertTrue(battle.cards.all { it.type == RoundCardType.BATTLE })
    }

    @Test
    fun run_recordsGameCompletionOnlyAfterFinalExecutorFinishes() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        val observations = mutableListOf<Boolean>()
        val executor = RoundExecutor { receivedGame, _ ->
            observations.add(receivedGame.chronicle.entries.any { it is GameEntry.GameCompleted })
            receivedGame.chronicle.record(Moment.Marker("EXECUTOR_FINISHED"))
        }

        // Act
        runner(executor, executor).run(game)

        // Assert
        assertEquals(listOf(false, false), observations)
        val entries = game.chronicle.entries
        assertEquals(2, (entries.last() as GameEntry.GameCompleted).roundsCompleted)
        val finalRoundIndex = entries.indexOfLast {
            it is GameEntry.RoundCompleted && it.roundNumber == 2
        }
        val firstFinalScoreIndex = entries.indexOfFirst { it is GameEntry.FinalScore }
        assertTrue(finalRoundIndex >= 0)
        assertTrue(firstFinalScoreIndex > finalRoundIndex)
    }

    @Test
    fun run_whenGameAlreadyCompleted_rejectsSecondInvocation() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        val runner = runner()
        runner.run(game)
        val entriesBefore = game.chronicle.entries

        // Act / Assert
        assertFailsWith<GameLifecycleException> { runner.run(game) }
        assertEquals(entriesBefore, game.chronicle.entries)
    }

    @Test
    fun run_whenExecutorFails_leavesGameRunningWithoutCompletionMarker() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        val failure = IllegalStateException("failed round")
        val cultivation = RoundExecutor { _, _ -> throw failure }

        // Act
        val thrown = assertFailsWith<IllegalStateException> {
            runner(cultivation = cultivation).run(game)
        }

        // Assert
        assertTrue(thrown === failure)
        assertEquals(GameStatus.RUNNING, game.status)
        assertFalse(game.isComplete)
        assertFalse(game.chronicle.entries.any { it is GameEntry.GameCompleted })
        assertFalse(game.chronicle.entries.any { it is GameEntry.RoundCompleted })
    }

    @Test
    fun run_whenEarlierFailureLeftGameRunning_rejectsRestart() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        val failing = runner(cultivation = RoundExecutor { _, _ -> error("fail") })
        assertFailsWith<IllegalStateException> { failing.run(game) }

        // Act / Assert
        assertFailsWith<GameLifecycleException> { runner().run(game) }
        assertEquals(GameStatus.RUNNING, game.status)
    }

    @Test
    fun separateGamesAndRunners_doNotShareExecutionState() {
        // Arrange
        val first = GameEngineTestFixture.game(1, 1, seed = 10L)
        val second = GameEngineTestFixture.game(2, 1, seed = 20L)
        val firstExecutor = RecordingExecutor()
        val secondExecutor = RecordingExecutor()

        // Act
        val firstResult = runner(firstExecutor, firstExecutor).run(first)
        val secondResult = runner(secondExecutor, secondExecutor).run(second)

        // Assert
        assertEquals(2, firstResult.roundsCompleted)
        assertEquals(3, secondResult.roundsCompleted)
        assertEquals(2, firstExecutor.cards.size)
        assertEquals(3, secondExecutor.cards.size)
        assertEquals(8, first.chronicle.entries.size)
        assertEquals(10, second.chronicle.entries.size)
        assertTrue(first.chronicle !== second.chronicle)
    }

    private fun runner(
        cultivation: RoundExecutor = RecordingExecutor(),
        battle: RoundExecutor = RecordingExecutor()
    ): GameRunner =
        GameRunner(RoundCoordinator(cultivation, battle))


    private class RecordingExecutor : RoundExecutor {
        val cards = mutableListOf<RoundCard>()
        var onExecute: (RoundCard) -> Unit = {}

        override fun execute(game: Game, roundCard: RoundCard) {
            cards.add(roundCard)
            onExecute(roundCard)
        }
    }
}
