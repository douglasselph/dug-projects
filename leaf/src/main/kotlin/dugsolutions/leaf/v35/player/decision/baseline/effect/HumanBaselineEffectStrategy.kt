package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.HumanBaselinePolicy
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleAnalysisMode
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleBeeSourceAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleGustOfPetalsStrikeRowAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleGustOfPetalsTargetAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleImmediateStrikeResolveEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDiePlacementAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleDeterministicStrikeRowTargetAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleOwnDieCollateralAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleOwnDieSwapPairAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleOwnTotalChangeAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleEnabledPlantAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleOpponentPlantTargetAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleOEdelweissAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattlePetalToDie4Analyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattlePollenTheftEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleRootWellTargetAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleSetDieToMatchAnalyzer
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleTwoStepUpgradeEvaluator
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.CompostPriority
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.MulchPriority
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.resource.SunlightPriority
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluenceRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionCandidate
import dugsolutions.leaf.v35.player.decision.baseline.scoring.DecisionTag
import dugsolutions.leaf.v35.player.decision.baseline.scoring.LegalChoiceCombinations
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.effect.*
import dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy
import dugsolutions.leaf.v35.tokens.Critter
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Contextual target/branch selection for Plant, Wisp, and Round Effects in Human Baseline.
 *
 * Cultivation target choices deliberately share valuation with the top-level
 * action scorers. Compost, Mulch, and Sunlight delegate to their Round Effect
 * target scorers, and other die-targeting Cultivation effects receive the same
 * policy-defined normal purchasing power used when their card activation was
 * valued. Petal To Die 4 preserves the same Cultivation branch scorer and, in
 * Battle, shares complete branch-realization analysis with its activation value.
 *
 * This prevents the strategy from choosing an action because one target or
 * branch makes it attractive and then realizing a different, weaker result.
 * See `doc/HUMAN_BASELINE_CULTIVATION_PLAN.md`, section 6.
 */
