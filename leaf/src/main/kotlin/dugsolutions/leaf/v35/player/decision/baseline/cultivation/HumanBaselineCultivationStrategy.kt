package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.card.wisp.OvergrowthUseHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.*
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.cultivation.*
import dugsolutions.leaf.v35.player.decision.mechanical.cultivation.MechanicalCultivationStrategy
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.support.SupportAction

/**
 * Human Baseline policy for one decision opportunity during Cultivation Build.
 *
 * The rules engine owns legality and repeatedly supplies the currently legal
 * choices. This strategy ranks those choices on one common [PriorityScore]
 * scale; [BaselineScoreEngine] applies semantic card influences. Strategy RNG
 * breaks genuine ties and models the explicitly probabilistic Compost tendency.
 *
 * Target design, at a high level:
 *
 * - Main Actions compare Draw, Plant activation, and the two Round Effects by
 *   their actual ordinary-human benefit in the current state.
 * - Support Actions are scored from the specific benefit of the offered
 *   Wisp/reroll/refresh/Mulch/Worm/Butterfly, with soft reserve penalties for
 *   consumable resources.
 * - After both Main Actions, `Done` is a benchmark: useful Supports may beat it,
 *   while weak or wasteful Supports should be saved and lose to it.
 * - When an action's value depends on a target (for example Compost, Mulch, or
 *   Sunlight), the top-level scorer and later Effect Choice must use compatible
 *   target valuation so the chosen target realizes the value that justified
 *   choosing the action.
 * - Shared development, reserve, purchase-threshold, card-scoring, and
 *   influence heuristics should be used only where they correspond to simple,
 *   explainable ordinary-human judgment rather than deep optimization.
 *
 * B2 wiring makes threshold-sensitive Main Actions use the injected policy's
 * normal purchasing power (so protected Critters are not treated as ordinary
 * Buy power). Compost receives the policy's modest permanent dice-development
 * nudge. B3 routes Plant activation through [PlantActivationPriority], keeping
 * card-local valuation in the card scorer while applying the same cross-cutting
 * policy to Plants that unambiguously improve permanent dice-pool strength.
 * Draw deliberately does not receive that long-term nudge because drawing an
 * existing die does not increase total dice-pool power.
 *
 * B6 aligns downstream Effect choices with the value that justified the
 * top-level action: Compost, Mulch, and Sunlight share target scorers with the
 * Effect strategy; Cultivation die-targeting card effects use the same
 * reserve-aware purchasing-power model; and Petal To Die 4 shares branch
 * valuation between activation scoring and branch selection.
 *
 * Current implementation details and A1/A2 findings are documented in:
 *
 * `doc/HUMAN_BASELINE_CULTIVATION.md`
 *
 * The design rationale, approved target architecture, and completed certification
 * sequence are documented in:
 *
 * `doc/HUMAN_BASELINE_CULTIVATION_PLAN.md`
 *
 * Cross-cutting tuning knobs and their overridable policy layer are documented in:
 *
 * `doc/HUMAN_BASELINE_POLICY.md`
 *
 * The executable ordinary-human behavior contract is kept in:
 *
 * `src/test/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/HumanBaselineCultivationStrategyTest.kt`
 *
 * Milestone-2 Cultivation is CERTIFIED. The approved behavior contract, focused
 * verification, target/branch alignment, designer review, and full unit +
 * integration + simulation regression all agree for this decision area.
 */
