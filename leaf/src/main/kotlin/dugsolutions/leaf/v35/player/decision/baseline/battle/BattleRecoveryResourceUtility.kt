package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore

/**
 * Conservative, observable opportunity bounds for Battle recovery resources.
 * An opportunity is not a promise of a legal future action: Support/Main timing
 * still belongs to the battle orchestrator. Unknown timing earns no guessed value.
 */
internal object BattleRecoveryResourceUtility {
    data class Opportunity(val available: Boolean, val increment: Int, val explanation: String)

    /**
     * Actual Step-5 Final Main: no Support follows the action in this Battle.
     * Existing stock/destinations alone cannot justify an immediate recovery premium.
     * Future Battle utility is deliberately zero until future access is projected.
     */
    fun finalMain(context: DecisionContext, effect: GameEffect): PriorityScore? {
        if (effect != GameEffect.GAIN_TWO_WORMS && effect != GameEffect.REFRESH_CREATURE) return null
        val futureBattles = context.progress.battleRoundsRemaining
            ?: context.progress.upcomingRoundTypes.count { it.name == "BATTLE" }
        if (futureBattles <= 0) return PriorityScore(0).adjusted(0,
            "Final Main closes Battle Support; no future Battle recovery opportunity")
        val opportunity = if (effect == GameEffect.GAIN_TWO_WORMS) worms(context) else refresh(context)
        if (!opportunity.available) return PriorityScore(0).adjusted(0, opportunity.explanation)
        // Conservative future option; no imaginary immediate Strike or Support.
        return PriorityScore(BattleResourceUtility.ORIGIN).adjusted(opportunity.increment,
            "Recovery reserved for future Battle; Support closes after Final Main")
    }

    /**
     * Only credit a same-Battle continuation after a Sunlight-funded Main when
     * the caller provides evidence of a *currently legal* follow-up.
     *
     * The pre-Main legal list cannot predict newly unlocked Support/Plant actions.
     * In that case we conservatively withhold immediate credit. This is a
     * valuation bound, not a change to legal-action generation.
     */
    fun sunlightFundedMain(
        context: DecisionContext,
        effect: GameEffect,
        legalSupports: Collection<BattleSupportAction>,
        legalFinalMains: Collection<BattleMainAction>
    ): PriorityScore? {
        if (effect != GameEffect.GAIN_TWO_WORMS && effect != GameEffect.REFRESH_CREATURE) return null
        val opportunity = when (effect) {
            GameEffect.GAIN_TWO_WORMS -> worms(context)
            else -> refresh(context)
        }
        val confirmed = when (effect) {
            GameEffect.GAIN_TWO_WORMS -> legalSupports.any {
                (it is BattleSupportAction.PlaceCritter && it.critter == Critter.WORM) ||
                    (it is BattleSupportAction.Shared && it.action is SupportAction.UseWormFlip)
            }
            else -> legalFinalMains.any { it is BattleMainAction.ActivatePlant }
        }
        if (!confirmed || !opportunity.available) return PriorityScore(0).adjusted(0,
            "No currently legal follow-up for Sunlight-funded recovery; newly unlocked actions unconfirmed")
        return PriorityScore(BattleResourceUtility.ORIGIN).adjusted(opportunity.increment,
            "Sunlight-funded recovery with observable legal follow-up")
    }

    fun worms(context: DecisionContext): Opportunity {
        val board = context.self.board
        val usable = context.grove.worms.coerceAtLeast(0).coerceAtMost(2)
        val battle = context.battle
        val mayPlace = battle?.takeUnless { it.isDone(context.self.id) }?.rows?.any { row ->
            !row.closed && row.forPlayer(context.self.id)?.withdrawn == false
        } ?: false
        val mayFlip = battle != null && !battle.isDone(context.self.id) &&
            board.creature.any { it.isFaceDown }
        if (usable == 0 || (!mayPlace && !mayFlip)) return Opportunity(
            false, 0, "No available Worms or observable Battle Support destination"
        )
        // One point per potentially usable Worm, not imaginary Strike VP.
        // Other Worms already held reduce the marginal value of further stock.
        val incremental = (usable - (board.worms - 1).coerceAtLeast(0)).coerceAtLeast(0)
        return Opportunity(incremental > 0, incremental,
            "Up to $usable Worms with an open placement/flip opportunity; timing unconfirmed")
    }

    fun refresh(context: DecisionContext): Opportunity {
        val battle = context.battle
        val faceDown = context.self.board.creature.count { it.isFaceDown }
        if (battle == null || battle.isDone(context.self.id) || faceDown == 0) return Opportunity(
            false, 0, "No active future Main opportunity or face-down Plant to refresh"
        )
        // Recovering a Plant creates an option, but its complete tactical value
        // requires a legal future Main choice and cannot be presumed from context.
        return Opportunity(true, 1,
            "$faceDown face-down Plant(s) may be refreshed; future Main activation unconfirmed")
    }
}