class HumanBaselineEffectStrategy(
    private val delegate: EffectStrategy = MechanicalEffectStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers),
    internal val policy: HumanBaselinePolicy = HumanBaselinePolicy(),
    internal val pollenTheftEvaluator: BattlePollenTheftEvaluator = BattlePollenTheftEvaluator(policy),
    internal val rootWellTargetAnalyzer: BattleRootWellTargetAnalyzer = BattleRootWellTargetAnalyzer(policy),
    internal val immediateStrikeResolveEvaluator: BattleImmediateStrikeResolveEvaluator =
        BattleImmediateStrikeResolveEvaluator(policy),
    internal val twoStepUpgradeEvaluator: BattleTwoStepUpgradeEvaluator = BattleTwoStepUpgradeEvaluator(policy),
    internal val ownTotalChangeAnalyzer: BattleOwnTotalChangeAnalyzer = BattleOwnTotalChangeAnalyzer(policy),
    internal val ownDieCollateralAnalyzer: BattleOwnDieCollateralAnalyzer = BattleOwnDieCollateralAnalyzer(policy),
    internal val ownDieSwapPairAnalyzer: BattleOwnDieSwapPairAnalyzer = BattleOwnDieSwapPairAnalyzer(policy),
    internal val gustOfPetalsTargetAnalyzer: BattleGustOfPetalsTargetAnalyzer =
        BattleGustOfPetalsTargetAnalyzer(policy),
    internal val gustOfPetalsStrikeRowAnalyzer: BattleGustOfPetalsStrikeRowAnalyzer =
        BattleGustOfPetalsStrikeRowAnalyzer(policy),
    internal val setDieToMatchAnalyzer: BattleSetDieToMatchAnalyzer =
        BattleSetDieToMatchAnalyzer(policy),
    internal val diePlacementAnalyzer: BattleDiePlacementAnalyzer = BattleDiePlacementAnalyzer(policy),
    internal val deterministicStrikeRowTargetAnalyzer: BattleDeterministicStrikeRowTargetAnalyzer =
        BattleDeterministicStrikeRowTargetAnalyzer(policy),
    internal val enabledPlantAnalyzer: BattleEnabledPlantAnalyzer =
        BattleEnabledPlantAnalyzer(cardScorers, policy),
    internal val opponentPlantTargetAnalyzer: BattleOpponentPlantTargetAnalyzer =
        BattleOpponentPlantTargetAnalyzer(cardScorers, enabledPlantAnalyzer),
    internal val beeSourceAnalyzer: BattleBeeSourceAnalyzer = BattleBeeSourceAnalyzer(),
    internal val petalToDie4Analyzer: BattlePetalToDie4Analyzer = BattlePetalToDie4Analyzer(policy),
    internal val oEdelweissAnalyzer: BattleOEdelweissAnalyzer =
        BattleOEdelweissAnalyzer(cardScorers, enabledPlantAnalyzer)
) : EffectStrategy {

    override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDie(request)
        if (request.effect == GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW) {
            val evaluations = twoStepUpgradeEvaluator.evaluateAll(request.context, request.legalChoices)
            if (evaluations.isNotEmpty()) {
                val bestSize = evaluations.maxOf { it.resultingSides.value }
                val finalists = evaluations.filter { it.resultingSides.value == bestSize }
                return choose(
                    request.context,
                    finalists.map { evaluation ->
                        DecisionCandidate(
                            evaluation.choice,
                            PriorityScore(evaluation.battleAnalysis?.tacticalValue?.roundToInt() ?: 0)
                                .adjusted(bestSize, "Maximize Overgrowth resulting die size")
                        )
                    }
                )
            }
        }
        val normalPower = policy.normalPurchasingPower(request.context)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val die = dugsolutions.leaf.v35.player.decision.context.DieView(
                    index = choice.index,
                    sides = choice.sides,
                    value = choice.value
                )
                val score = when {
                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect in COLLATERAL_OWN_DIE_BATTLE_TRANSFORMS ->
                        battleCollateralOwnDieTargetScore(request.context, request.effect, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect == GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW ->
                        battleGustOfPetalsTargetScore(request.context, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect == GameEffect.UPGRADE_DIE_AND_USE_NOW ->
                        battleOneStepUpgradeUseNowTargetScore(request.context, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect in SIMPLE_REROLL_OWN_DIE_BATTLE_EFFECTS ->
                        battleSimpleRerollOwnDieTargetScore(request.context, request.effect, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect in DISCARD_DRAW_SOURCE_BATTLE_EFFECTS ->
                        battleDiscardDrawSourceTargetScore(request.context, request.effect, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect == GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND ->
                        battleRollDiscardIntoBattleTargetScore(request.context, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect in SECONDARY_OWN_DIE_BATTLE_TRANSFORMS ->
                        battleSecondaryOwnDieTargetScore(request.context, request.effect, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                        request.effect in DETERMINISTIC_OWN_DIE_BATTLE_TRANSFORMS ->
                        battleDeterministicOwnDieTargetScore(request.context, request.effect, choice)
                            ?: CardScoringHelpers.scoreDieTarget(
                                effect = request.effect,
                                context = request.context,
                                die = choice,
                                normalPurchasingPower = normalPower
                            )

                    request.effect == GameEffect.UPGRADE_DIE_FROM_HAND ->
                        CompostPriority.targetScore(request.context, die, normalPower)
                            ?: PriorityScore(0).adjusted(-1000, "Not a legal normal Compost upgrade target")

                    request.effect == GameEffect.MULCH_DIE_FROM_HAND ->
                        MulchPriority.targetScore(request.context, die, normalPower)

                    request.effect == GameEffect.RAISE_DIE_PLUS_3 ->
                        SunlightPriority.targetScore(request.context, die, normalPower)

                    else ->
                        CardScoringHelpers.scoreDieTarget(
                            effect = request.effect,
                            context = request.context,
                            die = choice,
                            normalPurchasingPower = normalPower
                        )
                }
                DecisionCandidate(choice, score)
            }
        )
    }

    override fun chooseBattleDie(request: ChooseEffectBattleDieRequest): EffectBattleDieChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseBattleDie(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val own = choice.ownerId == request.context.self.id
                val swing = if (own) {
                    DieValueHeuristics.expectedRerollGain(choice.die.sides, choice.die.value)
                } else {
                    -DieValueHeuristics.expectedRerollGain(choice.die.sides, choice.die.value)
                }
                var score = PriorityScore(50 + (swing * 4).roundToInt())
                if (own) score = score.adjusted(CardScoringHelpers.rowNeedBonus(request.context, choice.row), "Own needy row")
                else score = score.adjusted(choice.die.value, "Disrupt a strong opposing die")
                DecisionCandidate(choice, score)
            }
        )
    }

    override fun chooseRootWellBattle(request: ChooseRootWellBattleRequest): RootWellBattleChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseRootWellBattle(request)
        val analyzed = request.legalChoices.mapNotNull { choice ->
            rootWellTargetAnalyzer(request.context, choice)?.let { analysis ->
                DecisionCandidate(
                    choice,
                    PriorityScore(analysis.tacticalValue.roundToInt())
                        .adjusted(0, "Complete expected Root Well Battle realization"),
                    setOf(DecisionTag.SPEND_WATER)
                )
            }
        }
        if (analyzed.isEmpty()) return delegate.chooseRootWellBattle(request)
        return choose(request.context, analyzed)
    }

    override fun chooseCrossPlayerDieSwap(request: ChooseEffectCrossPlayerDieSwapRequest): EffectCrossPlayerDieSwapChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCrossPlayerDieSwap(request)
        val evaluations = pollenTheftEvaluator.evaluateAll(request.context, request.legalChoices)
        if (evaluations.isEmpty()) return delegate.chooseCrossPlayerDieSwap(request)
        return choose(
            request.context,
            evaluations.map { evaluation ->
                DecisionCandidate(
                    evaluation.choice,
                    PriorityScore(evaluation.analysis.tacticalValue.roundToInt())
                        .adjusted(0, "Complete Pollen Theft Battle Swing")
                )
            }
        )
    }

    override fun chooseOptionalDie(request: ChooseOptionalEffectDieRequest): EffectDieChoice? {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOptionalDie(request)
        val candidates = request.legalChoices.map { choice ->
            val score = when (request.effect) {
                GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN -> {
                    val protected = choice.value - DieValueHeuristics.expectedRoll(choice.sides)
                    PriorityScore(50 + (protected * 5).roundToInt())
                }
                else -> CardScoringHelpers.scoreDieTarget(
                    effect = request.effect,
                    context = request.context,
                    die = choice,
                    normalPurchasingPower = policy.normalPurchasingPower(request.context)
                )
            }
            DecisionCandidate<EffectDieChoice?>(choice, score)
        } + DecisionCandidate<EffectDieChoice?>(null, PriorityScore(50))
        return choose(request.context, candidates)
    }

    override fun chooseDice(request: ChooseEffectDiceRequest): EffectDiceChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDice(request)
        val candidates = LegalChoiceCombinations.between(
            choices = request.legalChoices,
            minSize = request.minChoices,
            maxSize = request.maxChoices
        ).map { dice ->
            val choice = EffectDiceChoice(dice)
            DecisionCandidate(choice, scoreDiceSet(request.effect, request.context, choice))
        }
        return choose(request.context, candidates)
    }

    override fun chooseDiePair(request: ChooseEffectDiePairRequest): EffectDiePairChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDiePair(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                DecisionCandidate(choice, scorePair(request.effect, request.context, choice))
            }
        )
    }

    override fun chooseOptionalDiePair(request: ChooseOptionalEffectDiePairRequest): EffectDiePairChoice? {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOptionalDiePair(request)
        val candidates = request.legalChoices.map { choice ->
            DecisionCandidate<EffectDiePairChoice?>(choice, scorePair(request.effect, request.context, choice))
        } + DecisionCandidate<EffectDiePairChoice?>(null, PriorityScore(55))
        return choose(request.context, candidates)
    }

    override fun chooseCritterAndDie(request: ChooseEffectCritterDieRequest): EffectCritterDieChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCritterAndDie(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val gain = DieValueHeuristics.actualRaiseGain(choice.die.sides, choice.die.value, 5)
                val count = if (choice.critter == Critter.BEE) {
                    request.context.self.board.bees
                } else {
                    request.context.self.board.worms
                }
                val reservePenalty = if (count <= 2) -25 else if (count == 3) -10 else 0
                val baseScore = if (
                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                    request.effect == GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5
                ) {
                    val row = rowFor(request.context, choice.die.index)
                    val analysis = row?.let {
                        ownTotalChangeAnalyzer(
                            context = request.context,
                            realization = choice,
                            row = it,
                            change = gain.toDouble(),
                            mode = BattleAnalysisMode.DETERMINISTIC
                        )
                    }
                    analysis?.tacticalValue?.roundToInt() ?: (45 + gain * 4)
                } else {
                    45 + gain * 4
                }
                val critterLabel = if (choice.critter == Critter.BEE) "Bee" else "Worm"
                DecisionCandidate(
                    choice = choice,
                    score = PriorityScore(baseScore).adjusted(
                        reservePenalty,
                        "Existing $critterLabel availability preference"
                    ),
                    tags = setOf(if (choice.critter == Critter.BEE) DecisionTag.SPEND_BEE else DecisionTag.SPEND_WORM)
                )
            }
        )
    }

    override fun choosePetalToDie4(request: ChoosePetalToDie4Request): PetalToDie4Choice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePetalToDie4(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val score = if (
                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE
                ) {
                    petalToDie4Analyzer(request.context, choice)
                        ?.let { analysis ->
                            PriorityScore(analysis.tacticalValue.roundToInt())
                                .adjusted(
                                    0,
                                    "Complete immediate Battle realization for Petal To Die 4 branch"
                                )
                        }
                        ?: CardScoringHelpers.petalToDie4BranchScore(request.context, choice)
                } else {
                    CardScoringHelpers.petalToDie4BranchScore(request.context, choice)
                }
                DecisionCandidate(choice, score)
            }
        )
    }

    override fun chooseBeeSource(request: ChooseBeeSourceRequest): EffectBeeSourceChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseBeeSource(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val score = when (choice) {
                    EffectBeeSourceChoice.Grove -> PriorityScore(50)
                    is EffectBeeSourceChoice.Opponent -> {
                        var score = PriorityScore(62).adjusted(8, "Steal also denies an opponent a Bee")
                        if (request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE) {
                            val denial = beeSourceAnalyzer(request.context, choice.playerId).roundToInt()
                            if (denial > 0) {
                                score = score.adjusted(
                                    denial,
                                    "Steal the Bee whose remaining Battle Support is most tactically dangerous"
                                )
                            }
                        }
                        score
                    }
                }
                DecisionCandidate(choice, score, setOf(DecisionTag.ACQUIRE_BEE))
            }
        )
    }

    override fun chooseButterflyTarget(request: ChooseEffectButterflyTargetRequest): EffectButterflyTargetChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseButterflyTarget(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                if (choice.ownerId == null) {
                    DecisionCandidate(choice, PriorityScore(58))
                } else {
                    val owner = request.context.opponents.firstOrNull { it.id == choice.ownerId }
                    val faceUp = owner?.board?.butterflies
                        ?.firstOrNull { it.butterfly == choice.butterfly }
                        ?.isFaceUp == true
                    DecisionCandidate(choice, PriorityScore(if (faceUp) 70 else 62))
                }
            }
        )
    }

    override fun chooseOptionalPlant(request: ChooseOptionalEffectPlantRequest): EffectPlantChoice? {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOptionalPlant(request)
        val candidates = request.legalChoices.map { choice ->
            val view = request.context.self.board.creature.firstOrNull { it.id == choice.cardId }
            val value = view?.let { cardScorers.forPlant(it).lossValue(request.context, it) } ?: 30
            val score = if (!choice.isFaceUp) PriorityScore(45 + value / 3) else PriorityScore(15 - value / 5)
            DecisionCandidate<EffectPlantChoice?>(choice, score)
        } + DecisionCandidate<EffectPlantChoice?>(null, PriorityScore(40))
        return choose(request.context, candidates)
    }

    override fun chooseOpponentPlantWound(request: ChooseEffectOpponentPlantWoundRequest): EffectOpponentPlantWoundChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOpponentPlantWound(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val battleScore = opponentPlantTargetAnalyzer(
                    context = request.context,
                    effect = request.effect,
                    choice = choice
                )
                if (battleScore != null) {
                    DecisionCandidate(choice, battleScore)
                } else {
                    val owner = request.context.opponents.firstOrNull { it.id == choice.ownerId }
                    val view = owner?.board?.creature?.firstOrNull { it.id == choice.cardId }
                    val value = if (view != null) {
                        cardScorers.findByName(view.name)?.lossValue(request.context, view) ?: (view.cost * 4)
                    } else 0
                    val snipBonus = if (choice is EffectOpponentPlantWoundChoice.Snip) 20 else 0
                    DecisionCandidate(choice, PriorityScore(40 + value + snipBonus))
                }
            }
        )
    }

    override fun choosePlantEffect(request: ChooseEffectPlantRequest): EffectPlantChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePlantEffect(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val view = request.context.self.board.creature.firstOrNull {
                    it.id == choice.cardId && it.name == choice.cardName
                }
                val score = if (
                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE &&
                    request.effect == GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT &&
                    view != null
                ) {
                    enabledPlantAnalyzer(request.context, view).priority
                } else {
                    cardScorers.forName(choice.cardName)
                        .playScore(request.context, CardPhase.from(request.context.phase), choice.cardName)
                }
                DecisionCandidate(choice, score)
            }
        )
    }

    override fun chooseOEdelweiss(request: ChooseOEdelweissRequest): OEdelweissChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOEdelweiss(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val score = if (
                    request.context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE
                ) {
                    oEdelweissAnalyzer.scoreChoice(request.context, choice)
                        ?: scoreOEdelweissFallback(request.context, choice)
                } else {
                    scoreOEdelweissFallback(request.context, choice)
                }
                DecisionCandidate(choice, score)
            }
        )
    }

    private fun scoreOEdelweissFallback(
        context: DecisionContext,
        choice: OEdelweissChoice
    ): PriorityScore =
        when (choice) {
            OEdelweissChoice.Done -> PriorityScore(35)
            is OEdelweissChoice.Play -> {
                cardScorers.forName(choice.card.cardName)
                    .playScore(context, CardPhase.from(context.phase), choice.card.cardName)
            }
            is OEdelweissChoice.Flip -> {
                val view = context.self.board.creature.firstOrNull { it.id == choice.card.cardId }
                val value = view?.let { cardScorers.forPlant(it).lossValue(context, it) } ?: 30
                if (!choice.card.isFaceUp) PriorityScore(45 + value / 3) else PriorityScore(10 - value / 5)
            }
        }

    override fun chooseWispsToKeep(request: ChooseWispsToKeepRequest): EffectWispsChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseWispsToKeep(request)
        val candidates = LegalChoiceCombinations.exactly(request.legalChoices, request.keepLimit).map { wisps ->
            val choice = EffectWispsChoice(wisps)
            DecisionCandidate(choice, scoreWispKeepSet(request.context, choice))
        }
        return choose(request.context, candidates)
    }

    override fun chooseDieSize(request: ChooseEffectDieSizeRequest) =
        if (request.context == DecisionContext.EMPTY) delegate.chooseDieSize(request)
        else choose(
            request.context,
            request.legalChoices.map { sides ->
                DecisionCandidate(sides, PriorityScore(40 + sides.value))
            }
        )

    override fun choosePlayer(request: ChooseEffectPlayerRequest) =
        if (request.context == DecisionContext.EMPTY) delegate.choosePlayer(request)
        else choose(
            request.context,
            request.legalChoices.map { playerId ->
                val target = request.context.opponents.firstOrNull { it.id == playerId }
                DecisionCandidate(playerId, PriorityScore(40 + (target?.board?.vp ?: 0) + (target?.wispCount ?: 0) * 4))
            }
        )

    override fun chooseStrikeRow(request: ChooseEffectStrikeRowRequest) =
        if (request.context == DecisionContext.EMPTY) delegate.chooseStrikeRow(request)
        else if (request.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW) {
            val evaluations = immediateStrikeResolveEvaluator.evaluateAll(request.context, request.legalChoices)
            if (evaluations.isEmpty()) delegate.chooseStrikeRow(request)
            else choose(
                request.context,
                evaluations.map { evaluation ->
                    DecisionCandidate(
                        evaluation.row,
                        PriorityScore(if (evaluation.passesPolicy) 10_000 else 0)
                            .adjusted(evaluation.currentStrikeVp, "Immediate Strike VP")
                    )
                }
            )
        } else if (
            request.effect ==
                GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        ) {
            val analyzed = request.legalChoices.mapNotNull { row ->
                gustOfPetalsStrikeRowAnalyzer(request.context, row)?.let { analysis ->
                    DecisionCandidate(
                        row,
                        PriorityScore(analysis.tacticalValue.roundToInt())
                            .adjusted(0, "Expected complete Gust of Petals Strike-row realization")
                    )
                }
            }
            if (analyzed.isEmpty()) delegate.chooseStrikeRow(request)
            else choose(request.context, analyzed)
        } else if (request.effect in DETERMINISTIC_STRIKE_ROW_BATTLE_EFFECTS) {
            val analyzed = request.legalChoices.mapNotNull { row ->
                deterministicStrikeRowTargetAnalyzer(request.context, request.effect, row)?.let { analysis ->
                    DecisionCandidate(
                        row,
                        PriorityScore(analysis.tacticalValue.roundToInt())
                            .adjusted(0, "Complete deterministic Strike-row realization")
                    )
                }
            }
            if (analyzed.isEmpty()) delegate.chooseStrikeRow(request)
            else choose(request.context, analyzed)
        } else choose(
            request.context,
            request.legalChoices.map { row ->
                DecisionCandidate(row, PriorityScore(40 + CardScoringHelpers.rowNeedBonus(request.context, row)))
            }
        )

    private fun scoreDiceSet(
        effect: GameEffect,
        context: DecisionContext,
        choice: EffectDiceChoice
    ): PriorityScore {
        if (choice.selected.isEmpty()) return PriorityScore(50).adjusted(0, "Choose no dice")

        return when (effect) {
            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE -> {
                val expectedGain = choice.selected.sumOf {
                    DieValueHeuristics.expectedRerollGain(it.sides, it.value)
                }
                PriorityScore(50 + (expectedGain * 5).roundToInt())
                    .adjusted(choice.selected.size, "Evaluate the complete redraw subset")
            }
            else -> {
                val targetDelta = choice.selected.sumOf { die ->
                    CardScoringHelpers.scoreDieTarget(effect, context, die).total - 50
                }
                PriorityScore(50 + targetDelta)
                    .adjusted(choice.selected.size, "Evaluate the complete multi-die target set")
            }
        }
    }

    private fun scoreWispKeepSet(
        context: DecisionContext,
        choice: EffectWispsChoice
    ): PriorityScore {
        val total = choice.selected.sumOf { wisp ->
            val view = context.self.wisps.firstOrNull { it.index == wisp.index && it.name == wisp.name }
            val scorer = cardScorers.findByName(wisp.name)
            val play = scorer?.let { max(it.cultivationPlayBase, it.battlePlayBase) } ?: 40
            (view?.endGameVp ?: 0) * 30 + play
        }
        return PriorityScore(total)
            .adjusted(choice.selected.size, "Score the complete Wisp keep set")
    }

    private fun scorePair(effect: GameEffect, context: DecisionContext, choice: EffectDiePairChoice): PriorityScore {
        return when (effect) {
            GameEffect.SET_DIE_TO_MATCH_ANOTHER -> {
                if (context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE) {
                    val analysis = setDieToMatchAnalyzer(
                        context = context,
                        pair = choice,
                        realization = choice
                    )
                    if (analysis != null) {
                        PriorityScore(analysis.tacticalValue.roundToInt())
                            .adjusted(0, "Complete Battle realization for set-die-to-match pair")
                    } else {
                        val gain = minOf(choice.target.sides, choice.source.value) - choice.target.value
                        PriorityScore(50 + gain * 5)
                    }
                } else {
                    val gain = minOf(choice.target.sides, choice.source.value) - choice.target.value
                    PriorityScore(50 + gain * 5)
                }
            }
            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
            GameEffect.DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE -> {
                if (context.phase == dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE) {
                    val analysis = ownDieSwapPairAnalyzer(context, effect, choice)
                    if (analysis != null) {
                        PriorityScore(50 + analysis.tacticalValue.roundToInt())
                            .adjusted(0, "Complete Battle realization for own-die swap pair")
                    } else {
                        PriorityScore(45)
                    }
                } else {
                    when (effect) {
                        GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE -> {
                            val sourceRow = rowFor(context, choice.source.index)
                            val targetRow = rowFor(context, choice.target.index)
                            if (sourceRow == null || targetRow == null || sourceRow == targetRow) return PriorityScore(45)
                            val sourceNeed = CardScoringHelpers.rowNeedBonus(context, sourceRow)
                            val targetNeed = CardScoringHelpers.rowNeedBonus(context, targetRow)
                            val improvement = (sourceNeed - targetNeed) * (choice.target.value - choice.source.value) / 10
                            PriorityScore(50 + improvement)
                        }

                        GameEffect.DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE -> {
                            val sourceRow = rowFor(context, choice.source.index)
                            val targetRow = rowFor(context, choice.target.index)
                            if (sourceRow == null || targetRow == null || sourceRow == targetRow) return PriorityScore(45)
                            val sourceNeed = CardScoringHelpers.rowNeedBonus(context, sourceRow)
                            val targetNeed = CardScoringHelpers.rowNeedBonus(context, targetRow)
                            val swapImprovement = (sourceNeed - targetNeed) * (choice.target.value - choice.source.value) / 10
                            val raiseGain = DieValueHeuristics.actualRaiseGain(choice.source.sides, choice.source.value, 2)
                            PriorityScore(50 + swapImprovement + raiseGain * 4)
                        }

                        else -> PriorityScore(50)
                    }
                }
            }
            else -> PriorityScore(50)
        }
    }

    private companion object {
        val COLLATERAL_OWN_DIE_BATTLE_TRANSFORMS = setOf(
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW
        )

        val SIMPLE_REROLL_OWN_DIE_BATTLE_EFFECTS = setOf(
            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS,
            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE
        )

        val DISCARD_DRAW_SOURCE_BATTLE_EFFECTS = setOf(
            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE,
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO
        )

        val SECONDARY_OWN_DIE_BATTLE_TRANSFORMS = setOf(
            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE,
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE
        )

        val DETERMINISTIC_STRIKE_ROW_BATTLE_EFFECTS = setOf(
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3
        )

        val DETERMINISTIC_OWN_DIE_BATTLE_TRANSFORMS = setOf(
            GameEffect.DOUBLE_ONE_DIE,
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE,
            GameEffect.RAISE_DIE_PLUS_3,
            GameEffect.RAISE_DIE_PLUS_4,
            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE,
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX
        )
    }

    /**
     * Scores deterministic own-die effects whose selected row also changes
     * opposing dice. The dedicated analyzer mirrors the complete engine
     * realization so target choice sees both the actor raise and collateral.
     */
    private fun battleCollateralOwnDieTargetScore(
        context: DecisionContext,
        effect: GameEffect,
        choice: EffectDieChoice
    ): PriorityScore? {
        val analysis = ownDieCollateralAnalyzer(
            context = context,
            effect = effect,
            handIndex = choice.index,
            realization = choice
        ) ?: return null
        return PriorityScore(analysis.tacticalValue.roundToInt())
            .adjusted(0, "Complete immediate Battle realization for collateral own-die transform")
    }


    /**
     * Scores Gust of Petals' first, own-die reroll target using the complete
     * expected immediate Battle realization. The later Strike-Row choice remains
     * a separate post-reroll decision; this valuation uses the best visible
     * expected later row branch without committing it or consuming RNG.
     */
    private fun battleGustOfPetalsTargetScore(
        context: DecisionContext,
        choice: EffectDieChoice
    ): PriorityScore? {
        val analysis = gustOfPetalsTargetAnalyzer(
            context = context,
            choice = choice,
            realization = choice
        ) ?: return null

        return PriorityScore(analysis.tacticalValue.roundToInt())
            .adjusted(0, "Expected complete Battle realization for Gust of Petals own-die target")
    }

    /**
     * Scores Root Awakening's one-step Upgrade target by the expected value of
     * the replacement die on the selected die's existing Strike row. The
     * request already owns normal-step/Graft-Bed legality; strategy analysis
     * only projects the legal target before the replacement is actually rolled.
     */
    private fun battleOneStepUpgradeUseNowTargetScore(
        context: DecisionContext,
        choice: EffectDieChoice
    ): PriorityScore? {
        val expectedChange = DieValueHeuristics.expectedNormalUpgradeUseNowGain(
            sides = choice.sides,
            value = choice.value
        ) ?: return null
        val row = rowFor(context, choice.index) ?: return null
        val analysis = ownTotalChangeAnalyzer(
            context = context,
            realization = choice,
            row = row,
            change = expectedChange,
            mode = BattleAnalysisMode.EXPECTED
        ) ?: return null

        return PriorityScore(analysis.tacticalValue.roundToInt())
            .adjusted(0, "Expected Battle realization for one-step Upgrade-and-use-now target")
    }

    /**
     * Scores simple one-die Battle rerolls by their honest expected change on
     * the die's current Strike row. This is target-before-RNG analysis only:
     * no roll is sampled and the live die remains in its existing row.
     */
    private fun battleSimpleRerollOwnDieTargetScore(
        context: DecisionContext,
        effect: GameEffect,
        choice: EffectDieChoice
    ): PriorityScore? {
        val expectedChange = when (effect) {
            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS ->
                DieValueHeuristics.expectedRerollUntilAtLeastGain(
                    sides = choice.sides,
                    value = choice.value,
                    minimum = 3
                )

            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE ->
                DieValueHeuristics.expectedRerollGain(choice.sides, choice.value)

            else -> return null
        }
        val row = rowFor(context, choice.index) ?: return null
        val analysis = ownTotalChangeAnalyzer(
            context = context,
            realization = choice,
            row = row,
            change = expectedChange,
            mode = BattleAnalysisMode.EXPECTED
        ) ?: return null

        return PriorityScore(analysis.tacticalValue.roundToInt())
            .adjusted(0, "Expected Battle realization for simple own-die reroll target")
    }

    /**
     * Chooses the Discard die for Forget-Me-Not during Battle by the honest
     * expected value of the die after it is rolled and then placed. The source
     * choice happens before that roll, so this deliberately uses mathematical
     * expectation and best currently legal expected placement without consuming
     * mechanical RNG or committing the later actual placement row.
     */
    private fun battleRollDiscardIntoBattleTargetScore(
        context: DecisionContext,
        choice: EffectDieChoice
    ): PriorityScore? {
        val placement = diePlacementAnalyzer(
            context = context,
            dieValue = DieValueHeuristics.expectedRoll(choice.sides),
            mode = BattleAnalysisMode.EXPECTED
        ).maxByOrNull { it.tacticalValue } ?: return null

        return PriorityScore(placement.tacticalValue.roundToInt())
            .adjusted(0, "Expected Battle value after rolling and placing this Discard die")
    }

    /**
     * Scores only the source die that is discarded before one/two Battle Draws.
     *
     * The selected die is still on the Battle Grid, so its row can be projected
     * immediately. Draw values use the exact next physical die sizes visible in
     * Supply/Discard and mathematical expectation; no mechanical RNG is consumed.
     *
     * Transplant Tulip's one replacement is forced back into the discarded row.
     * Reap What You Roll's two-draw forced-row version receives the mean expected
     * replacement because the later post-roll die choice remains a separate fresh
     * decision. Any optional swap or free Battle placement is deliberately not
     * pre-committed here. For the free-placement two-draw effect, source targeting
     * therefore measures the tactical cost of removing the chosen die, with only a
     * small expected-value adjustment for source-dependent future draw sizes.
     */
    private fun battleDiscardDrawSourceTargetScore(
        context: DecisionContext,
        effect: GameEffect,
        choice: EffectDieChoice
    ): PriorityScore? {
        val row = rowFor(context, choice.index) ?: return null
        val drawCount = when (effect) {
            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE -> 1
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE,
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO -> 2
            else -> return null
        }
        val expectedDraws = expectedDrawValuesAfterDiscard(context, choice, drawCount)
        val forcedReplacement = when (effect) {
            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE ->
                expectedDraws.firstOrNull() ?: 0.0

            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE ->
                expectedDraws.takeIf { it.isNotEmpty() }?.average() ?: 0.0

            GameEffect.DISCARD_ONE_DIE_DRAW_TWO -> 0.0
            else -> return null
        }
        val analysis = ownTotalChangeAnalyzer(
            context = context,
            realization = choice,
            row = row,
            change = forcedReplacement - choice.value,
            mode = BattleAnalysisMode.EXPECTED
        ) ?: return null

        var score = PriorityScore(analysis.tacticalValue.roundToInt())
            .adjusted(0, "Expected immediate Battle result of discarding this source die")

        val laterExpectedDrawValue = (expectedDraws.sum() - forcedReplacement).coerceAtLeast(0.0)
        if (laterExpectedDrawValue > 0.0) {
            score = score.adjusted(
                laterExpectedDrawValue.roundToInt(),
                "Expected later Draw value remains for fresh post-roll placement/choice"
            )
        }
        return score
    }

    /**
     * Mirrors PlayerDice.draw() using only public die sizes. The selected source
     * is first moved to Discard, Supply is used until empty, then Discard refills
     * Supply, and each Draw takes the lowest-sided available die. Values are never
     * rolled here; callers receive mathematical expected rolls only.
     */
    private fun expectedDrawValuesAfterDiscard(
        context: DecisionContext,
        choice: EffectDieChoice,
        count: Int
    ): List<Double> {
        val supply = context.self.board.supply.map { it.sides }.toMutableList()
        val discard = context.self.board.discard.map { it.sides }.toMutableList().apply {
            add(choice.sides)
        }
        return buildList {
            repeat(count) {
                if (supply.isEmpty()) {
                    supply += discard
                    discard.clear()
                }
                val sides = supply.minOrNull() ?: return@repeat
                supply.remove(sides)
                add(DieValueHeuristics.expectedRoll(sides))
            }
        }
    }

    /**
     * Scores the initial die target for effects whose +1 Raise is followed by a
     * separate automatic/branching consequence. The selected row is evaluated
     * tactically now, while later legal placement/withdrawal decisions remain
     * separate fresh decisions. Bursting Blossom also receives the same
     * target-specific "create another maximum die" value used by its card
     * scorer; existing maximum-die Draws are target-invariant.
     */
    private fun battleSecondaryOwnDieTargetScore(
        context: DecisionContext,
        effect: GameEffect,
        choice: EffectDieChoice
    ): PriorityScore? {
        val row = rowFor(context, choice.index) ?: return null
        val gain = DieValueHeuristics.actualRaiseGain(choice.sides, choice.value, 1)
        val analysis = ownTotalChangeAnalyzer(
            context = context,
            realization = choice,
            row = row,
            change = gain.toDouble(),
            mode = BattleAnalysisMode.DETERMINISTIC
        ) ?: return null

        var score = PriorityScore(analysis.tacticalValue.roundToInt())
            .adjusted(0, "Immediate Battle realization for secondary-consequence +1 target")

        if (
            effect == GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE &&
            choice.value == choice.sides - 1 &&
            (context.self.board.supply.isNotEmpty() || context.self.board.discard.isNotEmpty())
        ) {
            score = score.adjusted(18, "Selected +1 creates an additional maximum die and Draw")
        }

        return score
    }

    /**
     * Aligns downstream targets for straightforward deterministic own-die value
     * transforms with the same immediate affected-row Battle realization used by
     * top-level Battle analysis. Numeric value change alone is not enough: a
     * smaller change that flips a Strike can beat a larger change elsewhere.
     */
    private fun battleDeterministicOwnDieTargetScore(
        context: DecisionContext,
        effect: GameEffect,
        choice: EffectDieChoice
    ): PriorityScore? {
        val change = when (effect) {
            GameEffect.DOUBLE_ONE_DIE -> minOf(choice.sides, choice.value * 2) - choice.value
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE ->
                DieValueHeuristics.flipGain(choice.sides, choice.value)
            GameEffect.RAISE_ANY_DIE_PLUS_1 -> DieValueHeuristics.actualRaiseGain(choice.sides, choice.value, 1)
            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER ->
                DieValueHeuristics.actualRaiseGain(
                    choice.sides,
                    choice.value,
                    context.self.board.creature.count {
                        it.type == PlantType.VINE || it.type == PlantType.FLOWER
                    }
                )
            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE ->
                DieValueHeuristics.actualRaiseGain(
                    choice.sides,
                    choice.value,
                    context.self.board.creature.count {
                        it.type == PlantType.ROOT || it.type == PlantType.VINE
                    }
                )
            GameEffect.RAISE_DIE_PLUS_3 -> DieValueHeuristics.actualRaiseGain(choice.sides, choice.value, 3)
            GameEffect.RAISE_DIE_PLUS_4 -> DieValueHeuristics.actualRaiseGain(choice.sides, choice.value, 4)
            // Every legal Down Payment target becomes one showing 1, so the VP reward
            // is identical across targets. The target decision therefore compares only
            // the target-dependent Battle cost of reducing this die to 1.
            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE -> 1 - choice.value
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX ->
                DieValueHeuristics.setToMaximumGain(choice.sides, choice.value)
            else -> return null
        }
        val row = rowFor(context, choice.index) ?: return null
        val analysis = ownTotalChangeAnalyzer(
            context = context,
            realization = choice,
            row = row,
            change = change.toDouble(),
            mode = BattleAnalysisMode.DETERMINISTIC
        ) ?: return null
        return PriorityScore(analysis.tacticalValue.roundToInt())
            .adjusted(0, "Immediate Battle realization for deterministic own-die transform")
    }

    private fun rowFor(context: DecisionContext, handIndex: Int) =
        context.battle?.rows?.firstOrNull { row ->
            row.forPlayer(context.self.id)?.dice?.any { it.handIndex == handIndex } == true
        }?.row

    private fun <T> choose(
        context: DecisionContext,
        candidates: List<DecisionCandidate<T>>
    ): T = scoreEngine.chooseValue(
        context = context,
        candidates = candidates,
        influenceRegistry = influenceRegistry
    )
}
