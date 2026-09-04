package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.plant.domain.PlantType

/**
 * Related effects that directly change visible die values without moving the
 * die between gameplay zones.
 *
 * Phase-combined cards are deliberately executable only in phases for which
 * their complete behavior is implemented. For example, Sapping Snapdragon's
 * Cultivation +2 is supported now, while its Battle row drain waits for the
 * Battle model rather than silently executing only half the card.
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

            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.actor.dice.hand.isNotEmpty()

            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE ->
                request.actor.dice.hand.any { it.sides > 4 }

            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE ->
                request.actor.dice.hand.any { it.value >= 2 }

            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
            GameEffect.RAISE_ALL_DICE_PLUS_2 ->
                request.actor.dice.hand.isNotEmpty()

            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 ->
                request.phase == GameEffectPhase.CULTIVATION &&
                    request.actor.dice.hand.isNotEmpty()

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
        check(canExecute(request)) {
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

            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                raiseOne(request, 1)

            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW ->
                raiseOne(request, 2)

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
                chooseRequiredHandDie(
                    request,
                    handChoices(request.actor)
                ).adjustTo(3)

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

            else -> error(
                "Unsupported effect reached DieValueEffectHandler: ${request.effect}"
            )
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
