package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.GenCardID
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.Cost
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.cards.domain.Phase
import java.io.File

class CardRegistry {
    companion object {
        private const val DELIMITER = ","
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
        if (parts.isEmpty() || parts[0].isEmpty() || parts.size < 14) {
            throw IllegalArgumentException("Invalid card line: $line (#fields=${parts.size})")
        }
        var column = 0
        val name = parts[column]
        val id = GenCardID.generateId(name)
        val flourishType = parseFlourishType(parts[++column])
        val resilience = parseValue(parts[++column])
        val cost = parseCost(parts[++column])
        val primaryEffect = parseEffect(parts[++column])
        val primaryValue = parseValue(parts[++column])
        val phase = parsePhase(parts[++column])
        val matchWith = parseMatchWith(parts[++column])
        val matchEffect = parseEffect(parts[++column])
        val matchValue = parseValue(parts[++column])
        val imageValue = parts.getOrNull(++column)
        val count = parseValue(parts[++column])
        val notes = parts[++column]

        return GameCard(
            id = id,
            name = name,
            type = flourishType,
            resilience = resilience,
            cost = cost,
            primaryEffect = primaryEffect,
            primaryValue = primaryValue,
            phase = phase,
            matchWith = matchWith,
            matchEffect = matchEffect,
            matchValue = matchValue,
            image = imageValue,
            count = count,
            notes = notes
        )
    }

    private fun parseFlourishType(incoming: String): FlourishType {
        return FlourishType.from(incoming)
    }

    private fun parseEffect(effect: String): CardEffect? {
        // Handle empty or no effect cases
        if (effect.isEmpty() || effect == "-" || effect.startsWith(":")) {
            return null
        }
        return CardEffect.from(effect)
            ?: throw IllegalArgumentException("No matching CardEffect found for: $effect")
    }

    private fun parsePhase(phase: String): Phase {
        if (phase.isEmpty() || phase == "-") {
            return Phase.Cultivation
        }
        return Phase.from(phase)
    }

    private fun parseValue(value: String): Int {
        return when {
            value.isEmpty() || value == "-" -> 0
            value.toIntOrNull() != null -> value.toInt()
            else -> throw IllegalArgumentException("Invalid value: $value")
        }
    }

    private fun parseCost(value: String): Cost {
        return when {
            value.isEmpty() || value == "-" -> Cost.None
            value.toIntOrNull() != null -> Cost.Value(value.toInt())
            else -> throw IllegalArgumentException("Invalid value: $value")
        }
    }

    private fun parseMatchWith(match: String): MatchWith {
        if (match.isEmpty() || match == "-") {
            return MatchWith.None
        }
        return MatchWith.from(match)
    }

    private fun flowerOf(name: String): GameCard? {
        if (cards.contains(name)) {
            return cards[name]
        }
        return null
    }

}
