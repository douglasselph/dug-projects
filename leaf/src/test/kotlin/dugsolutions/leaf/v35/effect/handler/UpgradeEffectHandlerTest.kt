package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.LastEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
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
    fun compost_withUnavailableNextSize_remainsExecutableForTrashFallback() {
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

        assertTrue(handler.canExecute(request))
        assertFalse(handler.canExecute(request.copy(effect = GameEffect.UPGRADE_DIE_AND_USE_NOW)))

        handler.execute(request, nestedExecutor)
        assertTrue(actor.dice.hand.isEmpty())
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(0, game.grove.graftBed.count(DieSides.D10))
        assertTrue(game.chronicle.entries.any {
            it is GameEntry.TrashDie && it.playerId == actor.id && it.sides == DieSides.D8
        })
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
        assertTrue(game.chronicle.entries.any {
            it is GameEntry.DieRolled && it.playerId == actor.id
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
    fun compost_canChooseUnavailableDieForTrashEvenWhenOtherDieCanUpgrade() {
        val d6 = FixedEffectDie(6, 2)
        val d8 = FixedEffectDie(8, 1)
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
            EffectTestFixture.request(game, actor, GameEffect.UPGRADE_DIE_FROM_HAND),
            nestedExecutor
        )

        assertEquals(listOf(d6), actor.dice.hand)
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(9, game.grove.graftBed.count(DieSides.D8))
        assertTrue(game.chronicle.entries.any {
            it is GameEntry.TrashDie && it.playerId == actor.id && it.sides == DieSides.D8
        })
    }

    @Test
    fun compost_whenD6Missing_canTrashD4AndReturnItToGraftBed() {
        val d4 = FixedEffectDie(4, 2)
        val actor = EffectTestFixture.player(1, hand = listOf(d4))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        repeat(9) {
            assertTrue(game.grove.graftBed.take(DieSides.D6))
        }

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.UPGRADE_DIE_FROM_HAND),
            nestedExecutor
        )

        assertTrue(actor.dice.hand.isEmpty())
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
        assertEquals(0, game.grove.graftBed.count(DieSides.D6))
        assertTrue(game.chronicle.entries.any {
            it is GameEntry.TrashDie && it.playerId == actor.id && it.sides == DieSides.D4
        })
    }

    @Test
    fun compost_canTrashD20WhenNoNextSizeExists() {
        val die = FixedEffectDie(20, 8)
        val actor = EffectTestFixture.player(1, hand = listOf(die))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val originalD20Stock = game.grove.graftBed.count(DieSides.D20)

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.UPGRADE_DIE_FROM_HAND),
            nestedExecutor
        )

        assertTrue(actor.dice.hand.isEmpty())
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(originalD20Stock, game.grove.graftBed.count(DieSides.D20))
    }

    @Test
    fun compost_upgradesChosenDieWhenNextNormalSizeIsAvailable() {
        val d6 = FixedEffectDie(6, 5)
        val d8 = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(d6, d8),
            effectStrategy = FirstEffectChoiceStrategy()
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
