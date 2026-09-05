package dugsolutions.leaf.v35.game.round

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoundCoordinatorTest {


    @Test
    fun revealNext_recordsRevealWithoutInvokingExecutorOrCompletion() {
        // Arrange
        val cultivation = RecordingExecutor()
        val battle = RecordingExecutor()
        val game = GameEngineTestFixture.game(1, 1)
        val coordinator = coordinator(cultivation, battle)

        // Act
        val reveal = coordinator.revealNext(game)

        // Assert
        assertEquals(1, reveal!!.roundNumber)
        assertEquals(RoundCardType.CULTIVATION, reveal.card.type)
        assertTrue(cultivation.cards.isEmpty())
        assertTrue(battle.cards.isEmpty())
        assertEquals(1, game.roundNumber)
        assertTrue(game.currentRound === reveal.card)
        assertEquals(1, game.chronicle.entries.size)
        assertTrue(game.chronicle.entries.single() is GameEntry.RoundRevealed)
    }

    @Test
    fun executeRevealed_executesSameCardAndRecordsCompletion() {
        // Arrange
        val cultivation = RecordingExecutor()
        val battle = RecordingExecutor()
        val game = GameEngineTestFixture.game(1, 1)
        val coordinator = coordinator(cultivation, battle)
        val reveal = coordinator.revealNext(game)!!

        // Act
        val execution = coordinator.executeRevealed(game, reveal)

        // Assert
        assertEquals(reveal.roundNumber, execution.roundNumber)
        assertTrue(reveal.card === execution.card)
        assertEquals(listOf(reveal.card), cultivation.cards)
        assertTrue(battle.cards.isEmpty())
        assertTrue(game.chronicle.entries.first() is GameEntry.RoundRevealed)
        assertTrue(game.chronicle.entries.last() is GameEntry.RoundCompleted)
    }

    @Test
    fun executeRevealed_rejectsStaleRevealAfterGameAdvances() {
        // Arrange
        val game = GameEngineTestFixture.game(2, 0)
        val coordinator = coordinator()
        val stale = coordinator.revealNext(game)!!
        game.roundDeck.next()

        // Act / Assert
        val error = assertFailsWith<IllegalStateException> {
            coordinator.executeRevealed(game, stale)
        }
        assertTrue(error.message.orEmpty().contains("stale Round reveal"))
    }

    @Test
    fun executeNext_whenNoCardsRemain_returnsNull() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        game.roundDeck.next()
        game.roundDeck.next()
        val coordinator = coordinator()

        // Act
        val result = coordinator.executeNext(game)

        // Assert
        assertNull(result)
        assertTrue(game.chronicle.entries.isEmpty())
    }

    @Test
    fun executeNext_cultivationCardInvokesOnlyCultivationExecutor() {
        // Arrange
        val cultivation = RecordingExecutor()
        val battle = RecordingExecutor()
        val game = GameEngineTestFixture.game(1, 1)

        // Act
        val result = coordinator(cultivation, battle).executeNext(game)

        // Assert
        assertEquals(RoundCardType.CULTIVATION, result!!.card.type)
        assertEquals(listOf(result.card), cultivation.cards)
        assertTrue(battle.cards.isEmpty())
    }

    @Test
    fun executeNext_battleCardInvokesOnlyBattleExecutor() {
        // Arrange
        val cultivation = RecordingExecutor()
        val battle = RecordingExecutor()
        val game = GameEngineTestFixture.game(1, 1)
        coordinator(cultivation, battle).executeNext(game)
        cultivation.cards.clear()

        // Act
        val result = coordinator(cultivation, battle).executeNext(game)

        // Assert
        assertEquals(RoundCardType.BATTLE, result!!.card.type)
        assertTrue(cultivation.cards.isEmpty())
        assertEquals(listOf(result.card), battle.cards)
    }

    @Test
    fun executeNext_executorObservesExactRevealedCardAndAdvancedRoundNumber() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        var observedCard: RoundCard? = null
        var observedRoundNumber = 0
        val cultivation = RoundExecutor { receivedGame, card ->
            observedCard = receivedGame.currentRound
            observedRoundNumber = receivedGame.roundNumber
            assertTrue(card === receivedGame.currentRound)
        }

        // Act
        val result = coordinator(cultivation = cultivation).executeNext(game)!!

        // Assert
        assertTrue(result.card === observedCard)
        assertEquals(1, observedRoundNumber)
        assertEquals(1, result.roundNumber)
    }

    @Test
    fun executeNext_recordsRevealBeforeExecutorAndCompletionAfterIt() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        val cultivation = RoundExecutor { receivedGame, _ ->
            receivedGame.chronicle.record(Moment.Marker("EXECUTOR"))
        }

        // Act
        coordinator(cultivation = cultivation).executeNext(game)

        // Assert
        val entries = game.chronicle.entries
        assertTrue(entries[0] is GameEntry.RoundRevealed)
        assertEquals(1, (entries[0] as GameEntry.RoundRevealed).roundNumber)
        assertEquals("EXECUTOR", (entries[1] as GameEntry.Marker).message)
        assertTrue(entries[2] is GameEntry.RoundCompleted)
        assertEquals(1, (entries[2] as GameEntry.RoundCompleted).roundNumber)
    }

    @Test
    fun executeNext_whenExecutorThrows_recordsRevealButNotCompletion() {
        // Arrange
        val game = GameEngineTestFixture.game(1, 1)
        val failure = IllegalArgumentException("round failed")
        val cultivation = RoundExecutor { _, _ -> throw failure }

        // Act
        val thrown = assertFailsWith<IllegalArgumentException> {
            coordinator(cultivation = cultivation).executeNext(game)
        }

        // Assert
        assertTrue(thrown === failure)
        val entries = game.chronicle.entries
        assertEquals(1, entries.size)
        assertTrue(entries.single() is GameEntry.RoundRevealed)
    }

    @Test
    fun executeNext_dispatchesEveryCardIndependentlyByItsType() {
        // Arrange
        val cultivation = RecordingExecutor()
        val battle = RecordingExecutor()
        val game = GameEngineTestFixture.game(2, 2)
        val coordinator = coordinator(cultivation, battle)

        // Act
        val types = buildList {
            while (!game.roundDeck.isEmpty) {
                add(coordinator.executeNext(game)!!.card.type)
            }
        }

        // Assert
        assertEquals(
            listOf(
                RoundCardType.CULTIVATION,
                RoundCardType.CULTIVATION,
                RoundCardType.BATTLE,
                RoundCardType.BATTLE
            ),
            types
        )
        assertEquals(2, cultivation.cards.size)
        assertEquals(2, battle.cards.size)
    }

    private fun coordinator(
        cultivation: RoundExecutor = RecordingExecutor(),
        battle: RoundExecutor = RecordingExecutor()
    ): RoundCoordinator = RoundCoordinator(cultivation, battle)


    private class RecordingExecutor : RoundExecutor {
        val cards = mutableListOf<RoundCard>()

        override fun execute(game: Game, roundCard: RoundCard) {
            cards.add(roundCard)
        }
    }
}
