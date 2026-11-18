package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * The result of upgrading (or legally Trashing) ONE specific Hand die now.
 * The value is measured against doing nothing in this decision, not against
 * Draw; [CompostLifetimeComparison.drawPipEquivalent] supplies that alternative.
 * Buy tiers are intentionally kept separate from expected pips.
 */
data class CompostLifetimeTarget(
    val die: DieView,
    val replacementSides: Int?,
    val currentSpendingPipsLost: Int,
    val futureSpendingPipsChange: Double,
    val futureRewardEquivalentChange: Double,
    val buyPowerAfter: Int,
    val lostAffordableBuyTiers: List<Int>
) {
    val isTrash: Boolean get() = replacementSides == null
    val modeledPipEquivalent: Double get() =
        -currentSpendingPipsLost + futureSpendingPipsChange + futureRewardEquivalentChange
}

data class CompostLifetimeComparison(
    val drawPipEquivalent: Double?,
    val targets: List<CompostLifetimeTarget>,
    val assumption: CultivationDrawAssumption
) {
    /** A null Draw value means no die can actually be drawn right now. */
    val drawReference: Double get() = drawPipEquivalent ?: 0.0
}

/**
 * Pure, deterministic Compost versus Draw forecast. No strategy preferences,
 * personality settings, policy weights, or RNG enter this calculation.
 *
 * Current Hand removal is charged at its ACTUAL showing face. An available
 * next-size die enters Discard. If it is unavailable, official v38 Compost
 * Trashes the selected die; that alternative has no replacement. Each future
 * comparison uses the SAME phase sequence and Draw assumption. As with
 * FutureDiceForecast, unknown acquisitions, Battle Doom, and actual Buy
 * decisions cannot be predicted and are not assigned fictional pip values.
 */
object CompostLifetimeValuation {
    private val steps = mapOf(4 to 6, 6 to 8, 8 to 10, 10 to 12, 12 to 20)

    fun compare(
        context: DecisionContext,
        normalPurchasingPower: Int,
        assumption: CultivationDrawAssumption = CultivationDrawAssumption.TYPICAL_ONE_EXTRA
    ): CompostLifetimeComparison {
        require(context.phase == RoundCardType.CULTIVATION) { "Compost forecast requires a Cultivation context" }
        require(normalPurchasingPower >= 0) { "Normal purchasing power cannot be negative" }
        val zones = FutureDiceZones.fromBoard(context.self.board)
        val rewards = RollRewardOpportunities.fromVisibleContext(context)
        val future = context.progress.upcomingRoundTypes
        val baseline = FutureDiceForecast.project(zones, future, assumption, rewards).expectedValue
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)

        val drawn = drawNow(zones)
        val drawValue = drawn?.let { (changed, die) ->
            val now = ImmediatePipValuation.roll(die.sides, rewards)
            val later = FutureDiceForecast.project(changed, future, assumption, rewards).expectedValue
            now.pipEquivalentTotal + later.pipEquivalentTotal - baseline.pipEquivalentTotal
        }

        val targets = context.self.board.hand.mapIndexed { position, die ->
            val next = steps[die.sides]?.takeIf { sides ->
                (context.grove.graftBed[DieSides.from(sides)] ?: 0) > 0
            }
            val changed = if (next != null) {
                zones.withHandUpgradeIntoDiscard("h$position", next)
            } else {
                // The current rule allows choosing any Hand die. If there is
                // no AVAILABLE normal next step, the chosen die is Trashed.
                zones.copy(hand = zones.hand.filterNot { it.id == "h$position" })
            }
            val later = FutureDiceForecast.project(changed, future, assumption, rewards).expectedValue
            val powerAfter = (normalPurchasingPower - die.value).coerceAtLeast(0)
            CompostLifetimeTarget(
                die = die,
                replacementSides = next,
                currentSpendingPipsLost = die.value,
                futureSpendingPipsChange = later.spendingPips - baseline.spendingPips,
                futureRewardEquivalentChange =
                    (later.critterOptionValue + later.wispOptionValue) -
                        (baseline.critterOptionValue + baseline.wispOptionValue),
                buyPowerAfter = powerAfter,
                lostAffordableBuyTiers = tiers.filter { it <= normalPurchasingPower && it > powerAfter }
            )
        }
        return CompostLifetimeComparison(drawValue, targets, assumption)
    }

    /** Same lowest-first Supply / refill-from-Discard behavior as normal Draw. */
    private fun drawNow(zones: FutureDiceZones): Pair<FutureDiceZones, FutureDie>? {
        val supply = zones.supply.toMutableList()
        val discard = zones.discard.toMutableList()
        if (supply.isEmpty()) {
            supply.addAll(discard)
            discard.clear()
        }
        val index = supply.indices.minByOrNull { supply[it].sides } ?: return null
        val die = supply.removeAt(index)
        return FutureDiceZones(supply, zones.hand + die, discard) to die
    }
}
