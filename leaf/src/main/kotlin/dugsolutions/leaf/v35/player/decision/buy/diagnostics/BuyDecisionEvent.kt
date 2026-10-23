package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest

/**
 * Immutable, one-decision audit observation. Both recommendations were made from
 * the same ChoosePurchaseRequest; only the actual strategy is executed.
 *
 * This is the in-memory D3 contract. Persistence, run-level metadata, and report
 * aggregation are handled by later tasks. Plant titles are the report identity;
 * mutable Component Studio IDs are retained solely for tracing/catalog joins.
 */
data class BuyDecisionEvent(
    val sequenceInObserver: Long,
    val actingPlayerId: Int,
    val roundNumber: Int,
    val cultivationRoundNumber: Int?,
    val purchasesMadeThisBuy: Int,
    val playerCount: Int,
    val phase: String?,
    val baseline: BuySelection,
    val expert: BuySelection,
    val kind: BuyDisagreementKind,
    /** Exactly the legal purchase choices supplied to BOTH policies. */
    val legalOptions: List<BuyOptionObservation>,
    /** Market items including currently unaffordable / ungraftable items. */
    val marketOptions: List<BuyOptionObservation>,
    val handDice: List<BuyDieObservation>,
    val bees: Int,
    val worms: Int,
    val beeValue: Int,
    val wormValue: Int
) {
    val disagrees: Boolean get() = kind == BuyDisagreementKind.DIFFERENT_ITEM ||
        kind == BuyDisagreementKind.EXPERT_BUYS_BASELINE_DONE ||
        kind == BuyDisagreementKind.BASELINE_BUYS_EXPERT_DONE

    /** This is potential aggregate power, NOT a claim about remaining purchase plans. */
    val aggregatePurchasingPower: Int get() = handDice.sumOf { it.value } +
        bees * beeValue + worms * wormValue
    val diceOnlyPurchasingPower: Int get() = handDice.sumOf { it.value }

    companion object {
        fun capture(
            sequence: Long,
            request: ChoosePurchaseRequest,
            baselineChoice: BuyChoice,
            expertChoice: BuyChoice
        ): BuyDecisionEvent {
            val comparison = BuyDisagreement.compare(baselineChoice, expertChoice)
            val context = request.context
            val board = context.self.board
            return BuyDecisionEvent(
                sequenceInObserver = sequence,
                actingPlayerId = context.self.id.value,
                roundNumber = context.progress.roundNumber,
                cultivationRoundNumber = context.progress.currentCultivationRoundNumber,
                purchasesMadeThisBuy = request.purchasesMadeThisBuy,
                playerCount = context.opponents.size + 1,
                phase = context.phase?.name,
                baseline = comparison.baseline,
                expert = comparison.expert,
                kind = comparison.kind,
                legalOptions = request.options.map(BuyOptionObservation::from),
                marketOptions = request.marketOptions.map(BuyOptionObservation::from),
                handDice = board.hand.map { BuyDieObservation(it.sides, it.value) },
                bees = board.bees,
                worms = board.worms,
                beeValue = board.beeValue,
                wormValue = board.wormValue
            )
        }
    }
}

data class BuyDieObservation(val sides: Int, val value: Int)

data class BuyOptionObservation(val selection: BuySelection, val cost: Int) {
    companion object {
        fun from(item: BuyItem): BuyOptionObservation = when (item) {
            is BuyItem.Plant -> BuyOptionObservation(
                BuySelection.Plant(item.card.title, item.card.name), item.cost
            )
            is BuyItem.Die -> BuyOptionObservation(BuySelection.Die(item.sides.value), item.cost)
        }
    }
}
