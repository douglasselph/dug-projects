package dugsolutions.leaf.player.components

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.domain.CardID
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.random.die.DieSides

class CreatureManager(
    private val cardManager: CardManager
) {

    companion object {
        // Hardcoded position patterns for vine/flower cards (above canopy)
        // Pattern: left, right, top, then fan out further
        private val VINE_FLOWER_POSITIONS = listOf(
            // First ring: left, right, top
            Pair(-1, 0),  // left
            Pair(1, 0),   // right
            Pair(0, 1),   // top

            // Second ring: further out
            Pair(-2, 0),  // left
            Pair(-1, 1),  // left-up
            Pair(0, 2),   // top
            Pair(1, 1),   // right-up
            Pair(2, 0),   // right

            // Third ring: even further out
            Pair(-3, 0),  // left
            Pair(-2, 1),  // left-up
            Pair(-1, 2),  // left-up-up
            Pair(0, 3),   // top
            Pair(1, 2),   // right-up-up
            Pair(2, 1),   // right-up
            Pair(3, 0),   // right

            // Fourth ring: maximum spread
            Pair(-4, 0),  // left
            Pair(-3, 1),  // left-up
            Pair(-2, 2),  // left-up-up
            Pair(-1, 3),  // left-up-up-up
            Pair(0, 4),   // top
            Pair(1, 3),   // right-up-up-up
            Pair(2, 2),   // right-up-up
            Pair(3, 1),   // right-up
            Pair(4, 0)    // right
        )

        // Hardcoded position patterns for root cards (below canopy)
        // Pattern: below, then fan out left and right
        private val ROOT_POSITIONS = listOf(
            // First ring: directly below
            Pair(0, -1),  // below

            // Second ring: left and right of first root
            Pair(-1, -1), // left
            Pair(1, -1),  // right
            Pair(0, -2),  // below

            // Third ring: fan out further
            Pair(-2, -1), // left
            Pair(-1, -2), // left-down
            Pair(0, -3),  // below
            Pair(1, -2), // right-down
            Pair(2, -1),  // right

            // Fourth ring: even further out
            Pair(-3, -1), // left
            Pair(-2, -2), // left-down
            Pair(-1, -3), // left-down-down
            Pair(0, -4),  // below
            Pair(1, -3),  // right-down-down
            Pair(2, -2),  // right-down
            Pair(3, -1),  // right

            // Fifth ring: maximum spread
            Pair(-4, -1), // left
            Pair(-3, -2), // left-down
            Pair(-2, -3), // left-down-down
            Pair(-1, -4), // left-down-down-down
            Pair(0, -5),  // below
            Pair(1, -4),  // right-down-down-down
            Pair(2, -3),  // right-down-down
            Pair(3, -2),  // right-down
            Pair(4, -1)   // right
        )
    }

    // Grid to represent the creature layout
    // Key: Pair(x, y) where (0,0) is the center canopy
    // Value: CardID of the card at that position
    private val creatureGrid = mutableMapOf<Pair<Int, Int>, CardID>()

    private fun cardOf(cardId: CardID): GameCard? = cardManager.getCard(cardId)

    // Dice on creature is equal to 1 + #Roots
    private val graftedDiceHeld = mutableListOf<Die>()

    private var sapTotal: Int = 1
    private var sapLeftCount: Int = 1

    // region public

    fun addCard(cardId: CardID): Boolean {
        val card = cardOf(cardId) ?: return false

        return when (card.type) {
            FlourishType.ROOT -> addRootCard(cardId)
            FlourishType.VINE, FlourishType.FLOWER -> addVineOrFlowerCard(cardId)
            else -> false
        }
    }

    fun removeCard(cardId: CardID): Boolean {
        // Find the position of the card in the grid
        val positionToRemove = creatureGrid.entries.find { (_, gridCardId) -> gridCardId == cardId }?.key

        return if (positionToRemove != null) {
            creatureGrid.remove(positionToRemove)
            true
        } else {
            false
        }
    }

    val leafCards: List<GameCard>
        get() {
            val leafCardIds = mutableListOf<CardID>()

            for ((position, cardId) in creatureGrid) {
                if (isLeafCard(position, cardId)) {
                    leafCardIds.add(cardId)
                }
            }
            return leafCardIds.mapNotNull { cardOf(it) }
        }


    // region Graft

    fun addDie(die: Die): Boolean {
        if (!hasRoomForGraftedDie) {
            return false
        }
        graftedDiceHeld.add(die)
        return true
    }

    // Pull die with at least the number of sides indicated.
    fun pullDie(sides: DieSides): Die? {
        // Find all dice that have at least the required number of sides
        val eligibleDice = graftedDiceHeld.filter { it.sides >= sides.value }
        
        if (eligibleDice.isEmpty()) {
            return null
        }
        
        // Find the die with the closest number of sides to the target
        val targetSides = sides.value
        val closestDie = eligibleDice.minByOrNull { die ->
            kotlin.math.abs(die.sides - targetSides)
        }
        
        if (closestDie == null) {
            return null
        }
        
        // Remove the selected die from the list
        val dieIndex = graftedDiceHeld.indexOf(closestDie)
        val die = graftedDiceHeld.removeAt(dieIndex)
        die.roll()
        return die
    }

    val hasRoomForGraftedDie: Boolean
        get() = graftedDiceHeld.size < (1+rootCount)

    private val rootCount: Int
        get() {
            return creatureGrid.values.count { cardId ->
                val card = cardOf(cardId)
                card?.type == FlourishType.ROOT
            }
        }

    val hasGraftedDice: Boolean
        get() = graftedDiceHeld.size > 0

    val graftedDice: List<Die>
        get() = graftedDiceHeld

    // endregion Graft

    // Helper method to get all cards in the creature
    val allCards: List<GameCard>
        get() = creatureGrid.values.mapNotNull { cardOf(it) }

    // Helper method to get card at specific position
    fun getCardAt(x: Int, y: Int): GameCard? {
        val cardId = creatureGrid[Pair(x, y)]
        return cardId?.let { cardOf(it) }
    }

    // Helper method to check if position is occupied
    fun isPositionOccupied(x: Int, y: Int): Boolean {
        return creatureGrid.containsKey(Pair(x, y))
    }

    // region Sap

    fun useSap(): Boolean {
        if (sapLeftCount > 0) {
            sapLeftCount--
            return true
        } else {
            return false
        }
    }

    fun resetSap() {
        sapLeftCount = sapTotal
    }

    fun addSap() {
        sapTotal++
        resetSap()
    }

    val sapLeft: Int = sapLeftCount
    val hasSap: Boolean
        get() = sapLeft > 0

    // endregion Sap

    // Cards executed

    private val storedCardsExecuted = mutableListOf<GameCard>()

    fun cardExecuted(card: GameCard) {
        storedCardsExecuted.add(card)
    }

    fun resetCardsExecuted() {
        storedCardsExecuted.clear()
    }

    val cardsExecuted: List<GameCard>
        get() = storedCardsExecuted

    // endregion public

    private fun addRootCard(cardId: CardID): Boolean {
        // Find the first available position in the hardcoded root pattern
        for (position in ROOT_POSITIONS) {
            if (!creatureGrid.containsKey(position)) {
                creatureGrid[position] = cardId
                return true
            }
        }
        return false // No more room
    }

    private fun addVineOrFlowerCard(cardId: CardID): Boolean {
        // Find the first available position in the hardcoded vine/flower pattern
        for (position in VINE_FLOWER_POSITIONS) {
            if (!creatureGrid.containsKey(position) && isValidVineFlowerPosition(position)) {
                creatureGrid[position] = cardId
                return true
            }
        }
        return false // No more room
    }

    /**
     * Validates if a position is valid for vine/flower placement.
     * For positions beyond the first ring, there must be a vine path back to the canopy.
     */
    private fun isValidVineFlowerPosition(position: Pair<Int, Int>): Boolean {
        val (x, y) = position

        // First ring positions (left, right, top) are always valid if empty
        if (position in listOf(Pair(-1, 0), Pair(1, 0), Pair(0, 1))) {
            return true
        }

        // For other positions, check if there's a vine path back to canopy
        return hasVinePathToCanopy(position)
    }

    /**
     * Checks if there's a continuous vine path from the given position back to the canopy (0,0).
     * Uses BFS to find a path through vine cards only.
     */
    private fun hasVinePathToCanopy(startPosition: Pair<Int, Int>): Boolean {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = mutableListOf(startPosition)
        visited.add(startPosition)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)

            // If we've reached the canopy, we found a path
            if (current == Pair(0, 0)) {
                return true
            }

            // Check all adjacent positions
            val adjacentPositions = listOf(
                Pair(current.first - 1, current.second),  // left
                Pair(current.first + 1, current.second),  // right
                Pair(current.first, current.second - 1),  // down
                Pair(current.first, current.second + 1)   // up
            )

            for (adjacent in adjacentPositions) {
                if (adjacent == Pair(0, 0)) {
                    // If we've reached the canopy, we found a path
                    return true
                }
                if (adjacent !in visited &&
                    creatureGrid.containsKey(adjacent) &&
                    isVineCard(adjacent)
                ) {
                    visited.add(adjacent)
                    queue.add(adjacent)
                }
            }
        }

        return false
    }

    /**
     * Checks if a card at the given position is a vine card.
     */
    private fun isVineCard(position: Pair<Int, Int>): Boolean {
        val cardId = creatureGrid[position] ?: return false
        val card = cardOf(cardId) ?: return false
        return card.type == FlourishType.VINE
    }

    private fun isLeafCard(position: Pair<Int, Int>, cardId: CardID): Boolean {
        val card = cardOf(cardId) ?: return false

        // Flower cards are always leaf cards (endpoints)
        if (card.type == FlourishType.FLOWER) {
            return true
        }

        // For vine and root cards, check if removing this card would leave any cards "hanging"
        // A card is a leaf if removing it doesn't break the connection to the canopy
        return isLeafCardByRemoval(position)
    }

    /**
     * Determines if a card is a leaf by checking if removing it would leave any cards
     * without a path back to the canopy (0,0).
     */
    private fun isLeafCardByRemoval(position: Pair<Int, Int>): Boolean {
        // Temporarily remove the card from the grid
        val removedCardId = creatureGrid.remove(position) ?: return false

        // Check if all remaining cards still have a path to the canopy
        val hasDisconnectedCards = creatureGrid.keys.any { remainingPosition ->
            !hasPathToCanopy(remainingPosition)
        }

        // Restore the card
        creatureGrid[position] = removedCardId

        // If no cards were disconnected, this is a leaf card
        return !hasDisconnectedCards
    }

    /**
     * Checks if a position has a path back to the canopy (0,0).
     * Uses BFS to find any path through any type of card.
     */
    private fun hasPathToCanopy(startPosition: Pair<Int, Int>): Boolean {
        val visited = mutableSetOf<Pair<Int, Int>>()
        val queue = mutableListOf(startPosition)
        visited.add(startPosition)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)

            // If we've reached the canopy, we found a path
            if (current == Pair(0, 0)) {
                return true
            }

            // Check all adjacent positions
            val adjacentPositions = listOf(
                Pair(current.first - 1, current.second),  // left
                Pair(current.first + 1, current.second),  // right
                Pair(current.first, current.second - 1),  // down
                Pair(current.first, current.second + 1)   // up
            )

            for (adjacent in adjacentPositions) {
                if (adjacent !in visited && creatureGrid.containsKey(adjacent)) {
                    visited.add(adjacent)
                    queue.add(adjacent)
                }
            }
        }
        return false
    }


}
