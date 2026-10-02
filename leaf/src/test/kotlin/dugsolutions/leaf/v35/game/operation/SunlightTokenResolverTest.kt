package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.SunlightTokenChange
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SunlightTokenResolverTest {

    @Test
    fun spend_movesSunlightFromPlayerBackToGroveAndRecordsSpend() {
        val player = Player(PlayerId(1), DecisionDirector.baseline())
        val other = Player(PlayerId(2), DecisionDirector.baseline())
        val game = GameEngineTestFixture.game(players = listOf(player, other))
        assertTrue(game.grove.tokens.pull(Token.SUNLIGHT) != null)
        player.tokens.add(Token.SUNLIGHT)

        assertTrue(SunlightTokenResolver.spend(game, player))

        assertEquals(0, player.tokens.sunlightCount)
        assertEquals(9, game.grove.tokens.sunlightCount)
        val change = game.chronicle.entries.filterIsInstance<GameEntry.SunlightTokenChanged>().single()
        assertEquals(player.id, change.playerId)
        assertEquals(SunlightTokenChange.SPENT, change.change)
    }

    @Test
    fun spend_withoutSunlight_returnsFalseAndDoesNotChangeGrove() {
        val player = Player(PlayerId(1), DecisionDirector.baseline())
        val other = Player(PlayerId(2), DecisionDirector.baseline())
        val game = GameEngineTestFixture.game(players = listOf(player, other))

        assertFalse(SunlightTokenResolver.spend(game, player))
        assertEquals(9, game.grove.tokens.sunlightCount)
        assertTrue(game.chronicle.entries.filterIsInstance<GameEntry.SunlightTokenChanged>().isEmpty())
    }
}
