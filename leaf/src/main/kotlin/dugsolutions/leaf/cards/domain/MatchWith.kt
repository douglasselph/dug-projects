package dugsolutions.leaf.cards.domain

sealed class MatchWith {
    data object None : MatchWith()
    data class OnRoll(val value: Int, val discardDie: Boolean = true) : MatchWith()
    data class OnFlourishType(val type: FlourishType) : MatchWith()
    data class Flower(val flowerCardId: Int) : MatchWith()
}