package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.error.InvalidDecisionException
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.effect.ChooseBeeSourceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBeeSourceChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BeeLovedBloomEffectTest {

    private val effect =
        BeeLovedBloomEffect()

    private val nested =
        GameEffectExecutor { }

    @Test
    fun decisionCanExplicitlyStealBeeFromChosenOpponent() {
        val strategy =
            ChooseOpponentBeeStrategy(
                PlayerId(3)
            )

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    strategy
            )
        val opponent2 =
            EffectTestFixture.player(2)
        val opponent3 =
            EffectTestFixture.player(3)

        opponent2.critters.add(
            Critter.BEE
        )
        opponent3.critters.add(
            Critter.BEE
        )

        val game =
            EffectTestFixture.game(
                actor,
                opponent2,
                opponent3
            )

        val groveBefore =
            game.grove.critters.count(
                Critter.BEE
            )

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            ),
            nested
        )

        assertEquals(
            1,
            actor.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            1,
            opponent2.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            0,
            opponent3.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            groveBefore,
            game.grove.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            4,
            actor.critterValues.valueOf(
                Critter.BEE
            )
        )

        assertTrue(
            EffectBeeSourceChoice.Grove in
                strategy.seen
        )
        assertTrue(
            EffectBeeSourceChoice.Opponent(
                PlayerId(2)
            ) in strategy.seen
        )
        assertTrue(
            EffectBeeSourceChoice.Opponent(
                PlayerId(3)
            ) in strategy.seen
        )
    }

    @Test
    fun decisionCanGainBeeFromGrove() {
        val strategy =
            ChooseGroveBeeStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val before =
            game.grove.critters.count(
                Critter.BEE
            )

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            ),
            nested
        )

        assertEquals(
            before - 1,
            game.grove.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            1,
            actor.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            4,
            actor.critterValues.valueOf(
                Critter.BEE
            )
        )
    }

    @Test
    fun repeatedBeeLovedBloomDoesNotStackAboveFour() {
        val actor =
            EffectTestFixture.player(1)
        actor.critters.add(
            Critter.BEE
        )

        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        // Remove all available source Bees so only the value effect occurs.
        repeat(
            game.grove.critters.count(
                Critter.BEE
            )
        ) {
            game.grove.critters.remove(
                Critter.BEE
            )
        }

        val request =
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            )

        effect.execute(
            request,
            nested
        )
        effect.execute(
            request,
            nested
        )

        assertEquals(
            4,
            actor.critterValues.valueOf(
                Critter.BEE
            )
        )
    }

    @Test
    fun noBeeSourceStillEstablishesRoundValueForBeesGainedLater() {
        val actor =
            EffectTestFixture.player(1)
        val other =
            EffectTestFixture.player(2)
        val game =
            EffectTestFixture.game(
                actor,
                other
            )

        repeat(
            game.grove.critters.count(
                Critter.BEE
            )
        ) {
            game.grove.critters.remove(
                Critter.BEE
            )
        }

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            ),
            nested
        )

        actor.critters.add(
            Critter.BEE
        )

        assertEquals(
            4,
            actor.critterValues.valueOf(
                Critter.BEE
            )
        )
    }

    @Test
    fun effectWorksInBattleAsWellAsCultivation() {
        val actor =
            EffectTestFixture.player(1)
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val battle =
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
            ).copy(
                phase = GameEffectPhase.BATTLE
            )

        assertTrue(
            effect.canExecute(
                battle
            )
        )
    }

    @Test
    fun illegalOpponentSourceIsRejectedBeforeMutation() {
        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy =
                    ChooseOpponentBeeStrategy(
                        PlayerId(99)
                    )
            )
        val other =
            EffectTestFixture.player(2)
        other.critters.add(
            Critter.BEE
        )

        val game =
            EffectTestFixture.game(
                actor,
                other
            )

        val groveBefore =
            game.grove.critters.count(
                Critter.BEE
            )

        assertFailsWith<
            InvalidDecisionException
        > {
            effect.execute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND
                ),
                nested
            )
        }

        assertEquals(
            0,
            actor.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            1,
            other.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            groveBefore,
            game.grove.critters.count(
                Critter.BEE
            )
        )
        assertEquals(
            Critter.BEE.baseValue,
            actor.critterValues.valueOf(
                Critter.BEE
            )
        )
    }

    private abstract class BeeStrategy :
        EffectStrategy {
        override fun chooseDie(
            request:
            ChooseEffectDieRequest
        ): EffectDieChoice =
            request.legalChoices.first()
    }

    private class ChooseGroveBeeStrategy :
        BeeStrategy() {
        override fun chooseBeeSource(
            request:
            ChooseBeeSourceRequest
        ): EffectBeeSourceChoice =
            request.legalChoices.first {
                it ==
                    EffectBeeSourceChoice
                        .Grove
            }
    }

    private class ChooseOpponentBeeStrategy(
        private val target:
            PlayerId
    ) : BeeStrategy() {
        var seen:
            List<EffectBeeSourceChoice> =
            emptyList()

        override fun chooseBeeSource(
            request:
            ChooseBeeSourceRequest
        ): EffectBeeSourceChoice {
            seen =
                request.legalChoices

            return EffectBeeSourceChoice
                .Opponent(target)
        }
    }
}
