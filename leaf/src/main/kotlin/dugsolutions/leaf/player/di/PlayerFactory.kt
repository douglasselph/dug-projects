package dugsolutions.leaf.player.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.FloralBonusCount
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.random.di.DieFactory

class PlayerFactory(
    private val cardManager: CardManager,
    private val deckManager: () -> DeckManager,
    private val floralArray: () -> FloralArray,
    private val floralBonusCount: FloralBonusCount,
    private val costScore: CostScore,
    private val decisionDirector: () -> DecisionDirector,
    private val dieFactory: DieFactory
) {

    operator fun invoke(): Player {
        return Player(
            deckManager = deckManager(),
            floralArray = floralArray(),
            floralBonusCount = floralBonusCount,
            cardManager = cardManager,
            decisionDirector = decisionDirector(),
            dieFactory = dieFactory,
            costScore = costScore
        ).initialize()
    }

} 
