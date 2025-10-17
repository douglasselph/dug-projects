package dugsolutions.leaf.game.turn.config

import dugsolutions.leaf.player.Player

class IsEliminatedNoDiceNorCards : IsEliminated{

    override fun invoke (player: Player): Boolean {
        return player.totalCardCount == 0 || player.totalDiceCount == 0
    }

}
