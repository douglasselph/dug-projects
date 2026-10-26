package dugsolutions.leaf.simulation.v35.experiment.round

import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.SunlightTokenChange
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.round.RoundCoordinator
import dugsolutions.leaf.v35.game.round.RoundExecutor
import dugsolutions.leaf.v35.game.round.cultivation.CultivationRound
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.BuyStrategy
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.round.RoundValueResolver
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import dugsolutions.leaf.v35.effect.GameEffectConverter
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** B02: real CSV -> optional research CSV -> GameConfig -> round reveal ->
 * Cultivation Build Main action -> default effect executor -> Grove token transfer.
 * All cards and research files are the shipped assets, not synthetic replacements.
 */
class B02SunlightRoundRuntimeTest {
    private val sunlightNames = setOf(
        "Resource_Sunlight_Compost", "Resource_Sunlight_Mulch", "Resource_Sunlight_Water"
    )

    private val cards: List<RoundCard> by lazy {
        RoundCardRegistry(GameEffectConverter()).apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
        }.getAllCards()
    }

    @Test
    fun canonicalAndResearchResyncProduceTokensInActualCultivationRound() {
        val sunlight = cards.filter { it.name in sunlightNames }
        assertEquals(3, sunlight.size)
        assertTrue(sunlight.all { it.type == RoundCardType.CULTIVATION })

        // null = ordinary gameplay; current is header-only; token is the explicitly
        // authored three-row intervention; historical pre-official also changes Compost.
        val sources = listOf(
            null,
            "data/research/resync/round-resync-current.csv",
            "data/research/round-overrides/sunlight-token.csv",
            "data/research/resync/round-resync-pre-official-20261023.csv"
        )
        sources.forEach { source ->
            val research = RoundExperimentResearchConfig.resolve(source?.let { Paths.get(it) }, cards)
            if (source?.endsWith("round-resync-current.csv") == true) {
                assertTrue(research.values.overrides.isEmpty(), "Current resync file should be header-only")
            }
            sunlight.forEach { card ->
                val effective = research.values.cardFor(card)
                assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, effective.firstEffect.effect, "$source / ${card.name}")
                assertFalse(effective.firstEffect.effect == GameEffect.RAISE_DIE_PLUS_3)
                assertEquals(card.firstEffect.title, effective.firstEffect.title)
                assertEquals(card.secondEffect.title, effective.secondEffect.title)

                val players = listOf(scriptedPlayer(1), scriptedPlayer(2))
                val game = gameFor(card, research.values, players)
                val beforeSunlight = game.grove.tokens.sunlightCount
                val monitoredDice = players.map { it.dice.hand.single() }
                val revealed = RoundCoordinator(
                    cultivation = CultivationRound(DefaultGameEffectExecutor()),
                    battle = RoundExecutor { _, _ -> error("Expected Cultivation only") }
                ).executeNext(game)
                assertEquals(card.name, revealed?.card?.name)
                val entries = game.chronicle.entries
                val roundReveal = entries.filterIsInstance<GameEntry.RoundRevealed>().single()
                assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, roundReveal.firstEffect, "$source / ${card.name}")
                val sunlightActions = entries.filterIsInstance<GameEntry.MainAction>()
                    .filter { it.action == MainActionKind.ROUND_EFFECT_1 }
                assertEquals(2, sunlightActions.size, "$source / ${card.name}")
                val gains = entries.filterIsInstance<GameEntry.SunlightTokenChanged>()
                    .filter { it.change == SunlightTokenChange.GAINED }
                assertEquals(2, gains.size, "$source / ${card.name}")
                assertEquals(players.map { it.id }.toSet(), gains.map { it.playerId }.toSet())
                players.forEach { assertEquals(1, it.tokens.sunlightCount, "$source / ${card.name}") }
                assertEquals(beforeSunlight - 2, game.grove.tokens.sunlightCount)
                assertTrue(monitoredDice.all { it.value == 4 }, "$source / ${card.name}: Sunlight changed a die")
                assertTrue(entries.none { it is GameEntry.DieValueChanged }, "$source / ${card.name}: unexpected Raise")
                // Research resolution creates an effective view; canonical catalog stays unchanged.
                assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, card.firstEffect.effect)
            }
        }
    }

    @Test
    fun resyncOverridesAreScopedToNamedSlotAndDoNotChangeCanonicalSunlight() {
        val historical = RoundExperimentResearchConfig.resolve(
            Paths.get("data/research/resync/round-resync-pre-official-20261023.csv"), cards
        )
        val token = RoundExperimentResearchConfig.resolve(
            Paths.get("data/research/round-overrides/sunlight-token.csv"), cards
        )
        assertEquals(sunlightNames, token.values.overrides.map { it.roundCardName }.toSet())
        assertTrue(token.values.overrides.all {
            it.slot == RoundEffectSlot.FIRST && it.effect == GameEffect.GAIN_SUNLIGHT_TOKEN
        })
        assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN,
            historical.values.effectFor(cards.single { it.name == "Resource_Sunlight_Compost" }, RoundEffectSlot.FIRST))
        assertTrue(historical.values.overrides.any { it.slot == RoundEffectSlot.SECOND })
        assertTrue(cards.none { it.firstEffect.effect == GameEffect.RAISE_DIE_PLUS_3 ||
            it.secondEffect.effect == GameEffect.RAISE_DIE_PLUS_3 })
        val canonical = RoundValueResolver.CANONICAL
        sunlightNames.forEach { name ->
            assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN,
                canonical.effectFor(cards.single { it.name == name }, RoundEffectSlot.FIRST))
        }
    }

    private fun gameFor(card: RoundCard, resolver: RoundValueResolver, players: List<Player>): Game {
        val plantRegistry = PlantCardRegistry().apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
        }
        val manager = PlantCardManager().apply { loadCards(plantRegistry) }
        val groveCards = FirstGameDefault.PLANT_NAMES.map { requireNotNull(manager.getCard(it)) }
        val random = Randomizer.create(202602L)
        val roundManager = RoundCardManager().apply { loadCards(cards) }
        val deck = RoundDeck(roundManager, random).apply { setupExact(listOf(card)) }
        return Game(
            config = GameConfig(
                selectedPlantCards = groveCards,
                playerDecisionFactories = List(2) { PlayerDecisionFactory.humanBaseline() },
                roundSetup = GameRoundSetup.Ordered(1, 0),
                roundValues = resolver,
                seed = 202602L
            ),
            grove = Grove(groveCards, WispDeck(WispCardManager(), random)),
            players = players,
            chronicle = GameChronicle(),
            roundDeck = deck,
            randomizer = random
        )
    }

    private fun scriptedPlayer(id: Int): Player {
        val alwaysDrawAfterFirst = object : CultivationStrategy {
            override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
                if (request.mainActionsRemaining == 0) return CultivationAction.Done
                val options = request.legalChoices.filterIsInstance<CultivationAction.Main>().map { it.action }
                val chosen = if (request.mainActionsRemaining == 2) {
                    check(CultivationMainAction.RoundEffect1 in options) { "Sunlight action not legally offered" }
                    CultivationMainAction.RoundEffect1
                } else {
                    options.firstOrNull { it == CultivationMainAction.Draw } ?: options.first()
                }
                return CultivationAction.Main(chosen)
            }
        }
        val noBuy = object : BuyStrategy {
            override fun choosePurchase(request: ChoosePurchaseRequest): BuyChoice = BuyChoice.Done
            override fun choosePayment(request: ChoosePaymentRequest): BuyPayment = BuyPayment()
        }
        val director = DecisionDirector.humanBaseline().copy(cultivation = alwaysDrawAfterFirst, buy = noBuy)
        fun fixedDie(sides: Int) = object : Die(sides) {
            override fun roll(): Die = this
        }.apply { adjustTo(4) }
        return Player(PlayerId(id), director, dice = PlayerDice(
            hand = listOf(fixedDie(6)), supply = List(4) { fixedDie(6) }
        ))
    }
}
