package dugsolutions.leaf.v35.player.creature

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType

/**
 * Owns the topology and facing state of one player's Plant Creature.
 *
 * Geometry is modeled logically rather than visually:
 *
 * - Plant Core is fixed at (0, 0).
 * - Player Dice Supply is fixed at (0, -1).
 * - Roots grow horizontally from the Supply or another Root.
 * - Vines grow left/right from the Core, then left/right/above another Vine.
 * - Flowers grow left/right/above a Vine and never act as connectors.
 *
 * Every grafted card belongs to the LEFT or RIGHT side established by the
 * initial Root/Vine branch from the Supply/Core.
 */
class Creature {

    companion object {
        val PLANT_CORE_POSITION = CreaturePosition(0, 0)
        val DICE_SUPPLY_POSITION = CreaturePosition(0, -1)
    }

    private val cardsById = linkedMapOf<CreatureCardId, CreatureCard>()
    private var nextCardId = 1

    val cards: List<CreatureCard>
        get() = cardsById.values.toList()

    val leftCards: List<CreatureCard>
        get() = cards.filter { it.side == CreatureSide.LEFT }

    val rightCards: List<CreatureCard>
        get() = cards.filter { it.side == CreatureSide.RIGHT }

    val roots: List<CreatureCard>
        get() = cards.filter { it.card.type == PlantType.ROOT }

    val vines: List<CreatureCard>
        get() = cards.filter { it.card.type == PlantType.VINE }

    val flowers: List<CreatureCard>
        get() = cards.filter { it.card.type == PlantType.FLOWER }

    val size: Int
        get() = cardsById.size

    val isEmpty: Boolean
        get() = cardsById.isEmpty()

    val allFaceDown: Boolean
        get() = cards.isNotEmpty() && cards.all { it.isFaceDown }

    fun get(id: CreatureCardId): CreatureCard? =
        cardsById[id]

    /**
     * Returns every currently legal physical position for [card].
     *
     * The returned list is suitable for a decision strategy: choose one of
     * these placements, then pass it to [graft].
     */
    fun legalPlacements(card: PlantCard): List<GraftPlacement> =
        when (card.type) {
            PlantType.ROOT -> legalRootPlacements()
            PlantType.VINE -> legalVinePlacements()
            PlantType.FLOWER -> legalFlowerPlacements()
        }

    fun canGraft(
        card: PlantCard,
        placement: GraftPlacement
    ): Boolean =
        placement in legalPlacements(card)

    /**
     * Grafts a newly gained Plant card face down.
     */
    fun graft(
        card: PlantCard,
        placement: GraftPlacement
    ): CreatureCard {
        require(canGraft(card, placement)) {
            "Illegal ${card.type} placement at ${placement.position} on ${placement.side} side"
        }

        val grafted = CreatureCard(
            id = CreatureCardId(nextCardId++),
            card = card,
            side = placement.side,
            position = placement.position,
            facing = CreatureCard.Facing.FACE_DOWN
        )

        cardsById[grafted.id] = grafted
        return grafted
    }

    fun faceUp(id: CreatureCardId): Boolean =
        replace(id) { it.faceUp() }

    fun faceDown(id: CreatureCardId): Boolean =
        replace(id) { it.faceDown() }

    fun flip(id: CreatureCardId): Boolean =
        replace(id) { it.flip() }

    fun faceUpAll() {
        cardsById.keys.toList().forEach { id ->
            replace(id) { it.faceUp() }
        }
    }

    /**
     * Cards that satisfy the rulebook definition of an outer card:
     * removing one of them leaves every remaining Plant card connected.
     */
    val snippableCards: List<CreatureCard>
        get() = cards.filter { canSnip(it.id) }

    fun canSnip(id: CreatureCardId): Boolean {
        if (id !in cardsById) return false
        return structureIsConnected(excluding = id)
    }

    /**
     * Removes an outer card. Returns null if the card does not exist or if its
     * removal would disconnect another Plant card.
     */
    fun snip(id: CreatureCardId): CreatureCard? {
        if (!canSnip(id)) return null
        return cardsById.remove(id)
    }

    fun clear() {
        cardsById.clear()
        nextCardId = 1
    }

    private fun legalRootPlacements(): List<GraftPlacement> {
        val result = mutableListOf<GraftPlacement>()

        addCandidate(
            result,
            CreatureSide.LEFT,
            DICE_SUPPLY_POSITION.move(GraftDirection.LEFT)
        )
        addCandidate(
            result,
            CreatureSide.RIGHT,
            DICE_SUPPLY_POSITION.move(GraftDirection.RIGHT)
        )

        roots.forEach { root ->
            addCandidate(
                result,
                root.side,
                root.position.move(GraftDirection.LEFT)
            )
            addCandidate(
                result,
                root.side,
                root.position.move(GraftDirection.RIGHT)
            )
        }

        return result.distinct()
    }

    private fun legalVinePlacements(): List<GraftPlacement> {
        val result = mutableListOf<GraftPlacement>()

        addCandidate(
            result,
            CreatureSide.LEFT,
            PLANT_CORE_POSITION.move(GraftDirection.LEFT)
        )
        addCandidate(
            result,
            CreatureSide.RIGHT,
            PLANT_CORE_POSITION.move(GraftDirection.RIGHT)
        )

        vines.forEach { vine ->
            GraftDirection.entries.forEach { direction ->
                addCandidate(
                    result,
                    vine.side,
                    vine.position.move(direction)
                )
            }
        }

        return result.distinct()
    }

