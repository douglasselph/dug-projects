package dugsolutions.leaf.di.factory

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.StackManager

class PlayerFactory(
    private val cardManager: CardManager,
    private val retainedStack: () -> StackManager,
    private val deckManager: () -> DeckManager,
    private val floralArray: () -> FloralArray,
    private val costScore: CostScore,
    private val decisionDirectorFactory: DecisionDirectorFactory,
    private val dieFactory: DieFactory
) {

    operator fun invoke(): Player {
        return Player(
            deckManager = deckManager(),
            floralArray = floralArray(),
            retainedComponents = retainedStack(),
            cardManager = cardManager,
            decisionDirectorFactory = decisionDirectorFactory,
            dieFactory = dieFactory,
            costScore = costScore
        ).setDefaultName()
    }

} 
