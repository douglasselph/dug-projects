package dugsolutions.leaf.simulation.v35.strategy.planned

import dugsolutions.leaf.v35.plant.domain.PlantCard

/** Stable identity of a Plant card targeted by a simulation Creature plan. */
@JvmInline
value class TargetCard(val cardName: String) {
    init {
        require(cardName.isNotBlank()) { "Target card name cannot be blank" }
    }

    companion object {
        fun from(card: PlantCard): TargetCard = TargetCard(card.name)
    }
}
