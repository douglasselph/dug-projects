package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.effect.EffectBattleDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectCrossPlayerDieSwapChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BattleSpecialEffectEvaluatorsTest {
    private val p1 = PlayerId(1)
    private val p2 = PlayerId(2)
    private val p3 = PlayerId(3)

    @Nested
    inner class `Pollen Theft` {
        @Test
        fun `swap evaluates both affected rows including collateral`() {
            val context = context(
                rows = listOf(
                    row(
                        StrikeRow.TOP,
                        player(p1, 2, die(0, 6, 2)),
                        player(p2, 3, die(0, 4, 3))
                    ),
                    row(
                        StrikeRow.MIDDLE,
                        player(p1, 10, die(1, 10, 10)),
                        player(p2, 6, die(1, 6, 6))
                    )
                )
            )
            val choice = swap(
                own = EffectBattleDieChoice(p1, StrikeRow.TOP, EffectDieChoice(0, 6, 2)),
                opponent = EffectBattleDieChoice(p2, StrikeRow.MIDDLE, EffectDieChoice(1, 6, 6))
            )

            val result = assertNotNull(BattlePollenTheftEvaluator()(context, choice))

            assertEquals(setOf(StrikeRow.TOP, StrikeRow.MIDDLE), result.analysis.swing.rowSwings.map { it.row }.toSet())
            assertTrue(result.analysis.tacticalValue > 0.0)
            assertTrue(result.analysis.vpImpact.gain >= 2)
            assertTrue(result.passesSpendingGate)
        }

        @Test
        fun `meaningful VP threshold accepts exactly two and rejects the same swap when policy requires three`() {
            val context = context(
                rows = listOf(
                    row(
                        StrikeRow.TOP,
                        player(p1, 4, die(0, 6, 4)),
                        player(p2, 5, die(0, 6, 5))
                    )
                )
            )
            val choice = swap(
                EffectBattleDieChoice(p1, StrikeRow.TOP, EffectDieChoice(0, 6, 4)),
                EffectBattleDieChoice(p2, StrikeRow.TOP, EffectDieChoice(0, 6, 5))
            )

            val normal = assertNotNull(BattlePollenTheftEvaluator()(context, choice))
            assertEquals(2, normal.analysis.vpImpact.gain)
            assertTrue(normal.passesSpendingGate)

            val strict = object : HumanBaselinePolicy() {
                override fun battleMinimumMeaningfulVpGain(context: DecisionContext) = 3
            }
            assertFalse(assertNotNull(BattlePollenTheftEvaluator(strict)(context, choice)).passesSpendingGate)
        }

        @Test
        fun `generated legal swaps exclude withdrawn opponent dice`() {
            val context = context(
                rows = listOf(
                    row(
                        StrikeRow.TOP,
                        player(p1, 3, die(0, 6, 3)),
                        player(p2, 6, die(0, 6, 6), withdrawn = true),
                        player(p3, 5, die(0, 6, 5))
                    )
                ),
                opponentIds = listOf(p2, p3)
            )

            val choices = BattlePollenTheftEvaluator().legalChoices(context)

            assertEquals(listOf(p3), choices.map { it.opponentDie.ownerId }.distinct())
        }
    }

    @Nested
    inner class `Wisp Resolve` {
        @Test
        fun `exact five lead and three hidden Wisps meet both thresholds`() {
            val context = context(
                rows = listOf(row(StrikeRow.TOP, player(p1, 10), player(p2, 5))),
                opponentWisps = mapOf(p2 to 3)
            )

            val result = assertNotNull(BattleImmediateStrikeResolveEvaluator()(context, StrikeRow.TOP))

            assertTrue(result.leadOverEveryOpponent)
            assertEquals(3, result.activeContenderSupport.getValue(p2).wispMoves)
            assertTrue(result.passesPolicy)
        }

        @Test
        fun `lead of four fails the immediate resolve policy`() {
            val context = context(
                rows = listOf(row(StrikeRow.TOP, player(p1, 9), player(p2, 5))),
                opponentWisps = mapOf(p2 to 3)
            )

            assertFalse(assertNotNull(BattleImmediateStrikeResolveEvaluator()(context, StrikeRow.TOP)).passesPolicy)
        }

        @Test
        fun `done opponent is excluded from support gate while active contender is still required`() {
            val context = context(
                rows = listOf(row(StrikeRow.TOP, player(p1, 12), player(p2, 6), player(p3, 7))),
                opponentIds = listOf(p2, p3),
                opponentWisps = mapOf(p2 to 0, p3 to 3),
                done = setOf(p2)
            )

            val result = assertNotNull(BattleImmediateStrikeResolveEvaluator()(context, StrikeRow.TOP))

            assertEquals(setOf(p3), result.activeContenderSupport.keys)
            assertTrue(result.passesPolicy)
        }

        @Test
        fun `withdrawn opponent is not a participating lead or support contender`() {
            val context = context(
                rows = listOf(row(StrikeRow.TOP, player(p1, 10), player(p2, 20, withdrawn = true), player(p3, 5))),
                opponentIds = listOf(p2, p3),
                opponentWisps = mapOf(p2 to 0, p3 to 3)
            )

            val result = assertNotNull(BattleImmediateStrikeResolveEvaluator()(context, StrikeRow.TOP))

            assertEquals(setOf(p3), result.activeContenderSupport.keys)
            assertTrue(result.leadOverEveryOpponent)
            assertTrue(result.passesPolicy)
        }
    }

    @Nested
    inner class `Overgrowth` {
        @Test
        fun `larger legal resulting die size wins before Battle value`() {
            val context = context(
                rows = listOf(
                    row(
                        StrikeRow.TOP,
                        player(p1, 1, die(0, 4, 1), die(1, 6, 1)),
                        player(p2, 20)
                    )
                ),
                graftBed = mapOf(DieSides.D6 to 1, DieSides.D8 to 1, DieSides.D10 to 1)
            )
            val evaluations = BattleTwoStepUpgradeEvaluator().evaluateAll(
                context,
                listOf(EffectDieChoice(0, 4, 1), EffectDieChoice(1, 6, 1))
            )

            assertEquals(DieSides.D8, evaluations.first { it.choice.index == 0 }.resultingSides)
            assertEquals(DieSides.D10, evaluations.first { it.choice.index == 1 }.resultingSides)
        }

        @Test
        fun `equivalent resulting sizes retain expected Battle analysis for tie break`() {
            val context = context(
                rows = listOf(
                    row(
                        StrikeRow.TOP,
                        player(p1, 4, die(0, 4, 4)),
                        player(p2, 8)
                    ),
                    row(
                        StrikeRow.MIDDLE,
                        player(p1, 1, die(1, 6, 1)),
                        player(p2, 8)
                    )
                ),
                graftBed = mapOf(DieSides.D8 to 1, DieSides.D12 to 1)
            )
            val evaluations = BattleTwoStepUpgradeEvaluator().evaluateAll(
                context,
                listOf(EffectDieChoice(0, 4, 4), EffectDieChoice(1, 6, 1))
            )

            assertTrue(evaluations.all { it.resultingSides == DieSides.D12 })
            val top = evaluations.first { it.choice.index == 0 }.battleAnalysis
            val middle = evaluations.first { it.choice.index == 1 }.battleAnalysis
            assertNotNull(top)
            assertNotNull(middle)
            assertTrue(middle.tacticalValue > top.tacticalValue)
            assertEquals(BattleAnalysisMode.EXPECTED, middle.mode)
        }
    }

    private fun context(
        rows: List<BattleRowView>,
        opponentIds: List<PlayerId> = listOf(p2),
        opponentWisps: Map<PlayerId, Int> = emptyMap(),
        done: Set<PlayerId> = emptySet(),
        graftBed: Map<DieSides, Int> = emptyMap()
    ): DecisionContext {
        val allSelfDice = rows.flatMap { it.forPlayer(p1)?.dice.orEmpty() }
            .associateBy { it.handIndex }
            .values.sortedBy { it.handIndex }
            .map { DieView(it.handIndex, it.sides, it.value) }
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = p1, hand = allSelfDice)
            ),
            opponents = opponentIds.map { id ->
                OpponentView(
                    board = DecisionContext.EMPTY.self.board.copy(id = id),
                    wispCount = opponentWisps[id] ?: 0
                )
            },
            grove = DecisionContext.EMPTY.grove.copy(graftBed = graftBed),
            battle = BattleView(
                playerOrder = listOf(p1) + opponentIds,
                donePlayerIds = done,
                rows = rows
            )
        )
    }

    private fun row(row: StrikeRow, vararg players: BattlePlayerRowView) =
        BattleRowView(row = row, closed = false, players = players.toList())

    private fun player(
        id: PlayerId,
        total: Int,
        vararg dice: BattleDieView,
        withdrawn: Boolean = false
    ) = BattlePlayerRowView(
        playerId = id,
        row = StrikeRow.TOP,
        dice = dice.toList(),
        critters = emptyList(),
        dieTotal = total,
        critterTotal = 0,
        total = total,
        withdrawn = withdrawn
    )

    private fun die(index: Int, sides: Int, value: Int) = BattleDieView(index, sides, value)

    private fun swap(
        own: EffectBattleDieChoice,
        opponent: EffectBattleDieChoice
    ) = EffectCrossPlayerDieSwapChoice(own, opponent)
}
