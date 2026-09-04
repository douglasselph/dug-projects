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
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
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
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.actor.dice.hand.isNotEmpty()

            GameEffect.DRAW_TWO_DICE ->
                hasDrawableDie(request) &&
                    hasBattlePlacementCapacity(
                        request = request,
                        diceToAdd = minOf(
                            2,
                            drawableDieCount(request)
                        )
                    )

            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE ->
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.actor.dice.hand.isNotEmpty()

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

            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW -> {
                val die = chooseRequiredHandDie(
                    request,
                    handChoices(request.actor)
                )
                rollResolver.roll(request.actor, die)
            }

            GameEffect.DRAW_TWO_DICE ->
                repeat(2) {
                    drawAndPlaceIfBattle(
                        request = request,
                        rollResolver = rollResolver
                    )
                }

            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE -> {
                discardChosenHandDie(request)
                rollResolver.draw(request.actor)
            }

            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE -> {
                discardChosenHandDie(request)
                repeat(2) {
                    rollResolver.draw(request.actor)
                }
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
