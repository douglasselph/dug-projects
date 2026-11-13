package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.CardEffectReadiness
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.player.decision.effect.ChooseWispsToKeepRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectWispsChoice
import dugsolutions.leaf.v35.error.EffectExecutionException
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RevisedWispTrashEffectTest {
    private val registry = WispCardRegistry(GameEffectConverter()).apply {
        loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST))
    }
    private val honor = card("Wisp_Award_VP")
    private val patient = card("Wisp_Award_VP2")
    private val spare = card("Wisp_Gain_Critters")
    private val other = card("Wisp_Mulch_Die")

    @Test
    fun honorAllowsOwnTargetButExcludesPlayedCopy() {
        val actor = EffectTestFixture.player(1).apply { wisps.add(honor).add(spare) }
        val rival = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, rival)
        val request = request(game, actor, honor)
        val executor = DefaultGameEffectExecutor()
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(listOf(honor), actor.wisps.cards.cards)
        assertEquals(0, rival.wisps.size)
        assertEquals(0, actor.vp)
        // The played Wisp is removed by the normal Wisp-play coordinator,
        // after the effect returns, not by this handler.
        assertTrue(actor.wisps.remove(honor))
        assertTrue(actor.wisps.isEmpty)
    }

    @Test
    fun honorMakesOpponentChooseTheirOwnVictim() {
        val actor = EffectTestFixture.player(1).apply { wisps.add(honor) }
        val rival = EffectTestFixture.player(2).apply { wisps.add(spare).add(other) }
        val game = EffectTestFixture.game(actor, rival)
        DefaultGameEffectExecutor().execute(request(game, actor, honor))
        assertEquals(listOf(spare), rival.wisps.cards.cards) // mechanical keeps the first
        assertEquals(listOf(honor), actor.wisps.cards.cards)
    }

    @Test
    fun honorWithOnlyItsOwnSourceHasNoLegalTarget() {
        val actor = EffectTestFixture.player(1).apply { wisps.add(honor) }
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = request(game, actor, honor)
        assertFalse(DefaultGameEffectExecutor().canExecute(request))
        assertFailsWith<EffectExecutionException> { DefaultGameEffectExecutor().execute(request) }
        assertEquals(listOf(honor), actor.wisps.cards.cards)
    }

    @Test
    fun berryPatientAffectsAllHoldingPlayersAndNotTheSource() {
        val actor = EffectTestFixture.player(1).apply { wisps.add(patient).add(honor) }
        val rival = EffectTestFixture.player(2).apply { wisps.add(spare).add(other) }
        val none = EffectTestFixture.player(3)
        val game = EffectTestFixture.game(actor, rival, none)
        val request = request(game, actor, patient)
        DefaultGameEffectExecutor().execute(request)
        assertEquals(listOf(patient), actor.wisps.cards.cards)
        assertEquals(listOf(spare), rival.wisps.cards.cards)
        assertTrue(none.wisps.isEmpty)
        assertEquals(0, actor.vp)
    }

    @Test
    fun berryPatientWithNoOtherWispCanStillBePlayed() {
        val actor = EffectTestFixture.player(1).apply { wisps.add(patient) }
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = request(game, actor, patient)
        val executor = DefaultGameEffectExecutor()
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(listOf(patient), actor.wisps.cards.cards)
        assertTrue(actor.wisps.remove(patient))
        assertTrue(actor.wisps.isEmpty)
    }

    @Test
    fun twoIdenticalPhysicalWispsExcludeOnlyThePlayedCopy() {
        val actor = EffectTestFixture.player(1).apply { wisps.add(patient).add(patient) }
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        DefaultGameEffectExecutor().execute(request(game, actor, patient))
        assertEquals(1, actor.wisps.size)
        assertEquals(patient, actor.wisps.cards.cards.single())
        actor.wisps.remove(patient)
        assertTrue(actor.wisps.isEmpty)
    }

    @Test
    fun invalidChoiceDoesNotTrashAnyPlayerWisp() {
        val bad = object : FirstEffectChoiceStrategy() {
            override fun chooseWispsToKeep(request: ChooseWispsToKeepRequest): EffectWispsChoice =
                EffectWispsChoice(emptyList())
        }
        val actor = EffectTestFixture.player(1).apply { wisps.add(patient) }
        val rival = EffectTestFixture.player(2, effectStrategy = bad).apply { wisps.add(spare).add(other) }
        val game = EffectTestFixture.game(actor, rival)
        assertFailsWith<InvalidDecisionException> {
            DefaultGameEffectExecutor().execute(request(game, actor, patient))
        }
        assertEquals(listOf(spare, other), rival.wisps.cards.cards)
        assertEquals(listOf(patient), actor.wisps.cards.cards)
    }

    @Test
    fun converterAndRoutingUseCurrentOfficialEffects() {
        assertEquals(GameEffect.CHOOSE_PLAYER_WITH_WISPS_TRASH_ONE, honor.effect)
        assertEquals(GameEffect.EACH_PLAYER_WITH_WISPS_TRASH_ONE, patient.effect)
        assertEquals(2, honor.endGameVp)
        assertEquals(3, patient.endGameVp)
        assertFalse(honor.effect in CardEffectReadiness.pending)
        assertFalse(patient.effect in CardEffectReadiness.pending)
        val executor = DefaultGameEffectExecutor()
        assertTrue(executor.supports(honor.effect))
        assertTrue(executor.supports(patient.effect))
    }

    private fun card(name: String): WispCard = requireNotNull(registry.getCard(name))

    private fun request(
        game: dugsolutions.leaf.v35.game.Game,
        actor: dugsolutions.leaf.v35.player.Player,
        source: WispCard,
        phase: GameEffectPhase = GameEffectPhase.CULTIVATION
    ) = GameEffectRequest(game, actor, source.effect, GameEffectSource.Wisp(source), phase)
}
