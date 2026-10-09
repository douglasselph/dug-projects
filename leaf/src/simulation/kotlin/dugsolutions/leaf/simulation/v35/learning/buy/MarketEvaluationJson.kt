package dugsolutions.leaf.simulation.v35.learning.buy

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

internal const val MARKET_EVALUATION_JSON_SCHEMA = "leaf.market-evaluation"
internal const val MARKET_EVALUATION_JSON_VERSION = 3

internal data class MarketEvaluationJsonMetadata(
    val players: Int,
    val samples: Int,
    val mechanicalSeedStart: Long,
    val strategySeedStart: Long,
    val groveSeedStart: Long?,
    val grovePattern: String?,
    val roundPattern: String,
    val policyPath: String,
    val plantOverridesPath: String?,
    val plantOverridesSha256: String?,
    val roundOverridesPath: String?,
    val roundOverridesSha256: String?
)

/**
 * Stable machine-readable snapshot of one matched CONTROL/LEARNED Buy-policy evaluation.
 *
 * This is intentionally built from typed telemetry, never from human-readable evaluator output.
 * Raw exposure and purchase numerators/denominators are preserved without deriving rates.
 */
internal data class MarketEvaluationJsonDocument(
    val metadata: MarketEvaluationJsonMetadata,
    val comparison: MarketComparisonTelemetry
) {
    init {
        require(metadata.players == comparison.playerCount) { "JSON metadata player count does not match telemetry" }
        require(metadata.samples.toLong() == comparison.sampleCount) { "JSON metadata sample count does not match telemetry" }
        comparison.control.cards.forEach { card ->
            require(card.opportunity.selectedDecisionCount == card.purchaseCount) {
                "CONTROL selected Buy decisions do not reconcile with Plant purchases for ${card.cardName}"
            }
        }
        comparison.learned.cards.forEach { card ->
            require(card.opportunity.selectedDecisionCount == card.purchaseCount) {
                "LEARNED selected Buy decisions do not reconcile with Plant purchases for ${card.cardName}"
            }
        }
    }
}

internal object MarketEvaluationJsonWriter {
    fun write(path: Path, document: MarketEvaluationJsonDocument) {
        path.toAbsolutePath().parent?.let(Files::createDirectories)
        Files.writeString(path, render(document), StandardCharsets.UTF_8)
    }

    fun render(document: MarketEvaluationJsonDocument): String = buildString {
        val m = document.metadata
        val c = document.comparison.control
        val l = document.comparison.learned
        append("{\n")
        append("  \"schema\": ").append(jsonString(MARKET_EVALUATION_JSON_SCHEMA)).append(",\n")
        append("  \"schemaVersion\": ").append(MARKET_EVALUATION_JSON_VERSION).append(",\n")
        append("  \"experiment\": {\n")
        append("    \"players\": ").append(m.players).append(",\n")
        append("    \"samples\": ").append(m.samples).append(",\n")
        append("    \"mechanicalSeedStart\": ").append(m.mechanicalSeedStart).append(",\n")
        append("    \"strategySeedStart\": ").append(m.strategySeedStart).append(",\n")
        append("    \"groveSeedStart\": ").append(jsonLongOrNull(m.groveSeedStart)).append(",\n")
        append("    \"grovePattern\": ").append(jsonStringOrNull(m.grovePattern)).append(",\n")
        append("    \"roundPattern\": ").append(jsonString(m.roundPattern)).append(",\n")
        append("    \"policyPath\": ").append(jsonString(m.policyPath)).append(",\n")
        append("    \"plantOverrides\": ")
        appendOverrideObject(m.plantOverridesPath, m.plantOverridesSha256)
        append(",\n")
        append("    \"roundOverrides\": ")
        appendOverrideObject(m.roundOverridesPath, m.roundOverridesSha256)
        append("\n  },\n")
        append("  \"outcomes\": {\n")
        append("    \"control\": {\"role\": \"CONTROL\", \"sampleCount\": ").append(c.sampleCount)
            .append(", \"winShare\": ").append(jsonDouble(c.overallWinShare))
            .append(", \"plantPurchases\": ").append(c.totalPlantPurchases).append("},\n")
        append("    \"learned\": {\"role\": \"LEARNED\", \"sampleCount\": ").append(l.sampleCount)
            .append(", \"winShare\": ").append(jsonDouble(l.overallWinShare))
            .append(", \"plantPurchases\": ").append(l.totalPlantPurchases).append("}\n")
        append("  },\n")
        append("  \"cards\": [\n")
        document.comparison.cards.forEachIndexed { index, card ->
            append("    {\"identity\": ").append(jsonString(card.cardName))
            append(", \"type\": ").append(jsonString(card.type.name))
            append(", \"cost\": ").append(card.cost)
            append(", \"controlExposure\": ").append(card.controlGroveExposureCount)
            append(", \"learnedExposure\": ").append(card.learnedGroveExposureCount)
            append(", \"controlPurchases\": ").append(card.controlPurchaseCount)
            append(", \"learnedPurchases\": ").append(card.learnedPurchaseCount)
            append(", \"controlWinShareOnExposureSum\": ").append(jsonDouble(card.controlWinShareOnExposureSum))
            append(", \"learnedWinShareOnExposureSum\": ").append(jsonDouble(card.learnedWinShareOnExposureSum))
            append(", \"controlOpportunity\": ")
            appendOpportunity(card.controlOpportunity)
            append(", \"learnedOpportunity\": ")
            appendOpportunity(card.learnedOpportunity)
            append("}")
            if (index != document.comparison.cards.lastIndex) append(',')
            append('\n')
        }
        append("  ]\n")
        append("}\n")
    }

