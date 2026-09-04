package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice

/**
 * Shared Battle-targeting helpers for effect handlers.
 *
 * Battle decisions expose immutable row/die choices to strategies, while the
 * executor resolves them against the same live [BattleState] immediately
 * before mutation.
 */
internal fun battleStateForEffect(
    request: GameEffectRequest,
    context: String
): BattleState =
    stateNotNull(
        request.battleState,
        context = context
    ) {
        "Battle effect ${request.effect} requires BattleState"
    }

/** Every globally open Strike Row, in normal TOP-to-BOTTOM order. */
internal fun openStrikeRows(
    request: GameEffectRequest
): List<StrikeRow> {
    val battleState = request.battleState ?: return emptyList()
    return StrikeRow.entries.filterNot {
        battleState.grid.isRowClosed(it)
    }
}

/** Every globally open Strike Row from which the actor has not withdrawn. */
internal fun actorParticipatingStrikeRows(
    request: GameEffectRequest
): List<StrikeRow> {
    val battleState = request.battleState ?: return emptyList()
    return openStrikeRows(request).filterNot { row ->
        battleState.grid.isPlayerWithdrawn(request.actor.id, row)
    }
}

/**
 * Actor Hand dice that are currently located in an open Strike Row.
 *
 * Battle value-changing effects need the exact live placed die so the die's
 * row can be derived after the strategy selects it.
 */
internal fun battleHandChoices(
    request: GameEffectRequest
): List<EffectDieChoice> {
    val battleState = request.battleState ?: return emptyList()

    return request.actor.dice.hand.mapIndexedNotNull { index, die ->
        val location = battleState.grid.locationOf(die)
        if (
            location == null ||
            location.playerId != request.actor.id ||
            battleState.grid.isRowClosed(location.row)
        ) {
            null
        } else {
            EffectDieChoice(
                index = index,
                sides = die.sides,
                value = die.value
            )
        }
    }
}

internal fun chooseRequiredStrikeRow(
    request: GameEffectRequest,
    legalChoices: List<StrikeRow>
): StrikeRow {
    effectCheck(legalChoices.isNotEmpty()) {
        "No legal Strike Row targets for effect: ${request.effect}"
    }

    val chosen = request.actor.decisions.effect.chooseStrikeRow(
        ChooseEffectStrikeRowRequest(
            effect = request.effect,
            legalChoices = legalChoices
        )
    )

    decisionCheck(chosen in legalChoices) {
        "EffectStrategy returned illegal Strike Row: $chosen; legal=$legalChoices"
    }

    return chosen
}
