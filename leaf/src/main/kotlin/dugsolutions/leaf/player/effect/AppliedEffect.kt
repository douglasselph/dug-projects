package dugsolutions.leaf.player.effect

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CardOrDie
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.die.DieSides

/**
 * Represents an effect that can be applied to a player.
 * These effects are the result of processing a CardEffect and represent
 * concrete actions that will modify the player's state.
 */
sealed class AppliedEffect(
        open val trashAfterUse: CardID? = null
) {

    data class AddToTotal(
        val amount: Int,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class AdjustDieRoll(
        val adjustment: Int,
        val canTargetPlayer: Boolean = false,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class AdjustDieToMax(
        override val trashAfterUse: CardID? = null,
        val minOrMax: Boolean = false
    ) : AppliedEffect(trashAfterUse)

    data class Discard(
        val count: Int,
        val cardsOnly: Boolean = false,
        val diceOnly: Boolean = false,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class DeflectDamage(
        val amount: Int,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class DrawThenDiscard(
        val drawCount: Int,
        val discardCount: Int,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class DrawCards(
        val count: Int,
        val fromCompost: Boolean = false,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class DrawDice(
        val count: Int,
        val fromCompost: Boolean = false,
        val drawHighest: Boolean = false,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class FlourishOverride(
        override val trashAfterUse: CardID? = null
    ): AppliedEffect(trashAfterUse)

    data class MarketBenefit(
        val type: FlourishType? = null,
        val costReduction: Int = 0,
        val isFree: Boolean = false,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class RetainCard(
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class RetainDie(
        val withReroll: Boolean = false,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class RerollDie(
        val count: Int,
        val mustAcceptSecond: Boolean = false,
        val takeBetter: Boolean = false,
        val forceMax: Boolean = false,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class Reuse(
        val cardOrDie: CardOrDie,
        val flourishType: FlourishType? = null,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class Replay(
        val flourishType: FlourishType? = null,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class ThornEffect(
        val damage: Int,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class UpgradeDie(
        val discardAfterUse: Boolean = false,
        val only: List<DieSides> = listOf(),
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)

    data class UseOpponent(
        val cardOrDie: CardOrDie,
        override val trashAfterUse: CardID? = null
    ) : AppliedEffect(trashAfterUse)
} 
