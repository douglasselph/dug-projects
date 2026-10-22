package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.*
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.effect.*
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.*

/** Exercises the actual decision callback -> executor -> observable-state path. */
class HumanBaselineChooseThenExecuteTest {
    private val executor = DefaultGameEffectExecutor()

    @Test
    fun `S1 selected Battle die produces capped raise and actual opponent reduction`() {
        runSapping(GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW)
    }

    @Test
    fun `S2 chosen ordered pair changes both correct Battle rows`() {
        runSapping(GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW)
    }

    private fun runSapping(effect: GameEffect) {
        val chooser = RecordingHuman()
        val a = FixedEffectDie(20, 3)
        val b = FixedEffectDie(12, 4)
        val oppTop = FixedEffectDie(10, 5)
        val oppMiddle = FixedEffectDie(8, 7)
        val actor = EffectTestFixture.player(1, listOf(a, b), chooser)
        val opponent = EffectTestFixture.player(2, listOf(oppTop, oppMiddle))
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, a)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, b)
        battle.grid.placeDie(opponent, StrikeRow.TOP, oppTop)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, oppMiddle)
        val hand = actor.dice.hand.toList()
        val oldOwn = hand.map { it.value }
        val oldOpp = mapOf(StrikeRow.TOP to oppTop.value, StrikeRow.MIDDLE to oppMiddle.value)
        val request = EffectTestFixture.request(game, actor, effect)
            .copy(phase = GameEffectPhase.BATTLE, battleState = battle)

        assertTrue(executor.canExecute(request))
        executor.execute(request)

        val expectedSelections = if (effect == GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW) 1 else 2
        assertEquals(expectedSelections, chooser.dieChoices.size, "Must select through actual Human Baseline callback")
        val raised = chooser.dieChoices[0].index
        val anchor = chooser.dieChoices.last().index
        if (expectedSelections == 2) assertNotEquals(raised, anchor, "S2 requires different dice")
        val rowOf = { index: Int -> battle.grid.locationOf(hand[index])!!.row }
        val anchorRow = rowOf(anchor)
        val reduced = oldOpp.getValue(anchorRow) - (oldOpp.getValue(anchorRow) - 2).coerceAtLeast(1)
        val expected = oldOwn.toMutableList()
        expected[raised] = (expected[raised] + 2).coerceAtMost(hand[raised].sides)
        expected[anchor] = (expected[anchor] + reduced).coerceAtMost(hand[anchor].sides)
        assertEquals(expected, hand.map { it.value }, "Chosen dice must execute the exact Sapping sequence")
        assertEquals(oldOpp.getValue(StrikeRow.TOP) - if (anchorRow == StrikeRow.TOP) reduced else 0, oppTop.value)
        assertEquals(oldOpp.getValue(StrikeRow.MIDDLE) - if (anchorRow == StrikeRow.MIDDLE) reduced else 0, oppMiddle.value)
        assertEquals(StrikeRow.TOP, battle.grid.locationOf(a)?.row)
        assertEquals(StrikeRow.MIDDLE, battle.grid.locationOf(b)?.row)
    }

    @Test
    fun `Petal P1 through P4 choose an available source and execute its destination`() {
        val variants = listOf(
            GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S to false,
            GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S to true,
            GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S to false,
            GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S to true
        )
        for ((effect, toHand) in variants) {
            val chooser = RecordingHuman()
            val actor = EffectTestFixture.player(1, effectStrategy = chooser)
            val opponent = EffectTestFixture.player(2)
            val game = EffectTestFixture.game(actor, opponent)
            game.grove.graftBed.returnD4()
            val request = EffectTestFixture.request(game, actor, effect)
            assertTrue(executor.canExecute(request), "Petal should be legal: $effect")
            executor.execute(request)
            assertEquals(1, chooser.sources.size, "Human selected actual Petal source: $effect")
            assertEquals(PetalD4SourceChoice.Grove, chooser.sources.single(), "Only Grove source exists: $effect")
            assertEquals(if (toHand) 1 else 0, actor.dice.handSize, "$effect")
            assertEquals(if (toHand) 0 else 1, actor.dice.discardSize, "$effect")
            assertEquals(0, game.grove.graftBed.count(DieSides.D4), "$effect")
            assertTrue(actor.dice.hand.all { it.sides == 4 } && actor.dice.discard.all { it.sides == 4 })
        }
    }

    @Test
    fun `O Edelweiss decisions execute only legal nested Play or Flip transitions`() {
        val chooser = RecordingHuman()
        val die = FixedEffectDie(8, 1)
        val actor = EffectTestFixture.player(1, listOf(die), chooser)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val target = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("Vine_07_01", PlantType.VINE, GameEffect.RAISE_ANY_DIE_PLUS_1))
        val source = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("Flower_17_03", PlantType.FLOWER, GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE))
        val readySource = RecursivePlantEffectTestFixture.faceUp(actor, source)
        val request = EffectTestFixture.request(game, actor, GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
            GameEffectSource.Plant(readySource))
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertTrue(chooser.edelweissChoices.isNotEmpty(), "Must use Human Baseline nested chooser")
        assertTrue(chooser.edelweissChoices.size <= 2, "Only two choices permitted")
        assertTrue(chooser.edelweissChoices.all { (chosen, legal) -> chosen in legal }, "Every nested choice must be legal")
        assertTrue(die.value in 1..8)
        assertNotNull(actor.creature.get(target.id))
        assertNotNull(actor.creature.get(source.id))
    }

    private class RecordingHuman : EffectStrategy by HumanBaselineEffectStrategy() {
        private val baseline = HumanBaselineEffectStrategy()
        val dieChoices = mutableListOf<EffectDieChoice>()
        val sources = mutableListOf<PetalD4SourceChoice>()
        val edelweissChoices = mutableListOf<Pair<OEdelweissChoice, List<OEdelweissChoice>>>()

        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            baseline.chooseDie(request).also { choice ->
                assertTrue(choice in request.legalChoices)
                dieChoices += choice
            }

        override fun choosePetalD4Source(request: ChoosePetalD4SourceRequest): PetalD4SourceChoice =
            baseline.choosePetalD4Source(request).also { choice ->
                assertTrue(choice in request.legalChoices)
                sources += choice
            }

        override fun chooseOEdelweiss(request: ChooseOEdelweissRequest): OEdelweissChoice =
            baseline.chooseOEdelweiss(request).also { choice ->
                assertTrue(choice in request.legalChoices)
                edelweissChoices += choice to request.legalChoices
            }

        // Make optional cycling deterministic; source selection still uses the real Baseline.
        override fun chooseDice(request: ChooseEffectDiceRequest): EffectDiceChoice =
            EffectDiceChoice(emptyList())
    }
}
