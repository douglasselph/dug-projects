package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.SampleDie
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FutureDiceForecastTest {
    private val cultivation = RoundCardType.CULTIVATION
    private val battle = RoundCardType.BATTLE
    private val noRewards = RollRewardOpportunities(false, false)

    private fun dice(vararg sides: Int): FutureDiceZones = FutureDiceZones(
        supply = sides.mapIndexed { index, sides -> FutureDie("d$index", sides) }
    )

    @Test fun `simple six die pool has exact three four and five draw future totals`() {
        val zones = dice(4, 4, 4, 6, 6, 6)
        val rounds = listOf(cultivation, cultivation)
        val three = FutureDiceForecast.project(zones, rounds, CultivationDrawAssumption.OPENING_ONLY)
        val four = FutureDiceForecast.project(zones, rounds, CultivationDrawAssumption.TYPICAL_ONE_EXTRA)
        val five = FutureDiceForecast.project(zones, rounds, CultivationDrawAssumption.DRAW_FOCUSED)
        assertEquals(18.0, three.expectedSpendingPips, 1e-12)
        assertEquals(23.0, four.expectedSpendingPips, 1e-12)
        assertEquals(29.0, five.expectedSpendingPips, 1e-12)
        assertEquals(listOf(3, 3), three.rounds.map { it.realizedDraws })
        assertEquals(listOf(4, 4), four.rounds.map { it.realizedDraws })
        assertEquals(listOf(5, 5), five.rounds.map { it.realizedDraws })
        assertEquals(listOf(4, 4, 4, 6), four.rounds[0].encounters.map { it.die.sides })
        assertEquals(listOf(6, 6, 4, 4), four.rounds[1].encounters.map { it.die.sides })
    }

    @Test fun `a future battle starts with three draws regardless of cultivation assumption`() {
        val zones = dice(4, 6, 8, 10, 12, 20)
        for (assumption in CultivationDrawAssumption.entries) {
            val result = FutureDiceForecast.project(zones, listOf(battle), assumption)
            assertEquals(3, result.rounds.single().plannedDraws)
            assertEquals(listOf(4, 6, 8), result.encounters.map { it.die.sides })
        }
        val knownExtra = FutureDiceForecast.project(zones, listOf(battle),
            additionalDrawsByFutureRound = mapOf(1 to 1))
        assertEquals(listOf(4, 6, 8, 10), knownExtra.encounters.map { it.die.sides })
    }

    @Test fun `D20 purchased into Discard may never be seen before game end`() {
        val zones = dice(4, 4, 4).withPurchaseIntoDiscard("bought-d20", 20)
        val result = FutureDiceForecast.project(zones, listOf(battle))
        val bought = result.byDie.single { it.die.id == "bought-d20" }
        assertEquals(0, bought.encounters)
        assertEquals(null, bought.firstFutureRound)
        assertEquals(7.5, result.expectedSpendingPips, 1e-12)
        assertEquals(0.0, bought.expectedValue.spendingPips)
    }

    @Test fun `purchase latency follows exact supply then discard recycle`() {
        val zones = dice(4, 4, 4).withPurchaseIntoDiscard("bought-d20", 20)
        val result = FutureDiceForecast.project(zones, listOf(battle, battle, battle))
        val bought = result.byDie.single { it.die.id == "bought-d20" }
        // D20 is delayed until the THIRD round: on round two the three
        // recycled D4s are selected before the higher-sided purchased die.
        assertEquals(1, bought.encounters)
        assertEquals(3, bought.firstFutureRound)
        assertEquals(listOf(4, 4, 4), result.rounds[0].encounters.map { it.die.sides })
        assertEquals(listOf(4, 4, 4), result.rounds[1].encounters.map { it.die.sides })
        assertEquals(listOf(20, 4, 4), result.rounds[2].encounters.map { it.die.sides })
    }

    @Test fun `D12 to D20 gives four mean pips each future time and ordinary upgrades one`() {
        val d12 = FutureDiceZones(hand = listOf(FutureDie("target", 12)))
        val d20 = d12.withHandUpgradeIntoDiscard("target", 20)
        val old = FutureDiceForecast.project(d12, listOf(battle, cultivation), rewards = noRewards)
        val upgraded = FutureDiceForecast.project(d20, listOf(battle, cultivation), rewards = noRewards)
        assertEquals(2, old.byDie.single().encounters)
        assertEquals(8.0, upgraded.expectedSpendingPips - old.expectedSpendingPips, 1e-12)
        assertEquals(4.0, ImmediatePipValuation.roll(20, noRewards).spendingPips -
            ImmediatePipValuation.roll(12, noRewards).spendingPips, 1e-12)
        val d6 = FutureDiceZones(hand = listOf(FutureDie("target", 6)))
        val d8 = d6.withHandUpgradeIntoDiscard("target", 8)
        assertEquals(2.0,
            FutureDiceForecast.project(d8, listOf(battle, cultivation), rewards = noRewards).expectedSpendingPips -
                FutureDiceForecast.project(d6, listOf(battle, cultivation), rewards = noRewards).expectedSpendingPips,
            1e-12)
    }

    @Test fun `the current Hand is never counted as a current round roll`() {
        val zones = FutureDiceZones(
            supply = listOf(FutureDie("next", 4)),
            hand = listOf(FutureDie("hand", 20))
        )
        val noFuture = FutureDiceForecast.project(zones, emptyList())
        assertEquals(0.0, noFuture.expectedSpendingPips)
        val singleFuture = FutureDiceForecast.project(zones, listOf(battle))
        assertEquals(listOf(4, 20), singleFuture.encounters.map { it.die.sides })
        assertEquals(13.0, singleFuture.expectedSpendingPips)
        assertEquals(1, singleFuture.byDie.single { it.die.id == "hand" }.firstFutureRound)
    }

    @Test fun `future reward equivalents are separate from actual spending pips`() {
        val zones = dice(10)
        val eligible = FutureDiceForecast.project(zones, listOf(cultivation),
            rewards = RollRewardOpportunities(true, true))
        assertEquals(5.5, eligible.expectedSpendingPips)
        assertEquals(6.0, eligible.expectedPipEquivalents)
        assertEquals(0.2, eligible.expectedValue.critterOptionValue, 1e-12)
        assertEquals(0.3, eligible.expectedValue.wispOptionValue, 1e-12)
        assertEquals(5.5, FutureDiceForecast.project(zones, listOf(cultivation), rewards = noRewards)
            .expectedPipEquivalents)
    }

    @Test fun `forecast matches actual PlayerDice order without using RNG`() {
        val factory = SampleDie(Randomizer.create(seed = 9765L))
        val playerDice = PlayerDice(
            supply = listOf(factory.d8, factory.d4, factory.d4),
            hand = listOf(factory.d10),
            discard = listOf(factory.d6, factory.d20)
        )
        val forecast = FutureDiceForecast.project(
            FutureDiceZones(
                supply = playerDice.supply.mapIndexed { i, die -> FutureDie("s$i", die.sides) },
                hand = playerDice.hand.mapIndexed { i, die -> FutureDie("h$i", die.sides) },
                discard = playerDice.discard.mapIndexed { i, die -> FutureDie("d$i", die.sides) }
            ),
            listOf(cultivation, battle)
        )
        // End-of-current-round cleanup takes place before any future Draw.
        playerDice.discardHand()
        val actual = mutableListOf<List<Int>>()
        listOf(4, 3).forEach { count ->
            val round = buildList {
                repeat(count) { playerDice.draw { /* forecast never rolls */ }?.let { add(it.sides) } }
            }
            actual.add(round)
            playerDice.discardHand()
        }
        assertEquals(actual, forecast.rounds.map { it.encounters.map { draw -> draw.die.sides } })
    }

    @Test fun `decision context uses public future phases and immutable die snapshots`() {
        val context = DecisionContext.EMPTY.copy(
            progress = DecisionContext.EMPTY.progress.copy(
                upcomingRoundTypes = listOf(battle, cultivation)
            ),
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    supply = listOf(DieView(index = 0, sides = 6, value = 1)),
                    discard = listOf(DieView(index = 0, sides = 20, value = 20))
                )
            )
        )
        val value = FutureDiceForecast.fromContext(context)
        assertEquals(listOf(battle, cultivation), value.rounds.map { it.roundType })
        assertEquals(listOf(6, 20, 6, 20), value.encounters.map { it.die.sides })
        assertEquals(28.0, value.expectedSpendingPips)
        // The projector has not drawn or changed the source snapshot's dice.
        assertEquals(1, context.self.board.supply.single().value)
        assertEquals(20, context.self.board.discard.single().value)
    }

    @Test fun `extra Draw assumptions require valid future rounds`() {
        assertThrows(IllegalArgumentException::class.java) {
            FutureDiceForecast.project(dice(4), listOf(battle),
                additionalDrawsByFutureRound = mapOf(0 to 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutureDiceForecast.project(dice(4), listOf(battle),
                additionalDrawsByFutureRound = mapOf(2 to 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutureDiceForecast.project(dice(4), listOf(battle),
                additionalDrawsByFutureRound = mapOf(1 to -1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutureDiceZones(supply = listOf(FutureDie("same", 4)), hand = listOf(FutureDie("same", 6)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FutureDiceZones(hand = listOf(FutureDie("x", 8))).withHandUpgradeIntoDiscard("x", 6)
        }
        assertTrue(FutureDiceForecast.project(dice(4), listOf(battle)).byDie.isNotEmpty())
    }
}
