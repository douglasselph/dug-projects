package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** NC-05/08: provisional exact-upgrade and per-graft distinct-pair execution. */
class NC05NC08MechanicsTest {
    private val engine = DefaultGameEffectExecutor()
    private val awakening = GameEffect.UPGRADE_DIE_CONDITIONAL_TWO_STEPS_AND_USE_NOW
    private val saplink = GameEffect.RAISE_TWO_DICE_PLUS_1_PER_ROOT_OR_VINE

    @Test
    fun currentCsvIdentitiesAndPoliciesHaveExecutableDecisionContracts() {
        val roots = PlantCardRegistry().apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST))
        }
        val vines = PlantCardRegistry().apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
        }
        val scorer = HumanBaselineCardScorerRegistry()
        for ((card, effect) in listOf(
            assertNotNull(roots.getCard("Root_07_01")) to awakening,
            assertNotNull(vines.getCard("Vine_09_03")) to saplink
        )) {
            assertEquals(effect, card.effect)
            assertEquals(effect, scorer.forPlant(card).effect)
            assertTrue(engine.supports(effect))
            assertFalse(effect in CardEffectReadiness.pending)
            val contract = assertNotNull(GameEffectDecisionRequirements.forEffect(effect))
            assertEquals(setOf(EffectDecisionMechanism.EFFECT_DIE), contract.cultivation)
            assertEquals(setOf(EffectDecisionMechanism.EFFECT_DIE), contract.battle)
        }
        assertTrue(CardEffectReadiness.pending.isEmpty())
    }

    @Test
    fun awakeningDieShowingThreeAdvancesExactlyTwoLadderPositionsEvenIfIntermediateStockEmpty() {
        val source = FixedEffectDie(6, 3)
        val actor = EffectTestFixture.player(1, listOf(source))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        // D8 is the intermediate size, but D10 is the required destination.
        while (game.grove.graftBed.take(DieSides.D8)) { /* deliberately empty D8 */ }
        val request = EffectTestFixture.request(game, actor, awakening)
        assertTrue(engine.canExecute(request))
        engine.execute(request)
        assertEquals(1, actor.dice.handSize)
        assertEquals(10, actor.dice.hand.single().sides)
        assertTrue(actor.dice.hand.single() !== source)
        assertEquals(0, game.grove.graftBed.count(DieSides.D8))
    }

    @Test
    fun awakeningOtherFacesAdvanceOneStepAndAnUpgradedD4ReturnsToGrove() {
        val source = FixedEffectDie(4, 2)
        val actor = EffectTestFixture.player(1, listOf(source))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, actor, awakening)
        assertEquals(0, game.grove.graftBed.count(DieSides.D4))
        assertTrue(engine.canExecute(request))
        engine.execute(request)
        assertEquals(6, actor.dice.hand.single().sides)
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
    }

    @Test
    fun awakeningDoesNotFallBackWhenItsFinalSizeIsMissingAndRejectsBeyondD20() {
        val source = FixedEffectDie(6, 3)
        val actor = EffectTestFixture.player(1, listOf(source))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        // D8 remains available, but D10 (the two-step destination) is absent.
        while (game.grove.graftBed.take(DieSides.D10)) { /* deliberately empty D10 */ }
        val request = EffectTestFixture.request(game, actor, awakening)
        assertFalse(engine.canExecute(request))
        assertTrue(actor.dice.hand.single() === source)
        assertEquals(3, source.value)
        assertTrue(game.grove.graftBed.has(DieSides.D8))

        val beyond = FixedEffectDie(12, 3) // D12 + 2 ladder steps is impossible.
        val owner = EffectTestFixture.player(1, listOf(beyond))
        assertFalse(engine.canExecute(EffectTestFixture.request(
            EffectTestFixture.game(owner, EffectTestFixture.player(2)), owner, awakening
        )))
    }

    @Test
    fun awakeningReplacesTheExactBattleDieInItsExistingSquareWithoutAddingCapacity() {
        val source = FixedEffectDie(6, 3)
        val actor = EffectTestFixture.player(1, listOf(source))
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, source)
        val request = EffectTestFixture.request(game, actor, awakening).copy(
            phase = GameEffectPhase.BATTLE, battleState = battle
        )
        assertTrue(engine.canExecute(request))
        engine.execute(request)
        assertEquals(1, battle.grid.square(actor.id, StrikeRow.MIDDLE).dieCount)
        val upgraded = actor.dice.hand.single()
        assertEquals(10, upgraded.sides)
        assertEquals(StrikeRow.MIDDLE, battle.grid.locationOf(upgraded)?.row)
        assertEquals(null, battle.grid.locationOf(source))
    }

    @Test
    fun awakeningCannotChooseOnlyDiceInClosedBattleRows() {
        val source = FixedEffectDie(6, 3)
        val actor = EffectTestFixture.player(1, listOf(source))
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, source)
        battle.grid.closeRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(game, actor, awakening).copy(
            phase = GameEffectPhase.BATTLE, battleState = battle
        )
        assertFalse(engine.canExecute(request))
        assertEquals(3, source.value)
    }

    @Test
    fun saplinkChoosesTwoDistinctTargetsPerRootOrVineAndCanReuseInLaterIterations() {
        val a = FixedEffectDie(6, 2)
        val b = FixedEffectDie(6, 3)
        val c = FixedEffectDie(6, 4)
        val actor = EffectTestFixture.player(1, listOf(a, b, c), FirstEffectChoiceStrategy())
        graft(actor, PlantType.ROOT, "Test Root")
        graft(actor, PlantType.VINE, "Saplink Trellis")
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, actor, saplink)
        assertTrue(engine.canExecute(request))
        engine.execute(request)
        // Two iterations; each uses indices 0 and 1, never twice in one pair.
        assertEquals(listOf(4, 5, 4), listOf(a.value, b.value, c.value))
    }

    @Test
    fun saplinkOnlyOneUsefulDieGetsOneRaisePerIterationAndNeverExceedsCap() {
        val only = FixedEffectDie(6, 4)
        val actor = EffectTestFixture.player(1, listOf(only))
        graft(actor, PlantType.ROOT, "Test Root")
        graft(actor, PlantType.VINE, "Saplink Trellis")
        graft(actor, PlantType.ROOT, "Second Root")
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        engine.execute(EffectTestFixture.request(game, actor, saplink))
        assertEquals(6, only.value) // Third iteration has no eligible target.
    }

    @Test
    fun saplinkNeverTargetsLockedBattleDice() {
        val closed = FixedEffectDie(8, 2)
        val open = FixedEffectDie(8, 3)
        val actor = EffectTestFixture.player(1, listOf(closed, open))
        graft(actor, PlantType.ROOT, "Test Root")
        graft(actor, PlantType.VINE, "Saplink Trellis")
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, closed)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, open)
        battle.grid.closeRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(game, actor, saplink).copy(
            phase = GameEffectPhase.BATTLE, battleState = battle
        )
        engine.execute(request)
        assertEquals(2, closed.value)
        assertEquals(5, open.value)
    }

    private fun graft(actor: Player, type: PlantType, title: String) {
        val card = PlantCard(
            quantity = 1, name = title, title = title,
            type = type,
            cost = if (type == PlantType.ROOT) 5 else 9,
            lineIcon = null, vpIcon = "", typeIcon = "", fgColor = "",
            textColor = "", fullImage = "", backgroundImage = "",
            cardBackgroundImage = "", effect = GameEffect.UNKNOWN
        )
        actor.creature.graft(card, actor.creature.legalPlacements(card).first())
    }
}
