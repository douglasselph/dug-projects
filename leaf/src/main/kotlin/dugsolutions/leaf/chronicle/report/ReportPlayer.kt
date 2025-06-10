package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.player.Player

class ReportPlayer {

    operator fun invoke(player: Player): String {
        val cardsInSupply = cardsOf(player.cardsInSupply)
        val diceInSupply = player.diceInSupply.toString()
        val cardsInHand = cardsOf(player.cardsInHand)
        val diceInHand = player.diceInHand.values()
        val cardsInCompost = cardsOf(player.cardsInCompost)
        val diceInCompost = player.diceInCompost.toString()
        val floralArray = cardsOf(player.floralCards)
        
        // Combine strings, but only include non-empty ones
        val supplyItems = combineNonEmpty(cardsInSupply, diceInSupply)
        val handItems = combineNonEmpty(cardsInHand, diceInHand)
        val compostItems = combineNonEmpty(cardsInCompost, diceInCompost)
        val floralItems = combineNonEmpty(floralArray)

        val score = player.score

        return "${player.name}: Supply=[$supplyItems], Hand=[$handItems], Compost=[$compostItems], Floral=[$floralItems], Score=[$score]"
    }
    
    // Helper function to combine strings only if they're non-empty
    private fun combineNonEmpty(vararg strings: String): String {
        return strings.filter { it.isNotEmpty() }.joinToString(",")
    }

    private fun cardsOf(cards: List<GameCard>): String {
        if (cards.isEmpty()) {
            return ""
        }
        
        // Group cards by name and count occurrences
        val cardCounts = cards.groupBy { it.name }
            .mapValues { it.value.size }
        
        // Format each card entry based on count
        return cardCounts.entries.joinToString(",") { (name, count) ->
            if (count > 1) {
                "$count×$name"
            } else {
                name
            }
        }
    }

}
