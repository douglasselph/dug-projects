package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.effect.handler.EffectHandler
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultGameEffectExecutorRoutingTest {

    @Test
    fun routesSupportedEffectsToTheirExplicitOwners() {
        val dieValue = RecordingHandler()
        val draw = RecordingHandler()
        val resource = RecordingHandler()
        val upgrade = RecordingHandler()
        val vineAndDine = RecordingHandler()
        val overgrowth = RecordingHandler()
        val wispquake = RecordingHandler()
        val executor = DefaultGameEffectExecutor(
            dieValueEffects = dieValue,
            drawEffects = draw,
            resourceEffects = resource,
            upgradeEffects = upgrade,
            vineAndDineEffect = vineAndDine,
            overgrowthEffect = overgrowth,
            wispquakeEffect = wispquake
        )
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.RAISE_DIE_PLUS_3
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.DRAW_TWO_DICE
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_WATER_TOKEN
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.UPGRADE_DIE_FROM_HAND
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN
            )
        )

        assertEquals(
            listOf(GameEffect.RAISE_DIE_PLUS_3),
            dieValue.effects
        )
        assertEquals(
            listOf(GameEffect.DRAW_TWO_DICE),
            draw.effects
        )
        assertEquals(
            listOf(GameEffect.GAIN_WATER_TOKEN),
            resource.effects
        )
        assertEquals(
            listOf(GameEffect.UPGRADE_DIE_FROM_HAND),
            upgrade.effects
        )
        assertEquals(
            listOf(GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5),
            vineAndDine.effects
        )
        assertEquals(
            listOf(GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW),
            overgrowth.effects
        )
        assertEquals(
            listOf(GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN),
            wispquake.effects
        )
    }

    @Test
    fun unsupportedEffect_hasNoHandler() {
        val actor = EffectTestFixture.player(1)
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
        val executor = DefaultGameEffectExecutor()
        val request = EffectTestFixture.request(
            game,
            actor,
            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
        )

        assertFalse(executor.canExecute(request))
    }

    private class RecordingHandler : EffectHandler {
        val effects = mutableListOf<GameEffect>()

        override fun canExecute(
            request: GameEffectRequest
        ): Boolean = true

        override fun execute(
            request: GameEffectRequest,
            executor: GameEffectExecutor
        ) {
            effects += request.effect
        }
    }
}
