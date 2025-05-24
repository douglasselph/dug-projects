package dugsolutions.leaf.player.domain

import dugsolutions.leaf.components.CardID
import dugsolutions.leaf.components.CardOrDie
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.die.DieSides

/**
 * Represents an effect that can be applied to a player.
 * These effects are the result of processing a CardEffect and represent
 * concrete actions that will modify the player's state.
 */
sealed class AppliedEffect {

    data class AddToTotal(
        val amount: Int,
    ) : AppliedEffect()

    data class AdjustDieRoll(
        val adjustment: Int,
        val canTargetPlayer: Boolean = false,
    ) : AppliedEffect()

    data class AdjustDieToMax(
        val minOrMax: Boolean = false
    ) : AppliedEffect()

    data class Adorn(
        val flowerCard: CardID,
    ) : AppliedEffect()

    data class Discard(
        val count: Int,
        val cardsOnly: Boolean = false,
        val diceOnly: Boolean = false,
    ) : AppliedEffect()

    data class DeflectDamage(
        val amount: Int,
    ) : AppliedEffect()

    data class DrawThenDiscard(
        val drawCount: Int,
        val discardCount: Int,
    ) : AppliedEffect()

    data class DrawCards(
        val count: Int,
        val fromCompost: Boolean = false,
    ) : AppliedEffect()

    data class DrawDice(
        val count: Int,
        val fromCompost: Boolean = false,
        val drawHighest: Boolean = false,
    ) : AppliedEffect()

    data object FlourishOverride: AppliedEffect()

    data class MarketBenefit(
        val type: FlourishType? = null,
        val costReduction: Int = 0,
        val isFree: Boolean = false,
    ) : AppliedEffect()

    data object RetainCard : AppliedEffect()

    data class RetainDie(
        val withReroll: Boolean = false,
    ) : AppliedEffect()

    data class RerollDie(
        val count: Int,
        val mustAcceptSecond: Boolean = false,
        val takeBetter: Boolean = false,
        val forceMax: Boolean = false,
    ) : AppliedEffect()

    data class Reuse(
        val cardOrDie: CardOrDie,
        val flourishType: FlourishType? = null,
    ) : AppliedEffect()

    data class Replay(
        val flourishType: FlourishType? = null,
    ) : AppliedEffect()

    data class ResilienceBoost(
        val amount: Int
    ): AppliedEffect()

    data class TrashIfNeeded(
        val card: GameCard
    ) : AppliedEffect()

    data class UpgradeDie(
        val discardAfterUse: Boolean = false,
        val only: List<DieSides> = listOf(),
    ) : AppliedEffect()

    data class UseOpponent(
        val cardOrDie: CardOrDie,
    ) : AppliedEffect()
} 
