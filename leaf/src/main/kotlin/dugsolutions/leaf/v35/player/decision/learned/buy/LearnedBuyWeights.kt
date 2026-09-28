package dugsolutions.leaf.v35.player.decision.learned.buy

import java.nio.file.Files
import java.nio.file.Path

/** Versioned, strict, Git-friendly persistence for learned Buy weights. */
class LearnedBuyWeights private constructor(private val values: DoubleArray) {
    operator fun get(feature: BuyFeature): Double = values[feature.ordinal]
    fun with(feature: BuyFeature, value: Double): LearnedBuyWeights {
        require(value.isFinite()) { "Weight $feature must be finite: $value" }
        return LearnedBuyWeights(values.copyOf().also { it[feature.ordinal] = value })
    }

    fun save(path: Path) {
        Files.createDirectories(path.parent)
        val text = buildString {
            appendLine("formatVersion=1")
            appendLine("policy=buy-v1")
            BuyFeature.entries.forEach { appendLine("${it.name}=${this@LearnedBuyWeights[it]}") }
        }
        Files.writeString(path, text)
    }

    companion object {
        fun zeros(): LearnedBuyWeights = LearnedBuyWeights(DoubleArray(BuyFeature.entries.size))
        fun load(path: Path): LearnedBuyWeights {
            val entries = Files.readAllLines(path).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                .associate { line ->
                    val parts = line.split('=', limit = 2)
                    require(parts.size == 2) { "Invalid learned Buy weight line: $line" }
                    parts[0].trim() to parts[1].trim()
                }
            require(entries["formatVersion"] == "1") { "Unsupported learned Buy weight formatVersion=${entries["formatVersion"]}" }
            require(entries["policy"] == "buy-v1") { "Unsupported learned Buy policy=${entries["policy"]}" }
            val allowed = BuyFeature.entries.map { it.name }.toSet() + setOf("formatVersion", "policy")
            val unknown = entries.keys - allowed
            require(unknown.isEmpty()) { "Unknown learned Buy weight keys: ${unknown.sorted()}" }
            val missing = BuyFeature.entries.filter { it.name !in entries }
            require(missing.isEmpty()) { "Missing learned Buy weights: ${missing.map { it.name }}" }
            return LearnedBuyWeights(DoubleArray(BuyFeature.entries.size) { index ->
                val feature = BuyFeature.entries[index]
                entries.getValue(feature.name).toDouble().also {
                    require(it.isFinite()) { "Weight $feature must be finite: $it" }
                }
            })
        }
    }
}
