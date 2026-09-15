package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.baseline.scoring.ScoredChoice
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
    internal val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry()
) : EffectStrategy {

    override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDie(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            ScoredChoice(choice, CardScoringHelpers.scoreDieTarget(request.effect, request.context, choice))
        })
    }

    override fun chooseBattleDie(request: ChooseEffectBattleDieRequest): EffectBattleDieChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseBattleDie(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val own = choice.ownerId == request.context.self.id
            val swing = if (own) {
                DieValueHeuristics.expectedRerollGain(choice.die.sides, choice.die.value)
            } else {
                -DieValueHeuristics.expectedRerollGain(choice.die.sides, choice.die.value)
            }
            var score = PriorityScore(50 + (swing * 4).roundToInt())
            if (own) score = score.adjusted(CardScoringHelpers.rowNeedBonus(request.context, choice.row), "Own needy row")
            else score = score.adjusted(choice.die.value, "Disrupt a strong opposing die")
            ScoredChoice(choice, score)
        })
    }

    override fun chooseRootWellBattle(request: ChooseRootWellBattleRequest): RootWellBattleChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseRootWellBattle(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
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
            ScoredChoice(choice, score)
        })
    }

    override fun chooseCrossPlayerDieSwap(request: ChooseEffectCrossPlayerDieSwapRequest): EffectCrossPlayerDieSwapChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCrossPlayerDieSwap(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val gain = choice.opponentDie.die.value - choice.ownDie.die.value
            var score = PriorityScore(40 + gain * 8)
            score = score.adjusted(CardScoringHelpers.rowNeedBonus(request.context, choice.ownDie.row), "Own row need")
            ScoredChoice(choice, score)
        })
    }

    override fun chooseOptionalDie(request: ChooseOptionalEffectDieRequest): EffectDieChoice? {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOptionalDie(request)
        val choices = request.legalChoices.map { choice ->
            val score = when (request.effect) {
                GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN -> {
                    val protected = choice.value - DieValueHeuristics.expectedRoll(choice.sides)
                    PriorityScore(50 + (protected * 5).roundToInt())
                }
                else -> CardScoringHelpers.scoreDieTarget(request.effect, request.context, choice)
            }
            ScoredChoice<EffectDieChoice?>(choice, score)
        } + ScoredChoice<EffectDieChoice?>(null, PriorityScore(50))
        return scoreEngine.chooseValue(choices)
    }

    override fun chooseDice(request: ChooseEffectDiceRequest): EffectDiceChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDice(request)
        val ranked = request.legalChoices
            .map { it to DieValueHeuristics.expectedRerollGain(it.sides, it.value) }
            .sortedByDescending { it.second }
        val positive = ranked.filter { it.second > 0.5 }.map { it.first }.take(request.maxChoices)
        val selected = if (positive.size >= request.minChoices) positive
        else ranked.take(request.minChoices).map { it.first }
        return EffectDiceChoice(selected)
    }

    override fun chooseDiePair(request: ChooseEffectDiePairRequest): EffectDiePairChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseDiePair(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            ScoredChoice(choice, scorePair(request.effect, request.context, choice))
        })
    }

    override fun chooseOptionalDiePair(request: ChooseOptionalEffectDiePairRequest): EffectDiePairChoice? {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOptionalDiePair(request)
        val scored = request.legalChoices.map { choice ->
            ScoredChoice<EffectDiePairChoice?>(choice, scorePair(request.effect, request.context, choice))
        } + ScoredChoice<EffectDiePairChoice?>(null, PriorityScore(55))
        return scoreEngine.chooseValue(scored)
    }

    override fun chooseCritterAndDie(request: ChooseEffectCritterDieRequest): EffectCritterDieChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseCritterAndDie(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val gain = DieValueHeuristics.actualRaiseGain(choice.die.sides, choice.die.value, 5)
            val count = if (choice.critter == Critter.BEE) request.context.self.board.bees else request.context.self.board.worms
            val reservePenalty = if (count <= 2) -25 else if (count == 3) -10 else 0
            ScoredChoice(choice, PriorityScore(45 + gain * 4 + reservePenalty))
        })
    }

    override fun choosePetalToDie4(request: ChoosePetalToDie4Request): PetalToDie4Choice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePetalToDie4(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val score = when (choice) {
                PetalToDie4Choice.GainD4 -> PriorityScore(55).adjusted(8, "Reliable D4=4 added to Hand")
                is PetalToDie4Choice.TrashD4AndRaiseAll -> {
                    val gain = request.context.self.board.hand
                        .filterNot { it.index == choice.die.index }
                        .sumOf { DieValueHeuristics.actualRaiseGain(it.sides, it.value, 4) }
                    PriorityScore(45 + gain * 3).adjusted(-8, "Trashes one D4")
                }
            }
            ScoredChoice(choice, score)
        })
    }

    override fun chooseBeeSource(request: ChooseBeeSourceRequest): EffectBeeSourceChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseBeeSource(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val score = when (choice) {
                EffectBeeSourceChoice.Grove -> PriorityScore(50)
                is EffectBeeSourceChoice.Opponent -> PriorityScore(62).adjusted(8, "Steal also denies an opponent a Bee")
            }
            ScoredChoice(choice, score)
        })
    }

    override fun chooseButterflyTarget(request: ChooseEffectButterflyTargetRequest): EffectButterflyTargetChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseButterflyTarget(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val owner = request.context.opponents.firstOrNull { it.id == choice.ownerId }
            val faceUp = owner?.board?.butterflies?.firstOrNull { it.butterfly == choice.butterfly }?.isFaceUp == true
            ScoredChoice(choice, PriorityScore(if (faceUp) 70 else 55))
        })
    }

    override fun chooseOptionalPlant(request: ChooseOptionalEffectPlantRequest): EffectPlantChoice? {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOptionalPlant(request)
        val scored = request.legalChoices.map { choice ->
            val view = request.context.self.board.creature.firstOrNull { it.id == choice.cardId }
            val value = view?.let { cardScorers.forPlant(it).lossValue(request.context, it) } ?: 30
            val score = if (!choice.isFaceUp) PriorityScore(45 + value / 3) else PriorityScore(15 - value / 5)
            ScoredChoice<EffectPlantChoice?>(choice, score)
        } + ScoredChoice<EffectPlantChoice?>(null, PriorityScore(40))
        return scoreEngine.chooseValue(scored)
    }

    override fun chooseOpponentPlantWound(request: ChooseEffectOpponentPlantWoundRequest): EffectOpponentPlantWoundChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOpponentPlantWound(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val owner = request.context.opponents.firstOrNull { it.id == choice.ownerId }
            val view = owner?.board?.creature?.firstOrNull { it.id == choice.cardId }
            val value = if (view != null) {
                (cardScorers.findByName(view.name)?.lossValue(request.context, view) ?: (view.cost * 4))
            } else 0
            val snipBonus = if (choice is EffectOpponentPlantWoundChoice.Snip) 20 else 0
            ScoredChoice(choice, PriorityScore(40 + value + snipBonus))
        })
    }

    override fun choosePlantEffect(request: ChooseEffectPlantRequest): EffectPlantChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.choosePlantEffect(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
            val scorer = cardScorers.forName(choice.cardName)
            ScoredChoice(
                choice,
                scorer.playScore(request.context, CardPhase.from(request.context.phase), choice.cardName)
            )
        })
    }

    override fun chooseOEdelweiss(request: ChooseOEdelweissRequest): OEdelweissChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseOEdelweiss(request)
        return scoreEngine.chooseValue(request.legalChoices.map { choice ->
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
            ScoredChoice(choice, score)
        })
    }

    override fun chooseWispsToKeep(request: ChooseWispsToKeepRequest): EffectWispsChoice {
        if (request.context == DecisionContext.EMPTY) return delegate.chooseWispsToKeep(request)
        val ranked = request.legalChoices.sortedByDescending { choice ->
            val view = request.context.self.wisps.firstOrNull { it.index == choice.index && it.name == choice.name }
            val scorer = cardScorers.findByName(choice.name)
            val play = scorer?.let { max(it.cultivationPlayBase, it.battlePlayBase) } ?: 40
            (view?.endGameVp ?: 0) * 30 + play
        }
        return EffectWispsChoice(ranked.take(request.keepLimit))
    }

    override fun chooseDieSize(request: ChooseEffectDieSizeRequest) =
        if (request.context == DecisionContext.EMPTY) delegate.chooseDieSize(request)
        else request.legalChoices.maxBy { it.value }

    override fun choosePlayer(request: ChooseEffectPlayerRequest) =
        if (request.context == DecisionContext.EMPTY) delegate.choosePlayer(request)
        else scoreEngine.chooseValue(request.legalChoices.map { playerId ->
            val target = request.context.opponents.firstOrNull { it.id == playerId }
            ScoredChoice(playerId, PriorityScore(40 + (target?.board?.vp ?: 0) + (target?.wispCount ?: 0) * 4))
        })

    override fun chooseStrikeRow(request: ChooseEffectStrikeRowRequest) =
        if (request.context == DecisionContext.EMPTY) delegate.chooseStrikeRow(request)
        else scoreEngine.chooseValue(request.legalChoices.map { row ->
            ScoredChoice(row, PriorityScore(40 + CardScoringHelpers.rowNeedBonus(request.context, row)))
        })

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
            else -> PriorityScore(50)
        }
    }

    private fun rowFor(context: DecisionContext, handIndex: Int) =
        context.battle?.rows?.firstOrNull { row ->
            row.forPlayer(context.self.id)?.dice?.any { it.handIndex == handIndex } == true
        }?.row
}
