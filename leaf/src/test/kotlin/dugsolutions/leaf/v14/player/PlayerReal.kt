package dugsolutions.leaf.v14.player

import dugsolutions.leaf.v14.cards.CardManager
import dugsolutions.leaf.v14.cards.FakeCards
import dugsolutions.leaf.v14.cards.cost.CostScore
import dugsolutions.leaf.v14.cards.di.GameCardIDsFactory
import dugsolutions.leaf.v14.cards.di.GameCardsFactory
import dugsolutions.leaf.v14.player.components.DeckManager
import dugsolutions.leaf.v14.player.components.FloralArray
import dugsolutions.leaf.v14.player.components.StackManager
import dugsolutions.leaf.v14.player.decisions.DecisionDirector
import dugsolutions.leaf.v14.player.effect.FloralBonusCount
import dugsolutions.leaf.v14.random.RandomizerTD
import dugsolutions.leaf.v14.random.di.DieFactory
import io.mockk.mockk

class PlayerReal private constructor(
    deckManager: DeckManager,
    floralArray: FloralArray,
    floralBonusCount: FloralBonusCount,
    cardManager: CardManager,
    dieFactory: DieFactory,
    costScore: CostScore,
    decisionDirector: DecisionDirector
) : Player(
    deckManager,
    floralArray,
    floralBonusCount,
    cardManager,
    dieFactory,
    costScore,
    decisionDirector,
) {

    companion object {

        fun create(): PlayerReal {
            val randomizer = RandomizerTD()
            val costScore = CostScore()
            val gameCardsFactory = GameCardsFactory(randomizer, costScore)
            val cardManager = CardManager(gameCardsFactory)
            cardManager.loadCards(FakeCards.ALL_CARDS)
            val gameCardIDsFactory = GameCardIDsFactory(cardManager, PlayerTD.randomizerTD)
            val supplyStack = StackManager(cardManager, gameCardIDsFactory)
            val handStack = StackManager(cardManager, gameCardIDsFactory)
            val discardStack = StackManager(cardManager, gameCardIDsFactory)
            val floralBonusCount = FloralBonusCount()
            val floralArray = FloralArray(cardManager, gameCardIDsFactory)
            val dieFactory = DieFactory(PlayerTD.randomizerTD)
            val deckManager = DeckManager(supplyStack, handStack, discardStack, dieFactory)
            val decisionDirector = mockk<DecisionDirector>(relaxed = true)

            return PlayerReal(
                deckManager,
                floralArray,
                floralBonusCount,
                cardManager,
                dieFactory,
                costScore,
                decisionDirector
            ).apply {
                initialize()
            }
        }
    }
}
