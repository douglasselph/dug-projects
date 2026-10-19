package dugsolutions.leaf.common.domain.acquire

sealed class Choice {

    data class Die(val value: ChoiceDie): Choice()
    data class Card(val value: ChoiceCard): Choice()
    data class Bug(val value: ChoiceBug): Choice()

}
