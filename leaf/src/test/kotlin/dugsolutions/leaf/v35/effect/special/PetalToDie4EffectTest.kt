package dugsolutions.leaf.v35.effect.special

import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalToDie4Request
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PetalToDie4EffectTest {

    private val effect = PetalToDie4Effect()
    private val nested = GameEffectExecutor { }

    @Test
    fun gainBranchTakesAvailableGraftBedD4AndSetsItToFourWithoutRolling() {
        val strategy = ChooseGainD4Strategy()
        val actor =
            EffectTestFixture.player(
                id = 1,
                effectStrategy = strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        game.grove.graftBed.returnD4()

        val request =
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
            )

        effect.execute(request, nested)

        assertEquals(
            0,
            game.grove.graftBed.count(
                DieSides.D4
            )
        )
        assertEquals(1, actor.dice.handSize)
        assertEquals(4, actor.dice.hand.single().sides)
        assertEquals(4, actor.dice.hand.single().value)
        assertTrue(
            PetalToDie4Choice.GainD4 in
                strategy.seen
        )
    }

    @Test
    fun trashBranchNamesExactD4ThenRaisesEveryRemainingDiePlusFour() {
        val firstD4 = FixedEffectDie(4, 1)
        val secondD4 = FixedEffectDie(4, 4)
        val d8 = FixedEffectDie(8, 2)
        val strategy =
            ChooseSecondD4TrashStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(
                    firstD4,
                    secondD4,
                    d8
                ),
                effectStrategy = strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
            ),
            nested
        )

        assertEquals(
            listOf(firstD4, d8),
            actor.dice.hand
        )
        assertEquals(4, firstD4.value)
        assertEquals(6, d8.value)

        // Universal Trash rule: a Trashed D4 returns to the Graft Bed.
        assertEquals(
            1,
            game.grove.graftBed.count(
                DieSides.D4
            )
        )

        assertTrue(
            strategy.seen.any {
                it is PetalToDie4Choice.TrashD4AndRaiseAll &&
                    it.die.index == 1
            }
        )
    }

    @Test
    fun bothBranchesAreOfferedWhenBothAreActuallyAvailable() {
        val strategy =
            RecordingFirstPetalChoiceStrategy()

        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(
                    FixedEffectDie(4, 2)
                ),
                effectStrategy = strategy
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )
        game.grove.graftBed.returnD4()

        effect.execute(
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
            ),
            nested
        )

        assertEquals(2, strategy.seen.size)
        assertEquals(
            PetalToDie4Choice.GainD4,
            strategy.seen.first()
        )
        assertTrue(
            strategy.seen.last() is
                PetalToDie4Choice.TrashD4AndRaiseAll
        )
    }

    @Test
    fun noAvailableD4AndNoHandD4MeansEffectCannotExecute() {
        val actor =
            EffectTestFixture.player(
                1,
                hand = listOf(
                    FixedEffectDie(6, 2)
                )
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        assertFalse(
            effect.canExecute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
                )
            )
        )
    }

    @Test
    fun battleWaitsForBattleGridSupportRatherThanOfferingOnlyHalfTheCard() {
        val actor =
            EffectTestFixture.player(
                1,
                hand = listOf(
                    FixedEffectDie(4, 2)
                )
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )
        game.grove.graftBed.returnD4()

        val battleRequest =
            EffectTestFixture.request(
                game,
                actor,
                GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
            ).copy(
                phase = GameEffectPhase.BATTLE
            )

        assertFalse(
            effect.canExecute(
                battleRequest
            )
        )
    }

    @Test
    fun illegalBranchIsRejectedBeforeMutation() {
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(
                    FixedEffectDie(4, 2)
                ),
                effectStrategy =
                    IllegalGainPetalStrategy()
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        assertFailsWith<
            IllegalStateException
        > {
            effect.execute(
                EffectTestFixture.request(
                    game,
                    actor,
                    GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4
                ),
                nested
            )
        }

        assertEquals(
            listOf(2),
            actor.dice.hand.map {
                it.value
            }
        )
        assertEquals(
            0,
            game.grove.graftBed.count(
                DieSides.D4
            )
        )
    }

    private abstract class PetalStrategy :
        EffectStrategy {
        override fun chooseDie(
            request: ChooseEffectDieRequest
        ): EffectDieChoice =
            request.legalChoices.first()
    }

    private class ChooseGainD4Strategy :
        PetalStrategy() {
        var seen:
            List<PetalToDie4Choice> =
            emptyList()

        override fun choosePetalToDie4(
            request:
            ChoosePetalToDie4Request
        ): PetalToDie4Choice {
            seen =
                request.legalChoices

            return request
                .legalChoices
                .first {
                    it ==
                        PetalToDie4Choice
                            .GainD4
                }
        }
    }

    private class RecordingFirstPetalChoiceStrategy :
        PetalStrategy() {
        var seen:
            List<PetalToDie4Choice> =
            emptyList()

        override fun choosePetalToDie4(
            request:
            ChoosePetalToDie4Request
        ): PetalToDie4Choice {
            seen =
                request.legalChoices
            return request
                .legalChoices
                .first()
        }
    }

    private class ChooseSecondD4TrashStrategy :
        PetalStrategy() {
        var seen:
            List<PetalToDie4Choice> =
            emptyList()

        override fun choosePetalToDie4(
            request:
            ChoosePetalToDie4Request
        ): PetalToDie4Choice {
            seen =
                request.legalChoices

            return request
                .legalChoices
                .filterIsInstance<
                    PetalToDie4Choice
                        .TrashD4AndRaiseAll
                >()
                .first {
                    it.die.index == 1
                }
        }
    }

    private class IllegalGainPetalStrategy :
        PetalStrategy() {
        override fun choosePetalToDie4(
            request:
            ChoosePetalToDie4Request
        ): PetalToDie4Choice =
            PetalToDie4Choice.GainD4
    }
}
