package dugsolutions.leaf.components

object SimpleCost {
    operator fun invoke(value: Int): Cost {
        return Cost(listOf(CostElement.TotalDiceMinimum(value)))
    }

}
