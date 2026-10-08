package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.OwnedDiceSignature
import dugsolutions.leaf.simulation.v35.analysis.PlantCreatureSignature
import dugsolutions.leaf.simulation.v35.analysis.PlayerGameSummary
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.plant.PlantValueResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketEvaluationTelemetryTest {
    @Test
    fun `records exact per-card Grove exposure and win share directly from resolved Grove`() {
        val a = card("Root_05_01", PlantType.ROOT, 5)
        val b = card("Vine_07_01", PlantType.VINE, 7)
        val acc = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.LEARNED, listOf(a, b))

        acc.recordGame(listOf(a), 0.25)
        acc.recordGame(listOf(a, b), 0.50)

        val result = acc.snapshot()
        assertEquals(2L, result.sampleCount)
        assertEquals(0.375, result.overallWinShare)
        assertEquals(2L, result.cardsByName.getValue(a.name).groveExposureCount)
        assertEquals(0.75, result.cardsByName.getValue(a.name).winShareOnExposureSum)
        assertEquals(1L, result.cardsByName.getValue(b.name).groveExposureCount)
        assertEquals(0.50, result.cardsByName.getValue(b.name).winShareOnExposureSum)
    }

    @Test
    fun `duplicate Plant identity in one Grove counts as one game exposure`() {
        val a = card("Root_05_01", PlantType.ROOT, 5)
        val acc = MarketEvaluationTelemetryAccumulator(2, MarketEvaluationRole.CONTROL, listOf(a))

        acc.recordGame(listOf(a, a), 0.5)

        assertEquals(1L, acc.snapshot().cardsByName.getValue(a.name).groveExposureCount)
    }

    @Test
    fun `records exact per-card purchases including multiple copies`() {
        val a = card("Root_05_01", PlantType.ROOT, 5)
        val b = card("Vine_07_01", PlantType.VINE, 7)
        val acc = MarketEvaluationTelemetryAccumulator(3, MarketEvaluationRole.LEARNED, listOf(a, b))

        acc.recordPlantPurchase(a.name)
        acc.recordPlantPurchase(a.name)
        acc.recordPlantPurchase(b.name)

        val result = acc.snapshot()
        assertEquals(2L, result.cardsByName.getValue(a.name).purchaseCount)
        assertEquals(1L, result.cardsByName.getValue(b.name).purchaseCount)
        assertEquals(3L, result.totalPlantPurchases)
    }

    @Test
    fun `zero-purchase cards remain represented in typed result`() {
        val a = card("Root_05_01", PlantType.ROOT, 5)
        val b = card("Flower_17_04", PlantType.FLOWER, 17)
        val acc = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.LEARNED, listOf(a, b))

        acc.recordPlantPurchase(a.name)

        val result = acc.snapshot()
        assertEquals(setOf(a.name, b.name), result.cardsByName.keys)
        assertEquals(0L, result.cardsByName.getValue(b.name).purchaseCount)
        assertEquals(0L, result.cardsByName.getValue(b.name).groveExposureCount)
    }

    @Test
    fun `typed result preserves effective Plant type and cost metadata`() {
        val a = card("Vine_09_02", PlantType.VINE, 9)
        val override = object : PlantValueResolver {
            override fun typeFor(card: PlantCard) = PlantType.FLOWER
            override fun costFor(card: PlantCard) = 14
            override fun isAvailable(card: PlantCard) = true
        }
        val acc = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.LEARNED, listOf(a), override)

        val row = acc.snapshot().cards.single()
        assertEquals(PlantType.FLOWER, row.type)
        assertEquals(14, row.cost)
    }

    @Test
    fun `sum of per-card purchases equals typed total Plant purchases`() {
        val cards = listOf(
            card("Root_05_01", PlantType.ROOT, 5),
            card("Vine_09_02", PlantType.VINE, 9),
            card("Flower_17_04", PlantType.FLOWER, 17)
        )
        val acc = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.LEARNED, cards)
        repeat(3) { acc.recordPlantPurchase(cards[0].name) }
        repeat(2) { acc.recordPlantPurchase(cards[1].name) }
        acc.recordPlantPurchase(cards[2].name)

        val result = acc.snapshot()
        assertEquals(6L, result.cards.sumOf { it.purchaseCount })
        assertEquals(6L, result.totalPlantPurchases)
    }

    @Test
    fun `comparison preserves control and learned identities and raw purchase counts`() {
        val a = card("Vine_09_02", PlantType.VINE, 9)
        val control = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.CONTROL, listOf(a)).apply {
            recordGame(listOf(a), 0.25)
            recordPlantPurchase(a.name)
        }.snapshot()
        val learned = MarketEvaluationTelemetryAccumulator(4, MarketEvaluationRole.LEARNED, listOf(a)).apply {
            recordGame(listOf(a), 0.50)
            repeat(2) { recordPlantPurchase(a.name) }
        }.snapshot()

        val comparison = MarketComparisonTelemetry(control, learned)
        assertEquals(MarketEvaluationRole.CONTROL, comparison.control.role)
        assertEquals(MarketEvaluationRole.LEARNED, comparison.learned.role)
        assertEquals(1L, comparison.control.cardsByName.getValue(a.name).purchaseCount)
        assertEquals(2L, comparison.learned.cardsByName.getValue(a.name).purchaseCount)
        assertEquals(1L, comparison.cards.single().controlPurchaseCount)
        assertEquals(2L, comparison.cards.single().learnedPurchaseCount)
    }

    @Test
    fun `EvalAccumulator Plant purchase total reconciles with typed per-card telemetry`() {
        val a = card("Root_05_01", PlantType.ROOT, 5)
        val b = card("Vine_09_02", PlantType.VINE, 9)
        val player = PlayerId(1)
        val playerSummary = PlayerGameSummary(
            seat = 0,
            playerId = player,
            won = true,
            winShare = 1.0,
            existingVp = 0,
            plantVp = 0,
            unplayedWispVp = 0,
            totalVp = 0,
            battleStrikeVp = 0,
            woundsTaken = 0,
            rollRewardWispsGained = 0,
            wispsPlayed = 0,
            finalWispCount = 0,
            finalPlantCount = 0,
            finalPlantPrintedCost = 0,
            plantCreatureSignature = PlantCreatureSignature(emptyList()),
            finalDiceCount = 0,
            finalDicePower = 0,
            ownedDiceSignature = OwnedDiceSignature(0, 0, 0, 0, 0, 0)
        )
        val game = CompletedEvalGame(
            summary = GameSummary(1L, 2L, 1, listOf(player), listOf(playerSummary)),
            entries = listOf(
                GameEntry.Purchase(1, player, PurchaseKind.PLANT, a.name, 5, 5),
                GameEntry.Purchase(2, player, PurchaseKind.PLANT, a.name, 5, 6),
                GameEntry.Purchase(3, player, PurchaseKind.PLANT, b.name, 9, 9)
            )
        )
        val acc = EvalAccumulator(2, MarketEvaluationRole.LEARNED, listOf(a, b))

        acc.add(game, 0, mapOf(a.name to a, b.name to b), listOf(a, b))

        assertEquals(3L, acc.plantPurchases)
        assertEquals(3L, acc.marketTelemetry.totalPlantPurchases)
        assertEquals(acc.plantPurchases, acc.marketTelemetry.cards.sumOf { it.purchaseCount })
        assertEquals(1L, acc.marketTelemetry.sampleCount)
        assertEquals(1.0, acc.marketTelemetry.overallWinShare)
        assertEquals(1L, acc.marketTelemetry.cardsByName.getValue(a.name).groveExposureCount)
        assertEquals(1L, acc.marketTelemetry.cardsByName.getValue(b.name).groveExposureCount)
        assertEquals(2L, acc.marketTelemetry.cardsByName.getValue(a.name).purchaseCount)
        assertEquals(1L, acc.marketTelemetry.cardsByName.getValue(b.name).purchaseCount)
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
