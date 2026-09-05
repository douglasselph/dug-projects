package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.SequenceEffectDie
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.round.battle.BattleCleanupCoordinator
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectBattleDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseRootWellBattleRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCrossPlayerDieSwapRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBattleDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectCrossPlayerDieSwapChoice
import dugsolutions.leaf.v35.player.decision.effect.RootWellBattleChoice
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrossPlayerEffectHandlerTest {

    private val handler = CrossPlayerEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun rootWellCultivation_gainsOneWater() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(1, actor.tokens.waterCount)
        assertEquals(8, game.grove.tokens.waterCount)
    }

    @Test
    fun rootWellBattle_canTargetOpponentDie_spendsOneWater_andOpponentGetsRollReward() {
        val actorDie = FixedEffectDie(8, 4)
        val opponentDie = SequenceEffectDie(10, initial = 7, next = 1)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(actorDie),
            effectStrategy = CrossPlayerStrategy(
                battleTargetOwner = PlayerId(2)
            )
        )
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentDie)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, actorDie)
        battleState.grid.placeDie(opponent, StrikeRow.MIDDLE, opponentDie)

        val water = game.grove.tokens.pull(Token.WATER)
        check(water != null)
        actor.tokens.add(water)
        val groveWaterBefore = game.grove.tokens.waterCount
        val opponentBeesBefore = opponent.critters.count(Critter.BEE)

        val request = battleRequest(
            game = game,
            actor = actor,
            battleState = battleState,
            effect = GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(0, actor.tokens.waterCount)
        assertEquals(groveWaterBefore + 1, game.grove.tokens.waterCount)
        assertEquals(1, opponentDie.value)
        assertEquals(
            opponentBeesBefore + 1,
            opponent.critters.count(Critter.BEE)
        )
        assertEquals(0, actor.critters.count(Critter.BEE))
        assertEquals(
            StrikeRow.MIDDLE,
            battleState.grid.locationOf(opponentDie)?.row
        )
        assertEquals(
            opponent.id,
            battleState.grid.locationOf(opponentDie)?.playerId
        )
    }

    @Test
    fun rootWellBattle_requiresOneWaterAndEitherTwoOwnDiceOrOneOpponentDie() {
        val actorDie = FixedEffectDie(8, 4)
        val actor = EffectTestFixture.player(1, hand = listOf(actorDie))
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, actorDie)

        val request = battleRequest(
            game,
            actor,
            battleState,
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE
        )

        // One own die is not enough for the own-dice branch, and no opponent
        // Battle die exists yet.
        assertFalse(handler.canExecute(request))

        actor.tokens.add(Token.WATER)
        assertFalse(handler.canExecute(request))

        val secondOwn = FixedEffectDie(10, 5)
        actor.dice.addToHand(secondOwn)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, secondOwn)
        assertTrue(handler.canExecute(request))

        battleState.grid.removeDie(secondOwn)
        actor.dice.removeExactFromHand(secondOwn)
        val opponentDie = FixedEffectDie(6, 3)
        opponent.dice.addToHand(opponentDie)
        battleState.grid.placeDie(opponent, StrikeRow.BOTTOM, opponentDie)
        assertTrue(handler.canExecute(request))
    }

    @Test
    fun rootWellBattle_canRerollExactlyTwoOwnDice_forOneWater() {
        val first = SequenceEffectDie(8, initial = 2, next = 5)
        val second = SequenceEffectDie(10, initial = 3, next = 6)
        val opponentDie = FixedEffectDie(12, 9)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(first, second),
            effectStrategy = CrossPlayerStrategy(preferOwnRootWell = true)
        )
        val opponent = EffectTestFixture.player(2, hand = listOf(opponentDie))
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, first)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, second)
        battleState.grid.placeDie(opponent, StrikeRow.BOTTOM, opponentDie)
        val water = game.grove.tokens.pull(Token.WATER)
        check(water != null)
        actor.tokens.add(water)

        val request = battleRequest(
            game, actor, battleState,
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE
        )

        handler.execute(request, nested)

        assertEquals(0, actor.tokens.waterCount)
        assertEquals(5, first.value)
        assertEquals(6, second.value)
        assertEquals(9, opponentDie.value)
    }

    @Test
    fun pollenTheft_swapsSameSizeDiceAcrossPlayers_withoutRerolling_andTransfersHandOwnership() {
        val actorD8 = FixedEffectDie(8, 2)
        val actorD10 = FixedEffectDie(10, 6)
        val opponentD6 = FixedEffectDie(6, 5)
        val opponentD8 = FixedEffectDie(8, 7)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(actorD8, actorD10),
            effectStrategy = CrossPlayerStrategy(
                swapOpponentOwner = PlayerId(2)
            )
        )
        val opponent = EffectTestFixture.player(
            id = 2,
            hand = listOf(opponentD6, opponentD8)
        )
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, actorD8)
        battleState.grid.placeDie(actor, StrikeRow.MIDDLE, actorD10)
        battleState.grid.placeDie(opponent, StrikeRow.TOP, opponentD6)
        battleState.grid.placeDie(opponent, StrikeRow.BOTTOM, opponentD8)

        val request = battleRequest(
            game = game,
            actor = actor,
            battleState = battleState,
            effect = GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertTrue(actor.dice.hand.any { it === opponentD8 })
        assertFalse(actor.dice.hand.any { it === actorD8 })
        assertTrue(opponent.dice.hand.any { it === actorD8 })
        assertFalse(opponent.dice.hand.any { it === opponentD8 })

        assertEquals(7, opponentD8.value)
        assertEquals(2, actorD8.value)
        assertEquals(
            actor.id,
            battleState.grid.locationOf(opponentD8)?.playerId
        )
        assertEquals(
            StrikeRow.TOP,
            battleState.grid.locationOf(opponentD8)?.row
        )
        assertEquals(
            opponent.id,
            battleState.grid.locationOf(actorD8)?.playerId
        )
        assertEquals(
            StrikeRow.BOTTOM,
            battleState.grid.locationOf(actorD8)?.row
        )

        assertTrue(actor.dice.hand.any { it === actorD10 })
        assertTrue(opponent.dice.hand.any { it === opponentD6 })

        BattleCleanupCoordinator(
            RefreshResolver(game.chronicle)
        ).execute(game, battleState)

        assertTrue(actor.dice.discard.any { it === opponentD8 })
        assertTrue(actor.dice.discard.any { it === actorD10 })
        assertTrue(opponent.dice.discard.any { it === actorD8 })
        assertTrue(opponent.dice.discard.any { it === opponentD6 })
    }

    @Test
    fun pollenTheft_isBattleOnlyAndRequiresSameSizeOpponentPair() {
        val actorDie = FixedEffectDie(8, 4)
        val opponentDie = FixedEffectDie(10, 7)
        val actor = EffectTestFixture.player(1, hand = listOf(actorDie))
        val opponent = EffectTestFixture.player(2, hand = listOf(opponentDie))
        val game = EffectTestFixture.game(actor, opponent)
        val battleState = BattleState(listOf(actor, opponent))
        battleState.grid.placeDie(actor, StrikeRow.TOP, actorDie)
        battleState.grid.placeDie(opponent, StrikeRow.TOP, opponentDie)

        val battle = battleRequest(
            game,
            actor,
            battleState,
            GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE
        )

        assertFalse(handler.canExecute(battle))
        assertFalse(
            handler.canExecute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE
                )
            )
        )
    }

    private fun battleRequest(
        game: dugsolutions.leaf.v35.game.Game,
        actor: dugsolutions.leaf.v35.player.Player,
        battleState: BattleState,
        effect: GameEffect
    ) = EffectTestFixture.request(
        game = game,
        actor = actor,
        effect = effect
    ).copy(
        phase = GameEffectPhase.BATTLE,
        battleState = battleState
    )

    private class CrossPlayerStrategy(
        private val battleTargetOwner: PlayerId? = null,
        private val swapOpponentOwner: PlayerId? = null,
        private val preferOwnRootWell: Boolean = false
    ) : FirstEffectChoiceStrategy() {
        override fun chooseBattleDie(
            request: ChooseEffectBattleDieRequest
        ): EffectBattleDieChoice =
            battleTargetOwner?.let { owner ->
                request.legalChoices.first { it.ownerId == owner }
            } ?: request.legalChoices.first()


        override fun chooseRootWellBattle(
            request: ChooseRootWellBattleRequest
        ): RootWellBattleChoice =
            if (preferOwnRootWell) {
                request.legalChoices.first { it is RootWellBattleChoice.OwnDice }
            } else {
                battleTargetOwner?.let { owner ->
                    request.legalChoices.first { choice ->
                        choice is RootWellBattleChoice.OpponentDie &&
                            choice.die.ownerId == owner
                    }
                } ?: request.legalChoices.first()
            }

        override fun chooseCrossPlayerDieSwap(
            request: ChooseEffectCrossPlayerDieSwapRequest
        ): EffectCrossPlayerDieSwapChoice =
            swapOpponentOwner?.let { owner ->
                request.legalChoices.first {
                    it.opponentDie.ownerId == owner
                }
            } ?: request.legalChoices.first()
    }
}
