package dugsolutions.leaf.player.di

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CostScore
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.components.ButterflyManager
import dugsolutions.leaf.player.components.CreatureManager
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.InsectManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.random.di.DieFactory

class PlayerFactory(
    private val cardManager: CardManager,
    private val deckManager: () -> DeckManager,
    private val creatureManager: () -> CreatureManager,
    private val insectManager: () -> InsectManager,
    private val butterflyManager: () -> ButterflyManager,
    private val costScore: CostScore,
    private val decisionDirector: () -> DecisionDirector,
    private val dieFactory: DieFactory
) {

    operator fun invoke(): Player {
        return Player(
            deckManager = deckManager(),
            cardManager = cardManager,
            decisionDirector = decisionDirector(),
            creatureManager = creatureManager(),
            butterflyManager = butterflyManager(),
            insectManager = insectManager(),
            dieFactory = dieFactory,
            costScore = costScore
        ).initialize()
    }

} 
