package dugsolutions.leaf.main.domain

data class MainDomain(
    val players: List<PlayerInfo> = emptyList(),
    val groveInfo: GroveInfo? = null,
    val showDrawCount: Boolean = false,
    val simulationOutput: List<String> = emptyList()
)
