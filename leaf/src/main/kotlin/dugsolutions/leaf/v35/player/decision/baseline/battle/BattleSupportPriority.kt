package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.tokens.Critter
import kotlin.math.roundToInt

class BattleSupportPriority(
    private val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val directAnalyzer: BattleDirectSupportAnalyzer =
        BattleDirectSupportAnalyzer(policy),
    private val enablingAnalyzer: BattleEnablingSupportAnalyzer =
        BattleEnablingSupportAnalyzer(cardScorers, policy)
) {
    fun score(
        context: DecisionContext,
        action: BattleSupportAction
    ): PriorityScore {
        val direct = directAnalyzer(context, action)
        if (direct != null) return scoreDirect(direct)
        return scoreNonDirect(context, action)
    }

    fun score(
        context: DecisionContext,
        roundCard: RoundCard,
        action: BattleSupportAction,
        currentFinalMains: Collection<BattleMainAction>,
        legalSupports: Collection<BattleSupportAction>
    ): PriorityScore {
        val direct = directAnalyzer(context, action)
        if (direct != null) return scoreDirect(direct)
        val enabling = (action as? BattleSupportAction.Shared)?.let {
            enablingAnalyzer(
                context = context,
                roundCard = roundCard,
                action = it,
                currentFinalMains = currentFinalMains,
                legalSupports = legalSupports
            )
        }
        if (enabling != null) return enabling.priority
        return scoreNonDirect(context, action)
    }

    fun tags(action: BattleSupportAction): Set<DecisionTag> =
        when (action) {
            is BattleSupportAction.PlaceCritter -> setOf(
                when (action.critter) {
                    Critter.BEE -> DecisionTag.SPEND_BEE
                    Critter.WORM -> DecisionTag.SPEND_WORM
                }
            )
            is BattleSupportAction.Shared -> when (action.action) {
                is SupportAction.PlayWisp -> setOf(DecisionTag.PLAY_WISP)
                is SupportAction.UseWaterReroll -> setOf(DecisionTag.SPEND_WATER)
                SupportAction.UseWaterRefresh -> setOf(
                    DecisionTag.SPEND_WATER,
                    DecisionTag.REFRESH_CREATURE
                )
                is SupportAction.UseMulch -> setOf(DecisionTag.SPEND_MULCH)
                is SupportAction.UseWormFlip -> setOf(
                    DecisionTag.SPEND_WORM,
                    DecisionTag.REFRESH_CREATURE
                )
                is SupportAction.UseButterfly -> emptySet()
            }
        }

    private fun scoreDirect(direct: BattleDirectSupportAnalysis): PriorityScore {
        val tactical = direct.analysis.tacticalValue.roundToInt()
        if (!direct.passesSpendingGate) {
            return PriorityScore(DISQUALIFIED_PREMIUM_SCORE)
                .adjusted(tactical, "Projected direct Battle Swing")
                .adjusted(0, gateReason(direct.gate))
        }
        return PriorityScore(tactical)
            .adjusted(0, "Projected direct Battle Swing")
    }

    private fun scoreNonDirect(
        context: DecisionContext,
        action: BattleSupportAction
    ): PriorityScore =
        when (action) {
            is BattleSupportAction.PlaceCritter -> PriorityScore(0)
            is BattleSupportAction.Shared -> when (val shared = action.action) {
                is SupportAction.PlayWisp ->
                    cardScorers.forWisp(shared.card).wispPlayScore(context, shared.card)

                SupportAction.UseWaterRefresh -> {
                    val spent = context.self.board.creature.count { it.isFaceDown }
                    PriorityScore(20 + spent * 15)
                }

                is SupportAction.UseWormFlip -> {
                    val card = context.self.board.creature.firstOrNull { it.id == shared.cardId }
                    val preserve = card?.let { cardScorers.forPlant(it).lossValue(context, it) } ?: 30
                    PriorityScore(25 + preserve / 3).adjusted(10, "Refreshes a spent Plant")
                }

                is SupportAction.UseWaterReroll,
                is SupportAction.UseMulch,
                is SupportAction.UseButterfly -> PriorityScore(0)
            }
        }

    private fun gateReason(gate: BattleDirectSupportGate): String =
        when (gate) {
            BattleDirectSupportGate.WORM_REQUIRES_MEANINGFUL_VP_GAIN ->
                "Direct Worm requires the policy minimum projected Strike-VP gain"
            BattleDirectSupportGate.WATER_REQUIRES_POSITIVE_TRANSITION ->
                "Water reroll requires a positive named Battle transition"
            BattleDirectSupportGate.MULCH_REQUIRES_WIN_FLIPPED ->
                "Mulch requires expected WIN_FLIPPED"
            BattleDirectSupportGate.NONE,
            BattleDirectSupportGate.PASSED -> "Premium-resource gate passed"
        }

    private companion object {
        const val DISQUALIFIED_PREMIUM_SCORE = -10_000
    }
}
