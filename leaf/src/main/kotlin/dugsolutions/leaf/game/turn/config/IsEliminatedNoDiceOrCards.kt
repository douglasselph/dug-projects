package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.player.Player

class IsEliminatedNoDiceOrCards : IsEliminated{

    override fun invoke (player: Player): Boolean {
        return player.totalCardCount == 0 && player.totalDiceCount == 0
    }

}
