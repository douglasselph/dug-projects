package dugsolutions.leaf.v35.game.round

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameEngineTestFixture
import dugsolutions.leaf.v35.tokens.Token
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoundEffectResearchTest {
    @Test
    fun emptySunlightSupplyIsRecognizedAsSharedResourceBlock() {
        val game = GameEngineTestFixture.game(1, 1)
        game.grove.tokens.set(Token.SUNLIGHT, 0)

        assertTrue(blockedByEmptySharedResource(game, GameEffect.GAIN_SUNLIGHT_TOKEN))
        assertFalse(blockedByEmptySharedResource(game, GameEffect.GAIN_ONE_VP))
    }

    @Test
    fun availableSunlightIsNotReportedAsSupplyBlock() {
        val game = GameEngineTestFixture.game(1, 1)
        assertTrue(game.grove.tokens.hasSunlight)
        assertFalse(blockedByEmptySharedResource(game, GameEffect.GAIN_SUNLIGHT_TOKEN))
    }
}
