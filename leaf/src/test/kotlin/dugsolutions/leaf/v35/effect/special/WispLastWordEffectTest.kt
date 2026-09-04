package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.wisp.domain.WispCard
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WispLastWordEffectTest {

    @Test
    fun resolvesChosenStrikeThenReturnsContentsAndClosesOnlyThatRow() {
        val actorTop = FixedEffectDie(12, 9)
        val actorMiddle = FixedEffectDie(8, 6)
        val opponentTop = FixedEffectDie(12, 5)
        val opponentMiddle = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(actorTop, actorMiddle),
            effectStrategy = ChooseMiddleStrategy()
        )
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentTop, opponentMiddle)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, actorTop)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, actorMiddle)
        battleState.grid.placeDie(opponent, StrikeRow.TOP, opponentTop)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentMiddle)

        val groveBeeCountBefore = game.grove.critters.count(Critter.BEE)
        game.grove.critters.remove(Critter.BEE)
        actor.critters.add(Critter.BEE)
        battleState.grid.placeCritter(actor, StrikeRow.MIDDLE, Critter.BEE)

        val effect = WispLastWordEffect()
        val request = request(game, actor, battleState)

        assertTrue(effect.canExecute(request))
        effect.execute(request, GameEffectExecutor { })

        // MIDDLE resolves immediately: actor 6 + Bee 2 beats opponent 4.
        assertEquals(2, actor.vp)
        assertEquals(0, opponent.vp)

        assertTrue(battleState.grid.isRowClosed(StrikeRow.MIDDLE))
        assertFalse(battleState.grid.isRowClosed(StrikeRow.TOP))
        assertTrue(battleState.grid.square(actor.id, StrikeRow.MIDDLE).isEmpty)
        assertTrue(battleState.grid.square(opponent.id, StrikeRow.MIDDLE).isEmpty)

        assertTrue(actorMiddle !in actor.dice.hand)
        assertTrue(opponentMiddle !in opponent.dice.hand)
        assertTrue(actor.dice.discard.any { it === actorMiddle })
        assertTrue(opponent.dice.discard.any { it === opponentMiddle })
        assertTrue(actor.dice.hand.any { it === actorTop })
        assertTrue(opponent.dice.hand.any { it === opponentTop })
        assertEquals(groveBeeCountBefore, game.grove.critters.count(Critter.BEE))
    }

    @Test
    fun returnedDiceFollowCurrentOwnershipAfterPollenStyleSwap() {
        val actorTop = FixedEffectDie(8, 7)
        val actorMiddle = FixedEffectDie(8, 5)
        val opponentTop = FixedEffectDie(8, 2)
        val opponentMiddle = FixedEffectDie(8, 3)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(actorTop, actorMiddle),
            effectStrategy = ChooseMiddleStrategy()
        )
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentTop, opponentMiddle)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, actorTop)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, actorMiddle)
        battleState.grid.placeDie(opponent, StrikeRow.TOP, opponentTop)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentMiddle)

        // Reproduce Pollen Theft's ownership + location exchange across rows.
        assertTrue(
            actor.dice.swapExactHandDieWith(
                other = opponent.dice,
                ownDie = actorMiddle,
                otherDie = opponentTop
            )
        )
        battleState.grid.swapDieLocations(actorMiddle, opponentTop)

        WispLastWordEffect().execute(
            request = request(game, actor, battleState),
            executor = GameEffectExecutor { }
        )

        // actor's MIDDLE square now held opponentTop, which actor currently owns.
        assertTrue(actor.dice.discard.any { it === opponentTop })
        assertTrue(opponent.dice.hand.any { it === actorMiddle })
        assertFalse(opponent.dice.discard.any { it === actorMiddle })
        assertTrue(battleState.grid.locationOf(actorMiddle)?.row == StrikeRow.TOP)
    }

    @Test
    fun cannotExecuteOutsideBattleOrWhenEveryRowIsAlreadyClosed() {
        val actor = EffectTestFixture.player(1)
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        val effect = WispLastWordEffect()

        val battleRequest = request(game, actor, battleState)
        assertTrue(effect.canExecute(battleRequest))

        val cultivationRequest = battleRequest.copy(
            phase = GameEffectPhase.CULTIVATION
        )
        assertFalse(effect.canExecute(cultivationRequest))

        StrikeRow.entries.forEach(battleState.grid::closeRow)
        assertFalse(effect.canExecute(battleRequest))
    }

    private fun request(
        game: dugsolutions.leaf.v35.game.Game,
        actor: dugsolutions.leaf.v35.player.Player,
        battleState: BattleState
    ): GameEffectRequest =
        GameEffectRequest(
            game = game,
            actor = actor,
            effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
            source = GameEffectSource.Wisp(wispLastWord()),
            phase = GameEffectPhase.BATTLE,
            battleState = battleState
        )

    private fun wispLastWord() =
        WispCard(
            quantity = 1,
            name = "Wisps_Last_Word",
            title = "Wisp's Last Word",
            count = 1,
            effect = GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW,
            lineIcons = null,
            lineIconsHeight = 0,
            vpIcon = null,
            mainBackdrop = "",
            battleOnly = true
        )

    private class ChooseMiddleStrategy : FirstEffectChoiceStrategy() {
        override fun chooseStrikeRow(
            request: ChooseEffectStrikeRowRequest
        ): StrikeRow = StrikeRow.MIDDLE
    }
}
