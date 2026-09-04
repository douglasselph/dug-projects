package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.Chronicle
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice

sealed interface WoundResolution {

    data class Flipped(
        val card: CreatureCard
    ) : WoundResolution

    data class Snipped(
        val card: CreatureCard
    ) : WoundResolution

    data object NoLegalTarget : WoundResolution
}

/**
 * Coordinates one Flip It or Snip It wound without owning Creature geometry
 * or decision policy.
 */
class WoundResolver(
    private val grove: Grove,
    private val chronicle: Chronicle
) {

    fun resolve(player: Player): WoundResolution {
        val faceUpCards = player.creature.cards.filter { it.isFaceUp }
        val legalChoices =
            if (faceUpCards.isNotEmpty()) {
                faceUpCards.map(WoundChoice::Flip)
            } else {
                player.creature.snippableCards.map(WoundChoice::Snip)
            }

        if (legalChoices.isEmpty()) {
            return WoundResolution.NoLegalTarget
        }

        val choice = player.decisions.wound.choose(
            ChooseWoundRequest(legalChoices)
        )

        check(choice in legalChoices) {
            "Wound strategy returned a choice that was not offered: $choice"
        }

        return when (choice) {
            is WoundChoice.Flip -> flip(player, choice.card)
            is WoundChoice.Snip -> snip(player, choice.card)
        }
    }

    private fun flip(
        player: Player,
        card: CreatureCard
    ): WoundResolution.Flipped {
        check(player.creature.faceDown(card.id)) {
            "Unable to flip wound target ${card.id}"
        }

        val flipped = requireNotNull(player.creature.get(card.id))
        chronicle.record(
            Moment.Marker(
                "WOUND player=${player.id.value} FLIPPED plant=${card.card.name}"
            )
        )
        return WoundResolution.Flipped(flipped)
    }

    private fun snip(
        player: Player,
        card: CreatureCard
    ): WoundResolution.Snipped {
        val stack = grove.plantMarket.stackFor(card.card)
        check(stack != null && stack.remaining < stack.card.quantity) {
            "No Grove Plant stack can accept snipped card ${card.card.name}"
        }

        val snipped = checkNotNull(player.creature.snip(card.id)) {
            "Unable to snip wound target ${card.id}"
        }
        check(grove.plantMarket.returnCard(snipped.card)) {
            "Unable to return snipped card ${snipped.card.name} to the Grove"
        }

        chronicle.record(
            Moment.Marker(
                "WOUND player=${player.id.value} SNIPPED plant=${snipped.card.name}"
            )
        )
        return WoundResolution.Snipped(snipped)
    }
}
