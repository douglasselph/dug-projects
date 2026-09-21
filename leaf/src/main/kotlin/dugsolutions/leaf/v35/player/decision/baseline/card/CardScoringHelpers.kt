package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.RowNeedHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.wisp.domain.WispCard
import kotlin.math.max
import kotlin.math.roundToInt

/** Shared context math used by individual Plant/Wisp scorers. */
object CardScoringHelpers {
    private const val POINTS_PER_VALUE = 3

    fun playScore(
        context: DecisionContext,
        phase: CardPhase,
        effect: GameEffect,
        cardName: String,
        base: Int,
        normalPurchasingPower: Int? = null
    ): PriorityScore {
        var score = PriorityScore(base)
        val dice = context.self.board.hand
        val buyPower = normalPurchasingPower
            ?: PurchaseThresholdHeuristics.purchasingPower(context.self.board)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)

        fun addBestGain(gain: Int, reason: String) {
            if (gain != 0) score = score.adjusted(gain * POINTS_PER_VALUE, reason)
            if (phase == CardPhase.CULTIVATION && gain > 0) {
                val threshold = PurchaseThresholdHeuristics.thresholdBonus(
                    beforePower = buyPower,
                    afterPower = buyPower + gain,
                    costs = tiers,
                    pointsPerTier = 10
                )
                if (threshold != 0) score = score.adjusted(threshold, "Improves an available Buy tier")
            }
        }

