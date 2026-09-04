package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.operation.RollResolver
import dugsolutions.leaf.v35.game.operation.RollRewardPolicy
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest

/**
 * Effects whose main behavior is Draw / reroll / discard-redraw.
 *
 * Effects that add a die to Hand are currently executable only during
 * Cultivation. Battle uses the same "Dice Hand" terminology, but newly added
 * dice must also receive a Battle Grid location; that phase-specific placement
 * belongs with the future Battle model rather than being guessed here.
 */
class DrawEffectHandler : EffectHandler {

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
                request.phase == GameEffectPhase.CULTIVATION &&
                    hasDrawableDie(request)

            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE ->
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.actor.dice.hand.isNotEmpty()

            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE ->
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.actor.dice.hand.isNotEmpty()

            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND ->
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.actor.dice.discard.isNotEmpty()

            else -> false
        }

    override fun execute(
        request: GameEffectRequest,
        executor: GameEffectExecutor
    ) {
        check(canExecute(request)) {
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
                    check(attempts++ < MAX_REROLL_ATTEMPTS) {
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
                    rollResolver.draw(request.actor)
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
                    handChoices(request.actor)
                ).adjustBy(1)

                val maxCount = request.actor.dice.hand.count {
                    it.value == it.sides
                }
                repeat(maxCount) {
                    rollResolver.draw(request.actor)
                }
            }

            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND -> {
                val die = chooseRequiredDiscardDie(
                    request,
                    discardChoices(request.actor)
                )
                check(request.actor.dice.removeFromDiscard(die) != null) {
                    "Selected Discard die could not be removed: $die"
                }
                request.actor.dice.addToHand(die)
                rollResolver.roll(request.actor, die)
            }

            else -> error(
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

        check(chosen.selected.size <= legalChoices.size) {
            "Root Recall selected too many dice: ${chosen.selected}"
        }
        check(chosen.selected.all { it in legalChoices }) {
            "EffectStrategy returned illegal Root Recall dice: " +
                "${chosen.selected}; legal=$legalChoices"
        }
        check(chosen.selected.map { it.index }.distinct().size == chosen.selected.size) {
            "Root Recall selected the same die more than once: ${chosen.selected}"
        }

        /* Resolve the complete subset before any removal shifts Hand indices. */
        val selectedDice = resolveHandDice(
            player = request.actor,
            choices = chosen.selected
        )

        selectedDice.forEach { die ->
            check(request.actor.dice.removeFromHand(die) != null) {
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
        check(request.actor.dice.removeFromHand(die) != null) {
            "Selected Hand die could not be discarded: $die"
        }
        request.actor.dice.addToDiscard(die)
    }

    private fun hasDrawableDie(
        request: GameEffectRequest
    ): Boolean =
        !request.actor.dice.isSupplyEmpty ||
            !request.actor.dice.isDiscardEmpty

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
                        phase = request.phase
                    )
                )
            }
        )
}
