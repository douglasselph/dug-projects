package dugsolutions.leaf.game.battle

import dugsolutions.leaf.player.Player

class BattlePhaseTransition(
    private val bestFlowerCards: BestFlowerCards,
    private val matchingBloomCard: MatchingBloomCard
) {

    operator fun invoke(players: List<Player>) {
        players.forEach { player -> invoke(player) }
    }

    private operator fun invoke(player: Player) {

        val bestMatchingFlowerCards = bestFlowerCards(player)
        val bestMatchingBloomCards = bestMatchingFlowerCards.map { matchingBloomCard(it) }

        if (bestMatchingBloomCards.isNotEmpty()) {
            if (bestMatchingBloomCards.size == 1) {
                bestMatchingBloomCards[0]?.let { card ->
                    player.addCardToSupply(card.id)
                    player.addCardToSupply(card.id)
                }
            } else {
                bestMatchingBloomCards[0]?.let { card -> player.addCardToSupply(card.id) }
                bestMatchingBloomCards[1]?.let { card -> player.addCardToSupply(card.id) }
            }
        }

        // Move all flower cards to supply
        player.floralCards.forEach {
            flowerCard -> player.addCardToSupply(flowerCard.id)
        }

        // Clear floral cards and resupply
        player.clearFloralCards()
        player.resupply()
    }
}