        when (effect) {
            GameEffect.DOUBLE_ONE_DIE -> addBestGain(
                dice.maxOfOrNull { minOf(it.sides, it.value * 2) - it.value } ?: 0,
                "Best available doubling gain"
            )
            GameEffect.RAISE_DIE_PLUS_4 -> addBestGain(bestRaise(dice, 4), "Best +4 target")
            GameEffect.RAISE_ANY_DIE_PLUS_1 -> addBestGain(bestRaise(dice, 1), "Best +1 target")
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE -> {
                addBestGain(bestRaise(dice, 1), "Best +1 target")
                if (phase == CardPhase.BATTLE && context.battle != null) {
                    val worst = StrikeRow.entries.map { RowNeedHeuristics.calculate(context, it) }
                        .filter { it.available }
                        .minByOrNull { it.margin }
                    if (worst != null && worst.margin <= -5) {
                        score = score.adjusted(25, "A badly lost row can be abandoned")
                    }
                }
            }
            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS -> {
                val target = dice.filter { it.value <= 2 }.maxByOrNull { it.sides }
                score = if (target == null) score.adjusted(-30, "No die showing 1 or 2")
                else score.adjusted(10 + target.sides / 2, "Useful low-roll reroll target")
            }
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE -> {
                val reserveBonus = when (context.self.board.water) {
                    0 -> 20
                    1 -> 10
                    else -> -10
                }
                score = score.adjusted(reserveBonus, "Water reserve")
            }
            GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND -> {
                score = score.adjusted(context.self.board.worms * 10, "Existing Worms benefit from the round boost")
            }
            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE -> {
                val ones = dice.count { it.value == 1 }
                val sacrifice = dice.filter { it.value >= 2 }.minOfOrNull { it.value - 1 } ?: 10
                score = score.adjusted(ones * 8 - sacrifice * 3, "VP from existing 1s versus sacrificed die value")
            }
            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE -> {
                val improvement = dice.sumOf { max(0.0, DieValueHeuristics.expectedRerollGain(it)).roundToInt() }
                score = score.adjusted(improvement * 3, "Poor dice can be replaced")
            }
            GameEffect.UPGRADE_DIE_AND_USE_NOW -> {
                val smallest = dice.minByOrNull { it.sides }
                if (smallest != null) score = score.adjusted((nextSides(smallest.sides) - smallest.sides) * 2, "Persistent die upgrade")
                val rounds = context.progress.cultivationRoundsRemaining ?: 0
                score = score.adjusted(minOf(20, rounds * 3), "Upgrade remains useful in later rounds")
            }
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE -> addBestGain(
                dice.maxOfOrNull { DieValueHeuristics.flipGain(it) } ?: 0,
                "Best opposite-face gain"
            )
            GameEffect.SET_DIE_TO_MATCH_ANOTHER -> {
                val gain = dice.maxOfOrNull { target ->
                    dice.filter { it.index != target.index }.maxOfOrNull { source -> minOf(target.sides, source.value) - target.value } ?: 0
                } ?: 0
                addBestGain(max(0, gain), "Best copy-value pair")
            }
            GameEffect.MULCH_DIE_FROM_DISCARD -> {
                val best = context.self.board.discard.maxOfOrNull { it.sides } ?: 0
                score = score.adjusted(best / 2, "High-sided discard die can be prepared on Mulch")
                if (context.self.board.mulch.size + context.self.board.pendingMulch.size < 2) {
                    score = score.adjusted(10, "Below desired Mulch reserve")
                }
            }
            GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE,
            GameEffect.FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE -> {
                if (phase == CardPhase.BATTLE) {
                    val targets = context.opponents.sumOf { opponent ->
                        opponent.board.creature.count { it.type == PlantType.ROOT || it.type == PlantType.VINE }
                    }
                    if (targets > 0) score = score.adjusted(15, "Opponent has a Root or Vine that can be flipped")
                } else {
                    val spent = context.self.board.creature.count { it.isFaceDown }
                    if (spent > 0) score = score.adjusted(spent * 5, "Can refresh a spent Plant")
                }
            }
            GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT -> {
                val spent = context.self.board.creature.count { it.isFaceDown && (it.type == PlantType.ROOT || it.type == PlantType.VINE) }
                score = score.adjusted(spent * 6, "Spent Root/Vine effects are available to reuse")
            }
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX -> addBestGain(
                bestLowestToMax(dice),
                "Best lowest-value die can be set to maximum"
            )
            GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE -> {
                if (phase == CardPhase.BATTLE) score = score.adjusted(context.opponents.size * 20, "Wounds every opponent")
                else score = score.adjusted(context.self.board.creature.count { it.isFaceDown } * 5, "Can refresh a spent Plant")
            }
            GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE -> {
                if (phase == CardPhase.BATTLE) {
                    val targets = context.opponents.maxOfOrNull { it.board.creature.size } ?: 0
                    if (targets > 0) score = score.adjusted(22, "Can choose an opponent and their affected Plant")
                } else {
                    score = score.adjusted(context.self.board.creature.count { it.isFaceDown } * 5, "Can refresh a spent Plant")
                }
            }
            GameEffect.RAISE_ALL_DICE_PLUS_2 -> addBestGain(
                dice.sumOf { DieValueHeuristics.actualRaiseGain(it, 2) },
                "Total +2 gain across all dice"
            )
            GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5 -> {
                addBestGain(bestRaise(dice, 5), "Best +5 target")
                if (context.self.board.bees <= 2 && context.self.board.worms <= 2) score = score.adjusted(-20, "Critters are at reserve levels")
            }
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE,
            GameEffect.DISCARD_ONE_DIE_DRAW_TWO -> {
                val weakest = dice.minByOrNull { it.value }
                val nextExpected = expectedNextDraw(context)
                if (weakest != null) score = score.adjusted(((nextExpected * 2) - weakest.value).roundToInt() * 2, "Two expected draws replace the weakest die")
            }
            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE -> {
                val count = context.self.board.creature.count { it.type == PlantType.ROOT || it.type == PlantType.VINE }
                score = score.adjusted(count * 5, "Scales with Root/Vine grafts")
            }
            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 -> {
                if (phase == CardPhase.CULTIVATION) {
                    val gain = dice.maxOfOrNull { max(0, 3 - it.value) } ?: 0
                    addBestGain(gain, "Useful set-to-3 target")
                } else {
                    score = score.adjusted(bestBattleRowNeed(context) / 2, "Can pressure a needy Battle row")
                }
            }
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX -> addBestGain(
                dice.filter { it.sides <= 12 }.maxOfOrNull { DieValueHeuristics.setToMaximumGain(it) } ?: 0,
                "Best eligible die-to-maximum gain"
            )
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW -> {
                addBestGain(bestRaise(dice, 2), "Initial +2 Raise")
                if (phase == CardPhase.BATTLE) score = score.adjusted(bestBattleRowNeed(context) / 2, "Row drain is strongest where help is needed")
            }
            GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES,
            GameEffect.GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES -> {
                val spent = context.self.board.butterflies.count { !it.isFaceUp }
                val stealable = context.opponents.sumOf { it.board.butterflies.size }
                val gainable = if (effect == GameEffect.GAIN_OR_STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES) {
                    context.grove.butterflies.size
                } else 0
                score = score.adjusted(
                    spent * 10 + if (stealable + gainable > 0) 15 else -15,
                    "Butterfly refresh/acquisition opportunity"
                )
            }
            GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4 -> {
                val d4 = dice.any { it.sides == 4 }
                val raiseAll = if (d4) dice.filterNot { it.sides == 4 && it.value == dice.firstOrNull { d -> d.sides == 4 }?.value }.sumOf { DieValueHeuristics.actualRaiseGain(it, 4) } else 0
                score = score.adjusted(max(8, raiseAll * 2), "Best Petal To Die 4 branch")
            }
            GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE -> {
                val weakest = dice.minByOrNull { it.value }
                if (weakest != null) score = score.adjusted(max(0.0, expectedNextDraw(context) - weakest.value).roundToInt() * 3, "Replace weakest die")
                if (phase == CardPhase.BATTLE) score = score.adjusted(10, "Optional row-improving swap")
            }
            GameEffect.DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE -> {
                score = score.adjusted(expectedNextDraw(context).roundToInt() * 3, "Adds an extra die")
                if (phase == CardPhase.BATTLE) score = score.adjusted(16, "Mandatory swap also raises one swapped die +2")
            }
            GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND -> {
                score = score.adjusted(context.self.board.bees * 12, "Existing Bees benefit from the boost")
            }
            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER -> {
                val count = context.self.board.creature.count { it.type == PlantType.VINE || it.type == PlantType.FLOWER }
                score = score.adjusted(count * 5, "Scales with Vine/Flower grafts")
            }
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW -> {
                addBestGain(bestRaise(dice, 1), "Initial +1 Raise")
                if (phase == CardPhase.BATTLE) score = score.adjusted(bestBattleRowNeed(context) / 2, "Can flip opposing dice in a needy row")
            }
            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW -> {
                val gain = dice.maxOfOrNull { DieValueHeuristics.expectedRerollGain(it) } ?: 0.0
                score = score.adjusted((gain * 3).roundToInt(), "Best own reroll expectation")
                if (phase == CardPhase.BATTLE) score = score.adjusted(bestBattleRowNeed(context) / 3, "Can disturb higher opposing dice")
            }
            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE -> {
                val maxNow = dice.count { it.value >= it.sides }
                val canCreate = dice.any { it.value == it.sides - 1 }
                score = score.adjusted(maxNow * 15 + if (canCreate) 18 else 0, "Maximum dice create extra Draws")
            }
            GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND -> {
                val best = context.self.board.discard.maxByOrNull { it.sides }
                if (best != null) score = score.adjusted((DieValueHeuristics.expectedRoll(best.sides) * 3).roundToInt(), "Best discard die can return to Hand")
            }
            GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE -> {
                val useful = context.self.board.creature.count { it.isFaceDown }
                score = score.adjusted(minOf(35, useful * 10), "Can reuse/refresh useful cards twice")
            }
            GameEffect.DRAW_TWO_DICE -> {
                score = score.adjusted((expectedNextDraw(context) * 5).roundToInt(), "Two additional dice")
            }
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD -> {
                val best = context.self.board.discard.maxOfOrNull { it.sides } ?: 0
                score = score.adjusted(best / 2, "Can store a valuable discard die")
                if (context.self.board.mulch.size < 2) score = score.adjusted(10, "Below Mulch reserve")
            }
            GameEffect.SWAP_OWN_DIE_WITH_OPPONENT_SAME_SIZE -> {
                if (phase == CardPhase.BATTLE) score = score.adjusted(20, "Potential positive cross-player die swap")
            }
            GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW -> {
                val smallest = dice.minOfOrNull { it.sides } ?: 4
                score = score.adjusted(max(10, 30 - smallest), "Large persistent upgrade on a small die")
            }
            GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW -> {
                val winning = StrikeRow.entries.map { RowNeedHeuristics.calculate(context, it) }
                    .filter { it.available && it.currentlyWinning }
                score = if (winning.isEmpty()) score.adjusted(-35, "No currently winning row to lock")
                else score.adjusted(20 + winning.maxOf { max(0, 5 - it.margin) }, "Can lock a current Strike lead")
            }
            else -> Unit
        }

