package dugsolutions.leaf.tool

import dugsolutions.leaf.components.GenCardID
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.MatchWith
import java.io.File

class CardRegistry(
    private val parseCost: ParseCost
) {
    companion object {
        val EFFECT_MAP = mapOf(
            "AddToDie" to CardEffect.ADD_TO_DIE,
            "AddToTotal" to CardEffect.ADD_TO_TOTAL,
            "AdjustBy" to CardEffect.ADJUST_BY,
            "AdjustToMax" to CardEffect.ADJUST_TO_MAX,
            "AdjustToMinOrMax" to CardEffect.ADJUST_TO_MIN_OR_MAX,
            "Deflect" to CardEffect.DEFLECT,
            "Discard" to CardEffect.DISCARD,
            "DiscardCard" to CardEffect.DISCARD_CARD,
            "DiscardDie" to CardEffect.DISCARD_DIE,
            "DrawCard" to CardEffect.DRAW_CARD,
            "DrawCardCompost" to CardEffect.DRAW_CARD_COMPOST,
            "DrawDie" to CardEffect.DRAW_DIE,
            "DrawDieAny" to CardEffect.DRAW_DIE_ANY,
            "DrawDieCompost" to CardEffect.DRAW_DIE_COMPOST,
            "DrawThenDiscard" to CardEffect.DRAW_THEN_DISCARD,
            "FlourishOverride" to CardEffect.FLOURISH_OVERRIDE,
            "GainFreeRoot" to CardEffect.GAIN_FREE_ROOT,
            "GainFreeCanopy" to CardEffect.GAIN_FREE_CANOPY,
            "GainFreeVine" to CardEffect.GAIN_FREE_VINE,
            "ReduceCostRoot" to CardEffect.REDUCE_COST_ROOT,
            "ReduceCostCanopy" to CardEffect.REDUCE_COST_CANOPY,
            "ReduceCostVine" to CardEffect.REDUCE_COST_VINE,
            "RerollAccept2nd" to CardEffect.REROLL_ACCEPT_2ND,
            "RerollAllMax" to CardEffect.REROLL_ALL_MAX,
            "RerollTakeBetter" to CardEffect.REROLL_TAKE_BETTER,
            "RetainCard" to CardEffect.RETAIN_CARD,
            "RetainDie" to CardEffect.RETAIN_DIE,
            "RetainDieReroll" to CardEffect.RETAIN_DIE_REROLL,
            "ReuseCard" to CardEffect.REUSE_CARD,
            "ReuseDie" to CardEffect.REUSE_DIE,
            "ReplayVine" to CardEffect.REPLAY_VINE,
            "Thorn" to CardEffect.THORN,
            "UpgradeAnyRetain" to CardEffect.UPGRADE_ANY_RETAIN,
            "UpgradeAny" to CardEffect.UPGRADE_ANY,
            "UpgradeD4" to CardEffect.UPGRADE_D4,
            "UpgradeD6" to CardEffect.UPGRADE_D6,
            "UpgradeD4D6" to CardEffect.UPGRADE_D4_D6,
            "UseOpponentCard" to CardEffect.USE_OPPONENT_CARD,
            "UseOpponentDie" to CardEffect.USE_OPPONENT_DIE
        )
        private const val DELIMITER = ";"

    }

    private val cards: MutableMap<String, GameCard> = mutableMapOf()

    // region public

    fun loadFromCsv(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("CSV file not found at path: $filePath")
        }
        file.readLines()
            .drop(1) // Skip header row
            .forEach { line ->
                val card = parseCardFromCsv(line)
                cards[card.name] = card
            }
    }

    fun getCard(name: String): GameCard? = cards[name]

    fun getAllCards(): List<GameCard> = cards.values.toList()

    // endregion public

    private fun parseCardFromCsv(line: String): GameCard {
        val parts = line.split(DELIMITER).map { it.trim() }

        // Skip empty lines or header rows
        if (parts.isEmpty() || parts[0].isEmpty() || parts[0].startsWith("Flourish Type:") || parts.size < 10) {
            throw IllegalArgumentException("Invalid card line: $line")
        }

        // Parse effects and their values
        val primaryEffect = parseEffect(parts[4])
        val primaryValue = parseValue(parts[5])
        val matchEffect = parseEffect(parts[7])
        val matchValue = parseValue(parts[8])
        val trashEffect = parseEffect(parts[9])
        val trashValue = parseValue(parts[10])

        return GameCard(
            id = GenCardID.generateId(parts[0]),
            name = parts[0],
            type = parseFlourishType(parts[1]),
            resilience = parts[2].toIntOrNull() ?: 0,
            cost = parseCost(parts[3]),
            primaryEffect = primaryEffect,
            primaryValue = primaryValue,
            matchWith = parseMatchWith(parts[6]),
            matchEffect = matchEffect,
            matchValue = matchValue,
            trashEffect = trashEffect,
            trashValue = trashValue
        )
    }

    private fun parseFlourishType(type: String): FlourishType {
        return when (type.trim()) {
            "Seedling" -> FlourishType.SEEDLING
            "Root" -> FlourishType.ROOT
            "Canopy" -> FlourishType.CANOPY
            "Vine" -> FlourishType.VINE
            "Bloom" -> FlourishType.BLOOM
            else -> throw IllegalArgumentException("Unknown flourish type: $type")
        }
    }

    private fun parseEffect(effect: String): CardEffect? {
        // Handle empty or no effect cases
        if (effect.isEmpty() || effect == "-") {
            return null
        }

        // Map CSV effect names directly to CardEffect values
        return EFFECT_MAP[effect]
            ?: throw IllegalArgumentException("No matching CardEffect found for: $effect")
    }

    private fun parseValue(value: String): Int {
        return when {
            value.isEmpty() || value == "-" -> 0
            value.toIntOrNull() != null -> value.toInt()
            value == "All" -> 100
            else -> throw IllegalArgumentException("Invalid value: $value")
        }
    }

    private fun parseMatchWith(match: String): MatchWith {
        return when {
            match.isEmpty() || match == "-" -> MatchWith.None
            match.toIntOrNull() != null -> MatchWith.OnRoll(match.toInt())
            else -> MatchWith.OnFlourishType(parseFlourishType(match))
        }
    }
}
