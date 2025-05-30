package dugsolutions.leaf.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
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
    private val chronicle: GameChronicle
) {

    operator fun invoke(dieFactory: DieFactory): Player {
        return Player(
            deckManager = deckManager(),
            floralArray = floralArray(),
            retainedComponents = retainedStack(),
            cardManager = cardManager,
            decisionDirectorFactory = decisionDirectorFactory,
            dieFactory = dieFactory,
            costScore = costScore,
            chronicle = chronicle
        ).setDefaultName()
    }
} 