class HumanBaselineCultivationStrategy(
    private val delegate: CultivationStrategy = MechanicalCultivationStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers),
    internal val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    private val strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create()
) : CultivationStrategy {
    override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseAction(request)

        // Willingness gates are intentionally independent and ordered before scoring.
        // In particular, Compost is sampled before Sunlight. A declined Compost does
        // not imply Sunlight is accepted; Sunlight receives its own separate sample.
        // If both are accepted they remain legal and the normal score comparison wins.
        val overgrowth = applyOvergrowthWillingness(
            context = request.context,
            legalChoices = request.legalChoices
        )
        val pocketedSpark = applyPocketedSparkWillingness(
            context = request.context,
            legalChoices = overgrowth.legalChoices
        )
        val compost = applyCompostWillingness(
            request = request,
            legalChoices = pocketedSpark.legalChoices
        )
        val mulch = applyMulchWillingness(
            request = request,
            legalChoices = compost.legalChoices
        )
        val sunlight = applySunlightWillingness(
            request = request,
            legalChoices = mulch.legalChoices
        )
        val selected = scoreEngine.chooseValue(
            context = request.context,
            candidates = sunlight.legalChoices.map { choice ->
                DecisionCandidate(
                    choice = choice,
                    score = score(request, choice, compost),
                    tags = tags(request, choice)
                )
            },
            influenceRegistry = influenceRegistry
        )
        return attachSunlightProbability(
            selected = attachMulchProbability(
                selected = attachCompostProbability(
                    selected = attachPocketedSparkProbability(
                        selected = attachOvergrowthProbability(selected, overgrowth),
                        gate = pocketedSpark
                    ),
                    request = request,
                    gate = compost
                ),
                request = request,
                gate = mulch
            ),
            request = request,
            gate = sunlight
        )
    }

    private fun applyOvergrowthWillingness(
        context: DecisionContext,
        legalChoices: List<CultivationAction>
    ): CultivationOvergrowthGate {
        val overgrowthChoices = legalChoices.filter { choice ->
            val wisp = (choice as? CultivationAction.Support)?.action as? SupportAction.PlayWisp
            wisp?.card?.effect == GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW
        }
        if (overgrowthChoices.isEmpty()) return CultivationOvergrowthGate(legalChoices)

        val targetSides = OvergrowthUseHeuristics.preferredTargetSides(context)
            ?: return CultivationOvergrowthGate(legalChoices)
        val percentage = policy.overgrowthUsePercentage(context, targetSides)
        val accepted = percentage > 0 && strategyRandomizer.nextInt(100) < percentage
        if (accepted) {
            return CultivationOvergrowthGate(
                legalChoices = legalChoices,
                acceptedPercentage = percentage
            )
        }

        val filtered = legalChoices.filterNot { it in overgrowthChoices }
        return CultivationOvergrowthGate(
            legalChoices = filtered.ifEmpty { legalChoices }
        )
    }

    private fun attachOvergrowthProbability(
        selected: CultivationAction,
        gate: CultivationOvergrowthGate
    ): CultivationAction {
        val percent = gate.acceptedPercentage ?: return selected
        val support = selected as? CultivationAction.Support ?: return selected
        val wisp = support.action as? SupportAction.PlayWisp ?: return selected
        if (wisp.card.effect != GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW) return selected
        return CultivationAction.Support(wisp.withDecisionProbability(percent))
    }

    private fun applyPocketedSparkWillingness(
        context: DecisionContext,
        legalChoices: List<CultivationAction>
    ): CultivationWispGate {
        val choices = legalChoices.filter { choice ->
            val wisp = (choice as? CultivationAction.Support)?.action as? SupportAction.PlayWisp
            wisp?.card?.effect == GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD
        }
        if (choices.isEmpty()) return CultivationWispGate(legalChoices)

        val bestDiscardSides = context.self.board.discard.maxOfOrNull { it.sides }
            ?: return CultivationWispGate(removeChoices(legalChoices, choices))
        val percentage = policy.pocketedSparkUsePercentage(context, bestDiscardSides)
        val accepted = percentage > 0 && strategyRandomizer.nextInt(100) < percentage
        return CultivationWispGate(
            legalChoices = if (accepted) legalChoices else removeChoices(legalChoices, choices),
            acceptedPercentage = if (accepted) percentage else null
        )
    }

    private fun attachPocketedSparkProbability(
        selected: CultivationAction,
        gate: CultivationWispGate
    ): CultivationAction {
        val percent = gate.acceptedPercentage ?: return selected
        val support = selected as? CultivationAction.Support ?: return selected
        val wisp = support.action as? SupportAction.PlayWisp ?: return selected
        if (wisp.card.effect != GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD) return selected
        return CultivationAction.Support(wisp.withDecisionProbability(percent))
    }

    private fun applyCompostWillingness(
        request: ChooseCultivationActionRequest,
        legalChoices: List<CultivationAction>
    ): CultivationCompostGate {
        if (request.mainActionsRemaining !in 1..2) {
            return CultivationCompostGate(legalChoices)
        }

        val compostChoices = legalChoices.filter { choice ->
            val main = choice as? CultivationAction.Main ?: return@filter false
            when (main.action) {
                CultivationMainAction.RoundEffect1 ->
                    request.roundCard.firstEffect.effect == GameEffect.UPGRADE_DIE_FROM_HAND
                CultivationMainAction.RoundEffect2 ->
                    request.roundCard.secondEffect.effect == GameEffect.UPGRADE_DIE_FROM_HAND
                else -> false
            }
        }
        if (compostChoices.isEmpty()) return CultivationCompostGate(legalChoices)

        val normalPurchasingPower = policy.normalPurchasingPower(request.context)
        val percentage = CompostPriority.usePercentage(
            context = request.context,
            normalPurchasingPower = normalPurchasingPower,
            mainActionsRemaining = request.mainActionsRemaining
        )
        if (percentage <= 0) return CultivationCompostGate(legalChoices)

        val accepted = strategyRandomizer.nextInt(100) < percentage
        return CultivationCompostGate(
            legalChoices = legalChoices,
            percentage = percentage,
            accepted = accepted
        )
    }

    private fun attachCompostProbability(
        selected: CultivationAction,
        request: ChooseCultivationActionRequest,
        gate: CultivationCompostGate
    ): CultivationAction {
        if (gate.accepted != true) return selected
        val percent = gate.percentage ?: return selected
        val main = selected as? CultivationAction.Main ?: return selected
        val effect = when (main.action) {
            CultivationMainAction.RoundEffect1 -> request.roundCard.firstEffect.effect
            CultivationMainAction.RoundEffect2 -> request.roundCard.secondEffect.effect
            else -> return selected
        }
        if (effect != GameEffect.UPGRADE_DIE_FROM_HAND) return selected
        return main.withDecisionProbability(percent)
    }

    private fun applyMulchWillingness(
        request: ChooseCultivationActionRequest,
        legalChoices: List<CultivationAction>
    ): CultivationRoundEffectGate {
        val mulchChoices = legalChoices.filter { choice ->
            val main = choice as? CultivationAction.Main ?: return@filter false
            effectForMain(request, main.action) == GameEffect.MULCH_DIE_FROM_HAND
        }
        if (mulchChoices.isEmpty()) return CultivationRoundEffectGate(legalChoices)

        val normalPower = policy.normalPurchasingPower(request.context)
        val target = MulchPriority.preferredTarget(request.context, normalPower)
        val percentage = target?.let { policy.mulchUsePercentage(request.context, it.value) } ?: 0
        if (percentage <= 0) {
            return CultivationRoundEffectGate(removeChoices(legalChoices, mulchChoices))
        }
        val accepted = strategyRandomizer.nextInt(100) < percentage
        return CultivationRoundEffectGate(
            legalChoices = if (accepted) legalChoices else removeChoices(legalChoices, mulchChoices),
            acceptedPercentage = if (accepted) percentage else null
        )
    }

    private fun attachMulchProbability(
        selected: CultivationAction,
        request: ChooseCultivationActionRequest,
        gate: CultivationRoundEffectGate
    ): CultivationAction =
        attachRoundEffectProbability(
            selected = selected,
            request = request,
            gate = gate,
            effect = GameEffect.MULCH_DIE_FROM_HAND
        )

    private fun applySunlightWillingness(
        request: ChooseCultivationActionRequest,
        legalChoices: List<CultivationAction>
    ): CultivationRoundEffectGate {
        val sunlightChoices = legalChoices.filter { choice ->
            val main = choice as? CultivationAction.Main ?: return@filter false
            effectForMain(request, main.action) == GameEffect.RAISE_DIE_PLUS_3
        }
        if (sunlightChoices.isEmpty()) return CultivationRoundEffectGate(legalChoices)
        if (!SunlightPriority.isWorthConsidering(request.context)) {
            return CultivationRoundEffectGate(removeChoices(legalChoices, sunlightChoices))
        }

        val percentage = policy.sunlightUsePercentage(request.context)
        val accepted = percentage > 0 && strategyRandomizer.nextInt(100) < percentage
        return CultivationRoundEffectGate(
            legalChoices = if (accepted) legalChoices else removeChoices(legalChoices, sunlightChoices),
            acceptedPercentage = if (accepted) percentage else null
        )
    }

    private fun attachSunlightProbability(
        selected: CultivationAction,
        request: ChooseCultivationActionRequest,
        gate: CultivationRoundEffectGate
    ): CultivationAction =
        attachRoundEffectProbability(
            selected = selected,
            request = request,
            gate = gate,
            effect = GameEffect.RAISE_DIE_PLUS_3
        )

    private fun attachRoundEffectProbability(
        selected: CultivationAction,
        request: ChooseCultivationActionRequest,
        gate: CultivationRoundEffectGate,
        effect: GameEffect
    ): CultivationAction {
        val percent = gate.acceptedPercentage ?: return selected
        val main = selected as? CultivationAction.Main ?: return selected
        if (effectForMain(request, main.action) != effect) return selected
        return main.withDecisionProbability(percent)
    }

    private fun effectForMain(
        request: ChooseCultivationActionRequest,
        action: CultivationMainAction
    ): GameEffect? =
        when (action) {
            CultivationMainAction.RoundEffect1 -> request.roundCard.firstEffect.effect
            CultivationMainAction.RoundEffect2 -> request.roundCard.secondEffect.effect
            else -> null
        }

    private fun removeChoices(
        legalChoices: List<CultivationAction>,
        choicesToRemove: List<CultivationAction>
    ): List<CultivationAction> {
        val filtered = legalChoices.filterNot { it in choicesToRemove }
        return filtered.ifEmpty { legalChoices }
    }

    private fun score(
        request: ChooseCultivationActionRequest,
        choice: CultivationAction,
        compostGate: CultivationCompostGate
    ): PriorityScore =
        when (choice) {
            CultivationAction.Done -> PriorityScore(
                if (request.mainActionsRemaining == 0) policy.cultivationDoneScore(request.context) else 0
            )
            is CultivationAction.Support -> scoreSupport(
                context = request.context,
                action = choice.action
            )
            is CultivationAction.Main -> when (val action = choice.action) {
                CultivationMainAction.Draw -> DrawPriority.score(request.context)
                is CultivationMainAction.ActivatePlant ->
                    PlantActivationPriority.score(
                        context = request.context,
                        card = action.card,
                        cardScorers = cardScorers,
                        policy = policy
                    )
                CultivationMainAction.RoundEffect1 -> scoreRoundEffect(
                    effect = request.roundCard.firstEffect.effect,
                    context = request.context,
                    compostGate = compostGate
                )
                CultivationMainAction.RoundEffect2 -> scoreRoundEffect(
                    effect = request.roundCard.secondEffect.effect,
                    context = request.context,
                    compostGate = compostGate
                )
            }
        }


    private fun scoreSupport(
        context: DecisionContext,
        action: SupportAction
    ): PriorityScore {
        val base = CultivationSupportPriority.score(
            context = context,
            action = action,
            cardScorers = cardScorers,
            policy = policy
        )
        return base
    }

    private fun tags(
        request: ChooseCultivationActionRequest,
        choice: CultivationAction
    ): Set<DecisionTag> =
        when (choice) {
            CultivationAction.Done -> emptySet()
            is CultivationAction.Support -> supportTags(choice.action)
            is CultivationAction.Main -> when (choice.action) {
                CultivationMainAction.Draw -> emptySet()
                is CultivationMainAction.ActivatePlant -> emptySet()
                CultivationMainAction.RoundEffect1 -> roundEffectTags(request.roundCard.firstEffect.effect)
                CultivationMainAction.RoundEffect2 -> roundEffectTags(request.roundCard.secondEffect.effect)
            }
        }

    private fun scoreRoundEffect(
        effect: GameEffect,
        context: DecisionContext,
        compostGate: CultivationCompostGate
    ): PriorityScore =
        when (effect) {
            GameEffect.UPGRADE_DIE_FROM_HAND -> scoreCompost(context, compostGate)
            GameEffect.MULCH_DIE_FROM_HAND -> MulchPriority.score(
                context = context,
                normalPurchasingPower = policy.normalPurchasingPower(context)
            )
            GameEffect.GAIN_WATER_TOKEN -> WaterPriority.score(context)
            GameEffect.RAISE_DIE_PLUS_3 -> SunlightPriority.score(
                context = context,
                normalPurchasingPower = policy.normalPurchasingPower(context)
            )
            else -> PriorityScore(45)
        }

    private fun scoreCompost(
        context: DecisionContext,
        gate: CultivationCompostGate
    ): PriorityScore {
        val normalPurchasingPower = policy.normalPurchasingPower(context)
        val score = CompostPriority.score(
            context = context,
            normalPurchasingPower = normalPurchasingPower,
            developmentBonus = policy.cultivationDiceDevelopmentBonus(context)
        )
        val percentage = gate.percentage ?: return score
        return score.adjusted(
            amount = if (gate.accepted == true) 0 else -100,
            reason = "Compost tendency ${if (gate.accepted == true) "accepted" else "declined"} ($percentage%)"
        )
    }

    private fun roundEffectTags(effect: GameEffect): Set<DecisionTag> =
        when (effect) {
            GameEffect.GAIN_WATER_TOKEN -> WaterPriority.tags
            GameEffect.MULCH_DIE_FROM_HAND -> setOf(DecisionTag.ACQUIRE_MULCH)
            else -> emptySet()
        }

    private fun supportTags(action: SupportAction): Set<DecisionTag> =
        when (action) {
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
    private data class CultivationOvergrowthGate(
        val legalChoices: List<CultivationAction>,
        val acceptedPercentage: Int? = null
    )

    private data class CultivationWispGate(
        val legalChoices: List<CultivationAction>,
        val acceptedPercentage: Int? = null
    )

    private data class CultivationCompostGate(
        val legalChoices: List<CultivationAction>,
        val percentage: Int? = null,
        val accepted: Boolean? = null
    )

    private data class CultivationRoundEffectGate(
        val legalChoices: List<CultivationAction>,
        val acceptedPercentage: Int? = null
    )

}
