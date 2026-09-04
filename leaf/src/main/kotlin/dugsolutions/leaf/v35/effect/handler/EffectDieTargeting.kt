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
 * The zone-specific helpers expose immutable choices to strategies, then
 * validate the selected snapshot against the live PlayerDice state immediately
 * before mutation. Equivalent dice intentionally remain interchangeable; no
 * physical die identity is introduced.
 */
internal fun handChoices(
    player: Player,
    predicate: (Die) -> Boolean = { true }
): List<EffectDieChoice> =
    choices(player.dice.hand, predicate)

internal fun discardChoices(
    player: Player,
    predicate: (Die) -> Boolean = { true }
): List<EffectDieChoice> =
    choices(player.dice.discard, predicate)

private fun choices(
    dice: List<Die>,
    predicate: (Die) -> Boolean
): List<EffectDieChoice> =
    dice.mapIndexedNotNull { index, die ->
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
): Die =
    chooseRequiredDie(
        request = request,
        legalChoices = legalChoices,
        resolve = { resolveHandDie(request.actor, it) }
    )

internal fun chooseRequiredDiscardDie(
    request: GameEffectRequest,
    legalChoices: List<EffectDieChoice>
): Die =
    chooseRequiredDie(
        request = request,
        legalChoices = legalChoices,
        resolve = { resolveDiscardDie(request.actor, it) }
    )

private fun chooseRequiredDie(
    request: GameEffectRequest,
    legalChoices: List<EffectDieChoice>,
    resolve: (EffectDieChoice) -> Die
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

    return resolve(chosen)
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
): Die =
    resolveDie(
        dice = player.dice.hand,
        choice = choice,
        zone = "Hand"
    )

/**
 * Resolves several Hand snapshots against one unchanged zone snapshot.
 *
 * This matters for effects such as Root Recall: resolving/removing the first
 * selected die must not shift indices before the remaining choices are
 * validated.
 */
internal fun resolveHandDice(
    player: Player,
    choices: List<EffectDieChoice>
): List<Die> {
    val handSnapshot = player.dice.hand
    check(choices.map { it.index }.distinct().size == choices.size) {
        "Effect selected the same Hand die more than once: $choices"
    }
    return choices.map { choice ->
        resolveDie(
            dice = handSnapshot,
            choice = choice,
            zone = "Hand"
        )
    }
}

internal fun resolveDiscardDie(
    player: Player,
    choice: EffectDieChoice
): Die =
    resolveDie(
        dice = player.dice.discard,
        choice = choice,
        zone = "Discard"
    )

private fun resolveDie(
    dice: List<Die>,
    choice: EffectDieChoice,
    zone: String
): Die {
    val die = dice.getOrNull(choice.index)
    check(
        die != null &&
            die.sides == choice.sides &&
            die.value == choice.value
    ) {
        "Effect die choice is no longer valid in $zone: $choice"
    }
    return die
}
