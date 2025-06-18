package dugsolutions.leaf.main.domain

data class MainGameDomain(
    val turn: Int = 0,
    val players: List<PlayerInfo> = emptyList(),
    val groveInfo: GroveInfo? = null,
    val stepModeEnabled: Boolean = false,
    val askTrashEnabled: Boolean = false
)
