package dugsolutions.leaf.components

sealed class MatchWith {
    data object None : MatchWith()
    data class OnRoll(val value: Int) : MatchWith()
    data class OnFlourishType(val type: FlourishType) : MatchWith()
    data class Flower(val flowerCardId: Int) : MatchWith()
}
