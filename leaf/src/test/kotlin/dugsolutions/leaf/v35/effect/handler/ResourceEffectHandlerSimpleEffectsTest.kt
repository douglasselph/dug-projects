package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceEffectHandlerSimpleEffectsTest {

    private val handler = ResourceEffectHandler()

    @Test
    fun rootWellCultivationBranchGainsWaterButBattleBranchWaits() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val cultivation = EffectTestFixture.request(
            game,
            actor,
            GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE
        )

        assertTrue(handler.canExecute(cultivation))
        handler.execute(cultivation, GameEffectExecutor { })

        assertEquals(1, actor.tokens.waterCount)
        assertFalse(
            handler.canExecute(
                cultivation.copy(phase = GameEffectPhase.BATTLE)
            )
        )
    }

    @Test
    fun rootLootAndPocketedSparkStoreDiscardDieAsPendingMulch() {
        listOf(
            GameEffect.MULCH_DIE_FROM_DISCARD,
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD
        ).forEach { effect ->
            val actor = EffectTestFixture.player(1)
            val die = FixedEffectDie(10, 7)
            actor.dice.addToDiscard(die)
            val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

            handler.execute(
                EffectTestFixture.request(game, actor, effect),
                GameEffectExecutor { }
            )

            assertTrue(actor.dice.discard.isEmpty())
            assertEquals(
                listOf(Token.PENDING_MULCH(DieSides.D10)),
                actor.tokens.pendingMulchTokens
            )
            assertEquals(8, game.grove.tokens.mulchCount)
        }
    }

    @Test
    fun fixedDieGainTakesGraftBedDieAndAddsUnrolledDieToDiscard() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.GAIN_D12_TO_DISCARD),
            GameEffectExecutor { }
        )

        assertEquals(8, game.grove.graftBed.count(DieSides.D12))
        assertEquals(1, actor.dice.discardSize)
        assertEquals(12, actor.dice.discard.single().sides)
        assertEquals(1, actor.dice.discard.single().value)
    }

    @Test
    fun pollinatingWispTakesButterflyFromGroveThenRefreshesItWhenAlreadyOwned() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY
        )

        handler.execute(request, GameEffectExecutor { })

        assertTrue(Butterfly.GREEN in actor.butterflies.all)
        assertFalse(Butterfly.GREEN in game.grove.butterflies.all)

        actor.butterflies.faceDown(Butterfly.GREEN)
        handler.execute(request, GameEffectExecutor { })

        assertTrue(actor.butterflies.isFaceUp(Butterfly.GREEN))
    }

    @Test
    fun pollinatingWispMovesMatchingButterflyFromAnotherPlayer() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, other)

        check(game.grove.butterflies.remove(Butterfly.PURPLE))
        other.butterflies.add(Butterfly.PURPLE)

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY
            ),
            GameEffectExecutor { }
        )

        assertFalse(Butterfly.PURPLE in other.butterflies.all)
        assertTrue(Butterfly.PURPLE in actor.butterflies.all)
    }


    @Test
    fun whisperingWingsGainsUpToTwoChosenCritters() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_ANY_TWO_CRITTERS
            ),
            GameEffectExecutor { }
        )

        assertEquals(2, actor.critters.size)
        assertEquals(16, game.grove.critters.size)
    }

    @Test
    fun gainTwoWormsTakesUpToAvailableSupply() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_TWO_WORMS
            ),
            GameEffectExecutor { }
        )

        assertEquals(2, actor.critters.count(dugsolutions.leaf.v35.tokens.Critter.WORM))
        assertEquals(7, game.grove.critters.count(dugsolutions.leaf.v35.tokens.Critter.WORM))
    }

    @Test
    fun gainOneWispAddsNormalWispToPlayerHand() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        val normal = WispCard(
            quantity = 1,
            name = "Simple_Wisp",
            title = "Simple Wisp",
            count = 1,
            effect = GameEffect.GAIN_ONE_VP,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            playImmediately = false
        )
        val game = gameWithWisps(actor, other, listOf(normal))
        val request = EffectTestFixture.request(game, actor, GameEffect.GAIN_ONE_WISP)

        handler.execute(request, GameEffectExecutor { })

        assertEquals(listOf(normal), actor.wisps.cards.cards)
        assertEquals(0, game.grove.wispDeck.remaining)
    }

    @Test
    fun gainOneWispImmediatelyExecutesWispquakeStyleCardInsteadOfHoldingIt() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        val immediate = WispCard(
            quantity = 1,
            name = "Immediate",
            title = "Immediate",
            count = 1,
            effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            playImmediately = true
        )
        val game = gameWithWisps(actor, other, listOf(immediate))
        val nested = RecordingExecutor()

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.GAIN_ONE_WISP),
            nested
        )

        assertTrue(actor.wisps.isEmpty)
        assertEquals(
            listOf(GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN),
            nested.requests.map { it.effect }
        )
    }

    private fun gameWithWisps(
        actor: dugsolutions.leaf.v35.player.Player,
        other: dugsolutions.leaf.v35.player.Player,
        cards: List<WispCard>
    ): Game {
        val base = EffectTestFixture.game(actor, other)
        val manager = WispCardManager().apply {
            loadCards(cards)
        }
        val deck = WispDeck(manager, IdentityRandomizer()).apply {
            reset()
        }
        val grove = Grove(
            selectedPlantCards = base.config.selectedPlantCards,
            wispDeck = deck
        )
        return Game(
            config = base.config,
            grove = grove,
            players = base.players,
            chronicle = base.chronicle,
            roundDeck = base.roundDeck,
            randomizer = base.randomizer,
            dieFactory = base.dieFactory
        )
    }

    private class RecordingExecutor : GameEffectExecutor {
        val requests = mutableListOf<GameEffectRequest>()
        override fun execute(request: GameEffectRequest) {
            requests += request
        }
    }

    private class IdentityRandomizer : Randomizer {
        override fun nextBoolean(): Boolean = false
        override fun nextInt(from: Int, until: Int): Int = from
        override fun nextInt(until: Int): Int = 0
        override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
        override fun <T> shuffled(list: List<T>): List<T> = list
    }
}
