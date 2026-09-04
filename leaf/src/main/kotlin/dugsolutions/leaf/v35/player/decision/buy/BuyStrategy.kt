package dugsolutions.leaf.v35.player.decision.buy

import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter

sealed interface BuyItem {
    val cost: Int

    data class Plant(val card: PlantCard) : BuyItem {
        override val cost: Int get() = card.cost
    }

    data class Die(val sides: DieSides) : BuyItem {
        override val cost: Int get() = sides.value
    }
}

sealed interface BuyChoice {
    data object Done : BuyChoice
    data class Purchase(val item: BuyItem) : BuyChoice
}

data class BuyDieResource(
    val sides: Int,
    val value: Int
) {
    init {
        require(sides > 0) { "Buy die resource must have positive sides: $sides" }
        require(value > 0) { "Buy die resource must have positive value: $value" }
    }
}

/**
 * Immutable snapshot of one owned Critter and its current effective value.
 *
 * The same physical WORM may therefore appear here with value 1 normally or
 * value 3 after Root Appreciation. The strategy never has to infer temporary
 * boost state from the Critter enum itself.
 */
data class BuyCritterResource(
    val critter: Critter,
    val value: Int
) {
    init {
        require(value > 0) { "Buy Critter resource must have positive value: $value" }
    }
}

class BuyPayment(
    dice: List<BuyDieResource> = emptyList(),
    critters: List<BuyCritterResource> = emptyList()
) {
    val dice: List<BuyDieResource> = dice.toList()
    val critters: List<BuyCritterResource> = critters.toList()
    val total: Int get() = dice.sumOf { it.value } + critters.sumOf { it.value }
}

class ChoosePurchaseRequest(options: List<BuyItem>) {
    val options: List<BuyItem> = options.toList()
}

class ChoosePaymentRequest(
    val item: BuyItem,
    availableDice: List<BuyDieResource>,
    availableCritters: List<BuyCritterResource>
) {
    val availableDice: List<BuyDieResource> = availableDice.toList()
    val availableCritters: List<BuyCritterResource> = availableCritters.toList()
    val cost: Int get() = item.cost
}

interface BuyStrategy {
    fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice
    fun choosePayment(request: ChoosePaymentRequest): BuyPayment
}
