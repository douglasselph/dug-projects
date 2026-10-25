package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Prove the three calibration effects actually do what the controls claim. */
class ResearchCalibrationEffectsTest {
    private val executor = DefaultGameEffectExecutor()

    @Test fun `disabled effect records resolution but changes no dice`() {
        val die = FixedEffectDie(20, 3)
        val player = EffectTestFixture.player(1, hand = listOf(die))
        val game = EffectTestFixture.game(player, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(game, player, GameEffect.RESEARCH_NO_EFFECT)
        executor.execute(request)
        assertEquals(3, die.value)
        assertEquals(1, game.chronicle.entries.filterIsInstance<GameEntry.EffectResolved>().size)
        assertTrue(game.chronicle.entries.none { it is GameEntry.DieValueChanged || it is GameEntry.DieRolled })
    }

    @Test fun `raise twenty is powerful but never breaks the physical die cap`() {
        val die = FixedEffectDie(20, 2)
        val player = EffectTestFixture.player(1, hand = listOf(die), effectStrategy = LastEffectChoiceStrategy())
        val game = EffectTestFixture.game(player, EffectTestFixture.player(2))
        executor.execute(EffectTestFixture.request(game, player, GameEffect.RESEARCH_RAISE_DIE_PLUS_20))
        assertEquals(20, die.value)
        val events=game.chronicle.entries.filterIsInstance<GameEntry.DieValueChanged>()
        assertEquals(1, events.size)
        assertEquals(2, events.single().before)
        assertEquals(20, events.single().after)
    }

    @Test fun `draw four draws four available dice instead of two`() {
        val player = EffectTestFixture.player(1)
        repeat(4) { player.dice.addToSupply(FixedEffectDie(6, 3)) }
        val game = EffectTestFixture.game(player, EffectTestFixture.player(2))
        executor.execute(EffectTestFixture.request(game, player, GameEffect.RESEARCH_DRAW_FOUR_DICE))
        assertEquals(4, player.dice.handSize)
        assertEquals(0, player.dice.supplySize)
        assertEquals(4, game.chronicle.entries.filterIsInstance<GameEntry.DieRolled>().size)
    }
}
