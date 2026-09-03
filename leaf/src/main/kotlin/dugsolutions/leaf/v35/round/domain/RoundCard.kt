package dugsolutions.leaf.v35.round.domain

import dugsolutions.leaf.v35.effect.GameEffect

data class RoundCard(
    val quantity: Int,
    val name: String,
    val type: RoundCardType,
    val firstEffect: RoundCardEffect,
    val secondEffect: RoundCardEffect,
    val backImage: String
)

data class RoundCardEffect(
    val title: String,
    val backgroundColor: String,
    val textColor: String,
    val image: String,
    val icon: String?,
    val effect: GameEffect
)

enum class RoundCardType {
    BATTLE,
    CULTIVATION
}
