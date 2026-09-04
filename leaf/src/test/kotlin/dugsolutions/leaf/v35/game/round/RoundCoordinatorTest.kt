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
        val messages = markerMessages(game)
        assertTrue(messages[0].startsWith("ROUND_REVEALED number=1"))
        assertEquals("EXECUTOR", messages[1])
        assertTrue(messages[2].startsWith("ROUND_COMPLETED number=1"))
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
        val messages = markerMessages(game)
        assertEquals(1, messages.size)
        assertTrue(messages.single().startsWith("ROUND_REVEALED"))
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

    private fun markerMessages(game: Game): List<String> =
        game.chronicle.entries
            .filterIsInstance<GameEntry.Marker>()
            .map { it.message }

    private class RecordingExecutor : RoundExecutor {
        val cards = mutableListOf<RoundCard>()

        override fun execute(game: Game, roundCard: RoundCard) {
            cards.add(roundCard)
        }
    }
}
