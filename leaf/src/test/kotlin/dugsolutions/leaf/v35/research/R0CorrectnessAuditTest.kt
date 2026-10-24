package dugsolutions.leaf.v35.research

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyCardCatalog
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyProvenance
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedPolicyCardManifestMismatchException
import dugsolutions.leaf.v35.player.decision.learned.buy.PlantCardManifest
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.player.decision.buy.diagnostics.BuyDecisionEvent
import dugsolutions.leaf.v35.player.decision.buy.diagnostics.ShadowBuyStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.player.development.PlayerDevelopmentTracker
import org.junit.jupiter.api.Test
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** R0 correctness tests against the actual canonical CSVs and full GameRunner. */
class R0CorrectnessAuditTest {
    @Test
    fun canonicalGroveHasExactlyOneCardPerRequiredSlotAndOriginalSappingEffect() {
        val registry = PlantCardRegistry()
        registry.loadFromCsv(
            CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
            CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
        )
        assertEquals(36, registry.getAllCards().size)
        val grove = FirstGameDefault.PLANT_NAMES.map { assertNotNull(registry.getCard(it)) }
        assertEquals(9, grove.size)
        assertEquals(9, grove.map { it.name }.toSet().size)
        assertEquals(
            listOf(
                PlantType.ROOT to 5, PlantType.ROOT to 7, PlantType.ROOT to 9,
                PlantType.VINE to 7, PlantType.VINE to 9, PlantType.VINE to 11,
                PlantType.FLOWER to 11, PlantType.FLOWER to 14, PlantType.FLOWER to 17
            ),
            grove.map { it.type to it.cost }
        )
        val sapping = assertNotNull(registry.getCard("Flower_11_03"))
        assertEquals("Sapping Snapdragon", sapping.title)
        assertEquals(11, sapping.cost)
        assertEquals(GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW, sapping.effect)
        assertEquals(sapping, grove.first { it.name == "Flower_11_03" })
    }

    @Test
    fun trainedBuyCatalogFingerprintRejectsChangedSappingEffect() {
        val registry = PlantCardRegistry()
        registry.loadFromCsv(
            CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
            CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
        )
        val official = registry.getAllCards()
        val manifest = PlantCardManifest.from(official)
        val frozen = LearnedBuyWeights.zeros(
            provenance = LearnedBuyProvenance(trainingStatus = "trained", cardManifest = manifest)
        )
        assertEquals(manifest.catalogFingerprint,
            LearnedBuyCardCatalog.prepare(frozen, official).provenance.cardManifest?.catalogFingerprint)

        val variant = official.map { card ->
            if (card.name == "Flower_11_03") card.copy(
                effect = GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW
            ) else card
        }
        assertFailsWith<LearnedPolicyCardManifestMismatchException> {
            LearnedBuyCardCatalog.prepare(frozen, variant)
        }
    }

    @Test
    fun fullSeededGameIsIdenticalWithAndWithoutBuyShadowAndDevelopmentTracking() {
        val app = koinApplication { modules(appModules) }
        try {
            val koin = app.koin
            val plantRegistry = koin.get<PlantCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                    CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
            }
            koin.get<PlantCardManager>().loadCards(plantRegistry)
            val roundRegistry = koin.get<RoundCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
            }
            koin.get<RoundCardManager>().loadCards(roundRegistry)
            val wispRegistry = koin.get<WispCardRegistry>().apply {
                loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
            }
            koin.get<WispCardManager>().loadCards(wispRegistry)

            val grove = FirstGameDefault.PLANT_NAMES.map { assertNotNull(plantRegistry.getCard(it)) }
            val factory = koin.get<GameFactory>()
            val runner = koin.get<GameRunner>()
            val observations = mutableListOf<BuyDecisionEvent>()
            val observedFactory = object : PlayerDecisionFactory {
                private val delegate = PlayerDecisionFactory.humanBaseline()
                override fun create() = create(StrategyRandomizer.create(548121L))
                override fun create(strategyRandomizer: StrategyRandomizer) =
                    create(strategyRandomizer, DecisionReasoningSink.NONE)
                override fun create(strategyRandomizer: StrategyRandomizer, reasoningSink: DecisionReasoningSink) =
                    delegate.create(strategyRandomizer, reasoningSink).let { director ->
                        director.copy(buy = ShadowBuyStrategy.humanBaseline(
                            actual = director.buy,
                            shadowSeed = 19731L,
                            onComparison = {},
                            onEvent = { observations.add(it) }
                        ))
                    }
            }
            val normal = GameConfig(
                selectedPlantCards = grove,
                playerDecisionFactories = List(2) { PlayerDecisionFactory.humanBaseline() },
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 2, battleRounds = 1),
                seed = 300721L,
                strategySeed = 400721L
            )
            val observed = GameConfig(
                selectedPlantCards = grove,
                playerDecisionFactories = listOf(observedFactory, PlayerDecisionFactory.humanBaseline()),
                roundSetup = GameRoundSetup.Ordered(cultivationRounds = 2, battleRounds = 1),
                seed = 300721L,
                strategySeed = 400721L
            )
            val originalGame = factory(normal)
            val observedGame = factory(observed)
            val trackers = observedGame.players.map(PlayerDevelopmentTracker::attach)
            val originalResult = runner.run(originalGame)
            val observedResult = runner.run(observedGame)
            assertEquals(originalResult, observedResult, "R0: diagnostic hooks changed final game outcome")
            assertEquals(originalGame.chronicle.entries, observedGame.chronicle.entries,
                "R0: diagnostic hooks changed mechanical or strategy decisions")
            assertTrue(observations.isNotEmpty(), "R0: did not exercise shadow Buy comparisons")
            assertEquals(2, trackers.size)
            assertTrue(trackers.all { it.snapshot().dice.isNotEmpty() },
                "R0: development tracking missed players' dice")
        } finally {
            app.close()
        }
    }
}
