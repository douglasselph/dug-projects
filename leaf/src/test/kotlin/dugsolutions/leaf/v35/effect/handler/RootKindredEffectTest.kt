package dugsolutions.leaf.v35.effect.handler

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RootKindredEffectTest {

    private val handler = DieValueEffectHandler()
    private val nested = GameEffectExecutor { }

    @Test
    fun strategyChoosesSourceAndTargetTogether_andTargetMatchesSourceValue() {
        val target = FixedEffectDie(8, 2)
        val source = FixedEffectDie(10, 8)
        val strategy = KindredStrategy(sourceIndex = 1, targetIndex = 0)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(target, source),
            effectStrategy = strategy
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.SET_DIE_TO_MATCH_ANOTHER
        )

        assertTrue(handler.canExecute(request))
        handler.execute(request, nested)

        assertEquals(8, target.value)
        assertEquals(8, source.value)
        assertTrue(strategy.seen.any {
            it.source.index == 1 && it.target.index == 0
        })
    }

    @Test
    fun legalPairsExcludeTargetsTooSmallToMatchSource() {
        val smallTarget = FixedEffectDie(6, 2)
        val highSource = FixedEffectDie(10, 9)
        val safeSource = FixedEffectDie(8, 5)
        val strategy = CapturingPairStrategy()
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(smallTarget, highSource, safeSource),
            effectStrategy = strategy
        )
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        handler.execute(
            EffectTestFixture.request(game, actor, GameEffect.SET_DIE_TO_MATCH_ANOTHER),
            nested
        )

        assertTrue(strategy.seen.none {
            it.source.value == 9 && it.target.sides == 6
        })
        assertTrue(strategy.seen.all {
            it.source.index != it.target.index &&
                it.source.value <= it.target.sides
        })
    }

    private class KindredStrategy(
        private val sourceIndex: Int,
        private val targetIndex: Int
    ) : EffectStrategy {
        var seen: List<EffectDiePairChoice> = emptyList()

        override fun chooseDie(request: ChooseEffectDieRequest) = request.legalChoices.first()

        override fun chooseDiePair(
            request: ChooseEffectDiePairRequest
        ): EffectDiePairChoice {
            seen = request.legalChoices
            return request.legalChoices.first {
                it.source.index == sourceIndex && it.target.index == targetIndex
            }
        }
    }

    private class CapturingPairStrategy : EffectStrategy {
        var seen: List<EffectDiePairChoice> = emptyList()

        override fun chooseDie(request: ChooseEffectDieRequest) = request.legalChoices.first()

        override fun chooseDiePair(
            request: ChooseEffectDiePairRequest
        ): EffectDiePairChoice {
            seen = request.legalChoices
            return request.legalChoices.first()
        }
    }
}
