package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.SunlightTokenChange
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A05: lock down the official CSV -> resolved Round effect -> real token transfer. */
class A05SunlightSourceOfTruthTest {

    private val cards by lazy {
        RoundCardRegistry(GameEffectConverter()).apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
        }.getAllCards()
    }

    @Test
    fun allThreeOfficialSunlightRoundsResolveToGainTokenNotRaiseThree() {
        val sunlight = cards.filter { it.type == RoundCardType.CULTIVATION }
            .flatMap { card -> listOf(card.firstEffect, card.secondEffect).map { card to it } }
            .filter { (_, effect) -> effect.title.equals("Sunlight", ignoreCase = true) }

        assertEquals(3, sunlight.size)
        assertEquals(
            setOf("Resource_Sunlight_Compost", "Resource_Sunlight_Mulch", "Resource_Sunlight_Water"),
            sunlight.map { it.first.name }.toSet()
        )
        sunlight.forEach { (card, effect) ->
            assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, effect.effect, card.name)
        }
        assertFalse(cards.any { card ->
            card.firstEffect.effect == GameEffect.RAISE_DIE_PLUS_3 ||
                card.secondEffect.effect == GameEffect.RAISE_DIE_PLUS_3
        }, "Legacy Raise +3 must not be selected by any official Round card")
    }

    @Test
    fun canonicalResolverPreservesTokenEffectOnEverySunlightRound() {
        cards.filter { it.type == RoundCardType.CULTIVATION }
            .filter { it.firstEffect.title == "Sunlight" || it.secondEffect.title == "Sunlight" }
            .forEach { card ->
                val resolved = RoundValueResolver.CANONICAL.cardFor(card)
                assertEquals(card, resolved)
                assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, resolved.firstEffect.effect)
            }
    }

    /** A02 requires one actual legal card in each cost/type Grove slot. */
    private fun legalGame(actor: Player): Game {
        val randomizer = Randomizer.create(123L)
        val registry = PlantCardRegistry().apply {
            loadFromCsv(
                CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
            )
        }
        val catalog = PlantCardManager().apply { loadCards(registry) }
        val selected = FirstGameDefault.PLANT_NAMES.map { name ->
            requireNotNull(catalog.getCard(name))
        }
        val players = listOf(actor, Player(PlayerId(2), DecisionDirector.baseline()))
        val config = GameConfig(
            selectedPlantCards = selected,
            playerDecisionFactories = List(2) { PlayerDecisionFactory.humanBaseline() },
            roundSetup = GameRoundSetup.Ordered(1, 0),
            seed = 123L
        )
        val roundDeck = RoundDeck(
            RoundCardManager().apply { loadCards(cards) },
            randomizer
        ).apply { setup(numBattle = 0, numCultivation = 1) }
        return Game(
            config = config,
            grove = Grove(selected, WispDeck(WispCardManager(), randomizer)),
            players = players,
            chronicle = GameChronicle(),
            roundDeck = roundDeck,
            randomizer = randomizer
        )
    }

    @Test
    fun eachOfficialSunlightRoundGainsTokenWithoutChangingAnyDice() {
        val executor = DefaultGameEffectExecutor()
        val sunlightRounds = cards.filter { it.firstEffect.title == "Sunlight" && it.type == RoundCardType.CULTIVATION }
        assertEquals(3, sunlightRounds.size)

        sunlightRounds.forEach { card ->
            val die = object : Die(6) {
                override fun roll(): Die = this
            }.apply { adjustTo(2) }
            val actor = Player(
                id = PlayerId(1),
                decisions = DecisionDirector.baseline(),
                dice = PlayerDice(hand = listOf(die))
            )
            val game = legalGame(actor)
            val beforeTokens = game.grove.tokens.sunlightCount
            val request = GameEffectRequest(
                game = game,
                actor = actor,
                effect = RoundValueResolver.CANONICAL.effectFor(card, RoundEffectSlot.FIRST),
                source = GameEffectSource.Round(card, RoundEffectSlot.FIRST),
                phase = GameEffectPhase.CULTIVATION
            )

            assertTrue(executor.canExecute(request), card.name)
            executor.execute(request)

            assertEquals(2, die.value, "Sunlight must not raise a die: ${card.name}")
            assertEquals(1, actor.tokens.sunlightCount, card.name)
            assertEquals(beforeTokens - 1, game.grove.tokens.sunlightCount, card.name)
            assertTrue(game.chronicle.entries.none { it is GameEntry.DieValueChanged }, card.name)
            val gains = game.chronicle.entries.filterIsInstance<GameEntry.SunlightTokenChanged>()
            assertEquals(1, gains.size, card.name)
            assertEquals(SunlightTokenChange.GAINED, gains.single().change)
        }
    }
}
