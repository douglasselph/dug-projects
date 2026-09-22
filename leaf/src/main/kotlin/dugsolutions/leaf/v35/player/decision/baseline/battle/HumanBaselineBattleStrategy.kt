package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy

/**
 * Canonical ordinary-human Battle decision policy.
 *
 * Battle Stage A has completed designer review. The approved target behavior separates
 * multiplayer row facts, immediate Battle Swing, pre-random expectation versus
 * post-random actual information, and the Step-5 Support/Final-Main continuation
 * decision. B7 now applies the shared tactical layer to Step-4 Draw evaluation while later
 * checkpoints continue migrating actual placement, Support/Final-Main orchestration,
 * and target-dependent Effect alignment.
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
    internal val firstMainPriority: BattleFirstMainPriority = BattleFirstMainPriority(cardScorers, policy)
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
        val candidates = request.legalChoices.map { choice ->
            val score = when (choice) {
                is BattleTurnAction.FinalMain ->
                    BattleMainPriority.score(request.context, request.roundCard, choice.action, cardScorers)
                        .adjusted(5, "Final Main action ends participation")
                is BattleTurnAction.Support -> BattleSupportPriority.score(request.context, choice.action, cardScorers)
            }
            val tags = when (choice) {
                is BattleTurnAction.FinalMain -> mainTags(request, choice.action)
                is BattleTurnAction.Support -> BattleSupportPriority.tags(choice.action)
            }
            DecisionCandidate(choice, score, tags)
        }
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = candidates,
            influenceRegistry = influenceRegistry
        )
    }

    override fun chooseDiePlacement(request: ChooseBattleDiePlacementRequest): StrikeRow {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDiePlacement(request)
        return scoreEngine.chooseValue(
            context = request.context,
            candidates = request.legalRows.map { row ->
                DecisionCandidate(
                    choice = row,
                    score = BattlePlacementPriority.score(request.context, row, request.die.value)
                )
            },
            influenceRegistry = influenceRegistry
        )
    }

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
}
