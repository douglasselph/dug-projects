package dugsolutions.leaf.v35.chronicle.domain

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides

/** Compact immutable identity for one grafted Plant in an end-of-Round snapshot. */
data class GraftedPlantSnapshot(
    val type: PlantType,
    val cost: Int
)

/** Immutable end-of-Round player state used only for Chronicle diagnostics. */
data class PlayerRoundSummarySnapshot(
    val playerId: PlayerId,
    val vp: Int,
    val graftedPlants: List<GraftedPlantSnapshot>,
    val supplyDice: List<DieSides>,
    val discardDice: List<DieSides>,
    val beeCount: Int,
    val wormCount: Int,
    val waterCount: Int,
    val mulchDice: List<DieSides?>,
    val wispCount: Int,
    val butterflies: List<ButterflyStateSnapshot>
) {
    val graftedPlantCount: Int
        get() = graftedPlants.size
}
