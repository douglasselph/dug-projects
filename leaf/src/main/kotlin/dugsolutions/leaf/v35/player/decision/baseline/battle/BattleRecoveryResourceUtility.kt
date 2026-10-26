package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.context.DecisionContext

/**
 * Conservative, observable opportunity bounds for Battle recovery resources.
 * An opportunity is not a promise of a legal future action: Support/Main timing
 * still belongs to the battle orchestrator. Unknown timing earns no guessed value.
 */
internal object BattleRecoveryResourceUtility {
    data class Opportunity(val available: Boolean, val increment: Int, val explanation: String)

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
