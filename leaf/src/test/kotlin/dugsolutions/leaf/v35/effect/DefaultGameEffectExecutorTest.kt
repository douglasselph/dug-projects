package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.error.EffectExecutionException
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultGameEffectExecutorTest {

    private val executor = DefaultGameEffectExecutor()

    @Test
    fun canExecute_sharedCultivationEffectsReflectCurrentTargetsAndSupplies() {
        val actor = player(1)
        val game = game(actor)

        assertFalse(executor.canExecute(request(game, actor, GameEffect.RAISE_DIE_PLUS_3)))
        assertFalse(executor.canExecute(request(game, actor, GameEffect.MULCH_DIE_FROM_HAND)))
        assertFalse(executor.canExecute(request(game, actor, GameEffect.UPGRADE_DIE_FROM_HAND)))
        assertTrue(executor.canExecute(request(game, actor, GameEffect.GAIN_WATER_TOKEN)))

        actor.dice.addToHand(FixedDie(4, 3))

        assertTrue(executor.canExecute(request(game, actor, GameEffect.RAISE_DIE_PLUS_3)))
        assertTrue(executor.canExecute(request(game, actor, GameEffect.MULCH_DIE_FROM_HAND)))
        assertTrue(executor.canExecute(request(game, actor, GameEffect.UPGRADE_DIE_FROM_HAND)))
    }

    @Test
    fun raisePlus3_usesChosenLegalHandDie() {
        val first = FixedDie(6, 2)
        val second = FixedDie(8, 4)
        val actor = player(
            id = 1,
            hand = listOf(first, second),
            effectStrategy = LastChoiceStrategy()
        )
        val game = game(actor)

        executor.execute(request(game, actor, GameEffect.RAISE_DIE_PLUS_3))

        assertEquals(2, first.value)
        assertEquals(7, second.value)
    }

    @Test
    fun gainWater_movesOneTokenFromGroveToPlayer() {
        val actor = player(1)
        val game = game(actor)

        executor.execute(request(game, actor, GameEffect.GAIN_WATER_TOKEN))

        assertEquals(1, actor.tokens.waterCount)
        assertEquals(8, game.grove.tokens.waterCount)
    }

    @Test
    fun mulchFromHand_storesChosenDieAsPendingUntilCleanup() {
        val first = FixedDie(6, 3)
        val second = FixedDie(10, 7)
        val actor = player(
            id = 1,
            hand = listOf(first, second),
            effectStrategy = LastChoiceStrategy()
        )
        val game = game(actor)

        executor.execute(request(game, actor, GameEffect.MULCH_DIE_FROM_HAND))

        assertEquals(listOf(first), actor.dice.hand)
        assertEquals(0, actor.tokens.mulchCount)
        assertEquals(1, actor.tokens.pendingMulchCount)
        assertEquals(
            listOf(Token.PENDING_MULCH(DieSides.D10)),
            actor.tokens.pendingMulchTokens
        )
        assertEquals(8, game.grove.tokens.mulchCount)
    }

    @Test
    fun compost_upgradeUsesOnlyDiceWithAvailableImmediateNextSize() {
        val d6 = FixedDie(6, 5)
        val d8 = FixedDie(8, 4)
        val strategy = RecordingLastChoiceStrategy()
        val actor = player(
            id = 1,
            hand = listOf(d6, d8),
            effectStrategy = strategy
        )
        val game = game(actor)

        repeat(9) {
            assertTrue(game.grove.graftBed.take(DieSides.D10))
        }

        executor.execute(request(game, actor, GameEffect.UPGRADE_DIE_FROM_HAND))

        val offered = strategy.requiredRequests.single().legalChoices
        assertEquals(1, offered.size)
        assertEquals(6, offered.single().sides)
        assertEquals(listOf(d8), actor.dice.hand)
        assertEquals(8, actor.dice.discard.single().sides)
    }

    @Test
    fun gainOneVp_isExecutableAndAddsOneVp() {
        val actor = player(1)
        val game = game(actor)

        executor.execute(request(game, actor, GameEffect.GAIN_ONE_VP))

        assertEquals(1, actor.vp)
    }

    @Test
    fun wispquake_keepsChosenActorDieAndRerollsEveryOtherHandDie() {
        val kept = SequenceDie(6, 4, 6)
        val actorRerolled = SequenceDie(8, 3, 7)
        val opponentRerolled = SequenceDie(10, 5, 9)
        val actor = player(
            id = 1,
            hand = listOf(kept, actorRerolled),
            effectStrategy = FirstChoiceStrategy()
        )
        val opponent = player(
            id = 2,
            hand = listOf(opponentRerolled)
        )
        val game = GameEngineTestFixture.game(players = listOf(actor, opponent))

        executor.execute(
            request(
                game = game,
                actor = actor,
                effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
                source = GameEffectSource.Wisp(wispquake())
            )
        )

        assertEquals(4, kept.value)
        assertEquals(7, actorRerolled.value)
        assertEquals(9, opponentRerolled.value)
    }

    @Test
    fun invalidRequiredDieChoice_isRejectedBeforeMutation() {
        val die = FixedDie(6, 2)
        val actor = player(
            id = 1,
            hand = listOf(die),
            effectStrategy = InvalidChoiceStrategy()
        )
        val game = game(actor)

        assertFailsWith<InvalidDecisionException> {
            executor.execute(request(game, actor, GameEffect.RAISE_DIE_PLUS_3))
        }

        assertEquals(2, die.value)
    }

    @Test
    fun supportedEffectWithoutLegalTargets_isNotExecutableAndThrowsIfForced() {
        val actor = player(1)
        val game = game(actor)
        val request = request(game, actor, GameEffect.SET_DIE_TO_MATCH_ANOTHER)

        assertFalse(executor.canExecute(request))
        assertFailsWith<EffectExecutionException> {
            executor.execute(request)
        }
    }

    @Test
    fun successfulEffect_recordsOneEffectResolvedMarker() {
        val actor = player(1, hand = listOf(FixedDie(6, 2)))
        val game = game(actor)

        executor.execute(request(game, actor, GameEffect.RAISE_DIE_PLUS_3))

        val entries = game.chronicle.entries
            .filterIsInstance<GameEntry.EffectResolved>()

        assertEquals(1, entries.size)
        assertEquals(GameEffect.RAISE_DIE_PLUS_3, entries.single().effect)
    }

    @Test
    fun waterUnavailable_isRejectedWithoutChangingPlayerTokens() {
        val actor = player(1)
        val game = game(actor)
        repeat(9) {
            assertTrue(game.grove.tokens.pull(Token.WATER) != null)
        }
        val request = request(game, actor, GameEffect.GAIN_WATER_TOKEN)

        assertFalse(executor.canExecute(request))
        assertFailsWith<EffectExecutionException> {
            executor.execute(request)
        }
        assertEquals(0, actor.tokens.waterCount)
    }

    private fun game(actor: Player): Game =
        GameEngineTestFixture.game(
            players = listOf(actor, player(2))
        )

    private fun player(
        id: Int,
        hand: List<Die> = emptyList(),
        effectStrategy: EffectStrategy = DecisionDirector.baseline().effect
    ): Player = Player(
        id = PlayerId(id),
        decisions = DecisionDirector.baseline().copy(effect = effectStrategy),
        dice = PlayerDice(hand = hand)
    )

    private fun request(
        game: Game,
        actor: Player,
        effect: GameEffect,
        source: GameEffectSource = GameEffectSource.Round(
            roundCard(effect),
            RoundEffectSlot.FIRST
        )
    ): GameEffectRequest = GameEffectRequest(
        game = game,
        actor = actor,
        effect = effect,
        source = source,
        phase = GameEffectPhase.CULTIVATION
    )

    private fun roundCard(effect: GameEffect): RoundCard = RoundCard(
        quantity = 1,
        name = "Test_Round",
        type = RoundCardType.CULTIVATION,
        firstEffect = roundEffect(effect),
        secondEffect = roundEffect(effect),
        backImage = ""
    )

    private fun roundEffect(effect: GameEffect): RoundCardEffect = RoundCardEffect(
        title = "Test",
        backgroundColor = "",
        textColor = "",
        image = "",
        icon = null,
        effect = effect
    )

    private fun wispquake() = dugsolutions.leaf.v35.wisp.domain.WispCard(
        quantity = 1,
        name = "Wispquake",
        title = "Wispquake",
        count = 1,
        effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
        lineIcons = null,
        lineIconsHeight = 0,
        vpIcon = null,
        mainBackdrop = "",
        playImmediately = true
    )

    private open class FixedDie(sides: Int, value: Int) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }

    private class SequenceDie(
        sides: Int,
        initial: Int,
        private val next: Int
    ) : Die(sides) {
        init { adjustTo(initial) }
        override fun roll(): Die {
            adjustTo(next)
            return this
        }
    }

    private open class FirstChoiceStrategy : EffectStrategy {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            request.legalChoices.first()

        override fun chooseOptionalDie(
            request: ChooseOptionalEffectDieRequest
        ): EffectDieChoice? = request.legalChoices.firstOrNull()
    }

    private open class LastChoiceStrategy : EffectStrategy {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            request.legalChoices.last()

        override fun chooseOptionalDie(
            request: ChooseOptionalEffectDieRequest
        ): EffectDieChoice? = request.legalChoices.lastOrNull()
    }

    private class RecordingLastChoiceStrategy : LastChoiceStrategy() {
        val requiredRequests = mutableListOf<ChooseEffectDieRequest>()

        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice {
            requiredRequests += request
            return super.chooseDie(request)
        }
    }

    private class InvalidChoiceStrategy : EffectStrategy {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            EffectDieChoice(index = 99, sides = 20, value = 20)
    }
}
