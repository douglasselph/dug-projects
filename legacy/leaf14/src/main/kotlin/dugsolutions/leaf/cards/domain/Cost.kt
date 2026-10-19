package dugsolutions.leaf.cards.domain

sealed class Cost {

    data object None: Cost()

    data class Value(val amount: Int): Cost()

}
