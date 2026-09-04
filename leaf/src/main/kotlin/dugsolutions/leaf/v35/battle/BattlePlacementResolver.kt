package dugsolutions.leaf.v35.battle

import dugsolutions.leaf.v35.battle.domain.BattleDiePlacement
import dugsolutions.leaf.v35.battle.domain.BattleSquare
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleDiePlacementRequest
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.random.die.Die

/**
 * Shared rule boundary for placing a newly added Battle Hand die.
 *
 * The die already belongs to PlayerDice.hand. This resolver only asks the
 * player's BattleStrategy for a legal Strike Row and adds Grid location.
 */
class BattlePlacementResolver {

    fun legalRows(
        battleState: BattleState,
        player: Player
    ): List<StrikeRow> =
        StrikeRow.entries.filter { row ->
            !battleState.grid.isRowClosed(row) &&
                !battleState.grid.isPlayerWithdrawn(player.id, row) &&
                !battleState.grid.square(player.id, row).isFull
        }

    /**
     * Total number of additional dice this player can currently place.
     *
     * This is useful when an effect is known to add several dice and must
     * verify that every successfully added die can receive a legal Grid
     * location before the effect is offered.
     */
    fun availableSlots(
        battleState: BattleState,
        player: Player
    ): Int =
        legalRows(
            battleState = battleState,
            player = player
        ).sumOf { row ->
            BattleSquare.MAX_DICE -
                battleState.grid.square(
                    player.id,
                    row
                ).dieCount
        }

    /**
     * Places a newly added Hand die into one rule-mandated Strike Row.
     *
     * Unlike [placeNewHandDie], no strategy decision occurs because the effect
     * itself determines the destination (for example Reap What You Roll or
     * Transplant Tulip replacing a discarded die in its former square).
     */
    fun placeNewHandDieInRow(
        battleState: BattleState,
        player: Player,
        die: Die,
        row: StrikeRow
    ): BattleDiePlacement {
        stateNotNull(
            player.dice.hand.firstOrNull { it === die },
            context = "BattlePlacementResolver"
        ) {
            "Forced-placement Battle die is not the exact live Hand die for player ${player.id.value}: $die"
        }

        val legalRows = legalRows(
            battleState = battleState,
            player = player
        )

        stateNotNull(
            row.takeIf { it in legalRows },
            context = "BattlePlacementResolver"
        ) {
            "Rule-mandated Strike Row $row cannot accept new Battle die $die; legal=$legalRows"
        }

        return battleState.grid.placeDie(
            player = player,
            row = row,
            die = die
        )
    }

    fun placeNewHandDie(
        battleState: BattleState,
        player: Player,
        die: Die,
        reason: BattleDiePlacementReason
    ): BattleDiePlacement {
        val handIndex =
            player.dice.hand.indexOfFirst { it === die }

        stateNotNull(
            handIndex.takeIf { it >= 0 },
            context = "BattlePlacementResolver"
        ) {
            "New Battle die is not the exact live Hand die for player ${player.id.value}: $die"
        }

        val legalRows = legalRows(battleState, player)
        stateNotNull(
            legalRows.takeIf { it.isNotEmpty() },
            context = "BattlePlacementResolver"
        ) {
            "Player ${player.id.value} has no Strike Square with room for new Battle die $die"
        }

        val chosen =
            player.decisions.battle.chooseDiePlacement(
                ChooseBattleDiePlacementRequest(
                    die = HandDieChoice(
                        index = handIndex,
                        sides = die.sides,
                        value = die.value
                    ),
                    reason = reason,
                    legalRows = legalRows
                )
            )

        decisionCheck(
            chosen in legalRows,
            context = "BattlePlacementResolver"
        ) {
            "BattleStrategy chose illegal Strike Row $chosen for new die; legal=$legalRows"
        }

        return battleState.grid.placeDie(
            player = player,
            row = chosen,
            die = die
        )
    }
}
