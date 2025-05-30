package dugsolutions.leaf.main.domain

data class MainDomain(
    val turn: Int = 0,
    val players: List<PlayerInfo> = emptyList(),
    val groveInfo: GroveInfo? = null,
    val simulationOutput: List<String> = emptyList(),
    val showDrawCount: Boolean = false,
    val showRunButton: Boolean = false,
    val stepModeEnabled: Boolean = false,
    val showNextButton: Boolean = false
)
