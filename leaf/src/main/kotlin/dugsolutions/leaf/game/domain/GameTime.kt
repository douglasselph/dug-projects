package dugsolutions.leaf.game.domain

class GameTime {

    var turn: Int = 0
    var phase: GamePhase = GamePhase.CULTIVATION

    override fun toString(): String {
        return "$turn"
    }

}
