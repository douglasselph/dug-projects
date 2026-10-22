package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.*
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.effect.HumanBaselineEffectStrategy
import dugsolutions.leaf.v35.player.decision.effect.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
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

    @Test
    fun `Petal cycling selection executes exact discard and replacement draws for P1 through P4`() {
        val variants = listOf(
            GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S,
            GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S,
            GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S,
            GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S
        )
        variants.forEach { effect ->
            val chooser = RecordingHuman(useBaselineCycling = true)
            val originalD4 = FixedEffectDie(4, 1)
            val replacement = FixedEffectDie(8, 5)
            val secondReplacement = FixedEffectDie(10, 6)
            val actor = EffectTestFixture.player(1, listOf(originalD4), chooser)
            actor.dice.addToSupply(replacement)
            actor.dice.addToSupply(secondReplacement)
            val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
            game.grove.graftBed.returnD4()
            val request = EffectTestFixture.request(game, actor, effect)
            assertTrue(executor.canExecute(request), "$effect should be executable")
            executor.execute(request)

            assertEquals(1, chooser.cycleChoices.size, "$effect must ask the real Human Baseline about cycling")
            val (offered, selected) = chooser.cycleChoices.single()
            assertTrue(selected.isNotEmpty(), "$effect: a D4 showing 1 should be worth cycling with D8/D10 available")
            assertTrue(selected.all { it in offered }, "$effect must select only available Hand D4s")
            assertEquals(selected.size, selected.map { it.index }.distinct().size)
            assertEquals(1, chooser.sources.size)
            assertEquals(PetalD4SourceChoice.Grove, chooser.sources.single())
            assertEquals(0, game.grove.graftBed.count(DieSides.D4))

            // Exactly one die must be drawn per selected Hand D4. These
            // fixed dice suppress random outcome variance and Roll Rewards.
            val drawn = listOf(replacement, secondReplacement).count { die ->
                actor.dice.hand.any { it === die }
            }
            assertEquals(selected.size, drawn, "$effect: drawn replacements must match cycled count")
            assertTrue(selected.all { it.sides == 4 }, "$effect: only D4s may be cycled")
            assertEquals(2 - selected.size, actor.dice.supplySize)
            assertEquals(1 + (if (effect.name.contains("TO_HAND")) 1 else 0) - selected.size + drawn,
                actor.dice.handSize, "$effect: acquisition and cycling must conserve Hand count")
            assertTrue(actor.dice.discard.count { it.sides == 4 } >= selected.size,
                "$effect: selected D4s must enter Dice Discard")
        }
    }

    @Test
    fun `O Edelweiss second Baseline choice observes die Raise and spent first Plant`() {
        val chooser = RecordingHuman(scriptFirstEdelweissPlay = true)
        val die = FixedEffectDie(8, 1)
        val actor = EffectTestFixture.player(1, listOf(die), chooser)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val vine = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("Vine_07_01", PlantType.VINE, GameEffect.RAISE_ANY_DIE_PLUS_1))
        RecursivePlantEffectTestFixture.faceUp(actor, vine)
        val flower = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("Flower_17_03", PlantType.FLOWER,
                GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE))
        val source = RecursivePlantEffectTestFixture.faceUp(actor, flower)
        val request = EffectTestFixture.request(game, actor, GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
            GameEffectSource.Plant(source))
        executor.execute(request)

        assertEquals(2, chooser.edelweissChoices.size, "Both decision callbacks must execute")
        assertTrue(chooser.edelweissChoices.first().first is OEdelweissChoice.Play)
        val second = chooser.edelweissContexts[1]
        assertEquals(2, second.self.board.hand.single().value,
            "Second decision must observe the actual first Plant's Raise, not the original die")
        assertTrue(second.self.board.creature.single { it.id == vine.id }.isFaceDown,
            "Played Plant must be spent before the next decision")
        assertEquals(2, die.value)
        assertTrue(chooser.edelweissChoices[1].first in chooser.edelweissChoices[1].second)
    }

    @Test
    fun `O Edelweiss second Baseline choice sees Plant flipped face up by first choice`() {
        val chooser = RecordingHuman(scriptFirstEdelweissFlip = true)
        val die = FixedEffectDie(8, 1)
        val actor = EffectTestFixture.player(1, listOf(die), chooser)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val vine = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("Vine_07_01", PlantType.VINE, GameEffect.RAISE_ANY_DIE_PLUS_1))
        val flower = RecursivePlantEffectTestFixture.graft(actor,
            RecursivePlantEffectTestFixture.plant("Flower_17_03", PlantType.FLOWER,
                GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE))
        val source = RecursivePlantEffectTestFixture.faceUp(actor, flower)
        executor.execute(EffectTestFixture.request(game, actor, GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
            GameEffectSource.Plant(source)))

        assertEquals(2, chooser.edelweissChoices.size)
        assertTrue(chooser.edelweissChoices.first().first is OEdelweissChoice.Flip)
        val secondChoices = chooser.edelweissChoices[1].second
        assertTrue(chooser.edelweissContexts[1].self.board.creature.single { it.id == vine.id }.isFaceUp)
        assertTrue(secondChoices.any { it is OEdelweissChoice.Play && it.card.cardId == vine.id },
            "Flip-then-Play must be legal at step two")
        assertTrue(chooser.edelweissChoices[1].first in secondChoices)
    }

    private class RecordingHuman(
        private val useBaselineCycling: Boolean = false,
        private val scriptFirstEdelweissPlay: Boolean = false,
        private val scriptFirstEdelweissFlip: Boolean = false
    ) : EffectStrategy by HumanBaselineEffectStrategy() {
        private val baseline = HumanBaselineEffectStrategy()
        val dieChoices = mutableListOf<EffectDieChoice>()
        val sources = mutableListOf<PetalD4SourceChoice>()
        val edelweissChoices = mutableListOf<Pair<OEdelweissChoice, List<OEdelweissChoice>>>()
        val edelweissContexts = mutableListOf<DecisionContext>()
        val cycleChoices = mutableListOf<Pair<List<EffectDieChoice>, List<EffectDieChoice>>>()

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

        override fun chooseOEdelweiss(request: ChooseOEdelweissRequest): OEdelweissChoice {
            val choice = when {
                request.choiceNumber == 1 && scriptFirstEdelweissPlay ->
                    request.legalChoices.filterIsInstance<OEdelweissChoice.Play>().first()
                request.choiceNumber == 1 && scriptFirstEdelweissFlip ->
                    request.legalChoices.filterIsInstance<OEdelweissChoice.Flip>()
                        .first { !it.card.isFaceUp }
                else -> baseline.chooseOEdelweiss(request)
            }
            assertTrue(choice in request.legalChoices)
            edelweissContexts += request.context
            edelweissChoices += choice to request.legalChoices
            return choice
        }

        // Make optional cycling deterministic; source selection still uses the real Baseline.
        override fun chooseDice(request: ChooseEffectDiceRequest): EffectDiceChoice {
            val result = if (useBaselineCycling) baseline.chooseDice(request)
                else EffectDiceChoice(emptyList())
            assertTrue(result.selected.all { it in request.legalChoices })
            cycleChoices += request.legalChoices to result.selected
            return result
        }
    }
}
