package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
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

/** Contextual target selection for Plant/Wisp effects in Human Baseline. */
class HumanBaselineEffectStrategy(
    private val delegate: EffectStrategy = MechanicalEffectStrategy(),
    internal val scoreEngine: BaselineScoreEngine = BaselineScoreEngine(),
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    internal val influenceRegistry: BaselineInfluenceRegistry = BaselineInfluenceRegistry(cardScorers)
) : EffectStrategy {

    override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDie(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                DecisionCandidate(choice, CardScoringHelpers.scoreDieTarget(request.effect, request.context, choice))
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
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val score = when (choice) {
                    is RootWellBattleChoice.OwnDice -> {
                        val gain = choice.dice.sumOf { DieValueHeuristics.expectedRerollGain(it.die.sides, it.die.value) }
                        val need = choice.dice.sumOf { CardScoringHelpers.rowNeedBonus(request.context, it.row) }
                        PriorityScore(45 + (gain * 4).roundToInt() + need / 2)
                    }
                    is RootWellBattleChoice.OpponentDie -> {
                        val loss = -DieValueHeuristics.expectedRerollGain(choice.die.die.sides, choice.die.die.value)
                        PriorityScore(45 + (loss * 5).roundToInt() + choice.die.die.value)
                    }
                }
                DecisionCandidate(choice, score, setOf(DecisionTag.SPEND_WATER))
            }
        )
    }

    override fun chooseCrossPlayerDieSwap(request: ChooseEffectCrossPlayerDieSwapRequest): EffectCrossPlayerDieSwapChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCrossPlayerDieSwap(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val gain = choice.opponentDie.die.value - choice.ownDie.die.value
                var score = PriorityScore(40 + gain * 8)
                score = score.adjusted(CardScoringHelpers.rowNeedBonus(request.context, choice.ownDie.row), "Own row need")
                DecisionCandidate(choice, score)
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
                else -> CardScoringHelpers.scoreDieTarget(request.effect, request.context, choice)
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
                val count = if (choice.critter == Critter.BEE) request.context.self.board.bees else request.context.self.board.worms
                val reservePenalty = if (count <= 2) -25 else if (count == 3) -10 else 0
                DecisionCandidate(
                    choice = choice,
                    score = PriorityScore(45 + gain * 4 + reservePenalty),
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
                val score = when (choice) {
                    PetalToDie4Choice.GainD4 -> PriorityScore(55).adjusted(8, "Reliable D4=4 added to Hand")
                    is PetalToDie4Choice.TrashD4AndRaiseAll -> {
                        val gain = request.context.self.board.hand
                            .filterNot { it.index == choice.die.index }
                            .sumOf { DieValueHeuristics.actualRaiseGain(it.sides, it.value, 4) }
                        PriorityScore(45 + gain * 3).adjusted(-8, "Trashes one D4")
                    }
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
                    is EffectBeeSourceChoice.Opponent -> PriorityScore(62).adjusted(8, "Steal also denies an opponent a Bee")
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
                val owner = request.context.opponents.firstOrNull { it.id == choice.ownerId }
                val view = owner?.board?.creature?.firstOrNull { it.id == choice.cardId }
                val value = if (view != null) {
                    cardScorers.findByName(view.name)?.lossValue(request.context, view) ?: (view.cost * 4)
                } else 0
                val snipBonus = if (choice is EffectOpponentPlantWoundChoice.Snip) 20 else 0
                DecisionCandidate(choice, PriorityScore(40 + value + snipBonus))
            }
        )
    }

    override fun choosePlantEffect(request: ChooseEffectPlantRequest): EffectPlantChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePlantEffect(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val scorer = cardScorers.forName(choice.cardName)
                DecisionCandidate(
                    choice,
                    scorer.playScore(request.context, CardPhase.from(request.context.phase), choice.cardName)
                )
            }
        )
    }

    override fun chooseOEdelweiss(request: ChooseOEdelweissRequest): OEdelweissChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOEdelweiss(request)
        return choose(
            request.context,
            request.legalChoices.map { choice ->
                val score = when (choice) {
                    OEdelweissChoice.Done -> PriorityScore(35)
                    is OEdelweissChoice.Play -> {
                        cardScorers.forName(choice.card.cardName)
                            .playScore(request.context, CardPhase.from(request.context.phase), choice.card.cardName)
                    }
                    is OEdelweissChoice.Flip -> {
                        val view = request.context.self.board.creature.firstOrNull { it.id == choice.card.cardId }
                        val value = view?.let { cardScorers.forPlant(it).lossValue(request.context, it) } ?: 30
                        if (!choice.card.isFaceUp) PriorityScore(45 + value / 3) else PriorityScore(10 - value / 5)
                    }
                }
                DecisionCandidate(choice, score)
            }
        )
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
        else choose(
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
                val gain = minOf(choice.target.sides, choice.source.value) - choice.target.value
                PriorityScore(50 + gain * 5)
            }
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
