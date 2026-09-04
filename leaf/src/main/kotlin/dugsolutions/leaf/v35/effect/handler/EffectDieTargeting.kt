package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.random.die.Die

/**
 * Shared die-targeting mechanics used by multiple effect families.
 *
 * These helpers expose immutable choices to strategies, then validate the
 * selected snapshot against the live PlayerDice state immediately before
 * mutation. Equivalent dice intentionally remain interchangeable; no physical
 * die identity is introduced.
 */
internal fun handChoices(
    player: Player,
    predicate: (Die) -> Boolean = { true }
): List<EffectDieChoice> =
    player.dice.hand.mapIndexedNotNull { index, die ->
        if (!predicate(die)) {
            null
        } else {
            EffectDieChoice(
                index = index,
                sides = die.sides,
                value = die.value
            )
        }
    }

internal fun chooseRequiredHandDie(
    request: GameEffectRequest,
    legalChoices: List<EffectDieChoice>
): Die {
    check(legalChoices.isNotEmpty()) {
        "No legal die targets for effect: ${request.effect}"
    }

    val chosen = request.actor.decisions.effect.chooseDie(
        ChooseEffectDieRequest(
            effect = request.effect,
            legalChoices = legalChoices
        )
    )
    check(chosen in legalChoices) {
        "EffectStrategy returned an illegal die choice: $chosen; legal=$legalChoices"
    }

    return resolveHandDie(request.actor, chosen)
}

internal fun chooseOptionalHandDie(
    request: GameEffectRequest,
    legalChoices: List<EffectDieChoice>
): Die? {
    val chosen = request.actor.decisions.effect.chooseOptionalDie(
        ChooseOptionalEffectDieRequest(
            effect = request.effect,
            legalChoices = legalChoices
        )
    )
    check(chosen == null || chosen in legalChoices) {
        "EffectStrategy returned an illegal optional die choice: $chosen"
    }

    return chosen?.let {
        resolveHandDie(request.actor, it)
    }
}

internal fun resolveHandDie(
    player: Player,
    choice: EffectDieChoice
): Die {
    val die = player.dice.hand.getOrNull(choice.index)
    check(
        die != null &&
            die.sides == choice.sides &&
            die.value == choice.value
    ) {
        "Effect die choice is no longer valid: $choice"
    }
    return die
}
