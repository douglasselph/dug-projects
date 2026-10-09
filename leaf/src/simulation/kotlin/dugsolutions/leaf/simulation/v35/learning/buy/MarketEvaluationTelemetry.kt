package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.v35.plant.PlantValueResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.chronicle.domain.BuyDecisionOutcome
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind

/** Identifies which side of a matched Buy-policy evaluation produced telemetry. */
internal enum class MarketEvaluationRole {
    CONTROL,
    LEARNED,
    UNSPECIFIED
}

/** Immutable per-card market telemetry. Raw counts only; no derived rate is stored here. */
internal data class MarketAlternativeTelemetry(
    val kind: PurchaseKind?,
    val itemName: String?,
    val cost: Int?,
    val outcome: BuyDecisionOutcome,
    val count: Long
)

internal data class MarketCardOpportunityTelemetry(
    val marketDecisionCount: Long,
    val affordableDecisionCount: Long,
    val graftableDecisionCount: Long,
    val legalDecisionCount: Long,
    val selectedDecisionCount: Long,
    val playerDoneWhileLegalCount: Long,
    val noLegalItemsWhileMarketCount: Long,
    val firstDecisionLegalCount: Long,
    val postPurchaseLegalCount: Long,
    val legalWithHigherCostPlantCount: Long,
    val purchasingPowerOnMarketDecisionSum: Long,
    val marketByCultivationRound: Map<Int, Long>,
    val affordableByCultivationRound: Map<Int, Long>,
    val legalByCultivationRound: Map<Int, Long>,
    val selectedByCultivationRound: Map<Int, Long>,
    val rejectedLegalAlternatives: List<MarketAlternativeTelemetry>
)

