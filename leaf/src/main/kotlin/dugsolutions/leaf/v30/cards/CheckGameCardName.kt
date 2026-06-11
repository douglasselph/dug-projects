package dugsolutions.leaf.v30.cards

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment

class CheckGameCardName(
    private val chronicle: Chronicle = GameChronicle()
) {
    operator fun invoke(card: GameCard) {
        val parts = card.name.split("_")
        if (parts.size < 3) {
            warn(card, "Card name does not match expected TYPE_COST_INDEX pattern")
            return
        }
        val typeName = parts[0]
        val cost = parts[1].toIntOrNull()
        if (!typeName.equals(card.type.match, ignoreCase = true)) {
            warn(card, "Card name type '$typeName' does not match card type '${card.type.match}'")
        }
        if (cost != card.cost) {
            warn(card, "Card name cost '${parts[1]}' does not match card cost '${card.cost}'")
        }
    }

    private fun warn(
        card: GameCard,
        reason: String
    ) {
        chronicle(
            Moment.LoadingWarning(
                name = card.name,
                title = card.title,
                reason = reason
            )
        )
    }
}
