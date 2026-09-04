package dugsolutions.leaf.v35.battle.domain

import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Mutable per-Battle-Round location state.
 *
 * The Grid owns only Battle locations/commitments. In particular, a die placed
 * here remains in its owner's PlayerDice.hand. Changing that live die's value
 * therefore never changes its Grid location.
 */
class BattleGrid(
    playerIdsInGridOrder: List<PlayerId>
) {
    private val orderedPlayerIds = playerIdsInGridOrder.toList()

    init {
        require(orderedPlayerIds.size in 2..4) {
            "Battle Grid requires 2 to 4 player columns: ${orderedPlayerIds.size}"
        }
        require(orderedPlayerIds.distinct().size == orderedPlayerIds.size) {
            "Battle Grid player IDs must be unique: $orderedPlayerIds"
        }
    }

    val columns: List<BattleColumn> =
        orderedPlayerIds.mapIndexed { index, playerId ->
            BattleColumn(index, playerId)
        }

    val playerIdsInGridOrder: List<PlayerId>
        get() = orderedPlayerIds.toList()

    private val squares: Map<PlayerId, Map<StrikeRow, BattleSquare>> =
        orderedPlayerIds.associateWith { playerId ->
            StrikeRow.entries.associateWith { row ->
                BattleSquare(playerId, row)
            }
        }

    private val closedRows = mutableSetOf<StrikeRow>()

    val diePlacements: List<BattleDiePlacement>
        get() = buildList {
            orderedPlayerIds.forEach { playerId ->
                StrikeRow.entries.forEach { row ->
                    square(playerId, row).dice.forEach { die ->
                        add(BattleDiePlacement(playerId, row, die))
                    }
                }
            }
        }

    val critterPlacements: List<BattleCritterPlacement>
        get() = buildList {
            orderedPlayerIds.forEach { playerId ->
                StrikeRow.entries.forEach { row ->
                    square(playerId, row).critters.forEach { critter ->
                        add(BattleCritterPlacement(playerId, row, critter))
                    }
                }
            }
        }

    fun column(playerId: PlayerId): BattleColumn =
        stateNotNull(
            columns.firstOrNull { it.playerId == playerId },
            context = "BattleGrid"
        ) {
            "No Battle Grid column exists for player ${playerId.value}"
        }

    fun square(
        playerId: PlayerId,
        row: StrikeRow
    ): BattleSquare =
        stateNotNull(
            squares[playerId]?.get(row),
            context = "BattleGrid"
        ) {
            "No Strike Square exists for player ${playerId.value} row $row"
        }

    fun isRowClosed(row: StrikeRow): Boolean =
        row in closedRows

    /**
     * Marks a Strike Row closed. Existing contents stay where they are until a
     * resolver explicitly drains them; new placements/moves into the row are
     * rejected.
     */
    fun closeRow(row: StrikeRow) {
        closedRows.add(row)
    }

    /**
     * Adds Battle location to an exact live Hand die without moving the die out
     * of PlayerDice.hand.
     */
    fun placeDie(
        player: Player,
        row: StrikeRow,
        die: Die
    ): BattleDiePlacement {
        ensurePlayerHasColumn(player)
        ensureRowOpen(row)

        stateCheck(
            player.dice.hand.any { it === die },
            context = "BattleGrid"
        ) {
            "Cannot place die that is not the exact live Hand die for player ${player.id.value}: $die"
        }
        stateCheck(
            locationOf(die) == null,
            context = "BattleGrid"
        ) {
            "Die is already placed on the Battle Grid: $die"
        }

        square(player.id, row).addDie(die)
        return BattleDiePlacement(player.id, row, die)
    }

    /**
     * Initial Rank-and-Place helper. Equal-valued dice retain their existing
     * Hand order; callers that need a different legal tie arrangement may use
     * the overload accepting [topToBottom].
     */
    fun placeInitialHand(
        player: Player
    ): List<BattleDiePlacement> =
        placeInitialHand(
            player = player,
            topToBottom = player.dice.hand.sortedByDescending { it.value }
        )

    /**
     * Places the player's entire current Hand from TOP to BOTTOM.
     *
     * This validates rather than decides: callers may choose the order of
     * equal-valued dice, but values themselves must be non-increasing.
     */
    fun placeInitialHand(
        player: Player,
        topToBottom: List<Die>
    ): List<BattleDiePlacement> {
        ensurePlayerHasColumn(player)
        stateCheck(
            player.dice.hand.size <= StrikeRow.entries.size,
            context = "BattleGrid"
        ) {
            "Initial Battle placement supports at most 3 Hand dice; player ${player.id.value} has ${player.dice.hand.size}"
        }
        stateCheck(
            topToBottom.size == player.dice.hand.size,
            context = "BattleGrid"
        ) {
            "Initial placement must include every Hand die for player ${player.id.value}: expected ${player.dice.hand.size}, got ${topToBottom.size}"
        }
        stateCheck(
            sameDieIdentities(topToBottom, player.dice.hand),
            context = "BattleGrid"
        ) {
            "Initial placement contains dice that are not exactly player ${player.id.value}'s current Hand"
        }
        stateCheck(
            topToBottom.zipWithNext().all { (first, second) -> first.value >= second.value },
            context = "BattleGrid"
        ) {
            "Initial Battle dice must be ordered highest-to-lowest for player ${player.id.value}: ${topToBottom.map { it.value }}"
        }
        stateCheck(
            topToBottom.none { locationOf(it) != null },
            context = "BattleGrid"
        ) {
            "Initial placement attempted to place a Hand die that is already on the Grid for player ${player.id.value}"
        }

        return topToBottom.mapIndexed { index, die ->
            placeDie(
                player = player,
                row = StrikeRow.entries[index],
                die = die
            )
        }
    }

    /** Exact-reference lookup; equivalent dice in different squares stay distinct. */
    fun locationOf(die: Die): BattleLocation? {
        orderedPlayerIds.forEach { playerId ->
            StrikeRow.entries.forEach { row ->
                if (square(playerId, row).containsDieIdentity(die)) {
                    return BattleLocation(playerId, row)
                }
            }
        }
        return null
    }

    fun placementOf(die: Die): BattleDiePlacement? {
        val location = locationOf(die) ?: return null
        return BattleDiePlacement(location.playerId, location.row, die)
    }

    /**
     * Removes only the Grid location. The die deliberately remains in its
     * Player Dice Hand until the caller sends it to Discard/Trash/etc.
     */
    fun removeDie(die: Die): BattleDiePlacement? {
        val placement = placementOf(die) ?: return null
        val removed =
            square(placement.playerId, placement.row)
                .removeDieIdentity(die)
        stateCheck(removed, context = "BattleGrid") {
            "Located Battle die could not be removed from its Strike Square: $placement"
        }
        return placement
    }

    /**
     * Replaces one exact Grid die with another exact live die while preserving
     * the original player's Strike Square.
     *
     * This is the location half of Battle Upgrade/replacement effects. The
     * caller owns PlayerDice zone membership; the Grid owns only location.
     */
    fun replaceDie(
        oldDie: Die,
        newDie: Die
    ): BattleDiePlacement {
        val placement = stateNotNull(
            placementOf(oldDie),
            context = "BattleGrid"
        ) {
            "Cannot replace an unplaced Battle die: $oldDie"
        }

        stateCheck(
            locationOf(newDie) == null,
            context = "BattleGrid"
        ) {
            "Replacement die is already placed on the Battle Grid: $newDie"
        }

        val target =
            square(
                placement.playerId,
                placement.row
            )

        stateCheck(
            target.removeDieIdentity(oldDie),
            context = "BattleGrid"
        ) {
            "Located Battle die disappeared before replacement: $placement"
        }

        target.addDie(newDie)

        return BattleDiePlacement(
            playerId = placement.playerId,
            row = placement.row,
            die = newDie
        )
    }

    /**
     * Exchanges the Grid locations of two exact live dice without changing
     * either die's value or PlayerDice ownership.
     *
     * This supports same-player swaps such as Transplant Tulip now and can be
     * reused by later cross-player effects once their ownership rule is applied.
     */
    fun swapDieLocations(
        first: Die,
        second: Die
    ): Pair<BattleDiePlacement, BattleDiePlacement> {
        stateCheck(first !== second, context = "BattleGrid") {
            "Cannot swap a Battle die with itself"
        }

        val firstPlacement = stateNotNull(
            placementOf(first),
            context = "BattleGrid"
        ) {
            "First swap die is not placed on the Battle Grid: $first"
        }
        val secondPlacement = stateNotNull(
            placementOf(second),
            context = "BattleGrid"
        ) {
            "Second swap die is not placed on the Battle Grid: $second"
        }

        if (
            firstPlacement.playerId == secondPlacement.playerId &&
            firstPlacement.row == secondPlacement.row
        ) {
            return firstPlacement to secondPlacement
        }

        val firstSquare =
            square(
                firstPlacement.playerId,
                firstPlacement.row
            )
        val secondSquare =
            square(
                secondPlacement.playerId,
                secondPlacement.row
            )

        stateCheck(
            firstSquare.removeDieIdentity(first),
            context = "BattleGrid"
        ) {
            "First Battle die disappeared before swap: $firstPlacement"
        }
        stateCheck(
            secondSquare.removeDieIdentity(second),
            context = "BattleGrid"
        ) {
            "Second Battle die disappeared before swap: $secondPlacement"
        }

        firstSquare.addDie(second)
        secondSquare.addDie(first)

        return (
            BattleDiePlacement(
                playerId = secondPlacement.playerId,
                row = secondPlacement.row,
                die = first
            ) to
                BattleDiePlacement(
                    playerId = firstPlacement.playerId,
                    row = firstPlacement.row,
                    die = second
                )
            )
    }

    /**
     * Changes only a die's Battle location. The live die and PlayerDice Hand
     * ownership are unchanged.
     */
    fun moveDie(
        die: Die,
        toRow: StrikeRow
    ): BattleDiePlacement {
        val from = stateNotNull(
            placementOf(die),
            context = "BattleGrid"
        ) {
            "Cannot move an unplaced Battle die: $die"
        }

        if (from.row == toRow) return from
        ensureRowOpen(toRow)

        val target = square(from.playerId, toRow)
        stateCheck(!target.isFull, context = "BattleGrid") {
            "Cannot move die into full Strike Square for player ${from.playerId.value} row $toRow"
        }

        val source = square(from.playerId, from.row)
        stateCheck(source.removeDieIdentity(die), context = "BattleGrid") {
            "Source Strike Square lost die during move: $from"
        }
        target.addDie(die)
        return BattleDiePlacement(from.playerId, toRow, die)
    }

    /**
     * Commits one owned Critter to the Grid. Unlike dice, a placed Critter is
     * removed from the player's uncommitted Critter pool so it cannot be used
     * again elsewhere during this Battle Round.
     */
    fun placeCritter(
        player: Player,
        row: StrikeRow,
        critter: Critter
    ): BattleCritterPlacement {
        ensurePlayerHasColumn(player)
        ensureRowOpen(row)
        stateCheck(
            player.critters.count(critter) > 0,
            context = "BattleGrid"
        ) {
            "Player ${player.id.value} cannot place unowned Critter $critter"
        }

        val removed = player.critters.remove(critter)
        stateCheck(removed, context = "BattleGrid") {
            "Validated Critter $critter disappeared before Battle placement for player ${player.id.value}"
        }
        square(player.id, row).addCritter(critter)
        return BattleCritterPlacement(player.id, row, critter)
    }

    /** Removes one committed Critter from the Grid without choosing its destination. */
    fun removeCritter(
        playerId: PlayerId,
        row: StrikeRow,
        critter: Critter
    ): BattleCritterPlacement? {
        return if (square(playerId, row).removeCritter(critter)) {
            BattleCritterPlacement(playerId, row, critter)
        } else {
            null
        }
    }

    /** Drains every committed Critter, useful for Battle Cleanup. */
    fun drainCritters(): List<BattleCritterPlacement> =
        buildList {
            orderedPlayerIds.forEach { playerId ->
                StrikeRow.entries.forEach { row ->
                    square(playerId, row).drainCritters().forEach { critter ->
                        add(BattleCritterPlacement(playerId, row, critter))
                    }
                }
            }
        }

    /**
     * Drains every die placement without moving any die out of PlayerDice.hand.
     * The caller owns the subsequent dice-zone transition.
     */
    fun drainDice(): List<BattleDiePlacement> =
        buildList {
            orderedPlayerIds.forEach { playerId ->
                StrikeRow.entries.forEach { row ->
                    square(playerId, row).drainDice().forEach { die ->
                        add(BattleDiePlacement(playerId, row, die))
                    }
                }
            }
        }

    private fun ensurePlayerHasColumn(player: Player) {
        stateCheck(
            orderedPlayerIds.contains(player.id),
            context = "BattleGrid"
        ) {
            "Player ${player.id.value} does not own a Battle Grid column"
        }
    }

    private fun ensureRowOpen(row: StrikeRow) {
        stateCheck(
            !isRowClosed(row),
            context = "BattleGrid"
        ) {
            "Strike Row $row is closed"
        }
    }

    private fun sameDieIdentities(
        first: List<Die>,
        second: List<Die>
    ): Boolean {
        if (first.size != second.size) return false

        val unmatched = second.toMutableList()
        first.forEach { die ->
            val index = unmatched.indexOfFirst { it === die }
            if (index < 0) return false
            unmatched.removeAt(index)
        }
        return unmatched.isEmpty()
    }
}
