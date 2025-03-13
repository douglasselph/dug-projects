package dugsolutions.leaf.main

import androidx.compose.runtime.*

class MainDomain {
    data class MainState(
        val numPlayers: Int = 2,
        val numGames: Int = 100,
        val simulationOutput: List<String> = emptyList()
    )
    
    private var _state by mutableStateOf(MainState())
    val state: MainState get() = _state
    
    fun setNumPlayers(value: Int) {
        _state = _state.copy(numPlayers = value)
    }

    fun addSimulationOutput(message: String) {
        _state = _state.copy(
            simulationOutput = _state.simulationOutput + message
        )
    }
} 