package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.projection.SharedDieEffectOutcome
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction

/**
 * Opt-in A2 opportunity-cost diagnostic. Reports the Main Action ACTUALLY chosen
 * whenever a suppressed Root Four More activation was a legal alternative.
 * This is an observation of one policy decision, not a replay of the official
 * effect in an identical state and not a causal estimate of game-win value.
 *
 * Enable with LEAF_TRACE_SUPPRESSED_PLANT_ALTERNATIVES=Root_05_02.
 * No gameplay mutations, alternative choices, or additional RNG draws occur.
 */
object SuppressedPlantAlternativeTrace {
    private const val TARGET = "Root_05_02"
    private const val ENV = "LEAF_TRACE_SUPPRESSED_PLANT_ALTERNATIVES"

    fun enabled(): Boolean = System.getenv(ENV) == TARGET ||
        System.getProperty("leaf.trace.suppressedPlantAlternatives") == TARGET

    /** Return null unless the suppressed card is legally activatable now. */
    internal fun describe(
        request: ChooseCultivationActionRequest,
        options: List<ScoredChoice<CultivationAction>>,
        chosen: ScoredChoice<CultivationAction>
    ): String? {
        // Supports do not use either of the two Main Actions. Wait for the
        // actual Main choice rather than falsely labeling a Support a substitute.
        if (chosen.choice !is CultivationAction.Main) return null
        val suppressed = options.firstOrNull { candidate ->
            val activation = (candidate.choice as? CultivationAction.Main)?.action as?
                CultivationMainAction.ActivatePlant ?: return@firstOrNull false
            activation.card.card.name == TARGET && request.context.self.board.creature.any { card ->
                card.id == activation.card.id && card.effect == GameEffect.RESEARCH_NO_EFFECT && card.isFaceUp
            }
        } ?: return null
        val draw = options.firstOrNull {
            (it.choice as? CultivationAction.Main)?.action == CultivationMainAction.Draw
        }
        val source = request.context.self.board
        val nextSides = (source.supply.minOfOrNull { it.sides }
            ?: source.discard.minOfOrNull { it.sides })
        val expectedDraw = nextSides?.let { (it + 1) / 2.0 }
        val officialRaisePips = SharedDieEffectOutcome.bestProjectedPips(
            GameEffect.RAISE_DIE_PLUS_4, source.hand
        ) ?: 0
        val competingMain = options.filter { it.choice is CultivationAction.Main }
            .sortedByDescending { it.score.total }
            .joinToString("|") { candidate ->
                "${actionName(request, candidate.choice)}:${candidate.score.total}"
            }
        return listOf(
            "TRACE_SUPPRESSED_MAIN",
            "player=${request.context.self.id.value}",
            "round=${request.context.progress.roundNumber}",
            "remaining=${request.mainActionsRemaining}",
            "selected=${actionName(request, chosen.choice)}",
            "selectedScore=${chosen.score.total}",
            "suppressedScore=${suppressed.score.total}",
            "drawScore=${draw?.score?.total ?: "NA"}",
            "nextDie=${nextSides?.let { "D$it" } ?: "NONE"}",
            "expectedDrawPips=${expectedDraw ?: "NA"}",
            "possibleOfficialRaisePips=$officialRaisePips",
            "allMainScores=$competingMain"
        ).joinToString("\t")
    }

    fun record(
        request: ChooseCultivationActionRequest,
        options: List<ScoredChoice<CultivationAction>>,
        chosen: ScoredChoice<CultivationAction>
    ) {
        if (!enabled()) return
        describe(request, options, chosen)?.let { System.err.println(it) }
    }

    private fun actionName(request: ChooseCultivationActionRequest, choice: CultivationAction): String =
        when (choice) {
            is CultivationAction.Main -> when (val action = choice.action) {
                CultivationMainAction.Draw -> "DRAW"
                is CultivationMainAction.ActivatePlant -> "PLANT:${action.card.card.name}"
                CultivationMainAction.RoundEffect1 -> "ROUND1:${request.roundCard.firstEffect.effect}"
                CultivationMainAction.RoundEffect2 -> "ROUND2:${request.roundCard.secondEffect.effect}"
            }
            is CultivationAction.Support -> "SUPPORT"
            CultivationAction.Done -> "DONE"
        }
}
