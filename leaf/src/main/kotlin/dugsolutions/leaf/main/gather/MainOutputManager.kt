package dugsolutions.leaf.main.gather

import dugsolutions.leaf.main.domain.MainOutputDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainOutputManager {

    private val _state = MutableStateFlow(MainOutputDomain())

    val state: StateFlow<MainOutputDomain> = _state.asStateFlow()

    fun addSimulationOutput(message: String) {
        _state.update { currentState ->
            currentState.copy(
                simulationOutput = currentState.simulationOutput + message
            )
        }
    }
}
