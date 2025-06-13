package dugsolutions.leaf.player.decisions.local

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.player.Player
import dugsolutions.leaf.player.effect.FloralBonusCount
import kotlin.math.round

/**
 * Return a score to help determine if this card should be preferred to be used to absorb damage.
 * A lower score is more likely.
 * A die is sides. That is a D4 is worth 4, 6 is 6, etc. So scores should be relative to this.
 */
class CardEffectBattleScore(
    private val player: Player,
    private val effectBattleScore: EffectBattleScore,
    private val floralBonusCount: FloralBonusCount
) {

    operator fun invoke(card: GameCard): Int {
        val primaryScore = effectBattleScore(card.primaryEffect, card.primaryValue)
        val matchScore = computeMatchScore(card.matchWith, card.matchEffect, card.matchValue)
        val trashScore = round(effectBattleScore(card.trashEffect, card.trashValue).toFloat() / 4).toInt()
        return primaryScore + matchScore + trashScore
    }

    private fun computeMatchScore(with: MatchWith, effect: CardEffect?, value: Int): Int {
        effect ?: return 0
        val effectValue = effectBattleScore(effect, value)
        return when (with) {
            is MatchWith.Flower -> {
                val arrayCardIds = player.floralCards.map { it.id }
                val deckCardIds = player.allCardsInDeck.filter { it.type == FlourishType.FLOWER }.map { it.id }
                val rawValue = floralBonusCount(arrayCardIds, with.flowerCardId) + floralBonusCount(deckCardIds, with.flowerCardId)
                val score = effectValue * (rawValue * 1.5)
                round(score).toInt()
            }
            is MatchWith.OnFlourishType -> {
                val relevantDeckCardIds = player.allCardsInDeck.filter { it.type == with.type }
                val totalCards = player.allCardsInDeck.size
                return round(effectValue * (relevantDeckCardIds.size.toFloat() / totalCards.toFloat()) * 6).toInt()
            }
            is MatchWith.OnRoll -> (effectValue.toFloat() / 5).toInt()
            MatchWith.None -> effectValue
        }
    }
}
