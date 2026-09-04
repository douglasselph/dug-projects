package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieSizeRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlayerRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BattleRoundResourceEffectTest {

    private val handler = ResourceEffectHandler()

    @Test
    fun gainAnyDieOffersAvailableSizesAndUsesChosenSize() {
        val strategy = LastResourceChoiceStrategy()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.GAIN_ANY_DIE_TO_DISCARD),
            GameEffectExecutor { }
        )

        assertEquals(DieSides.entries.filter { it != DieSides.D4 }, strategy.seenDieSizes)
        assertEquals(20, actor.dice.discard.single().sides)
        assertEquals(8, game.grove.graftBed.count(DieSides.D20))
    }

    @Test
    fun gainAnyDieRejectsUnavailableD4Choice() {
        val actor = EffectTestFixture.player(
            1,
            effectStrategy = IllegalD4Strategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        assertEquals(0, game.grove.graftBed.count(DieSides.D4))
        assertFailsWith<InvalidDecisionException> {
            handler.execute(
                EffectTestFixture.request(game, actor, GameEffect.GAIN_ANY_DIE_TO_DISCARD),
                GameEffectExecutor { }
            )
        }
        assertTrue(actor.dice.discard.isEmpty())
    }

    @Test
    fun beguileLetsActorChooseEligibleOpponentThenStealsOneRandomWisp() {
        val strategy = LastResourceChoiceStrategy()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val p2 = EffectTestFixture.player(2)
        val p3 = EffectTestFixture.player(3)
        p2.wisps.add(wisp("P2"))
        p3.wisps.add(wisp("P3"))
        val game = EffectTestFixture.game(actor, p2, p3)

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT
            ),
            GameEffectExecutor { }
        )

        assertEquals(listOf(PlayerId(2), PlayerId(3)), strategy.seenPlayers)
        assertFalse(p2.wisps.isEmpty)
        assertTrue(p3.wisps.isEmpty)
        assertEquals(listOf("P3"), actor.wisps.cards.cards.map { it.name })
    }

    @Test
    fun beguileRejectsOpponentWithoutWisp() {
        val actor = EffectTestFixture.player(
            1,
            effectStrategy = IllegalPlayerStrategy(PlayerId(3))
        )
        val p2 = EffectTestFixture.player(2).also { it.wisps.add(wisp("P2")) }
        val p3 = EffectTestFixture.player(3)
        val game = EffectTestFixture.game(actor, p2, p3)

        assertFailsWith<InvalidDecisionException> {
            handler.execute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT
                ),
                GameEffectExecutor { }
            )
        }
        assertEquals(1, p2.wisps.size)
        assertTrue(actor.wisps.isEmpty)
    }

    @Test
    fun enthrallStealsOneRandomWispFromEveryOpponentWhoHasOne() {
        val actor = EffectTestFixture.player(1)
        val p2 = EffectTestFixture.player(2).also { it.wisps.add(wisp("P2")) }
        val p3 = EffectTestFixture.player(3)
        val p4 = EffectTestFixture.player(4).also { it.wisps.add(wisp("P4")) }
        val game = EffectTestFixture.game(actor, p2, p3, p4)

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS
            ),
            GameEffectExecutor { }
        )

        assertTrue(p2.wisps.isEmpty)
        assertTrue(p3.wisps.isEmpty)
        assertTrue(p4.wisps.isEmpty)
        assertEquals(setOf("P2", "P4"), actor.wisps.cards.cards.map { it.name }.toSet())
    }

    @Test
    fun stealEffectsAreNotExecutableWhenNoOpponentHasWisps() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        assertFalse(
            handler.canExecute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT
                )
            )
        )
        assertFalse(
            handler.canExecute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS
                )
            )
        )
    }

    @Test
    fun immediateWispGainedInBattlePreservesBattleStateForNestedExecution() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        val game = gameWithWisps(actor, other, listOf(immediateWisp()))
        val battleState = BattleState(listOf(actor, other))
        val nested = RecordingExecutor()
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.GAIN_ONE_WISP
        ).copy(
            phase = GameEffectPhase.BATTLE,
            battleState = battleState
        )

        handler.execute(request, nested)

        assertEquals(1, nested.requests.size)
        assertSame(battleState, nested.requests.single().battleState)
        assertEquals(GameEffectPhase.BATTLE, nested.requests.single().phase)
    }

    private fun wisp(name: String): WispCard =
        WispCard(
            quantity = 1,
            name = name,
            title = name,
            count = 1,
            effect = GameEffect.GAIN_ONE_VP,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = ""
        )

    private fun immediateWisp(): WispCard =
        WispCard(
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

    private fun gameWithWisps(
        actor: dugsolutions.leaf.v35.player.Player,
        other: dugsolutions.leaf.v35.player.Player,
        cards: List<WispCard>
    ): Game {
        val base = EffectTestFixture.game(actor, other)
        val manager = WispCardManager().apply { loadCards(cards) }
        val deck = WispDeck(manager, IdentityRandomizer()).apply { reset() }
        return Game(
            config = base.config,
            grove = Grove(base.config.selectedPlantCards, deck),
            players = base.players,
            chronicle = base.chronicle,
            roundDeck = base.roundDeck,
            randomizer = base.randomizer,
            dieFactory = base.dieFactory
        )
    }

    private open class FirstStrategy : EffectStrategy {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            request.legalChoices.first()
    }

    private class LastResourceChoiceStrategy : FirstStrategy() {
        var seenDieSizes: List<DieSides> = emptyList()
        var seenPlayers: List<PlayerId> = emptyList()

        override fun chooseDieSize(request: ChooseEffectDieSizeRequest): DieSides {
            seenDieSizes = request.legalChoices
            return request.legalChoices.last()
        }

        override fun choosePlayer(request: ChooseEffectPlayerRequest): PlayerId {
            seenPlayers = request.legalChoices
            return request.legalChoices.last()
        }
    }

    private class IllegalD4Strategy : FirstStrategy() {
        override fun chooseDieSize(request: ChooseEffectDieSizeRequest): DieSides =
            DieSides.D4
    }

    private class IllegalPlayerStrategy(
        private val playerId: PlayerId
    ) : FirstStrategy() {
        override fun choosePlayer(request: ChooseEffectPlayerRequest): PlayerId = playerId
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
