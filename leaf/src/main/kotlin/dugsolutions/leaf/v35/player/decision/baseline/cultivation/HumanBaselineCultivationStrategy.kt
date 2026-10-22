package dugsolutions.leaf.v35.player.decision.baseline.cultivation

import kotlin.math.roundToInt

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.baseline.card.wisp.OvergrowthUseHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.*
import dugsolutions.leaf.v35.player.decision.baseline.context.FutureDiceAvailability
import dugsolutions.leaf.v35.player.decision.baseline.context.PhaseProximity
import dugsolutions.leaf.v35.player.decision.baseline.context.DicePoolQuality
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.*
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
 * breaks genuine ties. Legacy mode separately retains historical willingness gates.
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
    private val strategyRandomizer: StrategyRandomizer = StrategyRandomizer.create(),
    private val modernScoring: HumanScoringConfiguration? = null
) : CultivationStrategy {
    override fun chooseAction(request: ChooseCultivationActionRequest): CultivationAction {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseAction(request)
        if (modernScoring != null) return chooseModernAction(request)

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
                    tags = tags(request, choice),
                    observations = calibrationObservations(request, choice)
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

    /** Phase B: one score for every legal candidate; no stochastic willingness gates. */
    private fun chooseModernAction(request: ChooseCultivationActionRequest): CultivationAction {
        val configuration = requireNotNull(modernScoring)
        val candidates = request.legalChoices.map { choice ->
            val original = score(request, choice, CultivationCompostGate(request.legalChoices))
            val actionId = actionScoreId(request, choice)
            val builder = DecisionScoreBuilder(actionId, strategyRandomizer)
            builder.add(configuration.influence(
                "$actionId.base", "Calibrated base", ScoreValue.Fixed(original.base)
            ))
            original.adjustments.forEachIndexed { index, adjustment ->
                val key = if (actionId == "compost") when {
                    adjustment.reason.startsWith("Permanent D") -> "compost.upgrade"
                    adjustment.reason == "Current-round Buy threshold impact" -> "compost.buy_threshold"
                    adjustment.reason.startsWith("Preserve at least") -> "compost.low_buy_power"
                    adjustment.reason == "Future rounds benefit from upgrade" -> "compost.future_round_bonus"
                    adjustment.reason == "Dice development is behind target" -> "compost.development_bonus"
                    else -> "compost.legacy.$index"
                } else "$actionId.legacy.$index"
                val fallback = ScoreValue.Fixed(adjustment.amount)
                val resolved = if (actionId == "compost") calibratedCompostAdjustment(
                    key, adjustment.amount
                ) else fallback
                builder.add(configuration.influence(key, adjustment.reason, resolved))
            }
            when (actionId) {
                "mulch" -> builder.add(configuration.influence("mulch.variability", "Mulch preference variability", ScoreValue.Range(-12, 12)))
                "sunlight" -> {
                    val effect = (choice as? CultivationAction.Main)?.let { effectForMain(request, it.action) }
                    if (effect == GameEffect.RAISE_DIE_PLUS_3) {
                        builder.add(configuration.influence("sunlight.variability", "Sunlight preference variability", ScoreValue.Range(-18, 12)))
                        if (!SunlightPriority.isWorthConsidering(request.context)) {
                            builder.add(configuration.influence("sunlight.weak", "Weak Sunlight relative to Draw", ScoreValue.Fixed(-30)))
                        }
                    }
                }
            }
            if (actionId == "compost") {
                addCompostInfluences(builder, request)
            }
            DecisionCandidate(
                choice = choice,
                score = builder.build().toPriorityScore(),
                tags = tags(request, choice),
                observations = calibrationObservations(request, choice)
            )
        }
        return scoreEngine.chooseValue(request.context, candidates, influenceRegistry)
    }

    /** Reuse existing score arithmetic, but expose its calibrated numeric coefficients. */
    private fun calibratedCompostAdjustment(
        key: String, legacyAmount: Int
    ): ScoreValue {
        val config = modernScoring ?: return ScoreValue.Fixed(legacyAmount)
        fun coefficient(name: String, old: Int): Int =
            (config.value(name, ScoreValue.Fixed(old)) as? ScoreValue.Fixed)?.amount ?: old
        return when (key) {
            "compost.upgrade" -> ScoreValue.Fixed(
                legacyAmount / 2 * coefficient("compost.upgrade_side", 2)
            )
            "compost.future_round_bonus" -> ScoreValue.Fixed(
                legacyAmount / 2 * coefficient("compost.future_round", 2)
            )
            "compost.buy_threshold" -> ScoreValue.Fixed(
                legacyAmount / -10 * coefficient("compost.buy_tier_penalty", -10)
            )
            "compost.low_buy_power" -> config.value("compost.low_buy_power", ScoreValue.Fixed(-40))
            else -> ScoreValue.Fixed(legacyAmount)
        }
    }

    private fun actionScoreId(request: ChooseCultivationActionRequest, choice: CultivationAction): String =
        when (choice) {
            CultivationAction.Done -> "done"
            is CultivationAction.Support -> "support"
            is CultivationAction.Main -> when (choice.action) {
                CultivationMainAction.Draw -> "draw"
                is CultivationMainAction.ActivatePlant -> "plant"
                else -> when (effectForMain(request, choice.action)) {
                    GameEffect.UPGRADE_DIE_FROM_HAND,
                    GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS,
                    GameEffect.UPGRADE_DIE_AND_USE_NOW -> "compost"
                    GameEffect.MULCH_DIE_FROM_HAND -> "mulch"
                    GameEffect.GAIN_WATER_TOKEN -> "water"
                    GameEffect.GAIN_SUNLIGHT_TOKEN, GameEffect.RAISE_DIE_PLUS_3 -> "sunlight"
                    else -> "round_other"
                }
            }
        }

    private fun addCompostInfluences(
        builder: DecisionScoreBuilder,
        request: ChooseCultivationActionRequest
    ) {
        val config = modernScoring ?: return
        val target = CompostPriority.bestTarget(request.context, policy.normalPurchasingPower(request.context))
            ?: return
        fun influence(id: String, label: String, fallback: ScoreValue) {
            builder.add(config.influence(id, label, fallback))
        }
        // Historical willingness percentages are *not* reinterpreted as score probabilities.
        // Defaults preserve the legacy deterministic score, with ranges starting neutral.
        if (target.die.value <= 2) {
            influence("compost.low_roll", "Low showing die preference", ScoreValue.Range(-10, 20))
        }
        else {
            influence("compost.ordinary_roll", "Ordinary showing die preference", ScoreValue.Range(-25, 10))
        }
        if (target.handDicePowerAfter < CompostPriority.MIN_HAND_DICE_BUY_POWER_AFTER_COMPOST) {
            influence("compost.low_power_variation", "Low hand power uncertainty", ScoreValue.Range(-20, 0))
        }
        if (request.mainActionsRemaining == 1 && request.context.self.board.hand.size <= 2) {
            influence("compost.repeat_commitment", "Second commitment", ScoreValue.Range(-12, 0))
        }
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
        val percentage = target?.let { policy.mulchUsePercentage(request.context, it) } ?: 0
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


    /** Compact, immutable facts used only when decision reasoning is enabled. */
    private fun calibrationObservations(
        request: ChooseCultivationActionRequest,
        choice: CultivationAction
    ): Map<String, String> {
        val context = request.context
        val common = linkedMapOf(
            "nextPhase" to if (PhaseProximity.battleIsNext(context)) "BATTLE" else "CULTIVATION",
            "faceUpPlants" to context.self.board.creature.count { it.isFaceUp }.toString(),
            "faceDownPlants" to context.self.board.creature.count { !it.isFaceUp }.toString(),
            "drawUtility" to DrawPriority.score(context).total.toString()
        )
        val main = (choice as? CultivationAction.Main)?.action ?: return common
        when (main) {
            is CultivationMainAction.ActivatePlant -> {
                common["card"] = main.card.card.name
                when (main.card.card.name) {
                    "Vine_11_02" -> {
                        val raises = context.self.board.creature.count { it.type.name == "ROOT" || it.type.name == "VINE" }
                        val headroom = context.self.board.hand.sumOf { (it.sides - it.value).coerceAtLeast(0) }
                        common["synergyPlants"] = raises.toString()
                        common["realizableRaises"] = minOf(raises, headroom).toString()
                        common["dieHeadroom"] = headroom.toString()
                    }
                    "Flower_11_02" -> {
                        val raises = context.self.board.creature.count { it.type.name == "VINE" || it.type.name == "FLOWER" }
                        val headroom = context.self.board.hand.sumOf { (it.sides - it.value).coerceAtLeast(0) }
                        common["synergyPlants"] = raises.toString()
                        common["realizableRaises"] = minOf(raises, headroom).toString()
                        common["dieHeadroom"] = headroom.toString()
                    }
                    "Flower_14_01" -> {
                        common["bees"] = context.self.board.bees.toString()
                        common["beeValue"] = context.self.board.beeValue.toString()
                    }
                    "Flower_14_04" -> {
                        common["ownedD4"] = ((context.self.board.supply + context.self.board.hand + context.self.board.discard).count { it.sides == 4 } + context.self.board.mulch.count { it.storedDieSides?.value == 4 } + context.self.board.pendingMulch.count { it.storedDieSides?.value == 4 }).toString()
                        val choices = CardScoringHelpers.petalToDie4CultivationChoices(context)
                        choices.forEach { petalChoice ->
                            val score = CardScoringHelpers.petalToDie4BranchScore(context, petalChoice).total
                            when (petalChoice) {
                                dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice.GainD4 -> common["gainD4Branch"] = score.toString()
                                is dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice.TrashD4AndRaiseAll -> {
                                    val prior = common["bestTrashD4Branch"]?.toIntOrNull()
                                    if (prior == null || score > prior) common["bestTrashD4Branch"] = score.toString()
                                }
                            }
                        }
                    }
                    "Flower_11_04" -> {
                        common["handDice"] = context.self.board.hand.size.toString()
                        common["handPower"] = context.self.board.hand.sumOf { it.value }.toString()
                    }
                    "Flower_17_03" -> {
                        common["spentPlants"] = context.self.board.creature.count { !it.isFaceUp }.toString()
                    }
                }
                if (main.card.card.name == "Flower_17_02") {
                    val best = context.self.board.discard.maxByOrNull { die ->
                        val availability = FutureDiceAvailability.forDiscardDie(context, die)
                        die.sides * 2 + (availability.approximateDrawsUntilAvailable - 1).coerceIn(0, 12) +
                            if (availability.battleIsNext && availability.approximateDrawsUntilAvailable > 1) 15 else 0
                    }
                    if (best != null) {
                        val availability = FutureDiceAvailability.forDiscardDie(context, best)
                        val quality = DicePoolQuality.observe(context)
                        common["targetDieSides"] = best.sides.toString()
                        common["recycleDistance"] = availability.approximateDrawsUntilAvailable.toString()
                        common["battleNext"] = availability.battleIsNext.toString()
                        common["upcomingDiceQuality"] = quality.nextSupplySides.take(3).joinToString("/").ifEmpty { "none" }
                        common["poolSize"] = quality.ownedDiceCount.toString()
                    }
                }
            }
            CultivationMainAction.RoundEffect1, CultivationMainAction.RoundEffect2 -> {
                val effect = if (main == CultivationMainAction.RoundEffect1) request.roundCard.firstEffect.effect else request.roundCard.secondEffect.effect
                common["effect"] = effect.name
                if (effect == GameEffect.MULCH_DIE_FROM_HAND) {
                    val power = policy.normalPurchasingPower(context)
                    val target = MulchPriority.preferredTarget(context, power)
                    if (target != null) {
                        val timing = MulchPriority.timingObservation(context, target)
                        common["targetDieSides"] = target.sides.toString()
                        common["targetDieValue"] = target.value.toString()
                        common["battleNext"] = timing.battleIsNext.toString()
                        common["upcomingDiceQuality"] = timing.upcomingSupplySides.take(3).joinToString("/").ifEmpty { "none" }
                        common["recycleDistance"] = timing.approximateNaturalRecycleDraws.toString()
                        common["preparedMulch"] = timing.preparedMulchCount.toString()
                    }
                }
                if (effect == GameEffect.GAIN_SUNLIGHT_TOKEN) {
                    val sunlight = SunlightTokenPriority.observe(context, cardScorers)
                    common["sunlightHeld"] = sunlight.sunlightHeld.toString()
                    common["battlesRemaining"] = sunlight.battlesRemaining.toString()
                    common["roundsUntilBattle"] = sunlight.roundsUntilBattle?.toString() ?: "none"
                    common["battleNext"] = sunlight.battleIsNext.toString()
                    common["sunlightBaseFutureAction"] = sunlight.baseFutureActionValue.toString()
                    common["sunlightBattleProximityAdjustment"] = sunlight.battleProximityAdjustment.toString()
                    common["sunlightBattlesRemainingAdjustment"] = sunlight.battlesRemainingAdjustment.toString()
                    common["sunlightExistingHoldingAdjustment"] = sunlight.existingSunlightAdjustment.toString()
                    common["sunlightVisibleBattlePlantAdjustment"] = sunlight.visibleBattlePlantAdjustment.toString()
                    common["sunlightThirdBattlePlantBase"] = sunlight.thirdBestFaceUpBattlePlantBase?.toString() ?: "none"
                    common["sunlightPriority"] = sunlight.finalPriority.toString()
                }
            }
            else -> Unit
        }
        return common
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
            GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS -> scoreCompostKeep(context, withRewards = false)
            GameEffect.UPGRADE_DIE_AND_USE_NOW -> scoreCompostKeep(context, withRewards = true)
            GameEffect.MULCH_DIE_FROM_HAND -> MulchPriority.score(
                context = context,
                normalPurchasingPower = policy.normalPurchasingPower(context)
            )
            GameEffect.GAIN_WATER_TOKEN -> WaterPriority.score(context)
            GameEffect.GAIN_SUNLIGHT_TOKEN -> SunlightTokenPriority.score(context, cardScorers)
            GameEffect.RAISE_DIE_PLUS_3 -> SunlightPriority.score(
                context = context,
                normalPurchasingPower = policy.normalPurchasingPower(context)
            )
            else -> PriorityScore(45)
        }

    /** Keep variants retain an immediate rolled replacement instead of losing the die.
     *  This is an expected-value heuristic; no game RNG or future roll is sampled here.
     */
    private fun scoreCompostKeep(context: DecisionContext, withRewards: Boolean): PriorityScore {
        val base = CompostPriority.score(
            context, policy.normalPurchasingPower(context), policy.cultivationDiceDevelopmentBonus(context)
        )
        val target = CompostPriority.bestTarget(context, policy.normalPurchasingPower(context))
            ?: return base
        val current = target.die.sides
        val newSides = when (current) {
            4 -> 6; 6 -> 8; 8 -> 10; 10 -> 12; 12 -> 20; else -> current
        }
        // Restore purchasing thresholds sacrificed by the Discard variant and
        // estimate value of a freshly rolled upgrade relative to losing the old roll.
        val discardTarget = target.score
        val purchasePenalty = discardTarget.adjustments
            .firstOrNull { it.reason == "Current-round Buy threshold impact" }?.amount ?: 0
        val lowPowerPenalty = discardTarget.adjustments
            .firstOrNull { it.reason.startsWith("Preserve at least") }?.amount ?: 0
        val expectedNewRoll = (newSides + 1) / 2.0
        val priorCurrentRoll = target.die.value
        return base
            .adjusted(-purchasePenalty - lowPowerPenalty, "Keeping replacement avoids discarding current die")
            .adjusted(((expectedNewRoll - priorCurrentRoll) * 2).roundToInt(), "Expected immediate upgraded roll versus current face")
            .adjusted(if (withRewards) 5 else 0, "Expected Roll Rewards for keep-with-rewards")
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
