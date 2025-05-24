package dugsolutions.leaf.game.turn.handle

import dugsolutions.leaf.player.Player

class HandlePassOrPlay {

    /**
     * @return true if a player still wants to play a card from their hand.
     */
    operator fun invoke(player: Player): Boolean {
        // Determine if a player should pass or not:
        //  If they have more cards in their hand, they will pass
        // A more advanced decision criteria might be if certain cards should not be played during
        //  this turn, they will pass
        // TODO: This class is not really doing anything now. All the cards are immediately
        //  played by each player each turn.
        if (!player.canPlayCard) {
            return false
        }
        if (player.cardsToPlay.isEmpty()) {
            player.hasPassed = true
            return false
        }
        return true
    }
} 
