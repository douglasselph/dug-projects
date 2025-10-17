package dugsolutions.leaf.player.domain

data class PlayerScore(val playerId: Int, val scoreDice: Int = 0, val scoreCards: Int = 0) {
    override fun toString(): String {
        return if (scoreCards > 0) {
            "$scoreDice+$scoreCards"
        } else {
            "$scoreDice"
        }
    }
}
