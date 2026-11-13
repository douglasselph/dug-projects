package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.CardEffectReadiness
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieSizeRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.random.die.di.DieFactory
import dugsolutions.leaf.v35.round.RoundCardRegistry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** NC-21/22: actual official Round CSV -> handler -> state / Roll Reward / row. */
class BoundedDieGainEffectTest {
    private val executor = DefaultGameEffectExecutor()
    private val rounds = RoundCardRegistry(dugsolutions.leaf.v35.effect.GameEffectConverter()).apply {
        loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
    }

    @Test
    fun surgeChoosesOnlyAvailableDiceAtOrUnderD12AndPlacesInDiscardWithoutRolling() {
        val strategy = CapturingSizeChoice()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        // Only D8 and D20 left; D20 is not eligible for Surge.
        for (size in listOf(DieSides.D6, DieSides.D10, DieSides.D12)) {
            while (game.grove.graftBed.take(size)) Unit
        }
        val request = request(game, actor, "Battle_Bloom_Surge")
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(listOf(DieSides.D8), strategy.seen)
        assertEquals(8, actor.dice.discard.single().sides)
        assertTrue(actor.dice.hand.isEmpty())
        assertEquals(8, game.grove.graftBed.count(DieSides.D8))
        assertEquals(0, game.chronicle.entries.filterIsInstance<GameEntry.DieRolled>().size)
        assertEquals(DieSides.D8, game.chronicle.entries.filterIsInstance<GameEntry.DieGained>().single().sides)
    }

    @Test
    fun surgeAllowsReturnedD4ButRejectsUnavailableOrTooLargeSelectionBeforeMutation() {
        val strategy = CapturingSizeChoice(DieSides.D20)
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        game.grove.graftBed.returnD4()
        val request = request(game, actor, "Battle_Bloom_Surge")
        assertFailsWith<InvalidDecisionException> { executor.execute(request) }
        assertTrue(DieSides.D4 in strategy.seen)
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(1, game.grove.graftBed.count(DieSides.D4))
    }

    @Test
    fun surgeUnavailableWhenEveryDieAtOrBelowD12IsExhaustedEvenIfD20Remains() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        for (sides in DieSides.entries.filter { it.value <= 12 }) {
            while (game.grove.graftBed.take(sides)) Unit
        }
        assertFalse(executor.canExecute(request(game, actor, "Battle_Bloom_Surge")))
        assertEquals(9, game.grove.graftBed.count(DieSides.D20))
    }

    @Test
    fun swellUsesChosenD8NowAndRecordsRollRewardAndActualBattlePlacement() {
        val strategy = CapturingSizeChoice()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val other = EffectTestFixture.player(2)
        val game = withAlwaysOneRoll(EffectTestFixture.game(actor, other))
        val battle = BattleState(listOf(actor, other))
        val request = request(game, actor, "Battle_Scatter_Swell", battle)
        assertTrue(executor.canExecute(request))
        executor.execute(request)
        assertEquals(listOf(DieSides.D6, DieSides.D8), strategy.seen)
        assertEquals(1, actor.dice.hand.single().value)
        assertEquals(8, actor.dice.hand.single().sides)
        assertTrue(actor.dice.discard.isEmpty())
        assertEquals(1, actor.critters.all.size) // Roll 1 gains an ordinary Critter.
        assertEquals(8, game.grove.graftBed.count(DieSides.D8))
        assertEquals(1, game.chronicle.entries.filterIsInstance<GameEntry.DieRolled>().size)
        assertEquals(1, game.chronicle.entries.filterIsInstance<GameEntry.BattleDieRow>().size)
        assertEquals(1, battle.grid.square(actor.id, StrikeRow.TOP).dieCount)
    }

    @Test
    fun swellNeverOffersUseNowWhenAllOwnSquaresAreFullAndNeverOpensLockedRows() {
        val actor = EffectTestFixture.player(1)
        val other = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, other)
        val battle = BattleState(listOf(actor, other))
        for (row in StrikeRow.entries) {
            repeat(3) {
                val die = FixedEffectDie(6, 4)
                actor.dice.addToHand(die)
                battle.grid.placeDie(actor, row, die)
            }
        }
        val originalHand = actor.dice.hand.toList()
        val request = request(game, actor, "Battle_Scatter_Swell", battle)
        assertFalse(executor.canExecute(request))
        assertEquals(9, game.grove.graftBed.count(DieSides.D8))
        // Dice placed on the Battle grid stay in the player's Hand; checking
        // Swell's legality must not consume or alter any of those live dice.
        assertEquals(9, actor.dice.hand.size)
        assertTrue(actor.dice.hand.zip(originalHand).all { (actual, before) -> actual === before })
        assertTrue(game.chronicle.entries.filterIsInstance<GameEntry.DieGained>().isEmpty())

        // Use a separate, fresh game for the closed-row check. The earlier
        // actor still has nine dice committed in its original Battle state.
        val freshActor = EffectTestFixture.player(1)
        val freshOther = EffectTestFixture.player(2)
        val freshGame = EffectTestFixture.game(freshActor, freshOther)
        val fresh = BattleState(listOf(freshActor, freshOther))
        fresh.grid.closeRow(StrikeRow.TOP)
        val legal = request(freshGame, freshActor, "Battle_Scatter_Swell", fresh)
        assertTrue(executor.canExecute(legal))
        executor.execute(legal)
        assertEquals(0, fresh.grid.square(freshActor.id, StrikeRow.TOP).dieCount)
        assertEquals(1, fresh.grid.square(freshActor.id, StrikeRow.MIDDLE).dieCount)
    }

    @Test
    fun bothNewRoundIdentitiesAreExecutableAndDecisionContractsRemainDistinct() {
        for ((id, effect) in listOf(
            "Battle_Bloom_Surge" to GameEffect.GAIN_UP_TO_D12_TO_DISCARD,
            "Battle_Scatter_Swell" to GameEffect.GAIN_UP_TO_D8_AND_USE_NOW
        )) {
            val card = requireNotNull(rounds.getCard(id))
            assertEquals(effect, card.secondEffect.effect)
            assertTrue(executor.supports(effect))
            assertFalse(effect in CardEffectReadiness.pending)
        }
    }

    private fun request(game: Game, actor: Player, name: String, battle: BattleState? = null): GameEffectRequest {
        val round = requireNotNull(rounds.getCard(name))
        return GameEffectRequest(
            game = game,
            actor = actor,
            effect = round.secondEffect.effect,
            source = GameEffectSource.Round(round, RoundEffectSlot.SECOND),
            phase = GameEffectPhase.BATTLE,
            battleState = battle
        )
    }

    private fun withAlwaysOneRoll(base: Game): Game = Game(
        config = base.config,
        grove = base.grove,
        players = base.players,
        chronicle = base.chronicle,
        roundDeck = base.roundDeck,
        randomizer = base.randomizer,
        dieFactory = DieFactory(object : Randomizer {
            override fun nextBoolean() = false
            override fun nextInt(from: Int, until: Int) = from
            override fun nextInt(until: Int) = 0
            override fun <T> randomOrNull(list: List<T>): T? = list.firstOrNull()
            override fun <T> shuffled(list: List<T>): List<T> = list
        })
    )

    private class CapturingSizeChoice(var forced: DieSides? = null) : EffectStrategy {
        var seen: List<DieSides> = emptyList()
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            request.legalChoices.first()
        override fun chooseDieSize(request: ChooseEffectDieSizeRequest): DieSides {
            seen = request.legalChoices
            return forced ?: request.legalChoices.last()
        }
    }
}
