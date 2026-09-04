package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FirstEffectChoiceStrategy
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectButterflyTargetRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectButterflyTargetChoice
import dugsolutions.leaf.v35.tokens.Butterfly
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlluringNectarEffectTest {

    private val effect = AlluringNectarEffect()
    private val nested = GameEffectExecutor { }

    @Test
    fun decisionSeesExactOpponentAndButterflyTargets_andChosenButterflyIsStolen() {
        val strategy = ChooseButterflyStrategy(
            EffectButterflyTargetChoice(
                ownerId = PlayerId(3),
                butterfly = Butterfly.PURPLE
            )
        )
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val opponent2 = EffectTestFixture.player(2)
        val opponent3 = EffectTestFixture.player(3)
        val game = EffectTestFixture.game(actor, opponent2, opponent3)

        moveButterflyFromGrove(game, opponent2, Butterfly.GREEN)
        moveButterflyFromGrove(game, opponent3, Butterfly.PURPLE)

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES
            ),
            nested
        )

        assertEquals(
            listOf(
                EffectButterflyTargetChoice(PlayerId(2), Butterfly.GREEN),
                EffectButterflyTargetChoice(PlayerId(3), Butterfly.PURPLE)
            ),
            strategy.offered
        )
        assertTrue(Butterfly.GREEN in opponent2.butterflies.all)
        assertFalse(Butterfly.PURPLE in opponent3.butterflies.all)
        assertTrue(Butterfly.PURPLE in actor.butterflies.all)
        assertTrue(actor.butterflies.isFaceUp(Butterfly.PURPLE))
    }

    @Test
    fun afterSteal_allActorButterfliesAreTurnedFaceUp() {
        val strategy = ChooseButterflyStrategy(
            EffectButterflyTargetChoice(PlayerId(2), Butterfly.RED)
        )
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)

        moveButterflyFromGrove(game, actor, Butterfly.YELLOW)
        actor.butterflies.faceDown(Butterfly.YELLOW)
        moveButterflyFromGrove(game, opponent, Butterfly.RED)
        opponent.butterflies.faceDown(Butterfly.RED)

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES
            ),
            nested
        )

        assertTrue(actor.butterflies.isFaceUp(Butterfly.YELLOW))
        assertTrue(actor.butterflies.isFaceUp(Butterfly.RED))
    }

    @Test
    fun whenNoOpponentHasButterfly_skipsStealDecisionButStillRefreshesOwnedButterflies() {
        val strategy = RecordingButterflyStrategy()
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)

        moveButterflyFromGrove(game, actor, Butterfly.GREEN)
        actor.butterflies.faceDown(Butterfly.GREEN)

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES
            ),
            nested
        )

        assertEquals(0, strategy.calls)
        assertTrue(actor.butterflies.isFaceUp(Butterfly.GREEN))
    }

    @Test
    fun illegalOpponentButterflyChoice_isRejectedBeforeMutation() {
        val strategy = ChooseButterflyStrategy(
            EffectButterflyTargetChoice(
                ownerId = PlayerId(99),
                butterfly = Butterfly.GREEN
            )
        )
        val actor = EffectTestFixture.player(1, effectStrategy = strategy)
        val opponent = EffectTestFixture.player(2)
        val game = EffectTestFixture.game(actor, opponent)

        moveButterflyFromGrove(game, actor, Butterfly.YELLOW)
        actor.butterflies.faceDown(Butterfly.YELLOW)
        moveButterflyFromGrove(game, opponent, Butterfly.GREEN)

        assertFailsWith<IllegalStateException> {
            effect.execute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES
                ),
                nested
            )
        }

        assertTrue(Butterfly.GREEN in opponent.butterflies.all)
        assertFalse(Butterfly.GREEN in actor.butterflies.all)
        assertTrue(actor.butterflies.isFaceDown(Butterfly.YELLOW))
    }

    private fun moveButterflyFromGrove(
        game: dugsolutions.leaf.v35.game.Game,
        player: dugsolutions.leaf.v35.player.Player,
        butterfly: Butterfly
    ) {
        check(game.grove.butterflies.remove(butterfly))
        player.butterflies.add(butterfly)
    }

    private open class RecordingButterflyStrategy : FirstEffectChoiceStrategy() {
        var calls = 0
        var offered: List<EffectButterflyTargetChoice> = emptyList()

        override fun chooseButterflyTarget(
            request: ChooseEffectButterflyTargetRequest
        ): EffectButterflyTargetChoice {
            calls++
            offered = request.legalChoices
            return request.legalChoices.first()
        }
    }

    private class ChooseButterflyStrategy(
        private val choice: EffectButterflyTargetChoice
    ) : RecordingButterflyStrategy() {
        override fun chooseButterflyTarget(
            request: ChooseEffectButterflyTargetRequest
        ): EffectButterflyTargetChoice {
            calls++
            offered = request.legalChoices
            return choice
        }
    }
}
