package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.cost.ParseCost
import dugsolutions.leaf.cards.domain.GenCardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.MatchWith
import java.io.File

class CardRegistry(
    private val parseCost: ParseCost
) {
    companion object {
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
        if (parts.isEmpty() || parts[0].isEmpty() || parts.size < 11) {
            throw IllegalArgumentException("Invalid card line: $line")
        }
        val name = parts[0]
        val id = GenCardID.generateId(name)
        val flourishType = parseFlourishType(parts[1]) ?: throw IllegalArgumentException("Unknown flourish type: ${parts[1]}")
        val resilience = parts[2].toIntOrNull() ?: 0
        val cost = parseCost(parts[3])
        val primaryEffect = parseEffect(parts[4])
        val primaryValue = parseValue(parts[5])
        val matchWith = parseMatchWith(parts[6])
        val matchEffect = parseEffect(parts[7])
        val matchValue = parseValue(parts[8])
        val trashEffect = parseEffect(parts[9])
        val trashValue = parseValue(parts[10])
        val thornValue = parseValue(parts[11])

        return GameCard(
            id = id,
            name = name,
            type = flourishType,
            resilience = resilience,
            cost = cost,
            primaryEffect = primaryEffect,
            primaryValue = primaryValue,
            matchWith = matchWith,
            matchEffect = matchEffect,
            matchValue = matchValue,
            trashEffect = trashEffect,
            trashValue = trashValue,
            thorn = thornValue
        )
    }

    private fun parseFlourishType(type: String): FlourishType? {
        return when (type.trim()) {
            "Seedling" -> FlourishType.SEEDLING
            "Root" -> FlourishType.ROOT
            "Canopy" -> FlourishType.CANOPY
            "Vine" -> FlourishType.VINE
            "Flower" -> FlourishType.FLOWER
            "Bloom" -> FlourishType.BLOOM
            else -> null
        }
    }

    private fun parseEffect(effect: String): CardEffect? {
        // Handle empty or no effect cases
        if (effect.isEmpty() || effect == "-" || effect.startsWith(":")) {
            return null
        }
        return CardEffect.from(effect)
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
            match.startsWith("d") -> MatchWith.OnRoll(match.substring(1).toInt(), discardDie = true)
            match.startsWith("k") -> MatchWith.OnRoll(match.substring(1).toInt(), discardDie = false)
            else -> {
                parseFlourishType(match)?.let {
                    type -> MatchWith.OnFlourishType(type)
                } ?: run {
                    flowerOf(match)?.let { card ->
                        MatchWith.Flower(card.id)
                    } ?: run {
                        throw IllegalArgumentException("Unknown flower: $match")
                    }
                }
            }
        }
    }

    private fun flowerOf(name: String): GameCard? {
        if (cards.contains(name)) {
            return cards[name]
        }
        return null
    }

}
