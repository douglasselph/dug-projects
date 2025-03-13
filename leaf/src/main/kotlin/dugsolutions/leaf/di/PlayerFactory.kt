package dugsolutions.leaf.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector

class PlayerFactory(
    private val cardManager: CardManager,
    private val retainedStack: () -> StackManager,
    private val deckManager: () -> DeckManager,
    private val chronicle: GameChronicle,
    private val costScore: CostScore,
    private val decisionDirectorFactory: DecisionDirectorFactory
) {

    operator fun invoke(dieFactory: DieFactory): Player {
        return Player(
            deckManager = deckManager(),
            retainedComponents = retainedStack(),
            cardManager = cardManager,
            decisionDirectorFactory = decisionDirectorFactory,
            dieFactory = dieFactory,
            costScore = costScore,
            chronicle = chronicle
        )
    }
} 