/** Immutable per-card market telemetry. Raw counts only; no derived rate is stored here. */
internal data class MarketCardTelemetry(
    val cardName: String,
    val type: PlantType,
    val cost: Int,
    val groveExposureCount: Long,
    val purchaseCount: Long,
    val winShareOnExposureSum: Double,
    val opportunity: MarketCardOpportunityTelemetry
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
    val learnedWinShareOnExposureSum: Double,
    val controlOpportunity: MarketCardOpportunityTelemetry,
    val learnedOpportunity: MarketCardOpportunityTelemetry
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
            learnedWinShareOnExposureSum = learnedCard.winShareOnExposureSum,
            controlOpportunity = controlCard.opportunity,
            learnedOpportunity = learnedCard.opportunity
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
        var winShareOnExposureSum: Double = 0.0,
        var marketDecisionCount: Long = 0,
        var affordableDecisionCount: Long = 0,
        var graftableDecisionCount: Long = 0,
        var legalDecisionCount: Long = 0,
        var selectedDecisionCount: Long = 0,
        var playerDoneWhileLegalCount: Long = 0,
        var noLegalItemsWhileMarketCount: Long = 0,
        var firstDecisionLegalCount: Long = 0,
        var postPurchaseLegalCount: Long = 0,
        var legalWithHigherCostPlantCount: Long = 0,
        var purchasingPowerOnMarketDecisionSum: Long = 0,
        val marketByCultivationRound: MutableMap<Int, Long> = sortedMapOf(),
        val affordableByCultivationRound: MutableMap<Int, Long> = sortedMapOf(),
        val legalByCultivationRound: MutableMap<Int, Long> = sortedMapOf(),
        val selectedByCultivationRound: MutableMap<Int, Long> = sortedMapOf(),
        val rejectedLegalAlternatives: MutableMap<AlternativeKey, Long> = linkedMapOf()
    )

    private data class AlternativeKey(
        val kind: PurchaseKind?,
        val itemName: String?,
        val cost: Int?,
        val outcome: BuyDecisionOutcome
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

    fun recordBuyDecision(entry: GameEntry.BuyDecision) {
        val higherLegalCosts = entry.plants.asSequence().filter { it.legal }.map { it.cost }.toList()
        entry.plants.forEach { plant ->
            val card = cardsByName[plant.cardName]
                ?: error("Buy decision references Plant not present in market telemetry catalog: ${plant.cardName}")
            card.marketDecisionCount++
            card.purchasingPowerOnMarketDecisionSum += entry.purchasingPower.toLong()
            entry.cultivationRoundNumber?.let { round -> card.marketByCultivationRound.bump(round) }
            if (plant.affordable) {
                card.affordableDecisionCount++
                entry.cultivationRoundNumber?.let { round -> card.affordableByCultivationRound.bump(round) }
            }
            if (plant.graftable) card.graftableDecisionCount++
            if (plant.legal) {
                card.legalDecisionCount++
                if (entry.purchasesMadeThisBuy == 0) card.firstDecisionLegalCount++ else card.postPurchaseLegalCount++
                if (higherLegalCosts.any { it > plant.cost }) card.legalWithHigherCostPlantCount++
                if (entry.outcome == BuyDecisionOutcome.PLAYER_DONE) card.playerDoneWhileLegalCount++
                entry.cultivationRoundNumber?.let { round -> card.legalByCultivationRound.bump(round) }

                val selectedThisPlant = entry.selectedKind == PurchaseKind.PLANT && entry.selectedItemName == plant.cardName
                if (!selectedThisPlant) {
                    val key = when (entry.outcome) {
                        BuyDecisionOutcome.PURCHASE -> AlternativeKey(
                            kind = requireNotNull(entry.selectedKind) { "PURCHASE Buy decision missing selectedKind" },
                            itemName = requireNotNull(entry.selectedItemName) { "PURCHASE Buy decision missing selectedItemName" },
                            cost = requireNotNull(entry.selectedCost) { "PURCHASE Buy decision missing selectedCost" },
                            outcome = entry.outcome
                        )
                        BuyDecisionOutcome.PLAYER_DONE -> AlternativeKey(null, null, null, entry.outcome)
                        BuyDecisionOutcome.NO_LEGAL_ITEMS -> error("Plant ${plant.cardName} cannot be legal in a NO_LEGAL_ITEMS Buy decision")
                    }
                    card.rejectedLegalAlternatives[key] = (card.rejectedLegalAlternatives[key] ?: 0L) + 1L
                }
            }
            if (entry.outcome == BuyDecisionOutcome.NO_LEGAL_ITEMS) card.noLegalItemsWhileMarketCount++
            if (entry.selectedKind == PurchaseKind.PLANT && entry.selectedItemName == plant.cardName) {
                card.selectedDecisionCount++
                entry.cultivationRoundNumber?.let { round -> card.selectedByCultivationRound.bump(round) }
            }
        }
    }

    private fun MutableMap<Int, Long>.bump(key: Int) { this[key] = (this[key] ?: 0L) + 1L }

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
                winShareOnExposureSum = card.winShareOnExposureSum,
                opportunity = MarketCardOpportunityTelemetry(
                    marketDecisionCount = card.marketDecisionCount,
                    affordableDecisionCount = card.affordableDecisionCount,
                    graftableDecisionCount = card.graftableDecisionCount,
                    legalDecisionCount = card.legalDecisionCount,
                    selectedDecisionCount = card.selectedDecisionCount,
                    playerDoneWhileLegalCount = card.playerDoneWhileLegalCount,
                    noLegalItemsWhileMarketCount = card.noLegalItemsWhileMarketCount,
                    firstDecisionLegalCount = card.firstDecisionLegalCount,
                    postPurchaseLegalCount = card.postPurchaseLegalCount,
                    legalWithHigherCostPlantCount = card.legalWithHigherCostPlantCount,
                    purchasingPowerOnMarketDecisionSum = card.purchasingPowerOnMarketDecisionSum,
                    marketByCultivationRound = card.marketByCultivationRound.toMap(),
                    affordableByCultivationRound = card.affordableByCultivationRound.toMap(),
                    legalByCultivationRound = card.legalByCultivationRound.toMap(),
                    selectedByCultivationRound = card.selectedByCultivationRound.toMap(),
                    rejectedLegalAlternatives = card.rejectedLegalAlternatives.entries
                        .sortedWith(compareBy({ it.key.outcome.name }, { it.key.kind?.name ?: "" }, { it.key.itemName ?: "" }, { it.key.cost ?: -1 }))
                        .map { (key, count) ->
                            MarketAlternativeTelemetry(key.kind, key.itemName, key.cost, key.outcome, count)
                        }
                )
            )
        }
    )
}
