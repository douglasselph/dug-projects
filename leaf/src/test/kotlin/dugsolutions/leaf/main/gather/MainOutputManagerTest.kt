package dugsolutions.leaf.main.gather

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainOutputManagerTest {

    private val SUT = MainOutputManager()

    @BeforeEach
    fun setup() {
    }

    @Test
    fun addSimulationOutput_whenCalled_appendsToOutput() = runBlocking {
        // Arrange
        val initialOutput = "Initial output"
        val newOutput = "New output"

        // Act
        SUT.addSimulationOutput(initialOutput)
        SUT.addSimulationOutput(newOutput)

        // Assert
        val state = SUT.state.first()
        assertEquals(listOf(initialOutput, newOutput), state.simulationOutput)
    }

} 
