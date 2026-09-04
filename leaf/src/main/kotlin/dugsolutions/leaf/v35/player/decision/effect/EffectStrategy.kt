package dugsolutions.leaf.v35.player.decision.effect

import dugsolutions.leaf.v35.effect.GameEffect

/**
 * Immutable reference to one currently visible die in the actor's Hand.
 *
 * The coordinator/executor validates the snapshot against live PlayerDice
 * before mutating anything. Equivalent dice remain intentionally
 * interchangeable; no physical die ID is introduced.
 */
data class EffectDieChoice(
    val index: Int,
    val sides: Int,
    val value: Int
) {
    init {
        require(index >= 0) { "Effect die index cannot be negative: $index" }
        require(sides > 0) { "Effect die sides must be positive: $sides" }
        require(value > 0) { "Effect die value must be positive: $value" }
    }
}

class ChooseEffectDieRequest(
    val effect: GameEffect,
    legalChoices: List<EffectDieChoice>
) {
    val legalChoices: List<EffectDieChoice> = legalChoices.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Effect die decision requires at least one legal choice: $effect"
        }
    }
}

class ChooseOptionalEffectDieRequest(
    val effect: GameEffect,
    legalChoices: List<EffectDieChoice>
) {
    val legalChoices: List<EffectDieChoice> = legalChoices.toList()
}

/**
 * Focused decision seam for effect-specific die targeting.
 *
 * Strategies choose from immutable legal choices only. The executor validates
 * the returned choice and performs all game-state mutation.
 */
interface EffectStrategy {
    fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice

    /**
     * Used by effects where choosing no die is explicitly legal, such as
     * Wispquake's "you may keep 1 of yours" instruction.
     */
    fun chooseOptionalDie(
        request: ChooseOptionalEffectDieRequest
    ): EffectDieChoice? = request.legalChoices.firstOrNull()
}
