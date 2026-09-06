package dugsolutions.leaf.v35.player.decision.baseline.common

import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftDirection
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView

/**
 * Pure mirror of Creature placement geometry for strategy look-ahead.
 *
 * This evaluator never mutates the real Creature. It can therefore ask what a
 * hypothetical placement would do to future growth slots before a strategy
 * commits to the choice.
 */
object GraftTopologyEvaluator {
    private val PLANT_CORE_POSITION = CreaturePosition(0, 0)
    private val DICE_SUPPLY_POSITION = CreaturePosition(0, -1)


    /** Every legal placement for [type] given the immutable Creature view. */
    fun legalPlacements(
        creature: List<CreatureCardView>,
        type: PlantType
    ): List<GraftPlacement> =
        legalPlacementsForTopology(creature.map(::TopologyCard), type)

    /**
     * Number of unique physical locations where a future Vine could be grafted.
     * These are the Creature's connector/growth slots; Flowers can consume such
     * a slot but do not create another connector.
     */
    fun openGrowthSlots(creature: List<CreatureCardView>): Int =
        openGrowthPositionsForTopology(creature.map(::TopologyCard)).size

    fun openGrowthPositions(
        creature: List<CreatureCardView>
    ): Set<CreaturePosition> =
        openGrowthPositionsForTopology(creature.map(::TopologyCard))

    /** Number of unique legal Flower locations right now. */
    fun openFlowerSlots(creature: List<CreatureCardView>): Int =
        legalPlacements(creature, PlantType.FLOWER)
            .map { it.position }
            .distinct()
            .size

    /**
     * Hypothetical number of future Vine-growth locations after grafting one
     * card at [placement]. The placement must currently be legal for [type].
     */
    fun futureGrowthSlotsAfter(
        creature: List<CreatureCardView>,
        type: PlantType,
        placement: GraftPlacement
    ): Int {
        val topology = creature.map(::TopologyCard)
        require(placement in legalPlacementsForTopology(topology, type)) {
            "Illegal hypothetical $type placement: $placement"
        }

        val after = topology + TopologyCard(
            type = type,
            side = placement.side,
            position = placement.position
        )
        return openGrowthPositionsForTopology(after).size
    }

    /**
     * Card-level form used by Buy scoring before a placement has been chosen.
     * True means every currently legal Flower placement consumes the final
     * remaining connector/growth location.
     */
    fun wouldFlowerConsumeLastGrowthSlot(
        creature: List<CreatureCardView>
    ): Boolean {
        if (openGrowthSlots(creature) != 1) return false
        val flowerPlacements = legalPlacements(creature, PlantType.FLOWER)
        return flowerPlacements.isNotEmpty() && flowerPlacements.all { placement ->
            futureGrowthSlotsAfter(
                creature = creature,
                type = PlantType.FLOWER,
                placement = placement
            ) == 0
        }
    }

    /** True only when this exact Flower placement consumes the final connector slot. */
    fun wouldFlowerConsumeLastGrowthSlot(
        creature: List<CreatureCardView>,
        placement: GraftPlacement
    ): Boolean {
        val before = openGrowthSlots(creature)
        if (before != 1) return false
        return futureGrowthSlotsAfter(
            creature = creature,
            type = PlantType.FLOWER,
            placement = placement
        ) == 0
    }

    private data class TopologyCard(
        val type: PlantType,
        val side: CreatureSide,
        val position: CreaturePosition
    ) {
        constructor(view: CreatureCardView) : this(
            type = view.type,
            side = view.side,
            position = view.position
        )
    }

    private fun legalPlacementsForTopology(
        creature: List<TopologyCard>,
        type: PlantType
    ): List<GraftPlacement> {
        val result = mutableListOf<GraftPlacement>()
        val occupied = buildSet {
            add(PLANT_CORE_POSITION)
            add(DICE_SUPPLY_POSITION)
            addAll(creature.map { it.position })
        }

        fun addCandidate(
            side: CreatureSide,
            position: CreaturePosition
        ) {
            if (position !in occupied) {
                result += GraftPlacement(side, position)
            }
        }

        when (type) {
            PlantType.ROOT -> {
                addCandidate(
                    CreatureSide.LEFT,
                    DICE_SUPPLY_POSITION.move(GraftDirection.LEFT)
                )
                addCandidate(
                    CreatureSide.RIGHT,
                    DICE_SUPPLY_POSITION.move(GraftDirection.RIGHT)
                )
                creature
                    .filter { it.type == PlantType.ROOT }
                    .forEach { root ->
                        addCandidate(root.side, root.position.move(GraftDirection.LEFT))
                        addCandidate(root.side, root.position.move(GraftDirection.RIGHT))
                    }
            }

            PlantType.VINE -> {
                addCandidate(
                    CreatureSide.LEFT,
                    PLANT_CORE_POSITION.move(GraftDirection.LEFT)
                )
                addCandidate(
                    CreatureSide.RIGHT,
                    PLANT_CORE_POSITION.move(GraftDirection.RIGHT)
                )
                creature
                    .filter { it.type == PlantType.VINE }
                    .forEach { vine ->
                        GraftDirection.entries.forEach { direction ->
                            addCandidate(vine.side, vine.position.move(direction))
                        }
                    }
            }

            PlantType.FLOWER -> {
                creature
                    .filter { it.type == PlantType.VINE }
                    .forEach { vine ->
                        GraftDirection.entries.forEach { direction ->
                            addCandidate(vine.side, vine.position.move(direction))
                        }
                    }
            }
        }

        return result.distinct()
    }

    private fun openGrowthPositionsForTopology(
        creature: List<TopologyCard>
    ): Set<CreaturePosition> =
        legalPlacementsForTopology(creature, PlantType.VINE)
            .mapTo(linkedSetOf()) { it.position }
}