        return score
    }

    fun acquireScore(
        context: DecisionContext,
        card: PlantCard,
        neutralPlayBase: Int
    ): PriorityScore {
        var score = PriorityScore(neutralPlayBase / 5)
        val vp = projectedVp(context, card.scoringRule)
        if (vp > 0) score = score.adjusted(vp * 3, "Projected end-game VP")
        if (card.effect == GameEffect.DRAW_TWO_DICE || card.effect == GameEffect.UPGRADE_DIE_AND_USE_NOW) {
            score = score.adjusted(4, "Broadly useful effect")
        }
        return score
    }

    fun lossValue(
        context: DecisionContext,
        card: CreatureCardView,
        neutralPlayBase: Int
    ): Int = neutralPlayBase + projectedVp(context, card.scoringRule) * 10

    fun wispPlayScore(
        context: DecisionContext,
        card: WispCard,
        base: Int
    ): PriorityScore {
        var score = playScore(context, CardPhase.from(context.phase), card.effect, card.name, base)
        if (card.endGameVp > 0) score = score.adjusted(-12 * card.endGameVp, "Preserve unplayed Wisp VP")
        val pressure = max(0, context.self.wispCount - 3) * 20
        if (pressure > 0) score = score.adjusted(pressure, "Wisp hand pressure above three")
        if (card.effect.name.startsWith("GAIN_OR_REFRESH_") && context.self.board.butterflies.all { it.isFaceUp }) {
            score = score.adjusted(-10, "Butterfly refresh has little immediate value")
        }
        return score
    }

    fun projectedVp(context: DecisionContext, rule: PlantScoringRule): Int =
        when (rule) {
            is PlantScoringRule.Fixed -> rule.points
            PlantScoringRule.PerButterfly -> context.self.board.butterflies.size
            PlantScoringRule.PerGraftedVine -> context.self.board.creature.count { it.type == PlantType.VINE }
            PlantScoringRule.PerOwnedD4 ->
                (context.self.board.supply + context.self.board.hand + context.self.board.discard)
                    .count { it.sides == 4 } +
                    context.self.board.mulch.count { it.storedDieSides?.value == 4 } +
                    context.self.board.pendingMulch.count { it.storedDieSides?.value == 4 }
        }

    fun scoreDieTarget(effect: GameEffect, context: DecisionContext, die: dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice): PriorityScore {
        val gain = when (effect) {
            GameEffect.DOUBLE_ONE_DIE -> minOf(die.sides, die.value * 2) - die.value
            GameEffect.RAISE_DIE_PLUS_4 -> DieValueHeuristics.actualRaiseGain(die.sides, die.value, 4)
            GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE -> DieValueHeuristics.actualRaiseGain(die.sides, die.value, 1)
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW -> DieValueHeuristics.actualRaiseGain(die.sides, die.value, 2)
            GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS -> if (die.value <= 2) max(1, die.sides / 2) else -8
            GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
            GameEffect.REROLL_ONE_DIE_AND_REROLL_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW -> DieValueHeuristics.expectedRerollGain(die.sides, die.value).roundToInt()
            GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE -> DieValueHeuristics.flipGain(die.sides, die.value)
            GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE -> -(die.value - 1)
            GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
            GameEffect.SET_DIE_UP_TO_D12_TO_MAX -> DieValueHeuristics.setToMaximumGain(die.sides, die.value)
            GameEffect.SET_ANY_DIE_TO_3_OR_REDUCE_OPPOSING_STRIKE_ROW_BY_3 -> 3 - die.value
            GameEffect.UPGRADE_DIE_AND_USE_NOW -> nextSides(die.sides) - die.sides
            else -> 0
        }
        var score = PriorityScore(50).adjusted(gain * 4, "Target value swing")
        if (context.phase != null && CardPhase.from(context.phase) == CardPhase.CULTIVATION && gain > 0) {
            val power = PurchaseThresholdHeuristics.purchasingPower(context.self.board)
            val bonus = PurchaseThresholdHeuristics.thresholdBonus(power, power + gain, PurchaseThresholdHeuristics.availableCostTiers(context.grove), 10)
            if (bonus != 0) score = score.adjusted(bonus, "Target crosses Buy threshold")
        }
        return score
    }

    fun rowNeedBonus(context: DecisionContext, row: StrikeRow): Int =
        RowNeedHeuristics.calculate(context, row).needScore / 2

    fun preservationValue(context: DecisionContext, ownerIsSelf: Boolean, cardName: String): Int {
        val cards = if (ownerIsSelf) context.self.board.creature else context.opponents.flatMap { it.board.creature }
        val card = cards.firstOrNull { it.name == cardName } ?: return 0
        return 20 + card.cost * 2 + projectedVp(context, card.scoringRule) * 6
    }

    private fun bestRaise(dice: List<DieView>, amount: Int): Int =
        dice.maxOfOrNull { DieValueHeuristics.actualRaiseGain(it, amount) } ?: 0

    private fun bestLowestToMax(dice: List<DieView>): Int {
        val low = dice.minOfOrNull { it.value } ?: return 0
        return dice.filter { it.value == low }.maxOfOrNull { DieValueHeuristics.setToMaximumGain(it) } ?: 0
    }

    private fun bestBattleRowNeed(context: DecisionContext): Int =
        if (context.battle == null) 0 else StrikeRow.entries.maxOfOrNull { RowNeedHeuristics.calculate(context, it).needScore } ?: 0

    private fun expectedNextDraw(context: DecisionContext): Double {
        val next = context.self.board.supply.minByOrNull { it.sides }
            ?: context.self.board.discard.minByOrNull { it.sides }
        return next?.let { DieValueHeuristics.expectedRoll(it.sides) } ?: 2.5
    }

    private fun nextSides(sides: Int): Int = when (sides) {
        4 -> 6
        6 -> 8
        8 -> 10
        10 -> 12
        12 -> 20
        else -> sides
    }
}
