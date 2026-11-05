package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.StrikeFinishCoordinator
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.SequenceEffectDie
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WispquakeEffectTest {

    @Test
    fun keepsChosenActorDieAndRerollsEveryOtherHandDie() {
        val kept = SequenceEffectDie(6, 4, 6)
        val actorRerolled = SequenceEffectDie(8, 3, 7)
        val opponentRerolled = SequenceEffectDie(10, 5, 9)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(kept, actorRerolled),
            effectStrategy = FirstEffectChoiceStrategy()
        )
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentRerolled)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val request = EffectTestFixture.request(
            game = game,
            actor = actor,
            effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            source = GameEffectSource.Wisp(EffectTestFixture.wispquake())
        )

        WispquakeEffect().execute(
            request = request,
            executor = GameEffectExecutor { }
        )

        assertEquals(4, kept.value)
        assertEquals(7, actorRerolled.value)
        assertEquals(9, opponentRerolled.value)
    }
    @Test
    fun battleWispquakeAfterRealStrikeCleanupOnlyRerollsRemainingHandDice() {
        val doomedTop = SequenceEffectDie(8, 5, 1)
        val keptMiddle = SequenceEffectDie(6, 4, 2)
        val discardedTop = SequenceEffectDie(10, 7, 1)
        val rerolledMiddle = SequenceEffectDie(6, 3, 6)
        val actor = EffectTestFixture.player(
            id = 1, hand = listOf(doomedTop, keptMiddle),
            effectStrategy = FirstEffectChoiceStrategy()
        )
        val opponent = EffectTestFixture.player(
            id = 2, hand = listOf(discardedTop, rerolledMiddle)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, doomedTop)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, keptMiddle)
        battle.grid.placeDie(opponent, StrikeRow.TOP, discardedTop)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, rerolledMiddle)
        battle.activateThreeEngagements()
        val finished = StrikeFinishCoordinator().finish(game, battle, StrikeRow.TOP) {
            DecisionContext.EMPTY
        }
        assertEquals(listOf(5), finished.doom.dice.map { it.value })
        val frozen = battle.frozenResolvedRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(
            game, actor, GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            source = GameEffectSource.Wisp(EffectTestFixture.wispquake())
        ).copy(phase = GameEffectPhase.BATTLE, battleState = battle)

        WispquakeEffect().execute(request, GameEffectExecutor { })

        assertEquals(5, doomedTop.value, "Doomed dice are not rerolled")
        assertEquals(7, discardedTop.value, "Already reclaimed dice are not rerolled")
        assertEquals(4, keptMiddle.value, "One current Hand die may be kept")
        assertEquals(6, rerolledMiddle.value, "Open-row opponents still reroll")
        assertEquals(frozen, battle.frozenResolvedRow(StrikeRow.TOP))
        assertEquals(null, battle.grid.locationOf(doomedTop))
        assertEquals(null, battle.grid.locationOf(discardedTop))
        assertEquals(StrikeRow.MIDDLE, battle.grid.locationOf(keptMiddle)?.row)
        assertEquals(StrikeRow.MIDDLE, battle.grid.locationOf(rerolledMiddle)?.row)
    }

    @Test
    fun battleWispquakeRerollsOnlyDiceInUnresolvedRows() {
        val lockedActor = SequenceEffectDie(6, 4, 6)
        val openActor = SequenceEffectDie(8, 3, 7)
        val lockedOpponent = SequenceEffectDie(10, 5, 9)
        val openOpponent = SequenceEffectDie(12, 4, 8)
        val actor = EffectTestFixture.player(
            id = 1, hand = listOf(lockedActor, openActor),
            effectStrategy = FirstEffectChoiceStrategy()
        )
        val opponent = EffectTestFixture.player(
            id = 2, hand = listOf(lockedOpponent, openOpponent)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battle = BattleState(listOf(actor, opponent))
        battle.grid.placeDie(actor, StrikeRow.TOP, lockedActor)
        battle.grid.placeDie(actor, StrikeRow.MIDDLE, openActor)
        battle.grid.placeDie(opponent, StrikeRow.TOP, lockedOpponent)
        battle.grid.placeDie(opponent, StrikeRow.MIDDLE, openOpponent)
        battle.lockResolvedRow(StrikeRow.TOP)
        val request = EffectTestFixture.request(
            game, actor, GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            source = GameEffectSource.Wisp(EffectTestFixture.wispquake())
        ).copy(phase = GameEffectPhase.BATTLE, battleState = battle)

        WispquakeEffect().execute(request, GameEffectExecutor { })
        assertEquals(4, lockedActor.value)
        assertEquals(5, lockedOpponent.value)
        // The actor may keep the one eligible open die; the opponent cannot.
        assertEquals(3, openActor.value)
        assertEquals(8, openOpponent.value)
        assertEquals(StrikeRow.TOP, battle.grid.locationOf(lockedActor)?.row)
    }

}
