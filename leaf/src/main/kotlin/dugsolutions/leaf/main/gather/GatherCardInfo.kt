package dugsolutions.leaf.main.gather

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.cards.FakeCards
import dugsolutions.leaf.cards.cost.CostScore
import dugsolutions.leaf.cards.di.GameCardsFactory
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.FlourishType.BLOOM
import dugsolutions.leaf.cards.domain.FlourishType.CANOPY
import dugsolutions.leaf.cards.domain.FlourishType.FLOWER
import dugsolutions.leaf.cards.domain.FlourishType.NONE
import dugsolutions.leaf.cards.domain.FlourishType.ROOT
import dugsolutions.leaf.cards.domain.FlourishType.SEEDLING
import dugsolutions.leaf.cards.domain.FlourishType.VINE
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.common.Commons
import dugsolutions.leaf.game.battle.MatchingBloomCard
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.random.RandomizerTD

class GatherCardInfo(
    private val matchingBloomCard: MatchingBloomCard
) {

    companion object {
        fun previewVariation(): GatherCardInfo {
            val costScore = CostScore()
            val randomizer = RandomizerTD()
            val gameCardsFactory = GameCardsFactory(randomizer, costScore)
            val cardManager = CardManager(gameCardsFactory)
            cardManager.loadCards(FakeCards.ALL_CARDS)
            val matchingBloomCard = MatchingBloomCard(cardManager)
            return GatherCardInfo(matchingBloomCard)
        }
    }
    operator fun invoke(
        index: Int = 0,
        card: GameCard,
        highlight: HighlightInfo = HighlightInfo.NONE
    ): CardInfo = with(card) {
        return CardInfo(
            index = index,
            name = name,
            type = floralType(type) ?: "?",
            resilience = resilience,
            nutrient = nutrient,
            cost = card.cost.toString(),
            thorn = thorn,
            primary = effectLine(primaryEffect, primaryValue),
            match = effectLine(matchEffect, matchValue, matchString(matchWith)),
            trash = effectLine(trashEffect, trashValue),
            bloom = bloomLine(card),
            highlight = highlight
        )
    }

    private fun effectLine(effect: CardEffect?, value: Int, prefix: String? = null): String? {
        effect ?: return null
        return (prefix?.let { "$it " } ?: "") + effectName(effect) + " $value"
    }

    private fun effectName(effect: CardEffect): String {
        return effect.name
            .split("_")
            .joinToString("") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }

    private fun matchString(match: MatchWith): String? {
        return when (match) {
            is MatchWith.Flower -> "FLOWER"
            MatchWith.None -> null
            is MatchWith.OnFlourishType -> floralType(match.type)
            is MatchWith.OnRoll -> match.value.toString()
        }
    }

    private fun floralType(type: FlourishType): String? {
        return when (type) {
            NONE -> null
            SEEDLING -> "S"
            ROOT -> "R"
            CANOPY -> "C"
            VINE -> "V"
            FLOWER -> "F"
            BLOOM -> "B"
        }
    }

    private fun bloomLine(card: GameCard): String? {
        if (card.type == FLOWER) {
            matchingBloomCard(card)?.let { bloomCard ->
                return bloomCard.name
            }
        }
        return null
    }

}
