package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseBeeSourceRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBeeSourceChoice
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyBeeSourceAlignmentTest {
    @Test
    fun `Battle Bee source steals from live opponent whose Bee could flip a Strike instead of first Done opponent`() {
        val randomizer = RecordingRandomizer(0)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = battleContext(
            done = setOf(DONE),
            opponentTotals = mapOf(DONE to 4, LIVE to 5),
            opponentBees = mapOf(DONE to 1, LIVE to 1),
            actorTotal = 6
        )

        val chosen = strategy.chooseBeeSource(
            ChooseBeeSourceRequest(
                effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
                legalChoices = listOf(
                    EffectBeeSourceChoice.Opponent(DONE),
                    EffectBeeSourceChoice.Opponent(LIVE),
                    EffectBeeSourceChoice.Grove
                ),
                context = context
            )
        )

        assertEquals(EffectBeeSourceChoice.Opponent(LIVE), chosen)
        assertEquals(emptyList(), randomizer.bounds)
    }

    @Test
    fun `when Battle creates no source distinction existing steal-over-Grove heuristic is preserved`() {
        val context = battleContext(
            done = setOf(DONE),
            opponentTotals = mapOf(DONE to 2),
            opponentBees = mapOf(DONE to 1),
            actorTotal = 10
        )

        val chosen = HumanBaselineEffectStrategy().chooseBeeSource(
            ChooseBeeSourceRequest(
                effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
                legalChoices = listOf(
                    EffectBeeSourceChoice.Grove,
                    EffectBeeSourceChoice.Opponent(DONE)
                ),
                context = context
            )
        )

        assertEquals(EffectBeeSourceChoice.Opponent(DONE), chosen)
    }

    private fun battleContext(
        done: Set<PlayerId>,
        opponentTotals: Map<PlayerId, Int>,
        opponentBees: Map<PlayerId, Int>,
        actorTotal: Int
    ): DecisionContext {
        val opponents = opponentTotals.keys.map { id ->
            OpponentView(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = id,
                    bees = opponentBees.getValue(id),
                    beeValue = 2
                ),
                wispCount = 0
            )
        }
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(id = ACTOR)
            ),
            opponents = opponents,
            battle = BattleView(
                playerOrder = listOf(ACTOR) + opponentTotals.keys,
                donePlayerIds = done,
                rows = listOf(
                    BattleRowView(
                        row = StrikeRow.TOP,
                        closed = false,
                        players = listOf(player(ACTOR, actorTotal)) +
                            opponentTotals.map { (id, total) -> player(id, total) }
                    )
                )
            )
        )
    }

    private fun player(id: PlayerId, total: Int) = BattlePlayerRowView(
        playerId = id,
        row = StrikeRow.TOP,
        dice = emptyList(),
        critters = emptyList(),
        dieTotal = total,
        critterTotal = 0,
        total = total,
        withdrawn = false
    )

    private class RecordingRandomizer(private val result: Int) : StrategyRandomizer {
        val bounds = mutableListOf<Int>()

        override fun nextInt(until: Int): Int {
            bounds += until
            return result
        }
    }

    private companion object {
        val ACTOR = PlayerId(0)
        val DONE = PlayerId(1)
        val LIVE = PlayerId(2)
    }
}
