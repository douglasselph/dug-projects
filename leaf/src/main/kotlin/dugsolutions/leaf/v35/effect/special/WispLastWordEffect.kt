package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.StrikeResolver
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.handler.EffectHandler
import dugsolutions.leaf.v35.effect.handler.decisionContextFor
import dugsolutions.leaf.v35.effect.handler.battleStateForEffect
import dugsolutions.leaf.v35.effect.handler.chooseRequiredStrikeRow
import dugsolutions.leaf.v35.effect.handler.openStrikeRows
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.game.operation.WoundResolver

/**
 * Wisp's Last Word resolves one still-open Strike immediately, clears every
 * die/Critter from that row to its normal destination, and then closes the row
 * for the remainder of the Battle Round.
 *
 * The immediate resolution deliberately delegates to [StrikeResolver] so tie,
 * Wound, VP, Root & Scoot withdrawal, and Critter-value rules remain identical
 * to normal Battle Step 6 resolution.
 */
class WispLastWordEffect : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        request.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW &&
            request.phase == GameEffectPhase.BATTLE &&
            request.source is GameEffectSource.Wisp &&
            request.battleState != null &&
            openStrikeRows(request).isNotEmpty()

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Wisp's Last Word is not executable in the current state"
        }
        effectCheck(request.source is GameEffectSource.Wisp) {
            "Wisp's Last Word must be executed from a Wisp source"
        }

        val battleState = battleStateForEffect(
            request = request,
            context = "WispLastWord"
        )
        val row = chooseRequiredStrikeRow(
            request = request,
            legalChoices = openStrikeRows(request)
        )

        /* Printed order: resolve the Strike before removing anything. */
        StrikeResolver(
            WoundResolver(
                grove = request.game.grove,
                chronicle = request.game.chronicle,
                decisionContext = { player -> request.decisionContextFor(player) }
            )
        ).resolveRow(
            game = request.game,
            battleState = battleState,
            row = row
        )

        /*
         * Return every die from the resolved row to its CURRENT controller's
         * Dice Discard Bin. This remains correct after Pollen Theft because the
         * Grid square owner and exact Hand owner move together.
         */
        battleState.playersInBattleOrder.forEach { player ->
            val diceToDiscard = battleState.grid.square(player.id, row).dice
            diceToDiscard.forEach { die ->
                stateCheck(
                    battleState.grid.removeDie(die) != null,
                    context = "WispLastWord"
                ) {
                    "Wisp's Last Word could not remove Battle die from row $row: $die"
                }

                val removed = player.dice.removeExactFromHand(die)
                stateCheck(
                    removed === die,
                    context = "WispLastWord"
                ) {
                    "Wisp's Last Word could not remove exact Hand die for " +
                        "player ${player.id.value} row $row: $die"
                }
                player.dice.addToDiscard(die)
            }

            val crittersToReturn = battleState.grid.square(player.id, row).critters
            crittersToReturn.forEach { critter ->
                stateCheck(
                    battleState.grid.removeCritter(
                        playerId = player.id,
                        row = row,
                        critter = critter
                    ) != null,
                    context = "WispLastWord"
                ) {
                    "Wisp's Last Word could not remove committed Critter " +
                        "$critter for player ${player.id.value} row $row"
                }
                request.game.grove.critters.add(critter)
            }
        }

        battleState.grid.closeRow(row)
    }
}
