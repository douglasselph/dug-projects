package dugsolutions.leaf.main.local

import dugsolutions.leaf.main.domain.ActionButton
import dugsolutions.leaf.player.decisions.local.monitor.DecisionID
import dugsolutions.leaf.player.decisions.local.monitor.DecisionMonitor
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainActionHandlerTest {

    private val mockDecisionMonitor: DecisionMonitor = mockk(relaxed = true)
    private val SUT: MainActionHandler = MainActionHandler(mockDecisionMonitor)

    @BeforeEach
    fun setup() {
    }

    @Test
    fun setActionActive_whenRun_setsWaitingForStartGame() {
        // Arrange
        // Act
        SUT.setActionActive(ActionButton.RUN)
        // Assert
        verify { mockDecisionMonitor.setWaitingFor(DecisionID.START_GAME) }
    }

    @Test
    fun setActionActive_whenNext_setsWaitingForNextStep() {
        // Arrange
        // Act
        SUT.setActionActive(ActionButton.NEXT)
        // Assert
        verify { mockDecisionMonitor.setWaitingFor(DecisionID.NEXT_STEP) }
    }

    @Test
    fun setActionActive_whenOtherAction_doesNothing() {
        // Arrange
        // Act
        SUT.setActionActive(ActionButton.DONE)
        SUT.setActionActive(ActionButton.NONE)
        SUT.setActionActive(null)
        // Assert
        verify(exactly = 0) { mockDecisionMonitor.setWaitingFor(DecisionID.START_GAME) }
        verify(exactly = 0) { mockDecisionMonitor.setWaitingFor(DecisionID.NEXT_STEP) }
    }

    @Test
    fun clearAction_whenCalled_setsWaitingForNone() {
        // Arrange
        // Act
        SUT.clearAction()
        // Assert
        verify { mockDecisionMonitor.setWaitingFor(DecisionID.NONE) }
    }
} 
