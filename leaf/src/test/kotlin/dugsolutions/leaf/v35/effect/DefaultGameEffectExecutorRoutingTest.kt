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
        val crossPlayer = RecordingHandler()
        val upgrade = RecordingHandler()
        val vineAndDine = RecordingHandler()
        val petalToDie4 = RecordingHandler()
        val beeLovedBloom = RecordingHandler()
        val alluringNectar = RecordingHandler()
        val partingThorn = RecordingHandler()
        val snipHappens = RecordingHandler()
        val vineAndAgain = RecordingHandler()
        val oEdelweiss = RecordingHandler()
        val wispReckoning = RecordingHandler()
        val overgrowth = RecordingHandler()
        val wispquake = RecordingHandler()
        val executor = DefaultGameEffectExecutor(
            dieValueEffects = dieValue,
            drawEffects = draw,
            resourceEffects = resource,
            crossPlayerEffects = crossPlayer,
            upgradeEffects = upgrade,
            vineAndDineEffect = vineAndDine,
            petalToDie4Effect = petalToDie4,
            beeLovedBloomEffect = beeLovedBloom,
            alluringNectarEffect = alluringNectar,
            partingThornEffect = partingThorn,
            snipHappensEffect = snipHappens,
            vineAndAgainEffect = vineAndAgain,
            oEdelweissEffect = oEdelweiss,
            wispReckoningEffect = wispReckoning,
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
                GameEffect.GAIN_ANY_DIE_TO_DISCARD
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE
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
                GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE
            )
        )
        executor.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS
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
            listOf(
                GameEffect.GAIN_WATER_TOKEN,
                GameEffect.GAIN_ANY_DIE_TO_DISCARD,
                GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT,
                GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS,
                GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND
            ),
            resource.effects
        )
        assertEquals(
            listOf(
                GameEffect.GAIN_WATER_AND_SPEND_3_TO_REROLL_BATTLE_DIE,
                GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE
            ),
            crossPlayer.effects
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
            listOf(GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4),
            petalToDie4.effects
        )
        assertEquals(
            listOf(GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND),
            beeLovedBloom.effects
        )
        assertEquals(
            listOf(GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES),
            alluringNectar.effects
        )
        assertEquals(
            listOf(GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE),
            partingThorn.effects
        )
        assertEquals(
            listOf(GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE),
            snipHappens.effects
        )
        assertEquals(
            listOf(GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT),
            vineAndAgain.effects
        )
        assertEquals(
            listOf(GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE),
            oEdelweiss.effects
        )
        assertEquals(
            listOf(GameEffect.LIMIT_WISPS_AND_TRASH_EXCESS),
            wispReckoning.effects
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
            GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
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
