package dugsolutions.leaf.v35.player.decision.buy

import dugsolutions.leaf.v35.tokens.Critter

/** Deterministic baseline purchase and minimum-sufficient-payment policy. */
class BaselineBuyStrategy : BuyStrategy {
    override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice =
        request.options.firstOrNull()?.let(BuyChoice::Purchase)
            ?: BuyChoice.Done

    override fun choosePayment(request: ChoosePaymentRequest): BuyPayment {
        data class Resource(
            val die: BuyDieResource? = null,
            val critter: Critter? = null,
            val value: Int
        )

        val resources =
            request.availableDice.map { Resource(die = it, value = it.value) } +
                request.availableCritters.map { Resource(critter = it, value = it.value) }

        var best: List<Resource>? = null
        var bestTotal = Int.MAX_VALUE

        fun search(index: Int, selected: MutableList<Resource>, total: Int) {
            if (total >= request.cost) {
                if (total < bestTotal || total == bestTotal && selected.size < (best?.size ?: Int.MAX_VALUE)) {
                    best = selected.toList()
                    bestTotal = total
                }
                return
            }
            if (index == resources.size || total >= bestTotal) return

            selected.add(resources[index])
            search(index + 1, selected, total + resources[index].value)
            selected.removeAt(selected.lastIndex)
            search(index + 1, selected, total)
        }

        search(0, mutableListOf(), 0)
        val chosen = best ?: return BuyPayment()
        return BuyPayment(
            dice = chosen.mapNotNull { it.die },
            critters = chosen.mapNotNull { it.critter }
        )
    }
}
