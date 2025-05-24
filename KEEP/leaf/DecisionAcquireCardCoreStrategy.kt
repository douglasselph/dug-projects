package dugsolutions.leaf.player.decisions

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CostScore
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.game.turn.local.CardIsFree
import dugsolutions.leaf.game.purchase.domain.Credits
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.decisions.DecisionAcquireCard.Result

class DecisionAcquireCardCoreStrategy(
    private val cardIsFree: CardIsFree,
    private val costScore: CostScore
) : DecisionAcquireCard {

    var cardUnderTest: CardID? = null
    
    override operator fun invoke(player: Player, possibleCards: List<GameCard>, credits: Credits): Result? {
        if (possibleCards.isEmpty()) {
            return null
        }
        
        // First priority: get the card under test if specified, available, and possible to afford.
        if (cardUnderTest != null) {
            val testCard = possibleCards.find { it.id == cardUnderTest }
            if (testCard != null && canAfford(testCard, credits)) {
                return Result(testCard)
            }
        }
        
        // Second priority: get free cards ordered by type preference
        val freeCards = possibleCards.filter { card -> cardIsFree(card, player) }
        if (freeCards.isNotEmpty()) {
            return Result(selectCardByTypePreference(freeCards))
        }
        
        // Third priority: get affordable cards ordered by type preference
        val affordableCards = possibleCards.filter { card -> 
            val combination = coverCost(player, card.cost)
            combination.isNotEmpty()
        }
        if (affordableCards.isNotEmpty()) {
            return Result(selectCardByTypePreference(affordableCards))
        }
        // No cards can be acquired
        return null
    }
    
    /**
     * Selects a card based on flourish type preference: Bloom > Vine > Canopy > Root
     * If multiple cards of the same type exist, chooses the one with highest cost score
     */
    private fun selectCardByTypePreference(cards: List<GameCard>): GameCard {
        // Group cards by type
        val cardsByType = cards.groupBy { it.type }
        
        // Check types in order of preference
        val preferredTypes = listOf(
            FlourishType.BLOOM,
            FlourishType.VINE, 
            FlourishType.CANOPY, 
            FlourishType.ROOT
        )
        
        for (type in preferredTypes) {
            val cardsOfType = cardsByType[type]
            if (!cardsOfType.isNullOrEmpty()) {
                // If multiple cards of same type, choose one with highest cost score
                return cardsOfType.maxByOrNull { costScore(it.cost) } ?: cardsOfType.first()
            }
        }
        
        // If no preferred types found, just return the first card
        return cards.first()
    }

    private fun canAfford(card: GameCard, credits: Credits): Boolean {

    }

}
