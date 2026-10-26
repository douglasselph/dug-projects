package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/**
 * Non-Strike Battle benefits expressed on the same decision-point scale as
 * BattleDrawUtility: one expected usable pip = one point. These are explicitly
 * opportunity estimates, not fictitious immediate Strike-row transitions.
 *
 * In particular, a die acquired into Discard cannot be placed this Battle.
 * Its expected pip value is credited only if a future Battle round exists;
 * uncertainty about cycling is represented by a conservative availability
 * factor instead of treating the die like an immediate hand draw.
 */
internal object BattleResourceUtility {
    const val ORIGIN = 35
    const val NON_STRIKE_VP = 3
    private const val WISP_CAP = 5

    fun immediateVp(vp: Int): Int = vp.coerceAtLeast(0) * NON_STRIKE_VP

    fun futureDiscardDie(context: DecisionContext, sides: Int): Int {
        require(sides > 0)
        val futureBattles = context.progress.battleRoundsRemaining
            ?: context.progress.upcomingRoundTypes.count { it.name == "BATTLE" }
        if (futureBattles <= 0) return 0
        // Acquiring a die now has an opportunity cost and it may not cycle into
        // hand: never value it more highly than an immediately drawn die.
        val futureAccess = if (futureBattles >= 2) 0.75 else 0.5
        return ((sides + 1) / 2.0 * futureAccess).roundToInt()
    }

    fun availableWispGain(context: DecisionContext): Int =
        if (context.grove.wispDeckRemaining > 0 && context.self.wispCount < WISP_CAP) 1 else 0

    fun opponentWispTransfers(context: DecisionContext, all: Boolean): Int {
        val count = context.opponents.count { it.wispCount > 0 }
        val capacity = (WISP_CAP - context.self.wispCount).coerceAtLeast(0)
        return minOf(if (all) count else minOf(count, 1), capacity)
    }

    /** Wisp acquisition is option value for future effect/VP, NOT Strike VP. */
    fun wisps(count: Int): Int = count.coerceAtLeast(0) * 5

    /** Denying a Wisp to an opponent has value separate from acquiring it. */
    fun deniedWisps(count: Int): Int = count.coerceAtLeast(0) * 2
}
