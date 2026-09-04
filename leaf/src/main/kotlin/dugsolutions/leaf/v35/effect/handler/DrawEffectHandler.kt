package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattlePlacementResolver
import dugsolutions.leaf.v35.error.unsupportedGameEffect
import dugsolutions.leaf.v35.error.effectCheck
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.RollRewardPolicy
import dugsolutions.leaf.v35.player.decision.battle.BattleDiePlacementReason
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice
import dugsolutions.leaf.v35.random.die.Die

/**
 * Effects whose main behavior is Draw / reroll / discard-redraw.
 *
 * During Battle, any die newly added to Dice Hand must also receive a Battle
 * Grid location. [BattlePlacementResolver] owns that phase-specific placement
 * rule; this handler invokes it only after the die has been rolled and its Roll
 * Reward has resolved.
 */
class DrawEffectHandler(
    private val battlePlacementResolver: BattlePlacementResolver =
        BattlePlacementResolver()
) : EffectHandler {

    private companion object {
        const val MAX_REROLL_ATTEMPTS = 1_000
    }

    override fun canExecute(
        request: GameEffectRequest
    ): Boolean =
        when (request.effect) {
            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> true
                    GameEffectPhase.BATTLE -> request.actor.dice.hand.isNotEmpty()
                }

            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS ->
                request.actor.dice.hand.isNotEmpty()

            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        request.actor.dice.hand.isNotEmpty()

                    GameEffectPhase.BATTLE ->
                        battleHandChoices(request).isNotEmpty() &&
                            openStrikeRows(request).isNotEmpty()
                }

            GameEffect.DRAW_TWO_DICE ->
                hasDrawableDie(request) &&
                    hasBattlePlacementCapacity(
                        request = request,
                        diceToAdd = minOf(
                            2,
                            drawableDieCount(request)
                        )
                    )

            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        request.actor.dice.hand.isNotEmpty()

                    GameEffectPhase.BATTLE ->
                        battleHandChoices(request).isNotEmpty()
                }

            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION ->
                        request.actor.dice.hand.isNotEmpty()

                    GameEffectPhase.BATTLE ->
                        battleHandChoices(request).isNotEmpty() &&
                            hasReapBattleCapacity(request)
                }

            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE ->
                burstingBlossomChoices(request).isNotEmpty()

            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND ->
                request.actor.dice.discard.isNotEmpty() &&
                    hasBattlePlacementCapacity(
                        request = request,
                        diceToAdd = 1
                    )

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        effectCheck(canExecute(request)) {
            "Draw handler cannot execute effect: ${request.effect}"
        }

        val rollResolver = rollResolver(request, executor)

        when (request.effect) {
            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> rootRecallCultivation(
                        request = request,
                        rollResolver = rollResolver
                    )

                    GameEffectPhase.BATTLE -> {
                        val die = chooseRequiredHandDie(
                            request,
                            handChoices(request.actor)
                        )
                        /* Reroll retains the live die in its Battle location. */
                        rollResolver.roll(request.actor, die)
                    }
                }

            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS -> {
                val die = chooseRequiredHandDie(
                    request,
                    handChoices(request.actor)
                )
                var attempts = 0
                do {
                    effectCheck(attempts++ < MAX_REROLL_ATTEMPTS) {
                        "Reroll-until-3+ exceeded $MAX_REROLL_ATTEMPTS attempts"
                    }
                    rollResolver.roll(
                        request.actor,
                        die,
                        RollRewardPolicy.IGNORE
                    )
                } while (die.value < 3)
            }

            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> {
                        val die = chooseRequiredHandDie(
                            request,
                            handChoices(request.actor)
                        )
                        rollResolver.roll(request.actor, die)
                    }

                    GameEffectPhase.BATTLE ->
                        gustOfPetalsBattle(
                            request = request,
                            rollResolver = rollResolver
                        )
                }

            GameEffect.DRAW_TWO_DICE ->
                repeat(2) {
                    drawAndPlaceIfBattle(
                        request = request,
                        rollResolver = rollResolver
                    )
                }

            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> {
                        discardChosenHandDie(request)
                        rollResolver.draw(request.actor)
                    }

                    GameEffectPhase.BATTLE ->
                        transplantTulipBattle(
                            request = request,
                            rollResolver = rollResolver
                        )
                }

            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE ->
                when (request.phase) {
                    GameEffectPhase.CULTIVATION -> {
                        discardChosenHandDie(request)
                        repeat(2) {
                            rollResolver.draw(request.actor)
                        }
                    }

                    GameEffectPhase.BATTLE ->
                        reapWhatYouRollBattle(
                            request = request,
                            rollResolver = rollResolver
                        )
                }

            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE -> {
                chooseRequiredHandDie(
                    request,
                    burstingBlossomChoices(request)
                ).adjustBy(1)

                val maxCount = request.actor.dice.hand.count {
                    it.value == it.sides
                }
                repeat(maxCount) {
                    drawAndPlaceIfBattle(
                        request = request,
                        rollResolver = rollResolver
                    )
                }
            }

            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND -> {
                val die = chooseRequiredDiscardDie(
                    request,
                    discardChoices(request.actor)
                )
                stateCheck(request.actor.dice.removeFromDiscard(die) != null) {
                    "Selected Discard die could not be removed: $die"
                }
                request.actor.dice.addToHand(die)
                rollResolver.roll(request.actor, die)
                placeIfBattle(
                    request = request,
                    die = die
                )
            }

            else -> unsupportedGameEffect(
                "Unsupported effect reached DrawEffectHandler: ${request.effect}"
            )
        }
    }

    private data class DiscardedBattleDie(
        val die: Die,
        val row: dugsolutions.leaf.v35.battle.domain.StrikeRow
    )

    private fun transplantTulipBattle(
        request: GameEffectRequest,
        rollResolver: RollResolver
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "TransplantTulip"
        )
        val discarded = discardBattleDie(
            request = request,
            context = "TransplantTulip"
        )

        val replacement =
            rollResolver.draw(request.actor)?.die

        if (replacement != null) {
            battlePlacementResolver.placeNewHandDieInRow(
                battleState = battleState,
                player = request.actor,
                die = replacement,
                row = discarded.row
            )
        }

        val legalPairs =
            optionalBattleSwapPairs(request)

        val chosen =
            request.actor.decisions.effect.chooseOptionalDiePair(
                ChooseOptionalEffectDiePairRequest(
                    effect = request.effect,
                    legalChoices = legalPairs
                )
            )

        decisionCheck(
            chosen == null || chosen in legalPairs
        ) {
            "EffectStrategy returned illegal Transplant Tulip swap pair: " +
                "$chosen; legal=$legalPairs"
        }

        if (chosen != null) {
            val (first, second) =
                resolveHandDice(
                    player = request.actor,
                    choices = listOf(
                        chosen.source,
                        chosen.target
                    )
                )

            val firstLocation =
                battleState.grid.locationOf(first)
            val secondLocation =
                battleState.grid.locationOf(second)

            decisionCheck(
                firstLocation != null &&
                    secondLocation != null &&
                    firstLocation.playerId == request.actor.id &&
                    secondLocation.playerId == request.actor.id &&
                    firstLocation.row != secondLocation.row
            ) {
                "Transplant Tulip swap pair is no longer legal: $chosen"
            }

            battleState.grid.swapDieLocations(
                first = first,
                second = second
            )
        }
    }

    private fun reapWhatYouRollBattle(
        request: GameEffectRequest,
        rollResolver: RollResolver
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "ReapWhatYouRoll"
        )
        val discarded = discardBattleDie(
            request = request,
            context = "ReapWhatYouRoll"
        )

        val drawn =
            buildList {
                repeat(2) {
                    rollResolver.draw(request.actor)?.die?.let(::add)
                }
            }

        if (drawn.isEmpty()) {
            return
        }

        val replacement =
            if (drawn.size == 1) {
                drawn.single()
            } else {
                chooseRequiredHandDie(
                    request = request,
                    legalChoices = choicesForExactHandDice(
                        request = request,
                        dice = drawn
                    )
                )
            }

        battlePlacementResolver.placeNewHandDieInRow(
            battleState = battleState,
            player = request.actor,
            die = replacement,
            row = discarded.row
        )

        drawn
            .filter { it !== replacement }
            .forEach { die ->
                battlePlacementResolver.placeNewHandDie(
                    battleState = battleState,
                    player = request.actor,
                    die = die,
                    reason = BattleDiePlacementReason.EFFECT
                )
            }
    }

    private fun discardBattleDie(
        request: GameEffectRequest,
        context: String
    ): DiscardedBattleDie {
        val battleState = battleStateForEffect(
            request = request,
            context = context
        )
        val die = chooseRequiredHandDie(
            request = request,
            legalChoices = battleHandChoices(request)
        )
        val placement =
            stateNotNull(
                battleState.grid.placementOf(die),
                context = context
            ) {
                "$context selected Battle die lost its Grid location: $die"
            }

        decisionCheck(
            placement.playerId == request.actor.id
        ) {
            "$context selected a die not owned by the actor: $placement"
        }

        stateCheck(
            battleState.grid.removeDie(die) != null
        ) {
            "$context could not remove selected die from Battle Grid: $die"
        }
        stateCheck(
            request.actor.dice.removeExactFromHand(die) != null
        ) {
            "$context could not remove selected exact die from Hand: $die"
        }
        request.actor.dice.addToDiscard(die)

        return DiscardedBattleDie(
            die = die,
            row = placement.row
        )
    }

    private fun choicesForExactHandDice(
        request: GameEffectRequest,
        dice: List<Die>
    ): List<EffectDieChoice> =
        request.actor.dice.hand.mapIndexedNotNull { index, die ->
            if (dice.none { candidate -> candidate === die }) {
                null
            } else {
                EffectDieChoice(
                    index = index,
                    sides = die.sides,
                    value = die.value
                )
            }
        }

    private fun optionalBattleSwapPairs(
        request: GameEffectRequest
    ): List<EffectDiePairChoice> {
        val battleState =
            request.battleState
                ?: return emptyList()
        val choices =
            battleHandChoices(request)

        return buildList {
            for (firstIndex in choices.indices) {
                for (secondIndex in (firstIndex + 1) until choices.size) {
                    val firstChoice = choices[firstIndex]
                    val secondChoice = choices[secondIndex]
                    val first =
                        request.actor.dice.hand.getOrNull(
                            firstChoice.index
                        ) ?: continue
                    val second =
                        request.actor.dice.hand.getOrNull(
                            secondChoice.index
                        ) ?: continue
                    val firstLocation =
                        battleState.grid.locationOf(first)
                    val secondLocation =
                        battleState.grid.locationOf(second)

                    if (
                        firstLocation != null &&
                        secondLocation != null &&
                        firstLocation.playerId == request.actor.id &&
                        secondLocation.playerId == request.actor.id &&
                        firstLocation.row != secondLocation.row
                    ) {
                        add(
                            EffectDiePairChoice(
                                source = firstChoice,
                                target = secondChoice
                            )
                        )
                    }
                }
            }
        }
    }

    private fun hasReapBattleCapacity(
        request: GameEffectRequest
    ): Boolean {
        val battleState =
            request.battleState
                ?: return false

        /*
         * Discarding the selected Grid die always creates one slot. Reap then
         * draws up to two dice, so only the net extra die needs pre-existing
         * Grid capacity.
         */
        val draws =
            minOf(
                2,
                drawableDieCount(request)
            )

        return battlePlacementResolver.availableSlots(
            battleState = battleState,
            player = request.actor
        ) + 1 >= draws
    }

    private fun gustOfPetalsBattle(
        request: GameEffectRequest,
        rollResolver: RollResolver
    ) {
        val battleState = battleStateForEffect(
            request = request,
            context = "GustOfPetals"
        )

        val actorDie = chooseRequiredHandDie(
            request = request,
            legalChoices = battleHandChoices(request)
        )
        rollResolver.roll(
            player = request.actor,
            die = actorDie
        )

        /*
         * The row choice occurs after the initial reroll, matching the card's
         * ordered text. Determine every forced opposing reroll before any of
         * those rerolls resolve so later Roll Rewards cannot change the target
         * set partway through the effect.
         */
        val row = chooseRequiredStrikeRow(
            request = request,
            legalChoices = openStrikeRows(request)
        )
        val ownDice = battleState.grid.square(
            request.actor.id,
            row
        ).dice

        val qualifying =
            if (ownDice.isEmpty()) {
                emptyList()
            } else {
                val lowestOwnValue = ownDice.minOf { it.value }
                battleState.playersInBattleOrder
                    .filter { it.id != request.actor.id }
                    .flatMap { opponent ->
                        battleState.grid.square(
                            opponent.id,
                            row
                        ).dice
                            .filter { it.value > lowestOwnValue }
                            .map { opponent to it }
                    }
            }

        qualifying.forEach { (opponent, die) ->
            rollResolver.roll(
                player = opponent,
                die = die
            )
        }
    }

    private fun rootRecallCultivation(
        request: GameEffectRequest,
        rollResolver: RollResolver
    ) {
        val legalChoices = handChoices(request.actor)
        val chosen = request.actor.decisions.effect.chooseDice(
            ChooseEffectDiceRequest(
                effect = request.effect,
                legalChoices = legalChoices,
                minChoices = 0,
                maxChoices = legalChoices.size
            )
        )

        decisionCheck(chosen.selected.size <= legalChoices.size) {
            "Root Recall selected too many dice: ${chosen.selected}"
        }
        decisionCheck(chosen.selected.all { it in legalChoices }) {
            "EffectStrategy returned illegal Root Recall dice: " +
                "${chosen.selected}; legal=$legalChoices"
        }
        decisionCheck(chosen.selected.map { it.index }.distinct().size == chosen.selected.size) {
            "Root Recall selected the same die more than once: ${chosen.selected}"
        }

        /* Resolve the complete subset before any removal shifts Hand indices. */
        val selectedDice = resolveHandDice(
            player = request.actor,
            choices = chosen.selected
        )

        selectedDice.forEach { die ->
            stateCheck(request.actor.dice.removeFromHand(die) != null) {
                "Validated Root Recall die could not be removed from Hand: $die"
            }
            request.actor.dice.addToDiscard(die)
        }

        repeat(selectedDice.size) {
            rollResolver.draw(request.actor)
        }
    }

    private fun discardChosenHandDie(
        request: GameEffectRequest
    ) {
        val die = chooseRequiredHandDie(
            request,
            handChoices(request.actor)
        )
        stateCheck(request.actor.dice.removeFromHand(die) != null) {
            "Selected Hand die could not be discarded: $die"
        }
        request.actor.dice.addToDiscard(die)
    }

    private fun drawAndPlaceIfBattle(
        request: GameEffectRequest,
        rollResolver: RollResolver
    ) {
        val resolution =
            rollResolver.draw(request.actor)
                ?: return

        placeIfBattle(
            request = request,
            die = resolution.die
        )
    }

    private fun placeIfBattle(
        request: GameEffectRequest,
        die: Die
    ) {
        if (request.phase != GameEffectPhase.BATTLE) {
            return
        }

        val battleState =
            stateNotNull(
                request.battleState,
                context = "DrawEffectHandler"
            ) {
                "Battle effect ${request.effect} requires BattleState to place die $die"
            }

        battlePlacementResolver.placeNewHandDie(
            battleState = battleState,
            player = request.actor,
            die = die,
            reason = BattleDiePlacementReason.EFFECT
        )
    }

    /**
     * Legal die targets for Bursting Blossom.
     *
     * In Cultivation every Hand die remains legal. In Battle a target is only
     * offered when every die that can actually be Drawn after that +1 Raise
     * can also be placed in an open Strike Square with room.
     */
    private fun burstingBlossomChoices(
        request: GameEffectRequest
    ): List<EffectDieChoice> {
        val choices = handChoices(request.actor)

        if (request.phase != GameEffectPhase.BATTLE) {
            return choices
        }

        val battleState =
            request.battleState
                ?: return emptyList()

        val availableSlots =
            battlePlacementResolver.availableSlots(
                battleState = battleState,
                player = request.actor
            )
        val drawableCount = drawableDieCount(request)

        return choices.filter { choice ->
            val die =
                request.actor.dice.hand.getOrNull(
                    choice.index
                ) ?: return@filter false

            val maxAfterRaise =
                request.actor.dice.hand.count { candidate ->
                    if (candidate === die) {
                        minOf(
                            candidate.sides,
                            candidate.value + 1
                        ) == candidate.sides
                    } else {
                        candidate.value == candidate.sides
                    }
                }

            minOf(
                maxAfterRaise,
                drawableCount
            ) <= availableSlots
        }
    }

    private fun hasBattlePlacementCapacity(
        request: GameEffectRequest,
        diceToAdd: Int
    ): Boolean {
        if (request.phase != GameEffectPhase.BATTLE) {
            return true
        }

        val battleState =
            request.battleState
                ?: return false

        return battlePlacementResolver.availableSlots(
            battleState = battleState,
            player = request.actor
        ) >= diceToAdd
    }

    private fun drawableDieCount(
        request: GameEffectRequest
    ): Int =
        request.actor.dice.supplySize +
            request.actor.dice.discardSize

    private fun hasDrawableDie(
        request: GameEffectRequest
    ): Boolean =
        drawableDieCount(request) > 0

    private fun rollResolver(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ): RollResolver =
        RollResolver(
            grove = request.game.grove,
            chronicle = request.game.chronicle,
            immediateWispHandler = { player, card ->
                executor.execute(
                    GameEffectRequest(
                        game = request.game,
                        actor = player,
                        effect = card.effect,
                        source = GameEffectSource.Wisp(card),
                        phase = request.phase,
                        battleState = request.battleState,
                        plantEffectPath = request.plantEffectPath
                    )
                )
            }
        )
}
