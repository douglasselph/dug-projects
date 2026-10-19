package dugsolutions.leaf.v14.player.di

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.cost.CostScore
import dugsolutions.leaf.v14.player.Player
import dugsolutions.leaf.v14.player.components.DeckManager
import dugsolutions.leaf.v14.player.components.FloralArray
import dugsolutions.leaf.v14.player.effect.FloralBonusCount
import dugsolutions.leaf.v14.player.decisions.DecisionDirector
import dugsolutions.leaf.v14.random.di.DieFactory

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
