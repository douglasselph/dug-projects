package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectBattleDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCrossPlayerDieSwapRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBattleDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectCrossPlayerDieSwapChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.random.die.Die

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

/**
 * Every live die currently controlled in an open Battle row, including
 * opponents' dice. The Hand index is relative to that die's current owner.
 */
internal fun battleDieChoices(
    request: GameEffectRequest
): List<EffectBattleDieChoice> {
    val battleState = request.battleState ?: return emptyList()

    return battleState.grid.diePlacements.mapNotNull { placement ->
        if (
            battleState.grid.isRowClosed(placement.row) ||
            battleState.grid.isPlayerWithdrawn(
                placement.playerId,
                placement.row
            )
        ) {
            return@mapNotNull null
        }

        val owner = battleState.player(placement.playerId)
        val index = owner.dice.hand.indexOfFirst { it === placement.die }
        stateCheck(index >= 0, context = "EffectBattleTargeting") {
            "Battle Grid die is not owned by its controlling player's Hand: $placement"
        }

        EffectBattleDieChoice(
            ownerId = owner.id,
            row = placement.row,
            die = EffectDieChoice(
                index = index,
                sides = placement.die.sides,
                value = placement.die.value
            )
        )
    }
}

/** All legal same-size actor/opponent Battle-die pairings for Pollen Theft. */
internal fun crossPlayerSameSizeSwapChoices(
    request: GameEffectRequest
): List<EffectCrossPlayerDieSwapChoice> {
    val all = battleDieChoices(request)
    val own = all.filter { it.ownerId == request.actor.id }
    val opponents = all.filter { it.ownerId != request.actor.id }

    return buildList {
        own.forEach { ownDie ->
            opponents
                .filter { it.die.sides == ownDie.die.sides }
                .forEach { opponentDie ->
                    add(
                        EffectCrossPlayerDieSwapChoice(
                            ownDie = ownDie,
                            opponentDie = opponentDie
                        )
                    )
                }
        }
    }
}

internal fun chooseRequiredBattleDie(
    request: GameEffectRequest,
    legalChoices: List<EffectBattleDieChoice>
): EffectBattleDieChoice {
    effectCheck(legalChoices.isNotEmpty()) {
        "No legal Battle die targets for effect: ${request.effect}"
    }

    val chosen = request.actor.decisions.effect.chooseBattleDie(
        ChooseEffectBattleDieRequest(
            effect = request.effect,
            legalChoices = legalChoices
        )
    )

    decisionCheck(chosen in legalChoices) {
        "EffectStrategy returned illegal Battle die: $chosen; legal=$legalChoices"
    }

    return chosen
}

internal fun chooseRequiredCrossPlayerDieSwap(
    request: GameEffectRequest,
    legalChoices: List<EffectCrossPlayerDieSwapChoice>
): EffectCrossPlayerDieSwapChoice {
    effectCheck(legalChoices.isNotEmpty()) {
        "No legal cross-player die swaps for effect: ${request.effect}"
    }

    val chosen = request.actor.decisions.effect.chooseCrossPlayerDieSwap(
        ChooseEffectCrossPlayerDieSwapRequest(
            effect = request.effect,
            legalChoices = legalChoices
        )
    )

    decisionCheck(chosen in legalChoices) {
        "EffectStrategy returned illegal cross-player die swap: " +
            "$chosen; legal=$legalChoices"
    }

    return chosen
}

/** Resolve and revalidate one immutable Battle target against current state. */
internal fun resolveBattleDieChoice(
    request: GameEffectRequest,
    choice: EffectBattleDieChoice
): Pair<Player, Die> {
    val battleState = battleStateForEffect(
        request = request,
        context = "EffectBattleTargeting"
    )
    val owner = battleState.player(choice.ownerId)
    val die = owner.dice.hand.getOrNull(choice.die.index)

    decisionCheck(
        die != null &&
            die.sides == choice.die.sides &&
            die.value == choice.die.value,
        context = "EffectBattleTargeting"
    ) {
        "Battle die choice is no longer valid in owner ${choice.ownerId.value}'s Hand: $choice"
    }

    val location = battleState.grid.locationOf(die)
    decisionCheck(
        location?.playerId == choice.ownerId &&
            location.row == choice.row &&
            !battleState.grid.isRowClosed(choice.row) &&
            !battleState.grid.isPlayerWithdrawn(choice.ownerId, choice.row),
        context = "EffectBattleTargeting"
    ) {
        "Battle die choice is no longer valid on the Grid: $choice"
    }

    return owner to die
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
