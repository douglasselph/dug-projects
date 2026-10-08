package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.v35.plant.PlantValueResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType

/** Identifies which side of a matched Buy-policy evaluation produced telemetry. */
internal enum class MarketEvaluationRole {
    CONTROL,
    LEARNED,
    UNSPECIFIED
}

/** Immutable per-card market telemetry. Raw counts only; no derived rate is stored here. */
internal data class MarketCardTelemetry(
    val cardName: String,
    val type: PlantType,
    val cost: Int,
    val groveExposureCount: Long,
    val purchaseCount: Long,
    val winShareOnExposureSum: Double
)

/** Immutable typed result for one role in a market evaluation. */
internal data class MarketEvaluationTelemetry(
    val playerCount: Int,
    val sampleCount: Long,
    val role: MarketEvaluationRole,
    val overallWinShare: Double,
    val cards: List<MarketCardTelemetry>
) {
    init {
        require(playerCount > 0) { "playerCount must be positive" }
        require(sampleCount >= 0) { "sampleCount must be non-negative" }
        require(cards.map { it.cardName }.distinct().size == cards.size) { "Duplicate Plant card identity in market telemetry" }
    }

    val cardsByName: Map<String, MarketCardTelemetry> = cards.associateBy { it.cardName }
    val totalPlantPurchases: Long = cards.sumOf { it.purchaseCount }
}

/**
 * Typed paired view for the matched CONTROL/LEARNED evaluator.
 *
 * This deliberately preserves both raw sides rather than calculating purchase/exposure rates.
 */
internal data class MarketComparisonCardTelemetry(
    val cardName: String,
    val type: PlantType,
    val cost: Int,
    val controlGroveExposureCount: Long,
    val learnedGroveExposureCount: Long,
    val controlPurchaseCount: Long,
    val learnedPurchaseCount: Long,
    val controlWinShareOnExposureSum: Double,
    val learnedWinShareOnExposureSum: Double
)

internal data class MarketComparisonTelemetry(
    val control: MarketEvaluationTelemetry,
    val learned: MarketEvaluationTelemetry
) {
    init {
        require(control.playerCount == learned.playerCount) { "CONTROL/LEARNED playerCount mismatch" }
        require(control.sampleCount == learned.sampleCount) { "CONTROL/LEARNED sampleCount mismatch" }
        require(control.cards.map { it.cardName } == learned.cards.map { it.cardName }) {
            "CONTROL/LEARNED market card catalogs differ"
        }
    }

    val playerCount: Int get() = control.playerCount
    val sampleCount: Long get() = control.sampleCount
    val cards: List<MarketComparisonCardTelemetry> = control.cards.map { controlCard ->
        val learnedCard = learned.cardsByName.getValue(controlCard.cardName)
        require(controlCard.type == learnedCard.type && controlCard.cost == learnedCard.cost) {
            "CONTROL/LEARNED Plant metadata differs for ${controlCard.cardName}"
        }
        MarketComparisonCardTelemetry(
            cardName = controlCard.cardName,
            type = controlCard.type,
            cost = controlCard.cost,
            controlGroveExposureCount = controlCard.groveExposureCount,
            learnedGroveExposureCount = learnedCard.groveExposureCount,
            controlPurchaseCount = controlCard.purchaseCount,
            learnedPurchaseCount = learnedCard.purchaseCount,
            controlWinShareOnExposureSum = controlCard.winShareOnExposureSum,
            learnedWinShareOnExposureSum = learnedCard.winShareOnExposureSum
        )
    }
}

/**
 * Mutable accumulation layer for complete-market evaluation telemetry.
 *
 * The catalog is materialized up front so zero-purchase and zero-exposure cards remain present
 * in the typed snapshot. Grove exposure is recorded directly from the resolved Grove and is
 * deduplicated by Plant identity per affected-role game/sample.
 */
internal class MarketEvaluationTelemetryAccumulator(
    private val playerCount: Int,
    private val role: MarketEvaluationRole,
    cards: List<PlantCard>,
    plantValues: PlantValueResolver = PlantValueResolver.CANONICAL
) {
    private data class MutableCardTelemetry(
        val cardName: String,
        val type: PlantType,
        val cost: Int,
        var groveExposureCount: Long = 0,
        var purchaseCount: Long = 0,
        var winShareOnExposureSum: Double = 0.0
    )

    private val cardsByName = linkedMapOf<String, MutableCardTelemetry>()
    private var samples: Long = 0
    private var winShareSum: Double = 0.0

    init {
        cards.sortedBy { it.name }.forEach { card ->
            require(card.name !in cardsByName) { "Duplicate Plant card identity in market catalog: ${card.name}" }
            cardsByName[card.name] = MutableCardTelemetry(
                cardName = card.name,
                type = plantValues.typeFor(card),
                cost = plantValues.costFor(card)
            )
        }
    }

    fun recordGame(grove: List<PlantCard>, winShare: Double) {
        samples++
        winShareSum += winShare
        grove.asSequence().map { it.name }.distinct().forEach { name ->
            val card = cardsByName[name]
                ?: error("Resolved Grove contains Plant not present in market telemetry catalog: $name")
            card.groveExposureCount++
            card.winShareOnExposureSum += winShare
        }
    }

    fun recordPlantPurchase(cardName: String) {
        val card = cardsByName[cardName]
            ?: error("Plant purchase references Plant not present in market telemetry catalog: $cardName")
        card.purchaseCount++
    }

    fun snapshot(): MarketEvaluationTelemetry = MarketEvaluationTelemetry(
        playerCount = playerCount,
        sampleCount = samples,
        role = role,
        overallWinShare = if (samples == 0L) 0.0 else winShareSum / samples.toDouble(),
        cards = cardsByName.values.map { card ->
            MarketCardTelemetry(
                cardName = card.cardName,
                type = card.type,
                cost = card.cost,
                groveExposureCount = card.groveExposureCount,
                purchaseCount = card.purchaseCount,
                winShareOnExposureSum = card.winShareOnExposureSum
            )
        }
    )
}
