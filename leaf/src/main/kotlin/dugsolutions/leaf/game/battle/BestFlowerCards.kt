package dugsolutions.leaf.game.battle

import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.player.Player

class BestFlowerCards(
    private val matchingBloomCard: MatchingBloomCard
) {
    operator fun invoke(player: Player): List<GameCard> {
        // Get all floral cards and group by frequency
        val floralCards = player.floralCards
        val frequencyMap = floralCards.groupingBy { it.id }.eachCount()
        
        // Find the maximum frequency
        val maxFrequency = frequencyMap.values.maxOrNull() ?: return emptyList()
        
        // Get all cards that have the maximum frequency
        val mostFrequentCards = floralCards.filter { card -> 
            frequencyMap[card.id] == maxFrequency 
        }.distinctBy { it.id }
        
        // If we have 2 or fewer cards, return them
        if (mostFrequentCards.size <= 2) {
            return mostFrequentCards
        }
        
        // For ties, use the decision director to break them
        val matchingBlooms = mostFrequentCards.mapNotNull { matchingBloomCard(it) }
        val preferredBloom = player.decisionDirector.bestBloomCard(matchingBlooms)
        
        // Find the flower card that matches the preferred bloom
        val preferredFlower = mostFrequentCards.find { flowerCard ->
            when (preferredBloom.matchWith) {
                is MatchWith.Flower -> preferredBloom.matchWith.flowerCardId == flowerCard.id
                else -> false
            }
        }
        
        // Return the preferred flower and one other from the most frequent cards
        return if (preferredFlower != null) {
            listOf(preferredFlower) + mostFrequentCards.filter { it != preferredFlower }.take(1)
        } else {
            mostFrequentCards.take(2)
        }
    }
}
