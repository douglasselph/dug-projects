package dugsolutions.leaf.v35.player.decision.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.tokens.Critter

/**
 * Immutable reference to one die in the zone named by the effect request.
 *
 * The executor validates the snapshot against the live PlayerDice zone before
 * mutating anything. Equivalent dice remain intentionally interchangeable; no
 * physical die ID is introduced.
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
 * One decision containing the complete set of dice targeted by an effect.
 *
 * This is used for effects such as Root Recall where selecting which dice are
 * affected is itself the decision. The immutable snapshots make the decision
 * explicit without exposing mutable Die instances to the strategy.
 */
data class EffectDiceChoice(
    val dice: List<EffectDieChoice>
) {
    val selected: List<EffectDieChoice> = dice.toList()

    init {
        require(selected.map { it.index }.distinct().size == selected.size) {
            "Effect dice choice cannot select the same die twice: $selected"
        }
    }
}

class ChooseEffectDiceRequest(
    val effect: GameEffect,
    legalChoices: List<EffectDieChoice>,
    val minChoices: Int = 0,
    val maxChoices: Int = legalChoices.size
) {
    val legalChoices: List<EffectDieChoice> = legalChoices.toList()

    init {
        require(minChoices >= 0) { "Minimum effect-die choices cannot be negative" }
        require(maxChoices >= minChoices) {
            "Maximum effect-die choices must be >= minimum"
        }
        require(maxChoices <= this.legalChoices.size) {
            "Maximum effect-die choices cannot exceed legal choices"
        }
    }
}

/** A complete source + target decision for two-die effects such as Root Kindred. */
data class EffectDiePairChoice(
    val source: EffectDieChoice,
    val target: EffectDieChoice
) {
    init {
        require(source.index != target.index) {
            "Effect source and target must be different dice"
        }
    }
}

class ChooseEffectDiePairRequest(
    val effect: GameEffect,
    legalChoices: List<EffectDiePairChoice>
) {
    val legalChoices: List<EffectDiePairChoice> = legalChoices.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Effect die-pair decision requires at least one legal choice: $effect"
        }
    }
}

/** A complete Critter-to-trash + die-to-target decision for Vine and Dine. */
data class EffectCritterDieChoice(
    val critter: Critter,
    val die: EffectDieChoice
)

class ChooseEffectCritterDieRequest(
    val effect: GameEffect,
    legalChoices: List<EffectCritterDieChoice>
) {
    val legalChoices: List<EffectCritterDieChoice> = legalChoices.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Effect Critter/die decision requires at least one legal choice: $effect"
        }
    }
}


/**
 * Complete branch/target decision for Petal To Die 4.
 *
 * GainD4 has no additional target. TrashD4AndRaiseAll names the exact Hand D4
 * that will be Trashed before all remaining dice are Raised +4.
 */
sealed interface PetalToDie4Choice {
    data object GainD4 : PetalToDie4Choice

    data class TrashD4AndRaiseAll(
        val die: EffectDieChoice
    ) : PetalToDie4Choice
}

class ChoosePetalToDie4Request(
    val effect: GameEffect,
    legalChoices: List<PetalToDie4Choice>
) {
    val legalChoices: List<PetalToDie4Choice> = legalChoices.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Petal To Die 4 requires at least one legal branch"
        }
    }
}

/**
 * Exact source of the Bee gained by Bee-loved Bloom.
 *
 * Opponent identifies the player from whom the Bee will be stolen. The actor
 * is never a legal Opponent source.
 */
sealed interface EffectBeeSourceChoice {
    data object Grove : EffectBeeSourceChoice

    data class Opponent(
        val playerId: PlayerId
    ) : EffectBeeSourceChoice
}

class ChooseBeeSourceRequest(
    val effect: GameEffect,
    legalChoices: List<EffectBeeSourceChoice>
) {
    val legalChoices: List<EffectBeeSourceChoice> = legalChoices.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Bee source decision requires at least one legal source: $effect"
        }
    }
}

/**
 * Focused decision seam for effect-specific targeting.
 *
 * Each request exposes immutable legal choices only. Combined effects return a
 * combined choice object so the strategy explicitly names every target/cost in
 * one decision rather than relying on hidden follow-up choices.
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

    /** Choose any legal subset, including none when the request permits it. */
    fun chooseDice(
        request: ChooseEffectDiceRequest
    ): EffectDiceChoice = EffectDiceChoice(request.legalChoices.take(1))

    /** Choose both source and target as one explicit decision. */
    fun chooseDiePair(
        request: ChooseEffectDiePairRequest
    ): EffectDiePairChoice = request.legalChoices.first()

    /** Choose both the Critter cost and die target as one explicit decision. */
    fun chooseCritterAndDie(
        request: ChooseEffectCritterDieRequest
    ): EffectCritterDieChoice = request.legalChoices.first()

    /** Choose Petal To Die 4's branch, including the D4 target when Trashing. */
    fun choosePetalToDie4(
        request: ChoosePetalToDie4Request
    ): PetalToDie4Choice = request.legalChoices.first()

    /** Choose exactly where Bee-loved Bloom obtains its Bee. */
    fun chooseBeeSource(
        request: ChooseBeeSourceRequest
    ): EffectBeeSourceChoice = request.legalChoices.first()
}