    private fun StringBuilder.appendOpportunity(value: MarketCardOpportunityTelemetry) {
        append('{')
        append("\"marketDecisionCount\": ").append(value.marketDecisionCount)
        append(", \"affordableDecisionCount\": ").append(value.affordableDecisionCount)
        append(", \"graftableDecisionCount\": ").append(value.graftableDecisionCount)
        append(", \"legalDecisionCount\": ").append(value.legalDecisionCount)
        append(", \"selectedDecisionCount\": ").append(value.selectedDecisionCount)
        append(", \"playerDoneWhileLegalCount\": ").append(value.playerDoneWhileLegalCount)
        append(", \"noLegalItemsWhileMarketCount\": ").append(value.noLegalItemsWhileMarketCount)
        append(", \"firstDecisionLegalCount\": ").append(value.firstDecisionLegalCount)
        append(", \"postPurchaseLegalCount\": ").append(value.postPurchaseLegalCount)
        append(", \"legalWithHigherCostPlantCount\": ").append(value.legalWithHigherCostPlantCount)
        append(", \"purchasingPowerOnMarketDecisionSum\": ").append(value.purchasingPowerOnMarketDecisionSum)
        append(", \"marketByCultivationRound\": ").appendRoundMap(value.marketByCultivationRound)
        append(", \"affordableByCultivationRound\": ").appendRoundMap(value.affordableByCultivationRound)
        append(", \"legalByCultivationRound\": ").appendRoundMap(value.legalByCultivationRound)
        append(", \"selectedByCultivationRound\": ").appendRoundMap(value.selectedByCultivationRound)
        append(", \"rejectedLegalAlternatives\": ").appendAlternatives(value.rejectedLegalAlternatives)
        append('}')
    }

    private fun StringBuilder.appendAlternatives(values: List<MarketAlternativeTelemetry>) {
        append('[')
        values.forEachIndexed { index, value ->
            if (index > 0) append(", ")
            append('{')
            append("\"outcome\": ").append(jsonString(value.outcome.name))
            append(", \"kind\": ").append(jsonStringOrNull(value.kind?.name))
            append(", \"itemName\": ").append(jsonStringOrNull(value.itemName))
            append(", \"cost\": ").append(value.cost?.toString() ?: "null")
            append(", \"count\": ").append(value.count)
            append('}')
        }
        append(']')
    }

    private fun StringBuilder.appendRoundMap(values: Map<Int, Long>) {
        append('{')
        values.toSortedMap().entries.forEachIndexed { index, (round, count) ->
            if (index > 0) append(", ")
            append(jsonString(round.toString())).append(": ").append(count)
        }
        append('}')
    }

    private fun StringBuilder.appendOverrideObject(path: String?, sha256: String?) {
        if (path == null) {
            append("null")
            return
        }
        append("{\"path\": ").append(jsonString(path))
        append(", \"sha256\": ").append(jsonStringOrNull(sha256)).append('}')
    }

    private fun jsonStringOrNull(value: String?): String = value?.let(::jsonString) ?: "null"
    private fun jsonLongOrNull(value: Long?): String = value?.toString() ?: "null"

    private fun jsonDouble(value: Double): String {
        require(value.isFinite()) { "JSON cannot represent non-finite Double: $value" }
        return value.toString()
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
        append('"')
    }
}

internal fun sha256IfRegularFile(path: Path?): String? {
    if (path == null || !Files.isRegularFile(path)) return null
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
