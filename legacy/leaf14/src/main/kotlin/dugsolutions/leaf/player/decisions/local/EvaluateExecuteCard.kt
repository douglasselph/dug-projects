package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.common.domain.Action
import dugsolutions.leaf.common.domain.Token
import dugsolutions.leaf.random.die.Die

class EvaluateExecuteCard {

    operator fun invoke(
        possibleCards: List<GameCard>,
        insectsThatCanBeUsed: List<Token>,
        diceToApplyTo: List<Die>
    ): Action {
        return Action.None
    }
}
