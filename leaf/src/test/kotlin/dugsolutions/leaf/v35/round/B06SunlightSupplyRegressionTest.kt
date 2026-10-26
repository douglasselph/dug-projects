package dugsolutions.leaf.v35.round

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.SunlightTokenChange
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.effect.RoundEffectSlot
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** B06 complements A05's card-by-card token/face checks and B02's full-round trace. */
class B06SunlightSupplyRegressionTest {
    private val sunlightRounds = RoundCardRegistry(GameEffectConverter()).apply {
        loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
    }.getAllCards().filter { it.name in setOf(
        "Resource_Sunlight_Compost", "Resource_Sunlight_Mulch", "Resource_Sunlight_Water"
    ) }

    private fun player(id: Int): Player {
        val die = object : Die(6) { override fun roll(): Die = this }.apply { adjustTo(2) }
        return Player(PlayerId(id), DecisionDirector.baseline(), dice = PlayerDice(hand = listOf(die)))
    }

    @Test
    fun everySunlightCardRespectsSharedSupplyAndNeverRaisesAnyDie() {
        assertEquals(3, sunlightRounds.size)
        val executor = DefaultGameEffectExecutor()
        sunlightRounds.forEach { card ->
            val actor = player(1)
            val opponent = player(2)
            val game = GameEngineTestFixture.game(players = listOf(actor, opponent))
            val request = GameEffectRequest(
                game = game, actor = actor,
                effect = RoundValueResolver.CANONICAL.effectFor(card, RoundEffectSlot.FIRST),
                source = GameEffectSource.Round(card, RoundEffectSlot.FIRST),
                phase = GameEffectPhase.CULTIVATION
            )
            assertEquals(GameEffect.GAIN_SUNLIGHT_TOKEN, request.effect, card.name)
            // Leave exactly one physical Sunlight in the Grove.
            while (game.grove.tokens.sunlightCount > 1) {
                assertTrue(game.grove.tokens.pull(Token.SUNLIGHT) != null)
            }
            assertTrue(executor.canExecute(request), card.name)
            executor.execute(request)
            assertEquals(1, actor.tokens.sunlightCount, card.name)
            assertEquals(0, opponent.tokens.sunlightCount, card.name)
            assertEquals(0, game.grove.tokens.sunlightCount, card.name)
            assertEquals(listOf(2, 2), game.players.map { it.dice.hand.single().value }, card.name)
            assertTrue(game.chronicle.entries.none { it is GameEntry.DieValueChanged }, card.name)
            assertEquals(1, game.chronicle.entries.filterIsInstance<GameEntry.SunlightTokenChanged>()
                .count { it.change == SunlightTokenChange.GAINED }, card.name)
            // Once depleted, no further player may execute the gain.
            val opponentRequest = GameEffectRequest(
                game = game, actor = opponent, effect = request.effect,
                source = GameEffectSource.Round(card, RoundEffectSlot.FIRST),
                phase = GameEffectPhase.CULTIVATION
            )
            assertFalse(executor.canExecute(request), card.name)
            assertFalse(executor.canExecute(opponentRequest), card.name)
            assertEquals(1, actor.tokens.sunlightCount)
            assertEquals(0, opponent.tokens.sunlightCount)
        }
    }

    @Test
    fun emptySupplyNeverMakesSunlightGainExecutable() {
        val executor = DefaultGameEffectExecutor()
        sunlightRounds.forEach { card ->
            val actor = player(1)
            val game = GameEngineTestFixture.game(players = listOf(actor, player(2)))
            while (game.grove.tokens.pull(Token.SUNLIGHT) != null) { /* drain */ }
            val request = GameEffectRequest(
                game = game, actor = actor,
                effect = RoundValueResolver.CANONICAL.effectFor(card, RoundEffectSlot.FIRST),
                source = GameEffectSource.Round(card, RoundEffectSlot.FIRST),
                phase = GameEffectPhase.CULTIVATION
            )
            assertFalse(executor.canExecute(request), card.name)
            assertEquals(0, actor.tokens.sunlightCount)
            assertEquals(2, actor.dice.hand.single().value)
            assertEquals(0, game.chronicle.entries.filterIsInstance<GameEntry.SunlightTokenChanged>().size)
        }
    }
}
