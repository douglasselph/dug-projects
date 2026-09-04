package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
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
 *
 * Normal wounds use [resolve] and let the wounded player's WoundStrategy pick
 * among the legal targets. Effects such as Snip Happens may instead choose the
 * exact target externally and call [resolve] with an explicit [WoundChoice].
 * Both paths use the same legality check and mutation code.
 */
class WoundResolver(
    private val grove: Grove,
    private val chronicle: Chronicle
) {

    /**
     * Current legal wound targets for [player].
     *
     * Face-up cards always take priority. Only when none remain may a Wound
     * Snip one of Creature's current outer/snippable cards.
     */
    fun legalChoices(
        player: Player
    ): List<WoundChoice> {
        val faceUpCards =
            player.creature.cards.filter {
                it.isFaceUp
            }

        return if (faceUpCards.isNotEmpty()) {
            faceUpCards.map(
                WoundChoice::Flip
            )
        } else {
            player.creature.snippableCards.map(
                WoundChoice::Snip
            )
        }
    }

    /**
     * Resolve a normal wound, asking the wounded player's WoundStrategy.
     */
    fun resolve(
        player: Player
    ): WoundResolution {
        val legalChoices =
            legalChoices(player)

        if (legalChoices.isEmpty()) {
            return WoundResolution.NoLegalTarget
        }

        val choice =
            player.decisions.wound.choose(
                ChooseWoundRequest(
                    legalChoices
                )
            )

        decisionCheck(choice in legalChoices) {
            "Wound strategy returned a choice that was not offered: $choice"
        }

        return resolveLegalChoice(
            player = player,
            choice = choice
        )
    }

    /**
     * Resolve a wound whose exact target was chosen by another rule/effect.
     *
     * The choice is revalidated against the player's CURRENT wound legality
     * before any mutation. This intentionally rejects a face-down/Snip target
     * whenever that player still has any face-up Plant cards.
     */
    fun resolve(
        player: Player,
        choice: WoundChoice
    ): WoundResolution {
        val legalChoices =
            legalChoices(player)

        decisionCheck(choice in legalChoices) {
            "Explicit wound target is illegal: $choice; legal=$legalChoices"
        }

        return resolveLegalChoice(
            player = player,
            choice = choice
        )
    }

    private fun resolveLegalChoice(
        player: Player,
        choice: WoundChoice
    ): WoundResolution =
        when (choice) {
            is WoundChoice.Flip ->
                flip(
                    player,
                    choice.card
                )

            is WoundChoice.Snip ->
                snip(
                    player,
                    choice.card
                )
        }

    private fun flip(
        player: Player,
        card: CreatureCard
    ): WoundResolution.Flipped {
        stateCheck(
            player.creature.faceDown(
                card.id
            )
        ) {
            "Unable to flip wound target ${card.id}"
        }

        val flipped =
            stateNotNull(
                player.creature.get(
                    card.id
                )
            ) {
                "Flipped wound target disappeared after mutation: ${card.id}"
            }

        chronicle.record(
            Moment.Marker(
                "WOUND player=${player.id.value} FLIPPED plant=${card.card.name}"
            )
        )

        return WoundResolution.Flipped(
            flipped
        )
    }

    private fun snip(
        player: Player,
        card: CreatureCard
    ): WoundResolution.Snipped {
        val stack =
            grove.plantMarket.stackFor(
                card.card
            )

        stateCheck(
            stack != null &&
                stack.remaining <
                stack.card.quantity
        ) {
            "No Grove Plant stack can accept snipped card ${card.card.name}"
        }

        val snipped =
            stateNotNull(
                player.creature.snip(
                    card.id
                )
            ) {
                "Unable to snip wound target ${card.id}"
            }

        stateCheck(
            grove.plantMarket.returnCard(
                snipped.card
            )
        ) {
            "Unable to return snipped card ${snipped.card.name} to the Grove"
        }

        chronicle.record(
            Moment.Marker(
                "WOUND player=${player.id.value} SNIPPED plant=${snipped.card.name}"
            )
        )

        return WoundResolution.Snipped(
            snipped
        )
    }
}
