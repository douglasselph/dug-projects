package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectBattleDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectCrossPlayerDieSwapChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice

/** Shared-analysis result for one legal Pollen Theft swap. */
data class BattlePollenTheftEvaluation(
    val choice: EffectCrossPlayerDieSwapChoice,
    val analysis: BattleActionAnalysis<EffectCrossPlayerDieSwapChoice>,
    val minimumMeaningfulVpGain: Int
) {
    val passesSpendingGate: Boolean
        get() = analysis.vpImpact.gain >= minimumMeaningfulVpGain
}

/**
 * Evaluates Pollen Theft as the complete same-size cross-player swap it really is.
 * Both affected rows are projected together so benefit in the actor's row and
 * collateral benefit/harm in the opponent die's row share the normal Battle
 * Swing and Strike-VP machinery.
 */
class BattlePollenTheftEvaluator(
    private val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val rowAssessor: BattleRowAssessor = BattleRowAssessor(policy),
    private val actionAnalyzer: BattleActionAnalyzer = BattleActionAnalyzer(
        swingEvaluator = BattleSwingEvaluator(policy)
    )
) {
    operator fun invoke(
        context: DecisionContext,
        choice: EffectCrossPlayerDieSwapChoice
    ): BattlePollenTheftEvaluation? {
        val battle = context.battle ?: return null
        val actorId = context.self.id
        if (choice.ownDie.ownerId != actorId || choice.opponentDie.ownerId == actorId) return null

        val affectedRows = linkedSetOf(choice.ownDie.row, choice.opponentDie.row)
        val before = affectedRows.associateWith { rowAssessor(context, it) }
        if (!before.getValue(choice.ownDie.row).available) return null

        val delta = choice.opponentDie.die.value - choice.ownDie.die.value
        val projected = battle.copy(
            rows = battle.rows.map { rowView ->
                when (rowView.row) {
                    choice.ownDie.row -> rowView.adjustTotal(actorId, delta)
                    else -> rowView
                }.let { adjusted ->
                    if (adjusted.row == choice.opponentDie.row) {
                        adjusted.adjustTotal(choice.opponentDie.ownerId, -delta)
                    } else adjusted
                }
            }
        )

        val rowChanges = affectedRows.mapNotNull { row ->
            val beforeAssessment = before.getValue(row)
            // If the actor has withdrawn from the opponent die's row, that row no
            // longer contributes to the actor's Strike outcome and shared analysis.
            if (!beforeAssessment.available) return@mapNotNull null
            val afterAssessment = rowAssessor(context, row, projected)
            BattleActionRowChange.from(beforeAssessment, afterAssessment)
        }
        if (rowChanges.isEmpty()) return null

        val analysis = actionAnalyzer(
            context = context,
            candidate = BattleActionRealization(
                realization = choice,
                mode = BattleAnalysisMode.DETERMINISTIC,
                rowChanges = rowChanges
            )
        )
        return BattlePollenTheftEvaluation(
            choice = choice,
            analysis = analysis,
            minimumMeaningfulVpGain = policy.battleMinimumMeaningfulVpGain(context)
        )
    }

    fun evaluateAll(
        context: DecisionContext,
        legalChoices: Collection<EffectCrossPlayerDieSwapChoice>
    ): List<BattlePollenTheftEvaluation> =
        legalChoices.mapNotNull { invoke(context, it) }

    /** Mirrors the engine's current public Pollen Theft target shape from DecisionContext. */
    fun legalChoices(context: DecisionContext): List<EffectCrossPlayerDieSwapChoice> {
        val battle = context.battle ?: return emptyList()
        val actorId = context.self.id
        val all = battle.rows.flatMap { row ->
            if (row.closed) return@flatMap emptyList()
            row.players.flatMap playerLoop@ { player ->
                if (player.withdrawn) return@playerLoop emptyList()
                player.dice.map { die ->
                    EffectBattleDieChoice(
                        ownerId = player.playerId,
                        row = row.row,
                        die = EffectDieChoice(die.handIndex, die.sides, die.value)
                    )
                }
            }
        }
        val own = all.filter { it.ownerId == actorId }
        val opponents = all.filter { it.ownerId != actorId }
        return buildList {
            own.forEach { ownDie ->
                opponents.filter { it.die.sides == ownDie.die.sides }.forEach { opponentDie ->
                    add(EffectCrossPlayerDieSwapChoice(ownDie, opponentDie))
                }
            }
        }
    }

    private fun BattleRowView.adjustTotal(playerId: PlayerId, delta: Int): BattleRowView =
        copy(
            players = players.map { player ->
                if (player.playerId != playerId) player
                else player.copy(
                    dieTotal = player.dieTotal + delta,
                    total = player.total + delta
                )
            }
        )
}
