package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.Chronicle
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest

data class GraftPlan(
    val card: PlantCard,
    val placement: GraftPlacement
)

class GraftResolver(
    private val chronicle: Chronicle
) {
    fun prepare(player: Player, card: PlantCard): GraftPlan? {
        val legal = player.creature.legalPlacements(card)
        if (legal.isEmpty()) return null

        val placement = player.decisions.placement.choose(
            ChooseCreaturePlacementRequest(card, legal)
        )
        check(placement in legal) {
            "CreaturePlacementStrategy returned an illegal placement: $placement"
        }
        return GraftPlan(card, placement)
    }

    fun resolve(player: Player, plan: GraftPlan): CreatureCard {
        check(player.creature.canGraft(plan.card, plan.placement)) {
            "Prepared graft placement is no longer legal: ${plan.placement}"
        }
        val grafted = player.creature.graft(plan.card, plan.placement)
        check(grafted.isFaceDown) { "Newly grafted Plant must be face down" }
        chronicle.record(
            Moment.Marker(
                "GRAFT player=${player.id.value} plant=${plan.card.name}"
            )
        )
        return grafted
    }

    fun resolve(player: Player, card: PlantCard): CreatureCard? =
        prepare(player, card)?.let { resolve(player, it) }
}
