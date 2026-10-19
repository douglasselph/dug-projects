package dugsolutions.leaf.common.domain.game

class GameTime {

    var turn: Int = 0
    var phase: GamePhase = GamePhase.CULTIVATION

    override fun toString(): String {
        return "$turn"
    }

}
