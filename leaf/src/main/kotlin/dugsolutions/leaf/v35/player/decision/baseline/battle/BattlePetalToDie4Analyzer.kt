package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Projects Petal To Die 4's two qualitative branches into their complete
 * immediate Battle consequences.
 *
 * GainD4 is deterministic (+ a D4 showing 4), but its real row is still chosen
 * later by the normal Battle placement decision. Analysis therefore uses the
 * best currently legal placement without committing that future decision.
 *
 * TrashD4AndRaiseAll removes the named D4 from its exact current Strike row,
 * then applies the capped +4 Raise to every remaining actor die. The resulting
 * signed row deltas are analyzed together so a large raw pip gain cannot hide
 * the tactical cost of abandoning the trashed die's row.
 */
class BattlePetalToDie4Analyzer(
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val diePlacementAnalyzer: BattleDiePlacementAnalyzer =
        BattleDiePlacementAnalyzer(policy),
    private val ownTotalChangeAnalyzer: BattleOwnTotalChangeAnalyzer =
        BattleOwnTotalChangeAnalyzer(policy)
) {
    fun legalChoices(context: DecisionContext): List<PetalToDie4Choice> =
        buildList {
            if (
                (context.grove.graftBed[DieSides.D4] ?: 0) > 0 &&
                diePlacementAnalyzer(
                    context = context,
                    dieValue = GAIN_D4_VALUE,
                    mode = BattleAnalysisMode.DETERMINISTIC
                ).isNotEmpty()
            ) {
                add(PetalToDie4Choice.GainD4)
            }

            locatedDice(context)
                .filter { it.die.sides == DieSides.D4.value }
                .forEach { located ->
                    add(
                        PetalToDie4Choice.TrashD4AndRaiseAll(
                            EffectDieChoice(
                                index = located.die.handIndex,
                                sides = located.die.sides,
                                value = located.die.value
                            )
                        )
                    )
                }
        }

    fun evaluateAll(
        context: DecisionContext,
        choices: Collection<PetalToDie4Choice> = legalChoices(context)
    ): List<BattleActionAnalysis<PetalToDie4Choice>> =
        choices.mapNotNull { choice -> invoke(context, choice) }

    operator fun invoke(
        context: DecisionContext,
        choice: PetalToDie4Choice
    ): BattleActionAnalysis<PetalToDie4Choice>? =
        when (choice) {
            PetalToDie4Choice.GainD4 -> analyzeGain(context)
            is PetalToDie4Choice.TrashD4AndRaiseAll -> analyzeTrashAndRaise(context, choice)
        }

    private fun analyzeGain(
        context: DecisionContext
    ): BattleActionAnalysis<PetalToDie4Choice>? {
        val bestPlacement = diePlacementAnalyzer(
            context = context,
            dieValue = GAIN_D4_VALUE,
            mode = BattleAnalysisMode.DETERMINISTIC
        ).maxWithOrNull(
            compareBy<BattleActionAnalysis<StrikeRow>> { it.tacticalValue }
                .thenBy { it.improvementStepCount }
        ) ?: return null

        return BattleActionAnalysis(
            realization = PetalToDie4Choice.GainD4,
            mode = bestPlacement.mode,
            swing = bestPlacement.swing,
            vpImpact = bestPlacement.vpImpact,
            improvementStepCount = bestPlacement.improvementStepCount
        )
    }

    private fun analyzeTrashAndRaise(
        context: DecisionContext,
        choice: PetalToDie4Choice.TrashD4AndRaiseAll
    ): BattleActionAnalysis<PetalToDie4Choice>? {
        val dice = locatedDice(context)
        val trashed = dice.firstOrNull { it.die.handIndex == choice.die.index }
            ?: return null
        if (trashed.die.sides != DieSides.D4.value) return null

        val changesByRow = mutableMapOf<StrikeRow, Double>()
        dice.forEach { located ->
            val delta = if (located.die.handIndex == choice.die.index) {
                -located.die.value.toDouble()
            } else {
                DieValueHeuristics.actualRaiseGain(
                    sides = located.die.sides,
                    value = located.die.value,
                    amount = RAISE_AMOUNT
                ).toDouble()
            }
            changesByRow[located.row] = (changesByRow[located.row] ?: 0.0) + delta
        }

        return ownTotalChangeAnalyzer(
            context = context,
            realization = choice,
            changesByRow = changesByRow,
            mode = BattleAnalysisMode.DETERMINISTIC
        )
    }

    private fun locatedDice(context: DecisionContext): List<LocatedBattleDie> =
        context.battle?.rows.orEmpty().flatMap { row ->
            row.forPlayer(context.self.id)?.dice.orEmpty().map { die ->
                LocatedBattleDie(row.row, die)
            }
        }

    private data class LocatedBattleDie(
        val row: StrikeRow,
        val die: BattleDieView
    )

    private companion object {
        const val GAIN_D4_VALUE = 4.0
        const val RAISE_AMOUNT = 4
    }
}
