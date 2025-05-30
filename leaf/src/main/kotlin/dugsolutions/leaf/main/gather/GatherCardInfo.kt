package dugsolutions.leaf.main.gather

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.FlourishType.BLOOM
import dugsolutions.leaf.components.FlourishType.CANOPY
import dugsolutions.leaf.components.FlourishType.FLOWER
import dugsolutions.leaf.components.FlourishType.NONE
import dugsolutions.leaf.components.FlourishType.ROOT
import dugsolutions.leaf.components.FlourishType.SEEDLING
import dugsolutions.leaf.components.FlourishType.VINE
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith
import dugsolutions.leaf.main.domain.CardInfo

class GatherCardInfo {

    operator fun invoke(incoming: GameCard): CardInfo = with(incoming) {
        return CardInfo(
            name = name,
            type = floralType(type) ?: "?",
            resilience = resilience,
            cost = incoming.cost.elements.map { it.toString() },
            thorn = thorn,
            primary = effectLine(primaryEffect, primaryValue),
            match = effectLine(matchEffect, matchValue, matchString(matchWith)),
            trash = effectLine(trashEffect, trashValue)
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

}
