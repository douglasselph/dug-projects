package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import kotlin.math.roundToInt

/** One face-down Plant's best visible immediate Final-Main opportunity. */
data class BattleEnabledPlantOpportunity(
    val card: CreatureCardView,
    val priority: PriorityScore,
    val tacticalAnalysis: BattleActionAnalysis<CreatureCardId>?
) {
    val improvementStepCount: Int
        get() = tacticalAnalysis?.improvementStepCount ?: 0
}

/**
 * Values the one immediate Plant activation unlocked by refresh Support.
 *
 * Intrinsic card knowledge stays in the card scorer. For current direct own-die
 * effects, this helper adds the best deterministic/expected Battle realization
 * through shared action analysis. It deliberately does not execute the effect,
 * consume RNG, or commit the downstream target; B15 aligns those later target
 * decisions with the same reasoning.
 */
class BattleEnabledPlantAnalyzer(
    private val cardScorers: HumanBaselineCardScorerRegistry =
        HumanBaselineCardScorerRegistry(),
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val ownTotalChangeAnalyzer: BattleOwnTotalChangeAnalyzer =
        BattleOwnTotalChangeAnalyzer(policy),
    private val ownDieCollateralAnalyzer: BattleOwnDieCollateralAnalyzer =
        BattleOwnDieCollateralAnalyzer(policy)
) {
    operator fun invoke(
        context: DecisionContext,
        card: CreatureCardView
    ): BattleEnabledPlantOpportunity {
        val intrinsic = cardScorers.forPlant(card).playScore(
            context = context,
            phase = CardPhase.BATTLE,
            cardName = card.name
        )
        val tactical = bestTacticalAnalysis(context, card)
        val priority = tactical?.tacticalValue?.roundToInt()?.takeIf { it != 0 }
            ?.let { adjustment ->
                intrinsic.adjusted(
                    adjustment,
                    "Best immediate Battle use unlocked by ${card.title}"
                )
            }
            ?: intrinsic
        return BattleEnabledPlantOpportunity(card, priority, tactical)
    }

    private fun bestTacticalAnalysis(
        context: DecisionContext,
        card: CreatureCardView
    ): BattleActionAnalysis<CreatureCardId>? {
        val dice = locatedDice(context)
        if (dice.isEmpty()) return null

        val candidates = when (card.effect) {
            GameEffect.DOUBLE_ONE_DIE -> singleDieCandidates(context, card, dice) { die ->
                minOf(die.sides, die.value * 2) - die.value
            }

            GameEffect.RAISE_DIE_PLUS_4 -> raiseCandidates(context, card, dice, 4)
            GameEffect.RAISE_DIE_PLUS_3 -> raiseCandidates(context, card, dice, 3)
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                collateralCandidates(context, card, dice)

            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE ->
                raiseCandidates(context, card, dice, 1)

            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER ->
                raiseCandidates(
                    context,
                    card,
                    dice,
                    context.self.board.creature.count {
                        it.type == PlantType.VINE || it.type == PlantType.FLOWER
                    }
                )

            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE ->
                raiseCandidates(
                    context,
                    card,
                    dice,
                    context.self.board.creature.count {
                        it.type == PlantType.ROOT || it.type == PlantType.VINE
                    }
                )

            GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5 ->
                if (context.self.board.bees + context.self.board.worms > 0) {
                    raiseCandidates(context, card, dice, 5)
                } else {
                    emptyList()
                }

            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE ->
                singleDieCandidates(context, card, dice) { die ->
                    DieValueHeuristics.flipGain(die.sides, die.value)
                }

            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX -> {
                val lowest = dice.minOf { it.die.value }
                singleDieCandidates(
                    context,
                    card,
                    dice.filter { it.die.value == lowest }
                ) { die -> die.sides - die.value }
            }

            GameEffect.SET_DIE_UP_TO_D12_TO_MAX ->
                singleDieCandidates(
                    context,
                    card,
                    dice.filter { it.die.sides <= 12 }
                ) { die -> die.sides - die.value }

            GameEffect.SET_DIE_TO_MATCH_ANOTHER -> {
                val highest = dice.maxOfOrNull { it.die.value } ?: return null
                singleDieCandidates(context, card, dice) { die ->
                    (highest - die.value).coerceAtLeast(0)
                }
            }

            GameEffect.RAISE_ALL_DICE_PLUS_2 ->
                listOfNotNull(
                    analyzeGroupedChanges(
                        context = context,
                        card = card,
                        dice = dice,
                        mode = BattleAnalysisMode.DETERMINISTIC
                    ) { die ->
                        DieValueHeuristics.actualRaiseGain(die.sides, die.value, 2).toDouble()
                    }
                )

            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS ->
                singleDieCandidates(
                    context,
                    card,
                    dice,
                    mode = BattleAnalysisMode.EXPECTED
                ) { die ->
                    DieValueHeuristics.expectedRerollUntilAtLeastGain(
                        sides = die.sides,
                        value = die.value,
                        minimum = 3
                    )
                }

            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE ->
                singleDieCandidates(
                    context,
                    card,
                    dice,
                    mode = BattleAnalysisMode.EXPECTED
                ) { die ->
                    DieValueHeuristics.expectedRerollGain(die.sides, die.value)
                }

            else -> emptyList()
        }

        return candidates.maxWithOrNull(
            compareBy<BattleActionAnalysis<CreatureCardId>> { it.tacticalValue }
                .thenBy { it.improvementStepCount }
        )
    }

    private fun collateralCandidates(
        context: DecisionContext,
        card: CreatureCardView,
        dice: List<LocatedBattleDie>
    ): List<BattleActionAnalysis<CreatureCardId>> =
        dice.mapNotNull { located ->
            ownDieCollateralAnalyzer(
                context = context,
                effect = card.effect,
                handIndex = located.die.handIndex,
                realization = card.id
            )
        }

    private fun raiseCandidates(
        context: DecisionContext,
        card: CreatureCardView,
        dice: List<LocatedBattleDie>,
        amount: Int
    ): List<BattleActionAnalysis<CreatureCardId>> =
        if (amount <= 0) {
            emptyList()
        } else {
            singleDieCandidates(context, card, dice) { die ->
                DieValueHeuristics.actualRaiseGain(die.sides, die.value, amount)
            }
        }

    private fun singleDieCandidates(
        context: DecisionContext,
        card: CreatureCardView,
        dice: List<LocatedBattleDie>,
        mode: BattleAnalysisMode = BattleAnalysisMode.DETERMINISTIC,
        change: (BattleDieView) -> Number
    ): List<BattleActionAnalysis<CreatureCardId>> =
        dice.mapNotNull { located ->
            val delta = change(located.die).toDouble()
            ownTotalChangeAnalyzer(
                context = context,
                realization = card.id,
                row = located.row,
                change = delta,
                mode = mode
            )
        }

    private fun analyzeGroupedChanges(
        context: DecisionContext,
        card: CreatureCardView,
        dice: List<LocatedBattleDie>,
        mode: BattleAnalysisMode,
        change: (BattleDieView) -> Double
    ): BattleActionAnalysis<CreatureCardId>? {
        val byRow = dice.groupBy { it.row }.mapValues { (_, rowDice) ->
            rowDice.sumOf { change(it.die) }
        }
        return ownTotalChangeAnalyzer(
            context = context,
            realization = card.id,
            changesByRow = byRow,
            mode = mode
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
}
