package dugsolutions.leaf.player.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.BuddingStack
import dugsolutions.leaf.player.components.DrawNewHand
import dugsolutions.leaf.player.effect.FloralBonusCount
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.random.di.DieFactory

class PlayerFactory(
    private val cardManager: CardManager,
    private val deckManager: () -> DeckManager,
    private val buddingStack: () -> BuddingStack,
    private val floralBonusCount: FloralBonusCount,
    private val costScore: CostScore,
    private val decisionDirector: () -> DecisionDirector,
    private val dieFactory: DieFactory,
    private val drawNewHand: DrawNewHand
) {

    operator fun invoke(): Player {
        return Player(
            deckManager = deckManager(),
            buddingStack = buddingStack(),
            floralBonusCount = floralBonusCount,
            cardManager = cardManager,
            decisionDirector = decisionDirector(),
            dieFactory = dieFactory,
            costScore = costScore,
            drawNewHand = drawNewHand
        ).initialize()
    }

} 
