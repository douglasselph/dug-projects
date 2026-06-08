package dugsolutions.leaf.v14.game.turn.config

import dugsolutions.leaf.v14.player.Player

class IsEliminatedNoDiceNorCards : IsEliminated{

    override fun invoke (player: Player): Boolean {
        return player.totalCardCount == 0 || player.totalDiceCount == 0
    }

}
