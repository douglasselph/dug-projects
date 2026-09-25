package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.card.wisp.OvergrowthUseHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.support.SupportAction

/**
 * Canonical ordinary-human Battle decision policy.
 *
 * Battle Stage A has completed designer review. The approved target behavior separates
 * multiplayer row facts, immediate Battle Swing, pre-random expectation versus
 * post-random actual information, and the Step-5 Support/Final-Main continuation
 * decision. B7 applies the shared tactical layer to Step-4 Draw evaluation, B8
 * reuses it after the roll for actual die placement, B9/B10 supply normalized
 * Support-capacity/reachability facts, B11 routes ordinary direct Supports
 * through shared tactical analysis, B12 evaluates enabling Supports through
 * one-step refreshed-Plant reasoning, B13 handles the approved special Battle
 * effects, and B14 orchestrates worthwhile Support versus Final Main through the
 * continuation gate. B15 aligns Battle action/target/branch decisions, B16 makes
 * the behavior contract directly testable, and B17 closes focused verification.
 * Milestone-2 Battle is CERTIFIED after the successful Stage C full regression.
 *
 * Durable behavior contract:
 *
 * `doc/HUMAN_BASELINE_BATTLE.md`
 *
 * Incremental implementation/certification plan:
 *
 * `doc/HUMAN_BASELINE_BATTLE_PLAN.md`
 *
 * Cross-cutting Battle tuning belongs in [HumanBaselinePolicy]. Card/effect-specific
 * intrinsic values remain beside their scorers. Strategy RNG remains reserved for
 * genuine decision ties; hypothetical Battle analysis must never consume mechanical RNG.
 */
