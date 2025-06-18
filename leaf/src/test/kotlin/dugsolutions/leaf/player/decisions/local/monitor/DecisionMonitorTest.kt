package dugsolutions.leaf.player.decisions.local.monitor

import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.player.Player
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DecisionMonitorTest {

    private val mockPlayer: Player = mockk(relaxed = true)
    private val monitor: DecisionMonitor = DecisionMonitor()
    private val capturedStates: MutableList<DecisionID?> = mutableListOf()

    @BeforeEach
    fun setup() {
    }

    @Test
    fun currentlyWaitingFor_whenInitialized_returnsNull() {
        // Assert
        assertNull(monitor.currentlyWaitingFor)
    }

    @Test
    fun setWaitingFor_whenCalled_updatesCurrentlyWaitingFor() {
        // Arrange
        val decisionId = DecisionID.DRAW_COUNT(mockPlayer)

        // Act
        monitor.setWaitingFor(decisionId)

        // Assert
        assertEquals(decisionId, monitor.currentlyWaitingFor)
    }

    @Test
    fun setWaitingFor_whenCalledWithNull_clearsCurrentlyWaitingFor() {
        // Arrange
        monitor.setWaitingFor(DecisionID.DRAW_COUNT(mockPlayer))

        // Act
        monitor.setWaitingFor(null)

        // Assert
        assertNull(monitor.currentlyWaitingFor)
    }

    @Test
    fun observe_whenStateChanges_notifiesObservers() {
        // Arrange
        monitor.observe { state -> capturedStates.add(state) }

        // Act
        monitor.setWaitingFor(DecisionID.DRAW_COUNT(mockPlayer))
        monitor.setWaitingFor(DecisionID.FLOWER_SELECT)
        monitor.setWaitingFor(null)

        // Assert
        assertEquals(3, capturedStates.size)
        assertEquals(DecisionID.DRAW_COUNT(mockPlayer), capturedStates[0])
        assertEquals(DecisionID.FLOWER_SELECT, capturedStates[1])
        assertNull(capturedStates[2])
    }

    @Test
    fun observe_whenMultipleObserversRegistered_notifiesAllObservers() {
        // Arrange
        val states1 = mutableListOf<DecisionID?>()
        val states2 = mutableListOf<DecisionID?>()

        monitor.observe { state -> states1.add(state) }
        monitor.observe { state -> states2.add(state) }

        // Act
        monitor.setWaitingFor(DecisionID.DRAW_COUNT(mockPlayer))
        monitor.setWaitingFor(null)

        // Assert
        assertEquals(2, states1.size)
        assertEquals(2, states2.size)
        assertEquals(DecisionID.DRAW_COUNT(mockPlayer), states1[0])
        assertEquals(DecisionID.DRAW_COUNT(mockPlayer), states2[0])
        assertNull(states1[1])
        assertNull(states2[1])
    }

    @Test
    fun observe_whenDataClassDecisionProvided_notifiesWithCorrectState() {
        // Arrange
        val card = FakeCards.fakeCanopy
        val decisionId = DecisionID.ACQUIRE_SELECT(listOf(card), emptyList())
        monitor.observe { state -> capturedStates.add(state) }

        // Act
        monitor.setWaitingFor(decisionId)

        // Assert
        assertEquals(1, capturedStates.size)
        assertEquals(decisionId, capturedStates[0])
    }

    @Test
    fun observe_whenNoneDecisionProvided_notifiesWithNoneState() {
        // Arrange
        monitor.observe { state -> capturedStates.add(state) }

        // Act
        monitor.setWaitingFor(DecisionID.NONE)

        // Assert
        assertEquals(1, capturedStates.size)
        assertEquals(DecisionID.NONE, capturedStates[0])
    }
} 
