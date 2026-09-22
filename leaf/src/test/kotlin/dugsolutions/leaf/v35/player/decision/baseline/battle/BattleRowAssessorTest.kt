package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BattleRowAssessorTest {
    private val actor = PlayerId(0)
    private val opponentA = PlayerId(1)
    private val opponentB = PlayerId(2)

    @Nested
    inner class `Strike reality` {
        @Test
        fun `shared high score is a winning Strike when a lower participant exists`() {
            val assessment = assess(
                actorTotal = 10,
                opponents = listOf(opponentA to 10, opponentB to 7)
            )

            assertTrue(assessment.currentlyWinning)
            assertFalse(assessment.everyoneTied)
            assertEquals(listOf(actor, opponentA), assessment.winnerIds)
            assertEquals(10, assessment.scoreBenchmarkTotal)
            assertEquals(0, assessment.scoreMargin)
        }

        @Test
        fun `all-player high tie has no winner`() {
            val assessment = assess(
                actorTotal = 10,
                opponents = listOf(opponentA to 10, opponentB to 10)
            )

            assertFalse(assessment.currentlyWinning)
            assertTrue(assessment.everyoneTied)
            assertEquals(emptyList(), assessment.winnerIds)
            assertEquals(0, assessment.scoreMargin)
            assertFalse(assessment.woundRisk)
        }

        @Test
        fun `withdrawn opponent is excluded from Strike benchmark and winner state`() {
            val assessment = assess(
                actorTotal = 8,
                opponents = listOf(opponentA to 20, opponentB to 7),
                withdrawn = setOf(opponentA)
            )

            assertEquals(listOf(actor, opponentB), assessment.participatingPlayerIds)
            assertEquals(7, assessment.scoreBenchmarkTotal)
            assertEquals(1, assessment.scoreMargin)
            assertTrue(assessment.currentlyWinning)
        }

        @Test
        fun `closed row is unavailable`() {
            val assessment = assess(
                actorTotal = 8,
                opponents = listOf(opponentA to 7),
                closed = true
            )

            assertFalse(assessment.available)
            assertEquals(8, assessment.ownTotal)
            assertNull(assessment.scoreBenchmarkTotal)
            assertFalse(assessment.currentlyWinning)
        }

        @Test
        fun `actor withdrawal makes row unavailable`() {
            val assessment = assess(
                actorTotal = 8,
                opponents = listOf(opponentA to 7),
                withdrawn = setOf(actor)
            )

            assertFalse(assessment.available)
            assertEquals(8, assessment.ownTotal)
        }

        @Test
        fun `wound risk follows actual Strike winner state`() {
            val wound = assess(
                actorTotal = 4,
                opponents = listOf(opponentA to 10, opponentB to 8)
            )
            val noWound = assess(
                actorTotal = 6,
                opponents = listOf(opponentA to 10, opponentB to 8)
            )

            assertTrue(wound.woundRisk)
            assertFalse(noWound.woundRisk)
        }
    }

    @Nested
    inner class `Benchmark versus live threat` {
        @Test
        fun `Done leader remains Score Benchmark while active lower player becomes Live Threat`() {
            val assessment = assess(
                actorTotal = 8,
                opponents = listOf(opponentA to 10, opponentB to 6),
                done = setOf(opponentA)
            )

            assertEquals(listOf(opponentA), assessment.scoreBenchmarkPlayerIds)
            assertEquals(10, assessment.scoreBenchmarkTotal)
            assertEquals(-2, assessment.scoreMargin)
            assertEquals(listOf(opponentB), assessment.liveThreatPlayerIds)
            assertEquals(6, assessment.liveThreatTotal)
            assertEquals(2, assessment.liveThreatMargin)
            assertFalse(assessment.allOpponentsDone)
        }

        @Test
        fun `all tied benchmark and live threat players are preserved`() {
            val assessment = assess(
                actorTotal = 7,
                opponents = listOf(opponentA to 12, opponentB to 12)
            )

            assertEquals(listOf(opponentA, opponentB), assessment.scoreBenchmarkPlayerIds)
            assertEquals(listOf(opponentA, opponentB), assessment.liveThreatPlayerIds)
            assertEquals(-5, assessment.scoreMargin)
            assertEquals(-5, assessment.liveThreatMargin)
        }

        @Test
        fun `all Done opponents leave fixed benchmark but no Live Threat`() {
            val assessment = assess(
                actorTotal = 8,
                opponents = listOf(opponentA to 10, opponentB to 6),
                done = setOf(opponentA, opponentB)
            )

            assertEquals(10, assessment.scoreBenchmarkTotal)
            assertEquals(-2, assessment.scoreMargin)
            assertNull(assessment.liveThreatTotal)
            assertNull(assessment.liveThreatMargin)
            assertTrue(assessment.allOpponentsDone)
            assertFalse(assessment.securedForNow)
        }
    }

    @Nested
    inner class `Human Baseline attention facts` {
        @Test
        fun `winning row is secured at configured Live Threat lead`() {
            val assessment = assess(
                actorTotal = 15,
                opponents = listOf(opponentA to 5)
            )

            assertTrue(assessment.currentlyWinning)
            assertEquals(10, assessment.liveThreatMargin)
            assertTrue(assessment.securedForNow)
        }

        @Test
        fun `Done closest opponent does not hide lower active Live Threat`() {
            val assessment = assess(
                actorTotal = 12,
                opponents = listOf(opponentA to 11, opponentB to 2),
                done = setOf(opponentA)
            )

            assertEquals(1, assessment.scoreMargin)
            assertEquals(10, assessment.liveThreatMargin)
            assertTrue(assessment.securedForNow)
        }

        @Test
        fun `shared win against Done tied opponent is secured when nobody can respond`() {
            val assessment = assess(
                actorTotal = 10,
                opponents = listOf(opponentA to 10, opponentB to 7),
                done = setOf(opponentA, opponentB)
            )

            assertTrue(assessment.currentlyWinning)
            assertEquals(0, assessment.scoreMargin)
            assertNull(assessment.liveThreatTotal)
            assertTrue(assessment.securedForNow)
        }

        @Test
        fun `large Score Benchmark deficit is potentially hopeless but not unavailable`() {
            val assessment = assess(
                actorTotal = 4,
                opponents = listOf(opponentA to 14, opponentB to 8)
            )

            assertTrue(assessment.available)
            assertFalse(assessment.currentlyWinning)
            assertEquals(-10, assessment.scoreMargin)
            assertTrue(assessment.potentiallyHopeless)
        }

        @Test
        fun `projected Battle view is assessed without mutating the original context`() {
            val originalRow = BattleRowView(
                row = StrikeRow.TOP,
                closed = false,
                players = listOf(
                    rowView(actor, 5, false),
                    rowView(opponentA, 5, false)
                )
            )
            val projectedRow = BattleRowView(
                row = StrikeRow.TOP,
                closed = false,
                players = listOf(
                    rowView(actor, 15, false),
                    rowView(opponentA, 5, false)
                )
            )
            val context = DecisionContext.EMPTY.copy(
                self = DecisionContext.EMPTY.self.copy(
                    board = DecisionContext.EMPTY.self.board.copy(id = actor)
                ),
                battle = BattleView(
                    playerOrder = listOf(actor, opponentA),
                    rows = listOf(originalRow)
                )
            )
            val projected = BattleView(
                playerOrder = listOf(actor, opponentA),
                rows = listOf(projectedRow)
            )

            val assessment = BattleRowAssessor()(context, StrikeRow.TOP, projected)

            assertEquals(15, assessment.ownTotal)
            assertEquals(10, assessment.liveThreatMargin)
            assertTrue(assessment.securedForNow)
            assertEquals(5, context.battle?.row(StrikeRow.TOP)?.forPlayer(actor)?.total)
        }

        @Test
        fun `policy overrides secured and hopeless thresholds`() {
            val policy = HumanBaselinePolicy(
                battleSecuredLeadValue = 6,
                battleHopelessDeficitValue = 6
            )
            val assessor = BattleRowAssessor(policy)
            val secured = assess(
                assessor = assessor,
                actorTotal = 12,
                opponents = listOf(opponentA to 6)
            )
            val hopeless = assess(
                assessor = assessor,
                actorTotal = 4,
                opponents = listOf(opponentA to 10)
            )

            assertTrue(secured.securedForNow)
            assertTrue(hopeless.potentiallyHopeless)
        }
    }

    private fun assess(
        actorTotal: Int,
        opponents: List<Pair<PlayerId, Int>>,
        done: Set<PlayerId> = emptySet(),
        withdrawn: Set<PlayerId> = emptySet(),
        closed: Boolean = false,
        assessor: BattleRowAssessor = BattleRowAssessor()
    ): BattleRowAssessment {
        val playerOrder = listOf(actor) + opponents.map { it.first }
        val row = BattleRowView(
            row = StrikeRow.TOP,
            closed = closed,
            players = listOf(rowView(actor, actorTotal, actor in withdrawn)) +
                opponents.map { (id, total) -> rowView(id, total, id in withdrawn) }
        )
        val context = DecisionContext.EMPTY.copy(
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = actor)
            ),
            battle = BattleView(
                playerOrder = playerOrder,
                donePlayerIds = done,
                rows = listOf(row)
            )
        )
        return assessor(context, StrikeRow.TOP)
    }

    private fun rowView(
        playerId: PlayerId,
        total: Int,
        withdrawn: Boolean
    ): BattlePlayerRowView =
        BattlePlayerRowView(
            playerId = playerId,
            row = StrikeRow.TOP,
            dice = emptyList(),
            critters = emptyList(),
            dieTotal = total,
            critterTotal = 0,
            total = total,
            withdrawn = withdrawn
        )
}
