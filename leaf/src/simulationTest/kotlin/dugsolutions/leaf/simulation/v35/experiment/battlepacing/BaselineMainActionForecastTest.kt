package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Pure deterministic probes of planning, not substitutes for full-engine tests. */
class BaselineMainActionForecastTest {
    private val high = StrikeRow.TOP
    private val mid = StrikeRow.MIDDLE
    private val low = StrikeRow.BOTTOM
    private val all = StrikeRow.entries
    private val card = RoundCard(1, "TEST_BATTLE", RoundCardType.BATTLE,
        RoundCardEffect("Bloom", "", "", "", null, GameEffect.GAIN_ONE_VP),
        RoundCardEffect("Burrow", "", "", "", null, GameEffect.GAIN_TWO_WORMS), "")
    private val plant = BattleMainAction.ActivatePlant(CreatureCard(
        CreatureCardId(9), PlantCard(1, "TEST_PLANT", "Test Plant", PlantType.ROOT,
            5, null, "", "", "", "", "", "", "", GameEffect.RAISE_DIE_PLUS_4),
        CreatureSide.LEFT, CreaturePosition(-1, 0)
    ))
    private val legal = listOf(BattleMainAction.Draw, plant,
        BattleMainAction.RoundEffect1, BattleMainAction.RoundEffect2)

    private fun context(open: List<StrikeRow> = all): DecisionContext = DecisionContext.EMPTY.copy(
        battle = BattleView(emptyList(), rows = all.map { BattleRowView(it, it !in open, emptyList()) })
    )

    @Test fun `a Plant with greater high row consequence beats Draw in timing allocation`() {
        val scorer = BaselineMainScore { ctx, _, action, _ ->
            val open = ctx.battle!!.rows.filterNot { it.closed }.map { it.row }
            when (action) {
                plant -> 10 + if (high in open) 510 else 0
                BattleMainAction.Draw -> 10 + if (low in open) 410 else 0
                BattleMainAction.RoundEffect1 -> 90
                else -> 80
            }
        }
        val planner = EngagementMainTimingPlanner(forecaster = BaselineMainActionForecaster(scorer))
        val state = context()
        val decision = planner.plan(state, high, 2, card, legal)
        assertTrue(decision.spendNow)
        assertEquals(listOf(high, low), decision.plannedRows)
        assertEquals(510.0, decision.projectedDrawScores.getValue(high))
        assertEquals(410.0, decision.projectedDrawScores.getValue(low))
        assertEquals(4, decision.actionPreviews.size)
        assertEquals(520, decision.actionPreviews.first { it.action == plant }.fullScore)
        assertEquals(state, context()) // counterfactuals never modify the provided snapshot
    }

    @Test fun `a future Plant target can defer high and reassess with high closed`() {
        var midImpact = 120
        val scorer = BaselineMainScore { ctx, _, action, _ ->
            val open = ctx.battle!!.rows.filterNot { it.closed }.map { it.row }
            when (action) {
                plant -> 20 + if (mid in open) midImpact else 0
                BattleMainAction.Draw -> 20 + if (low in open) 300 else 0
                else -> 30
            }
        }
        val planner = EngagementMainTimingPlanner(forecaster = BaselineMainActionForecaster(scorer))
        assertFalse(planner.plan(context(), high, 2, card, legal).spendNow)
        // After High actually resolves, a Wound/changed state may change the
        // next preview. The planner recomputes, not reuses the High decision.
        midImpact = 600
        val second = planner.plan(context(listOf(mid, low)), mid, 2, card, legal)
        assertTrue(second.spendNow) // force two remaining normal tokens
        assertEquals(listOf(mid, low), second.openRows)
        assertEquals(600.0, second.projectedDrawScores.getValue(mid))
        assertTrue(planner.plan(context(listOf(low)), low, 1, card, legal).spendNow)
    }

    @Test fun `Round effect attribution is distinct from actual non row value`() {
        val scorer = BaselineMainScore { ctx, _, action, _ ->
            val open = ctx.battle!!.rows.filterNot { it.closed }.map { it.row }
            when (action) {
                BattleMainAction.RoundEffect1 -> 800  // Immediate VP, not a row change
                BattleMainAction.RoundEffect2 -> 180  // Resource gain, not a row change
                BattleMainAction.Draw -> 10 + if (low in open) 25 else 0
                else -> 10
            }
        }
        val planner = EngagementMainTimingPlanner(forecaster = BaselineMainActionForecaster(scorer))
        val result = planner.plan(context(), high, 2, card, legal)
        val vp = result.actionPreviews.first { it.action == BattleMainAction.RoundEffect1 }
        assertEquals(800, vp.fullScore)
        assertEquals(800, vp.noOpenRowScore)
        assertTrue(vp.rowOpportunity.values.all { it == 0.0 })
        assertEquals(listOf(low, high), result.plannedRows)
        // Bloom's row-independent VP is available now; use High and keep Low
        // for the only positive row-specific opportunity. Deferral would
        // contradict the selected Main-action Engagements above.
        assertTrue(result.spendNow)
        assertEquals(EngagementMainTimingDecision.Reason.ACTION_SPECIFIC_PROJECTION, result.reason)
    }

    @Test fun `unattributed legal effects do not generate fictitious Strike value`() {
        val scorer = BaselineMainScore { _, _, _, _ -> 300 }
        val result = EngagementMainTimingPlanner(
            forecaster = BaselineMainActionForecaster(scorer)
        ).plan(context(), high, 2, card, legal)
        assertEquals(EngagementMainTimingDecision.Reason.UNATTRIBUTED_ACTIONS, result.reason)
        assertTrue(result.spendNow) // explicit earlier-window fallback
        assertTrue(result.projectedDrawScores.values.all { it == 0.0 })
    }

    @Test fun `forecaster only scores supplied legal choices and never changes closed rows`() {
        val requests = mutableListOf<List<StrikeRow>>()
        val forecaster = BaselineMainActionForecaster(BaselineMainScore { ctx, _, _, _ ->
            requests += ctx.battle!!.rows.filterNot { it.closed }.map { it.row }
            10
        })
        val original = context(listOf(mid, low))
        val forecasts = forecaster.forecast(original, card, listOf(BattleMainAction.RoundEffect2),
            1, listOf(mid, low))
        assertEquals(1, forecasts.size)
        assertEquals(4, requests.size) // full, none, middle-only, low-only
        assertTrue(requests.none { high in it })
        assertFalse(original.battle!!.rows.first { it.row == mid }.closed)
        assertTrue(original.battle!!.rows.first { it.row == high }.closed)
    }
}
