package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.error.unsupportedGameEffect
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice
import dugsolutions.leaf.v35.plant.domain.PlantType

/**
 * Related effects that directly change visible die values without moving the
 * die between gameplay zones.
 *
 * Phase-combined cards execute their phase-specific branches against the live
 * Battle Grid when a BattleState is present. Value changes never move dice;
 * row-aware effects derive location from the exact selected live Hand die.
 */
class DieValueEffectHandler : EffectHandler {

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.DOUBLE_ONE_DIE,
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_3 ->
                request.actor.dice.hand.isNotEmpty()

            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        request.actor.dice.hand.isNotEmpty()

                    GameEffectPhase.BATTLE ->
                        battleHandChoices(request).isNotEmpty() &&
                            actorParticipatingStrikeRows(request).isNotEmpty()
                }

            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        request.actor.dice.hand.isNotEmpty()

                    GameEffectPhase.BATTLE ->
                        battleHandChoices(request).isNotEmpty()
                }

            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE ->
                request.actor.dice.hand.any { it.sides > 4 }

            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE ->
                request.actor.dice.hand.any { it.value >= 2 }

            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
            GameEffect.RAISE_ALL_DICE_PLUS_2 ->
                request.actor.dice.hand.isNotEmpty()

            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        true

                    GameEffectPhase.BATTLE ->
                        openStrikeRows(request).isNotEmpty()
                }

            GameEffect.SET_DIE_TO_MATCH_ANOTHER ->
                kindredChoices(request).isNotEmpty()

            GameEffect.SET_DIE_UP_TO_D12_TO_MAX ->
                request.actor.dice.hand.any { it.sides <= 12 }

            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER ->
                request.actor.dice.hand.isNotEmpty() &&
                    request.actor.creature.cards.any {
                        it.card.type == PlantType.VINE ||
                            it.card.type == PlantType.FLOWER
                    }

            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE ->
                request.actor.dice.hand.isNotEmpty() &&
                    request.actor.creature.cards.any {
                        it.card.type == PlantType.ROOT ||
                            it.card.type == PlantType.VINE
                    }

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Die-value handler cannot execute effect: ${request.effect}"
        }

        when (request.effect) {
            GameEffect.DOUBLE_ONE_DIE -> {
                val die = chooseRequiredHandDie(
                    request,
                    handChoices(request.actor)
                )
                die.adjustBy(die.value)
            }

            GameEffect.RAISE_DIE_PLUS_4 ->
                raiseOne(request, 4)

            GameEffect.RAISE_ANY_DIE_PLUS_1 ->
                raiseOne(request, 1)

            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        raiseOne(request, 1)

                    GameEffectPhase.BATTLE ->
                        rootAndScootBattle(request)
                }

            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        raiseOne(request, 1)

                    GameEffectPhase.BATTLE ->
                        bloomBackflipBattle(request)
                }

            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        raiseOne(request, 2)

                    GameEffectPhase.BATTLE ->
                        sappingSnapdragonBattle(request)
                }

            GameEffect.RAISE_DIE_PLUS_3 ->
                raiseOne(request, 3)

            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE -> {
                chooseRequiredHandDie(
                    request,
                    handChoices(request.actor) { it.sides > 4 }
                ).flip()
            }

            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE -> {
                chooseRequiredHandDie(
                    request,
                    handChoices(request.actor) { it.value >= 2 }
                ).adjustTo(1)

                val ones = request.actor.dice.hand.count { it.value == 1 }
                if (ones > 0) {
                    request.actor.addVp(ones)
                }
            }

            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX -> {
                val lowest = request.actor.dice.hand.minOf { it.value }
                chooseRequiredHandDie(
                    request,
                    handChoices(request.actor) { it.value == lowest }
                ).adjustToMax()
            }

            GameEffect.RAISE_ALL_DICE_PLUS_2 ->
                request.actor.dice.hand.forEach { it.adjustBy(2) }

            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        chooseOptionalHandDie(
                            request,
                            handChoices(request.actor)
                        )?.adjustTo(3)

                    GameEffectPhase.BATTLE ->
                        vineAndPunishmentBattle(request)
                }

            GameEffect.SET_DIE_TO_MATCH_ANOTHER -> {
                val legalChoices = kindredChoices(request)
                val chosen = request.actor.decisions.effect.chooseDiePair(
                    ChooseEffectDiePairRequest(
                        effect = request.effect,
                        legalChoices = legalChoices,
                        context = request.decisionContext()
                    )
                )
                decisionCheck(chosen in legalChoices) {
                    "EffectStrategy returned illegal Root Kindred pair: " +
                        "$chosen; legal=$legalChoices"
                }

                /* No state mutates between resolving source and target. */
                val source = resolveHandDie(request.actor, chosen.source)
                val target = resolveHandDie(request.actor, chosen.target)
                target.adjustTo(source.value)
            }

            GameEffect.SET_DIE_UP_TO_D12_TO_MAX ->
                chooseRequiredHandDie(
                    request,
                    handChoices(request.actor) { it.sides <= 12 }
                ).adjustToMax()

            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER -> {
                val count = request.actor.creature.cards.count {
                    it.card.type == PlantType.VINE ||
                        it.card.type == PlantType.FLOWER
                }
                repeatRaises(request, count)
            }

            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE -> {
                val count = request.actor.creature.cards.count {
                    it.card.type == PlantType.ROOT ||
                        it.card.type == PlantType.VINE
                }
                repeatRaises(request, count)
            }

            else -> unsupportedGameEffect(
                "Unsupported effect reached DieValueEffectHandler: ${request.effect}"
            )
        }
    }

    private fun rootAndScootBattle(
        request: GameEffectRequest
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "RootAndScoot"
        )

        /* Printed order: first Raise any die +1, then choose the Strike to leave. */
        chooseRequiredHandDie(
            request = request,
            legalChoices = battleHandChoices(request)
        ).adjustBy(1)

        val row = chooseRequiredStrikeRow(
            request = request,
            legalChoices = actorParticipatingStrikeRows(request)
        )
        val square = battleState.grid.square(request.actor.id, row)
        val diceToDiscard = square.dice
        val crittersToReturn = square.critters

        diceToDiscard.forEach { die ->
            stateCheck(
                battleState.grid.removeDie(die) != null,
                context = "RootAndScoot"
            ) {
                "Root & Scoot could not remove Battle die from row $row: $die"
            }

            val removed = request.actor.dice.removeExactFromHand(die)
            stateCheck(
                removed === die,
                context = "RootAndScoot"
            ) {
                "Root & Scoot could not remove exact Hand die from row $row: $die"
            }
            request.actor.dice.addToDiscard(die)
        }

        crittersToReturn.forEach { critter ->
            stateCheck(
                battleState.grid.removeCritter(
                    playerId = request.actor.id,
                    row = row,
                    critter = critter
                ) != null,
                context = "RootAndScoot"
            ) {
                "Root & Scoot could not remove committed Critter $critter from row $row"
            }
            request.game.grove.critters.add(critter)
        }

        battleState.grid.withdrawPlayer(
            playerId = request.actor.id,
            row = row
        )
    }

    private fun vineAndPunishmentBattle(
        request: GameEffectRequest
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "VineAndPunishment"
        )
        val row = chooseRequiredStrikeRow(
            request = request,
            legalChoices = openStrikeRows(request)
        )

        battleState.playersInBattleOrder
            .filter { it.id != request.actor.id }
            .forEach { opponent ->
                battleState.grid.square(
                    opponent.id,
                    row
                ).dice.forEach { die ->
                    die.adjustBy(-3)
                }
            }
    }

    private fun bloomBackflipBattle(
        request: GameEffectRequest
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "BloomBackflip"
        )
        val die = chooseRequiredHandDie(
            request = request,
            legalChoices = battleHandChoices(request)
        )

        die.adjustBy(1)

        val location = stateNotNull(
            battleState.grid.locationOf(die),
            context = "BloomBackflip"
        ) {
            "Chosen Battle die lost its Strike Row: $die"
        }

        battleState.playersInBattleOrder
            .filter { it.id != request.actor.id }
            .forEach { opponent ->
                battleState.grid.square(
                    opponent.id,
                    location.row
                ).dice
                    .filter { opposing ->
                        opposing.value > die.value &&
                            opposing.sides > 4
                    }
                    .forEach { opposing ->
                        opposing.flip()
                    }
            }
    }

    private fun sappingSnapdragonBattle(
        request: GameEffectRequest
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "SappingSnapdragon"
        )
        val die = chooseRequiredHandDie(
            request = request,
            legalChoices = battleHandChoices(request)
        )

        die.adjustBy(2)

        val location = stateNotNull(
            battleState.grid.locationOf(die),
            context = "SappingSnapdragon"
        ) {
            "Chosen Battle die lost its Strike Row: $die"
        }

        var totalReduced = 0
        battleState.playersInBattleOrder
            .filter { it.id != request.actor.id }
            .forEach { opponent ->
                battleState.grid.square(
                    opponent.id,
                    location.row
                ).dice.forEach { opposing ->
                    val before = opposing.value
                    opposing.adjustBy(-2)
                    totalReduced += before - opposing.value
                }
            }

        die.adjustBy(totalReduced)
    }

    private fun kindredChoices(
        request: GameEffectRequest
    ): List<EffectDiePairChoice> {
        val dice = handChoices(request.actor)
        return dice.flatMap { source ->
            dice.mapNotNull { target ->
                if (
                    source.index == target.index ||
                    source.value > target.sides
                ) {
                    null
                } else {
                    EffectDiePairChoice(
                        source = source,
                        target = target
                    )
                }
            }
        }
    }

    private fun raiseOne(
        request: GameEffectRequest,
        amount: Int
    ) {
        chooseRequiredHandDie(
            request = request,
            legalChoices = handChoices(request.actor)
        ).adjustBy(amount)
    }

    private fun repeatRaises(
        request: GameEffectRequest,
        count: Int
    ) {
        repeat(count) {
            /* Rebuild choices each time so the strategy sees current values. */
            chooseRequiredHandDie(
                request = request,
                legalChoices = handChoices(request.actor)
            ).adjustBy(1)
        }
    }
}
