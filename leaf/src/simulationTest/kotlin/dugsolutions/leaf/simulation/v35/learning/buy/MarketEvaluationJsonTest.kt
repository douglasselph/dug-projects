package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.chronicle.domain.BuyDecisionOutcome
import dugsolutions.leaf.v35.chronicle.domain.BuyPlantOpportunitySnapshot
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketEvaluationJsonTest {
    @Test
    fun `machine readable JSON includes schema and every expected market card`() {
        val cards = completeMarketFixture()
        val document = document(cards)

        val json = MarketEvaluationJsonWriter.render(document)

        assertContains(json, "\"schema\": \"$MARKET_EVALUATION_JSON_SCHEMA\"")
        assertContains(json, "\"schemaVersion\": $MARKET_EVALUATION_JSON_VERSION")
        cards.forEach { card ->
            assertEquals(1, Regex("\\\"identity\\\": \\\"${Regex.escape(card.name)}\\\"").findAll(json).count(), card.name)
        }
        assertEquals(36, Regex("\\\"identity\\\":").findAll(json).count())
    }

    @Test
    fun `zero exposure and purchase values serialize explicitly`() {
        val card = card("Flower_17_04", PlantType.FLOWER, 17)
        val json = MarketEvaluationJsonWriter.render(document(listOf(card)))

        assertContains(json, "\"controlExposure\": 0")
        assertContains(json, "\"learnedExposure\": 0")
        assertContains(json, "\"controlPurchases\": 0")
        assertContains(json, "\"learnedPurchases\": 0")
        assertContains(json, "\"controlWinShareOnExposureSum\": 0.0")
        assertContains(json, "\"learnedWinShareOnExposureSum\": 0.0")
        assertContains(json, "\"controlOpportunity\": {")
        assertContains(json, "\"marketDecisionCount\": 0")
    }

    @Test
    fun `raw exposure and purchase values survive serialization exactly`() {
        val a = card("Vine_09_02", PlantType.VINE, 9)
        val control = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.CONTROL, listOf(a)).apply {
            recordGame(listOf(a), 0.25)
            recordGame(listOf(a), 0.50)
            recordPlantPurchase(a.name)
            recordSelectedPlantDecision(a, purchasingPower = 9)
        }.snapshot()
        val learned = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.LEARNED, listOf(a)).apply {
            recordGame(listOf(a), 0.75)
            recordGame(listOf(a), 1.0)
            repeat(3) { recordPlantPurchase(a.name) }
            repeat(3) { recordSelectedPlantDecision(a, purchasingPower = 12) }
        }.snapshot()

        val json = MarketEvaluationJsonWriter.render(
            document(MarketComparisonTelemetry(control, learned))
        )

        assertContains(json, "\"controlExposure\": 2")
        assertContains(json, "\"learnedExposure\": 2")
        assertContains(json, "\"controlPurchases\": 1")
        assertContains(json, "\"learnedPurchases\": 3")
        assertContains(json, "\"controlWinShareOnExposureSum\": 0.75")
        assertContains(json, "\"learnedWinShareOnExposureSum\": 1.75")
        assertContains(json, "\"plantPurchases\": 1")
        assertContains(json, "\"plantPurchases\": 3")
    }

    @Test
    fun `deterministic fixture renders stable v3 opportunity JSON`() {
        val a = card("Root_05_01", PlantType.ROOT, 5)
        val control = MarketEvaluationTelemetryAccumulator(2, MarketEvaluationRole.CONTROL, listOf(a)).apply {
            recordGame(listOf(a), 0.5)
            recordPlantPurchase(a.name)
            recordSelectedPlantDecision(a, purchasingPower = 5)
        }.snapshot()
        val learned = MarketEvaluationTelemetryAccumulator(2, MarketEvaluationRole.LEARNED, listOf(a)).apply {
            recordGame(listOf(a), 1.0)
            repeat(2) { recordPlantPurchase(a.name) }
            repeat(2) { recordSelectedPlantDecision(a, purchasingPower = 6) }
        }.snapshot()

        val document = MarketEvaluationJsonDocument(
            metadata = MarketEvaluationJsonMetadata(
                players = 2, samples = 1, mechanicalSeedStart = 100, strategySeedStart = 200,
                groveSeedStart = 300, grovePattern = "RANDOM", roundPattern = "3/2/2",
                policyPath = "weights/test.weights", plantOverridesPath = "plant.csv",
                plantOverridesSha256 = "abc", roundOverridesPath = null, roundOverridesSha256 = null
            ),
            comparison = MarketComparisonTelemetry(control, learned)
        )
        val first = MarketEvaluationJsonWriter.render(document)
        val second = MarketEvaluationJsonWriter.render(document)

        assertEquals(first, second)
        assertContains(first, "\"schemaVersion\": 3")
        assertContains(first, "\"controlOpportunity\": {")
        assertContains(first, "\"selectedDecisionCount\": 1")
        assertContains(first, "\"learnedOpportunity\": {")
        assertContains(first, "\"selectedDecisionCount\": 2")
    }

    @Test
    fun `writer creates parent directories and writes rendered document`() {
        val temp = Files.createTempDirectory("market-json-test")
        val output = temp.resolve("nested/market.json")
        val document = document(listOf(card("Root_05_01", PlantType.ROOT, 5)))

        MarketEvaluationJsonWriter.write(output, document)

        assertTrue(Files.isRegularFile(output))
        assertEquals(MarketEvaluationJsonWriter.render(document), Files.readString(output))
    }

    @Test
    fun `eval options accepts optional market json path`() {
        val options = EvalOptions.parse(listOf("--games", "5", "--market-json", "output/raw-market.json"))
        assertEquals("output/raw-market.json", options.marketJsonPath.toString())
    }

    private fun MarketEvaluationTelemetryAccumulator.recordSelectedPlantDecision(
        card: PlantCard,
        purchasingPower: Int
    ) {
        recordBuyDecision(
            GameEntry.BuyDecision(
                sequence = 1,
                playerId = PlayerId(1),
                roundNumber = 1,
                cultivationRoundNumber = 1,
                purchasesMadeThisBuy = 0,
                purchasingPower = purchasingPower,
                plants = listOf(BuyPlantOpportunitySnapshot(card.name, card.cost, 1, true, true, true)),
                outcome = BuyDecisionOutcome.PURCHASE,
                selectedKind = PurchaseKind.PLANT,
                selectedItemName = card.name,
                selectedCost = card.cost
            )
        )
    }

    private fun document(cards: List<PlantCard>): MarketEvaluationJsonDocument {
        val control = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.CONTROL, cards).snapshot()
        val learned = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.LEARNED, cards).snapshot()
        return document(MarketComparisonTelemetry(control, learned))
    }

    private fun document(comparison: MarketComparisonTelemetry): MarketEvaluationJsonDocument = MarketEvaluationJsonDocument(
        metadata = MarketEvaluationJsonMetadata(
            players = comparison.playerCount,
            samples = comparison.sampleCount.toInt(),
            mechanicalSeedStart = 10,
            strategySeedStart = 20,
            groveSeedStart = null,
            grovePattern = null,
            roundPattern = "3/2/2",
            policyPath = "test.weights",
            plantOverridesPath = null,
            plantOverridesSha256 = null,
            roundOverridesPath = null,
            roundOverridesSha256 = null
        ),
        comparison = comparison
    )

    private fun completeMarketFixture(): List<PlantCard> = buildList {
        fun addSlot(prefix: String, type: PlantType, cost: Int) {
            (1..4).forEach { index -> add(card("${prefix}_${cost.toString().padStart(2, '0')}_${index.toString().padStart(2, '0')}", type, cost)) }
        }
        addSlot("Root", PlantType.ROOT, 5)
        addSlot("Root", PlantType.ROOT, 7)
        addSlot("Root", PlantType.ROOT, 9)
        addSlot("Vine", PlantType.VINE, 7)
        addSlot("Vine", PlantType.VINE, 9)
        addSlot("Vine", PlantType.VINE, 11)
        addSlot("Flower", PlantType.FLOWER, 11)
        addSlot("Flower", PlantType.FLOWER, 14)
        addSlot("Flower", PlantType.FLOWER, 17)
    }

    private fun card(name: String, type: PlantType, cost: Int): PlantCard = PlantCard(
        quantity = 1,
        name = name,
        title = name,
        type = type,
        cost = cost,
        lineIcon = null,
        vpIcon = "",
        typeIcon = "",
        fgColor = "",
        textColor = "",
        fullImage = "",
        backgroundImage = "",
        cardBackgroundImage = "",
        effect = GameEffect.GAIN_ONE_VP,
        scoringRule = PlantScoringRule.Fixed(0)
    )
}