    private fun legalFlowerPlacements(): List<GraftPlacement> {
        val result = mutableListOf<GraftPlacement>()

        vines.forEach { vine ->
            GraftDirection.entries.forEach { direction ->
                addCandidate(
                    result,
                    vine.side,
                    vine.position.move(direction)
                )
            }
        }

        return result.distinct()
    }

    private fun addCandidate(
        result: MutableList<GraftPlacement>,
        side: CreatureSide,
        position: CreaturePosition
    ) {
        if (!isOccupied(position)) {
            result += GraftPlacement(
                side = side,
                position = position
            )
        }
    }

    private fun isOccupied(position: CreaturePosition): Boolean =
        position == PLANT_CORE_POSITION ||
            position == DICE_SUPPLY_POSITION ||
            cards.any { it.position == position }

    private fun replace(
        id: CreatureCardId,
        replacement: (CreatureCard) -> CreatureCard
    ): Boolean {
        val current = cardsById[id] ?: return false
        cardsById[id] = replacement(current)
        return true
    }

    /**
     * Checks whether all cards that would remain after [excluding] are still
     * connected according to Creature geometry.
     *
     * Roots connect only through Roots to the Player Dice Supply.
     * Vines connect only through Vines to the Plant Core.
     * Flowers are endpoints: they must touch at least one remaining Vine, but
     * they never provide connectivity for another card.
     */
    private fun structureIsConnected(
        excluding: CreatureCardId
    ): Boolean {
        val remaining = cards.filterNot { it.id == excluding }

        return CreatureSide.entries.all { side ->
            rootsAreConnected(
                remaining = remaining,
                side = side
            ) &&
                vinesAreConnected(
                    remaining = remaining,
                    side = side
                ) &&
                flowersAreConnected(
                    remaining = remaining,
                    side = side
                )
        }
    }

    private fun rootsAreConnected(
        remaining: List<CreatureCard>,
        side: CreatureSide
    ): Boolean {
        val sideRoots = remaining.filter {
            it.side == side &&
                it.card.type == PlantType.ROOT
        }

        if (sideRoots.isEmpty()) return true

        val startPosition =
            when (side) {
                CreatureSide.LEFT ->
                    DICE_SUPPLY_POSITION.move(GraftDirection.LEFT)

                CreatureSide.RIGHT ->
                    DICE_SUPPLY_POSITION.move(GraftDirection.RIGHT)
            }

        val start = sideRoots.firstOrNull {
            it.position == startPosition
        } ?: return false

        return connectedCount(
            start = start,
            candidates = sideRoots,
            adjacent = { first, second ->
                first.position.y == second.position.y &&
                    kotlin.math.abs(first.position.x - second.position.x) == 1
            }
        ) == sideRoots.size
    }

    private fun vinesAreConnected(
        remaining: List<CreatureCard>,
        side: CreatureSide
    ): Boolean {
        val sideVines = remaining.filter {
            it.side == side &&
                it.card.type == PlantType.VINE
        }

        if (sideVines.isEmpty()) return true

        val startPosition =
            when (side) {
                CreatureSide.LEFT ->
                    PLANT_CORE_POSITION.move(GraftDirection.LEFT)

                CreatureSide.RIGHT ->
                    PLANT_CORE_POSITION.move(GraftDirection.RIGHT)
            }

        val start = sideVines.firstOrNull {
            it.position == startPosition
        } ?: return false

        return connectedCount(
            start = start,
            candidates = sideVines,
            adjacent = { first, second ->
                first.position.isOrthogonallyAdjacentTo(second.position)
            }
        ) == sideVines.size
    }

    private fun flowersAreConnected(
        remaining: List<CreatureCard>,
        side: CreatureSide
    ): Boolean {
        val sideVines = remaining.filter {
            it.side == side &&
                it.card.type == PlantType.VINE
        }

        val sideFlowers = remaining.filter {
            it.side == side &&
                it.card.type == PlantType.FLOWER
        }

        return sideFlowers.all { flower ->
            sideVines.any { vine ->
                flower.position.isOrthogonallyAdjacentTo(vine.position)
            }
        }
    }

    private fun connectedCount(
        start: CreatureCard,
        candidates: List<CreatureCard>,
        adjacent: (CreatureCard, CreatureCard) -> Boolean
    ): Int {
        val visited = mutableSetOf<CreatureCardId>()
        val queue = ArrayDeque<CreatureCard>()

        visited += start.id
        queue.addLast(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            candidates.forEach { candidate ->
                if (
                    candidate.id !in visited &&
                    adjacent(current, candidate)
                ) {
                    visited += candidate.id
                    queue.addLast(candidate)
                }
            }
        }

        return visited.size
    }

    private fun CreaturePosition.isOrthogonallyAdjacentTo(
        other: CreaturePosition
    ): Boolean {
        val dx = kotlin.math.abs(x - other.x)
        val dy = kotlin.math.abs(y - other.y)
        return dx + dy == 1
    }
}