class HumanBaselineBattleStrategy(
    private val delegate: BattleStrategy = MechanicalBattleStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers),
    internal val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create(),
    internal val firstMainPriority: BattleFirstMainPriority = BattleFirstMainPriority(cardScorers, policy),
    internal val placementPriority: BattlePlacementPriority = BattlePlacementPriority(policy),
    internal val supportPriority: BattleSupportPriority = BattleSupportPriority(cardScorers, policy),
    internal val turnOrchestrator: BattleTurnOrchestrator = BattleTurnOrchestrator(cardScorers, policy)
) : BattleStrategy {
    override fun chooseFirstMainAction(request: ChooseBattleFirstMainActionRequest): BattleMainAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseFirstMainAction(request)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalChoices.map { action ->
                DecisionCandidate(
                    choice = action,
                    score = firstMainPriority(request.context, request.roundCard, action),
                    tags = mainTags(request, action)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }

    override fun chooseTurnAction(request: ChooseBattleTurnActionRequest): BattleTurnAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseTurnAction(request)

        val overgrowth = applyOvergrowthWillingness(request.context, request.legalChoices)
        val pocketedSpark = applyPocketedSparkWillingness(
            context = request.context,
            legalChoices = overgrowth.legalChoices
        )
        val legalChoices = pocketedSpark.legalChoices
        val orchestration = turnOrchestrator(
            context = request.context,
            roundCard = request.roundCard,
            legalChoices = legalChoices
        )
        val currentFinalMains = orchestration.finalMains
        val legalSupports = legalChoices
            .filterIsInstance<BattleTurnAction.Support>()
            .map { it.action }

        val candidates: List<BattleTurnAction> = if (orchestration.chooseSupport) {
            orchestration.worthwhileSupports.map { BattleTurnAction.Support(it) }
        } else {
            legalChoices.filterIsInstance<BattleTurnAction.FinalMain>()
        }
        if (candidates.isEmpty()) {
            val fallback = legalChoices.firstOrNull() ?: delegate.chooseTurnAction(request)
            return attachPocketedSparkProbability(
                attachOvergrowthProbability(fallback, overgrowth),
                pocketedSpark
            )
        }

        val selected = scoreEngine.chooseValue(
            context = request.context,
            candidates = candidates.map { choice ->
                val score = when (choice) {
                    is BattleTurnAction.FinalMain ->
                        BattleMainPriority.score(request.context, request.roundCard, choice.action, cardScorers)
                    is BattleTurnAction.Support -> {
                        val base = supportPriority.score(
                            context = request.context,
                            roundCard = request.roundCard,
                            action = choice.action,
                            currentFinalMains = currentFinalMains,
                            legalSupports = legalSupports
                        )
                        if (isSoftSupport(choice.action) && base.total > 0) {
                            base.adjusted(
                                orchestration.tempo.softSupportBonus,
                                "Acting-last tempo modifies useful soft Support"
                            )
                        } else {
                            base
                        }
                    }
                }
                val tags = when (choice) {
                    is BattleTurnAction.FinalMain -> mainTags(request, choice.action)
                    is BattleTurnAction.Support -> supportPriority.tags(choice.action)
                }
                DecisionCandidate(choice, score, tags)
            },
            influenceRegistry = influenceRegistry
        )
        return attachPocketedSparkProbability(
            attachOvergrowthProbability(selected, overgrowth),
            pocketedSpark
        )
    }


    private fun applyOvergrowthWillingness(
        context: DecisionContext,
        legalChoices: List<BattleTurnAction>
    ): BattleOvergrowthGate {
        val overgrowthChoices = legalChoices.filter(::isOvergrowthSupport)
        if (overgrowthChoices.isEmpty()) return BattleOvergrowthGate(legalChoices)

        val targetSides = OvergrowthUseHeuristics.preferredTargetSides(context)
            ?: return BattleOvergrowthGate(legalChoices)
        val percentage = policy.overgrowthUsePercentage(context, targetSides)
        val accepted = percentage > 0 && strategyRandomizer.nextInt(100) < percentage
        if (accepted) {
            return BattleOvergrowthGate(
                legalChoices = legalChoices,
                acceptedPercentage = percentage
            )
        }

        val filtered = legalChoices.filterNot(::isOvergrowthSupport)
        return BattleOvergrowthGate(
            legalChoices = filtered.ifEmpty { legalChoices }
        )
    }

    private fun attachOvergrowthProbability(
        selected: BattleTurnAction,
        gate: BattleOvergrowthGate
    ): BattleTurnAction {
        val percent = gate.acceptedPercentage ?: return selected
        val support = selected as? BattleTurnAction.Support ?: return selected
        val shared = support.action as? BattleSupportAction.Shared ?: return selected
        val wisp = shared.action as? SupportAction.PlayWisp ?: return selected
        if (wisp.card.effect != GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW) return selected
        return BattleTurnAction.Support(
            BattleSupportAction.Shared(wisp.withDecisionProbability(percent))
        )
    }

    private fun isOvergrowthSupport(choice: BattleTurnAction): Boolean {
        val support = (choice as? BattleTurnAction.Support)?.action as? BattleSupportAction.Shared
            ?: return false
        val wisp = support.action as? SupportAction.PlayWisp ?: return false
        return wisp.card.effect == GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW
    }

    private fun applyPocketedSparkWillingness(
        context: DecisionContext,
        legalChoices: List<BattleTurnAction>
    ): BattleWispGate {
        val choices = legalChoices.filter(::isPocketedSparkSupport)
        if (choices.isEmpty()) return BattleWispGate(legalChoices)

        val bestDiscardSides = context.self.board.discard.maxOfOrNull { it.sides }
            ?: return BattleWispGate(removeWispChoices(legalChoices, choices))
        val percentage = policy.pocketedSparkUsePercentage(context, bestDiscardSides)
        val accepted = percentage > 0 && strategyRandomizer.nextInt(100) < percentage
        return BattleWispGate(
            legalChoices = if (accepted) legalChoices else removeWispChoices(legalChoices, choices),
            acceptedPercentage = if (accepted) percentage else null
        )
    }

    private fun attachPocketedSparkProbability(
        selected: BattleTurnAction,
        gate: BattleWispGate
    ): BattleTurnAction {
        val percent = gate.acceptedPercentage ?: return selected
        val support = selected as? BattleTurnAction.Support ?: return selected
        val shared = support.action as? BattleSupportAction.Shared ?: return selected
        val wisp = shared.action as? SupportAction.PlayWisp ?: return selected
        if (wisp.card.effect != GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD) return selected
        return BattleTurnAction.Support(
            BattleSupportAction.Shared(wisp.withDecisionProbability(percent))
        )
    }

    private fun isPocketedSparkSupport(choice: BattleTurnAction): Boolean {
        val support = (choice as? BattleTurnAction.Support)?.action as? BattleSupportAction.Shared
            ?: return false
        val wisp = support.action as? SupportAction.PlayWisp ?: return false
        return wisp.card.effect == GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD
    }

    private fun removeWispChoices(
        legalChoices: List<BattleTurnAction>,
        choicesToRemove: List<BattleTurnAction>
    ): List<BattleTurnAction> {
        val filtered = legalChoices.filterNot { it in choicesToRemove }
        return filtered.ifEmpty { legalChoices }
    }

    override fun chooseDiePlacement(request: ChooseBattleDiePlacementRequest): StrikeRow {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDiePlacement(request)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalRows.map { row ->
                DecisionCandidate(
                    choice = row,
                    score = placementPriority(request.context, row, request.die.value)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }


    private fun isSoftSupport(action: BattleSupportAction): Boolean =
        (action as? BattleSupportAction.Shared)?.action is dugsolutions.leaf.v35.player.decision.support.SupportAction.PlayWisp

    private fun mainTags(
        request: ChooseBattleFirstMainActionRequest,
        action: BattleMainAction
    ): Set<DecisionTag> =
        when (action) {
            BattleMainAction.RoundEffect1 -> roundEffectTags(request.roundCard.firstEffect.effect)
            BattleMainAction.RoundEffect2 -> roundEffectTags(request.roundCard.secondEffect.effect)
            else -> emptySet()
        }

    private fun mainTags(
        request: ChooseBattleTurnActionRequest,
        action: BattleMainAction
    ): Set<DecisionTag> =
        when (action) {
            BattleMainAction.RoundEffect1 -> roundEffectTags(request.roundCard.firstEffect.effect)
            BattleMainAction.RoundEffect2 -> roundEffectTags(request.roundCard.secondEffect.effect)
            else -> emptySet()
        }

    private fun roundEffectTags(effect: GameEffect): Set<DecisionTag> =
        when (effect) {
            GameEffect.GAIN_TWO_WORMS -> setOf(DecisionTag.ACQUIRE_WORM)
            else -> emptySet()
        }
    private data class BattleOvergrowthGate(
        val legalChoices: List<BattleTurnAction>,
        val acceptedPercentage: Int? = null
    )

    private data class BattleWispGate(
        val legalChoices: List<BattleTurnAction>,
        val acceptedPercentage: Int? = null
    )

}
