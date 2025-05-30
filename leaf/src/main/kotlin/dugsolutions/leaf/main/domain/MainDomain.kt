package dugsolutions.leaf.main.domain

data class MainDomain(
    val turn: Int = 0,
    val players: List<PlayerInfo> = emptyList(),
    val groveInfo: GroveInfo? = null,
    val showDrawCount: Boolean = false,
    val simulationOutput: List<String> = emptyList()
)
