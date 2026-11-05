package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleFirstMainPriority
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleMainPriority
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard

/**
 * A deterministic *pre-action* preview using the same tactical Main scoring
 * functions as v37. No die is rolled and the observation is never mutated.
 *
 * Row attribution is a sensitivity probe: the scorer is evaluated with just
 * one still-open Strike versus no open Strikes. This is NOT a prediction of the
 * future board after an opponent's action, nor an exact VP estimate. An effect
 * whose scorer does not model row impact retains its global score, but gets no
 * fabricated row benefit. Its contribution is visible as unattributed.
 */
data class BaselineMainActionForecast(
    val action: BattleMainAction,
    val fullScore: Int,
    val noOpenRowScore: Int,
    val rowOpportunity: Map<StrikeRow, Double>
) {
    val bestRow: StrikeRow? get() = rowOpportunity.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key

    fun trace(): String = buildString {
        append(action.toString().replace(',', ';'))
        append(":full=").append(fullScore)
        append(":nonRow=").append(noOpenRowScore)
        append(":rows=").append(rowOpportunity.entries.joinToString("/") { "${it.key}:${it.value}" })
    }
}

/** Injectable scorer enables pure scheduling tests without creating a game. */
fun interface BaselineMainScore {
    fun score(context: DecisionContext, card: RoundCard, action: BattleMainAction, mainsRemaining: Int): Int
}

/** Calls the genuine v37 First/Final Main consequence-aware scorers. */
class V37BaselineMainScore : BaselineMainScore {
    private val cardScorers = HumanBaselineCardScorerRegistry()
    private val firstMain = BattleFirstMainPriority(cardScorers)
    private val influences = BaselineInfluenceRegistry(cardScorers)

    override fun score(
        context: DecisionContext,
        card: RoundCard,
        action: BattleMainAction,
        mainsRemaining: Int
    ): Int {
        val raw = if (mainsRemaining == 2) firstMain(context, card, action)
            else BattleMainPriority.finalMainScore(context, card, action, cardScorers)
        val effect = when (action) {
            BattleMainAction.RoundEffect1 -> card.firstEffect.effect
            BattleMainAction.RoundEffect2 -> card.secondEffect.effect
            else -> null
        }
        val tags = if (effect == GameEffect.GAIN_TWO_WORMS) setOf(DecisionTag.ACQUIRE_WORM)
            else emptySet()
        // The canonical chooser scores owned-card influences on top of these
        // priorities. Reproduce that scoring here WITHOUT tie-breaking RNG.
        return influences.score(context, DecisionCandidate(action, raw, tags)).score.total
    }
}

class BaselineMainActionForecaster(
    private val scorer: BaselineMainScore = V37BaselineMainScore()
) {
    fun forecast(
        context: DecisionContext,
        card: RoundCard,
        legalActions: List<BattleMainAction>,
        mainsRemaining: Int,
        openRows: List<StrikeRow>
    ): List<BaselineMainActionForecast> {
        require(mainsRemaining in 1..2)
        val battle = requireNotNull(context.battle) { "Battle forecast requires a Battle observation" }
        val open = openRows.toSet()
        check(open.all { row -> battle.rows.any { it.row == row && !it.closed } }) {
            "Cannot project an already closed Strike"
        }
        // Do not manufacture future legal actions: score only choices supplied
        // by the real canonical Main-action legality generator.
        fun projected(available: Set<StrikeRow>): DecisionContext = context.copy(
            battle = battle.copy(rows = battle.rows.map { view ->
                if (view.row in open && view.row !in available) view.copy(closed = true) else view
            })
        )
        val none = projected(emptySet())
        val oneRow = openRows.associateWith { projected(setOf(it)) }
        return legalActions.distinct().map { action ->
            val full = scorer.score(context, card, action, mainsRemaining)
            val noRows = scorer.score(none, card, action, mainsRemaining)
            val values = oneRow.mapValues { (_, single) ->
                (scorer.score(single, card, action, mainsRemaining) - noRows)
                    .coerceAtLeast(0).toDouble()
            }
            BaselineMainActionForecast(action, full, noRows, values)
        }
    }
}
