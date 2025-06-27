package dugsolutions.leaf.player

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.di.GameCardIDsFactory
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.player.PlayerTD.Companion.randomizerTD
import dugsolutions.leaf.player.components.DeckManager
import dugsolutions.leaf.player.components.DrawNewHand
import dugsolutions.leaf.player.components.FloralArray
import dugsolutions.leaf.player.components.StackManager
import dugsolutions.leaf.player.decisions.DecisionDirector
import dugsolutions.leaf.player.effect.FloralBonusCount
import dugsolutions.leaf.random.RandomizerTD
import dugsolutions.leaf.random.di.DieFactory
import io.mockk.mockk

class PlayerReal private constructor(
    deckManager: DeckManager,
    floralArray: FloralArray,
    floralBonusCount: FloralBonusCount,
    cardManager: CardManager,
    dieFactory: DieFactory,
    costScore: CostScore,
    drawNewHand: DrawNewHand,
    decisionDirector: DecisionDirector
) : Player(
    deckManager,
    floralArray,
    floralBonusCount,
    cardManager,
    dieFactory,
    costScore,
    drawNewHand,
    decisionDirector,
) {

    companion object {

        fun create(): PlayerReal {
            val randomizer = RandomizerTD()
            val costScore = CostScore()
            val gameCardsFactory = GameCardsFactory(randomizer, costScore)
            val cardManager = CardManager(gameCardsFactory)
            cardManager.loadCards(FakeCards.ALL_CARDS)
            val gameCardIDsFactory = GameCardIDsFactory(cardManager, randomizerTD)
            val supplyStack = StackManager(cardManager, gameCardIDsFactory)
            val handStack = StackManager(cardManager, gameCardIDsFactory)
            val discardStack = StackManager(cardManager, gameCardIDsFactory)
            val floralBonusCount = FloralBonusCount()
            val floralArray = FloralArray(cardManager, gameCardIDsFactory)
            val dieFactory = DieFactory(randomizerTD)
            val deckManager = DeckManager(supplyStack, handStack, discardStack, dieFactory)
            val drawNewHand = DrawNewHand()
            val decisionDirector = mockk<DecisionDirector>(relaxed = true)

            return PlayerReal(
                deckManager,
                floralArray,
                floralBonusCount,
                cardManager,
                dieFactory,
                costScore,
                drawNewHand,
                decisionDirector
            ).apply {
                initialize()
            }
        }
    }
}
