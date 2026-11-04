package dugsolutions.leaf.v35.game.round.battle

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.player.PlayerId

data class BattlePlayerCleanupResult(
    val playerId: PlayerId,
    val discardedDice: Int,
    val returnedCritters: Int,
    val refreshed: Boolean
)

data class BattleCleanupResult(
    val players: List<BattlePlayerCleanupResult>
) {
    val totalDiscardedDice: Int
        get() = players.sumOf { it.discardedDice }

    val totalReturnedCritters: Int
        get() = players.sumOf { it.returnedCritters }

    val refreshedPlayers: List<PlayerId>
        get() = players.filter { it.refreshed }.map { it.playerId }
}

/** Legacy full-Battle cleanup plus v38 per-Strike cleanup/final Refresh. */
class BattleCleanupCoordinator(
    private val refreshResolver: RefreshResolver
) {
    fun execute(
        game: Game,
        battleState: BattleState
    ): BattleCleanupResult {
        val resultByPlayer = linkedMapOf<PlayerId, MutablePlayerCleanup>()
        battleState.playersInBattleOrder.forEach { player ->
            resultByPlayer[player.id] = MutablePlayerCleanup()
        }

        /*
         * Dice still on the Grid are still the exact live dice in Hand.
         * Remove each exact Hand identity and send it to Discard.
         */
        battleState.grid.diePlacements.toList().forEach { placement ->
            val player = battleState.player(placement.playerId)

            stateCheck(
                battleState.grid.removeDie(placement.die) != null,
                context = "BattleCleanupCoordinator"
            ) {
                "Battle cleanup could not remove Grid die: $placement"
            }

            val removed = player.dice.removeExactFromHand(placement.die)
            stateCheck(
                removed === placement.die,
                context = "BattleCleanupCoordinator"
            ) {
                "Battle cleanup could not remove exact Hand die for player ${player.id.value}: ${placement.die}"
            }

            player.dice.addToDiscard(placement.die)
            resultByPlayer.getValue(player.id).discardedDice++
        }

        /* Every committed Battle Critter returns to the shared Grove. */
        battleState.grid.critterPlacements.toList().forEach { placement ->
            val removed = battleState.grid.removeCritter(
                playerId = placement.playerId,
                row = placement.row,
                critter = placement.critter
            )
            stateCheck(
                removed != null,
                context = "BattleCleanupCoordinator"
            ) {
                "Battle cleanup could not remove committed Critter: $placement"
            }

            game.grove.critters.add(placement.critter)
            resultByPlayer.getValue(placement.playerId).returnedCritters++
        }

        val results = battleState.playersInBattleOrder.map { player ->
            stateCheck(
                player.dice.hand.isEmpty(),
                context = "BattleCleanupCoordinator"
            ) {
                "Player ${player.id.value} has ${player.dice.handSize} unplaced Hand dice after Battle Grid cleanup"
            }

            val mutable = resultByPlayer.getValue(player.id)
            var refreshed = false

            game.chronicle.scoped(
                parent = {
                    Moment.Cleanup(
                        playerId = player.id,
                        phase = ChroniclePhase.BATTLE,
                        discardedDice = mutable.discardedDice,
                        returnedCritters = mutable.returnedCritters,
                        refreshed = refreshed
                    )
                }
            ) {
                refreshed = refreshResolver.refreshIfReady(player)

                /* Stored Mulch and temporary Critter values both roll over/reset here. */
                player.tokens.normalize()
                player.critterValues.clearRound()
            }

            BattlePlayerCleanupResult(
                playerId = player.id,
                discardedDice = mutable.discardedDice,
                returnedCritters = mutable.returnedCritters,
                refreshed = refreshed
            )
        }

        stateCheck(
            battleState.grid.diePlacements.isEmpty() &&
                battleState.grid.critterPlacements.isEmpty(),
            context = "BattleCleanupCoordinator"
        ) {
            "Battle Grid still contains placements after cleanup"
        }

        return BattleCleanupResult(results)
    }


    /**
     * v38 Strike cleanup: reclaim only dice and Critters in the settled row.
     * In particular this does NOT Refresh Plants/Butterflies or clear temporary
     * round-wide Critter bonuses; those happen only after the Low Strike.
     */
    fun executeRow(game: Game, battleState: BattleState,
                   row: dugsolutions.leaf.v35.battle.domain.StrikeRow): BattleCleanupResult {
        stateCheck(battleState.grid.isRowClosed(row), context = "BattleCleanupCoordinator") {
            "Cannot clean an unresolved Strike: $row"
        }
        val byPlayer = battleState.playersInBattleOrder.associate { it.id to MutablePlayerCleanup() }
        battleState.grid.withResolvedRowRemoval(row) {
            battleState.grid.diePlacements.filter { it.row == row }.toList().forEach { placement ->
                val player = battleState.player(placement.playerId)
                stateCheck(battleState.grid.removeDie(placement.die) != null,
                    context = "BattleCleanupCoordinator") { "Could not reclaim Strike die: $placement" }
                val removed = player.dice.removeExactFromHand(placement.die)
                stateCheck(removed === placement.die, context = "BattleCleanupCoordinator") {
                    "Strike cleanup lost exact Hand die: $placement"
                }
                player.dice.addToDiscard(placement.die)
                byPlayer.getValue(placement.playerId).discardedDice++
            }
            battleState.grid.critterPlacements.filter { it.row == row }.toList().forEach { placement ->
                stateCheck(battleState.grid.removeCritter(placement.playerId, row, placement.critter) != null,
                    context = "BattleCleanupCoordinator") { "Could not reclaim Strike Critter: $placement" }
                game.grove.critters.add(placement.critter)
                byPlayer.getValue(placement.playerId).returnedCritters++
            }
        }
        stateCheck(battleState.grid.diePlacements.none { it.row == row } &&
            battleState.grid.critterPlacements.none { it.row == row }, context = "BattleCleanupCoordinator") {
            "Settled Strike $row still contains game pieces"
        }
        return BattleCleanupResult(battleState.playersInBattleOrder.map { player ->
            val reclaimed = byPlayer.getValue(player.id)
            game.chronicle.record(Moment.Cleanup(
                playerId = player.id, phase = ChroniclePhase.BATTLE,
                discardedDice = reclaimed.discardedDice,
                returnedCritters = reclaimed.returnedCritters, refreshed = false
            ))
            BattlePlayerCleanupResult(player.id, reclaimed.discardedDice, reclaimed.returnedCritters, false)
        })
    }

    /** v38 Battle end: perform Refresh and round-boundary resets exactly once. */
    fun executeFinalRefresh(game: Game, battleState: BattleState): BattleCleanupResult {
        stateCheck(dugsolutions.leaf.v35.battle.domain.StrikeRow.entries.all {
            battleState.grid.isRowClosed(it)
        } && battleState.grid.diePlacements.isEmpty() &&
            battleState.grid.critterPlacements.isEmpty(), context = "BattleCleanupCoordinator") {
            "Final Refresh requires all Strikes resolved and physically cleared"
        }
        return BattleCleanupResult(battleState.playersInBattleOrder.map { player ->
            stateCheck(player.dice.hand.isEmpty(), context = "BattleCleanupCoordinator") {
                "Player ${player.id.value} has Hand dice after all Strikes"
            }
            var refreshed = false
            game.chronicle.scoped(parent = {
                Moment.Cleanup(playerId = player.id, phase = ChroniclePhase.BATTLE,
                    discardedDice = 0, returnedCritters = 0, refreshed = refreshed)
            }) {
                refreshed = refreshResolver.refreshIfReady(player)
                player.tokens.normalize()
                player.critterValues.clearRound()
            }
            BattlePlayerCleanupResult(player.id, 0, 0, refreshed)
        })
    }

    private class MutablePlayerCleanup(
        var discardedDice: Int = 0,
        var returnedCritters: Int = 0
    )
}
