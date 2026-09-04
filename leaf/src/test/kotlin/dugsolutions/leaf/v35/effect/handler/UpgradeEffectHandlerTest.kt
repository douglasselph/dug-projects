package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpgradeEffectHandlerTest {

    private val handler = UpgradeEffectHandler()
    private val nestedExecutor = GameEffectExecutor { }

    @Test
    fun compost_isExecutableOnlyWhenImmediateNextSizeIsAvailable() {
        val die = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.UPGRADE_DIE_FROM_HAND
        )

        assertTrue(handler.canExecute(request))

        repeat(9) {
            assertTrue(game.grove.graftBed.take(DieSides.D10))
        }

        assertFalse(handler.canExecute(request))
    }

    @Test
    fun rootAwakening_upgradesIntoHandAndRollsNewDieImmediately() {
        val old = FixedEffectDie(4, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(old))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.UPGRADE_DIE_AND_USE_NOW
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nestedExecutor)

        assertEquals(1, actor.dice.handSize)
        assertEquals(6, actor.dice.hand.single().sides)
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
        assertEquals(8, game.grove.graftBed.count(DieSides.D6))
        assertTrue(game.chronicle.entries.filterIsInstance<GameEntry.Marker>().any {
            it.message.startsWith("ROLL player=1")
        })
    }

    @Test
    fun rootAwakeningInBattle_replacesExactDieInSameStrikeSquare() {
        val old = FixedEffectDie(4, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(old))
        val other = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, other)
        val battleState = BattleState(listOf(actor, other))
        battleState.grid.placeDie(
            actor,
            StrikeRow.MIDDLE,
            old
        )

        val battle = EffectTestFixture.request(
            game,
            actor,
            GameEffect.UPGRADE_DIE_AND_USE_NOW
        ).copy(
            phase = GameEffectPhase.BATTLE,
            battleState = battleState
        )

        assertTrue(handler.canExecute(battle))
        handler.execute(battle, nestedExecutor)

        val replacement = actor.dice.hand.single()
        assertEquals(6, replacement.sides)
        assertEquals(
            StrikeRow.MIDDLE,
            battleState.grid.locationOf(replacement)?.row
        )
        assertEquals(null, battleState.grid.locationOf(old))
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
    }

    @Test
    fun compost_choosesOnlyEligibleDieAndMovesReplacementToDiscard() {
        val d6 = FixedEffectDie(6, 5)
        val d8 = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(d6, d8),
            effectStrategy = LastEffectChoiceStrategy()
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        repeat(9) {
            assertTrue(game.grove.graftBed.take(DieSides.D10))
        }

        handler.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.UPGRADE_DIE_FROM_HAND
            ),
            nestedExecutor
        )

        assertEquals(listOf(d8), actor.dice.hand)
        assertEquals(1, actor.dice.discard.size)
        assertEquals(8, actor.dice.discard.single().sides)
    }
}
