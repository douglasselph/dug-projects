package dugsolutions.leaf.v35.player.decision.learned.cultivation.support

import dugsolutions.leaf.v35.player.decision.baseline.common.DieValueHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationSupportActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationSupportDecision
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCardType

object CultivationSupportFeatureExtractor {
    fun extract(
        request: ChooseCultivationSupportActionRequest,
        decision: CultivationSupportDecision
    ): CultivationSupportFeatureVector = CultivationSupportFeatureVector.build {
        val context = request.observation.context
        val board = context.self.board
        val progress = context.progress
        val ownedDice = board.supply + board.hand + board.discard
        val purchasingPower = PurchaseThresholdHeuristics.purchasingPower(board)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        val affordableTier = PurchaseThresholdHeuristics.bestAffordableTier(purchasingPower, tiers) ?: 0
        val battlesRemaining = progress.battleRoundsRemaining
            ?: progress.upcomingRoundTypes.count { it == RoundCardType.BATTLE }
        val cultivationRemaining = progress.cultivationRoundsRemaining ?: 0
        val battleNext = progress.upcomingRoundTypes.firstOrNull() == RoundCardType.BATTLE
        val gameProgress = if (progress.totalRounds > 0) progress.roundsCompleted.toDouble() / progress.totalRounds else 0.0

        this[CultivationSupportFeature.BIAS] = 1
        this[CultivationSupportFeature.GAME_PROGRESS] = gameProgress
        this[CultivationSupportFeature.CULTIVATION_ROUNDS_REMAINING] = cultivationRemaining
        this[CultivationSupportFeature.BATTLE_ROUNDS_REMAINING] = battlesRemaining
        this[CultivationSupportFeature.BATTLE_NEXT] = if (battleNext) 1 else 0
        this[CultivationSupportFeature.MAIN_ACTIONS_REMAINING] = request.observation.mainActionsRemaining
        this[CultivationSupportFeature.SELF_DICE_COUNT] = ownedDice.size + board.mulch.count { it.storedDieSides != null } + board.pendingMulch.count { it.storedDieSides != null }
        this[CultivationSupportFeature.SELF_DICE_POWER] = board.dicePower
        this[CultivationSupportFeature.SELF_HAND_DICE_COUNT] = board.hand.size
        this[CultivationSupportFeature.SELF_PURCHASING_POWER] = purchasingPower
        this[CultivationSupportFeature.SELF_AFFORDABLE_TIER] = affordableTier
        this[CultivationSupportFeature.SELF_PLANT_COUNT] = board.plantCount
        this[CultivationSupportFeature.SELF_FACE_UP_PLANTS] = board.creature.count { it.isFaceUp }
        this[CultivationSupportFeature.SELF_FACE_DOWN_PLANTS] = board.creature.count { it.isFaceDown }
        this[CultivationSupportFeature.SELF_WATER] = board.water
        this[CultivationSupportFeature.SELF_MULCH] = board.mulch.size
        this[CultivationSupportFeature.SELF_WORM] = board.worms
        this[CultivationSupportFeature.SELF_BUTTERFLY] = board.butterflies.count { it.isFaceUp }
        this[CultivationSupportFeature.SELF_WISP] = context.self.wispCount
        this[CultivationSupportFeature.HUMAN_REFERENCE_MATCH] = when (decision) {
            CultivationSupportDecision.Pass -> if (request.referenceAction == null) 1 else 0
            is CultivationSupportDecision.Use -> if (decision.action == request.referenceAction) 1 else 0
        }

        val action = (decision as? CultivationSupportDecision.Use)?.action
        val actionKind = when (action) {
            null -> "PASS"
            is SupportAction.PlayWisp -> "WISP"
            is SupportAction.UseWaterReroll -> "WATER_REROLL"
            SupportAction.UseWaterRefresh -> "WATER_REFRESH"
            is SupportAction.UseMulch -> "MULCH"
            is SupportAction.UseWormFlip -> "WORM_FLIP"
            is SupportAction.UseButterfly -> "BUTTERFLY"
        }
        putNamed(LearnedCultivationSupportWeights.actionEffectFeature(actionKind, request.observation.firstRoundEffect), 1.0)
        putNamed(LearnedCultivationSupportWeights.actionEffectFeature(actionKind, request.observation.secondRoundEffect), 1.0)
        this[CultivationSupportFeature.ACTION_BATTLE_NEXT] = if (battleNext) 1 else 0
        this[CultivationSupportFeature.ACTION_BATTLES_REMAINING] = battlesRemaining
        this[CultivationSupportFeature.ACTION_MAIN_ACTIONS_REMAINING] = request.observation.mainActionsRemaining
        this[CultivationSupportFeature.ACTION_PURCHASING_POWER] = purchasingPower

        when (decision) {
            CultivationSupportDecision.Pass -> this[CultivationSupportFeature.ACTION_PASS] = 1
            is CultivationSupportDecision.Use -> when (val a = decision.action) {
                is SupportAction.PlayWisp -> {
                    require(a in request.legalActions)
                    this[CultivationSupportFeature.ACTION_WISP] = 1
                    putNamed(LearnedCultivationSupportWeights.effectFeature(a.card.effect), 1.0)
                }
                is SupportAction.UseWaterReroll -> {
                    require(a in request.legalActions)
                    this[CultivationSupportFeature.ACTION_WATER_REROLL] = 1
                    this[CultivationSupportFeature.TARGET_DIE_SIDES] = a.die.sides
                    this[CultivationSupportFeature.TARGET_DIE_VALUE] = a.die.value
                    this[CultivationSupportFeature.EXPECTED_REROLL_GAIN] = DieValueHeuristics.expectedRerollGain(a.die.sides, a.die.value)
                }
                SupportAction.UseWaterRefresh -> {
                    require(a in request.legalActions)
                    this[CultivationSupportFeature.ACTION_WATER_REFRESH] = 1
                    this[CultivationSupportFeature.REFRESH_FACE_DOWN_PLANTS] = board.creature.count { it.isFaceDown }
                    this[CultivationSupportFeature.REFRESH_SPENT_BUTTERFLIES] = board.butterflies.count { !it.isFaceUp }
                }
                is SupportAction.UseMulch -> {
                    require(a in request.legalActions)
                    this[CultivationSupportFeature.ACTION_MULCH] = 1
                    this[CultivationSupportFeature.MULCH_STORED_DIE_SIDES] = a.token.sides?.value ?: 0
                }
                is SupportAction.UseWormFlip -> {
                    require(a in request.legalActions)
                    this[CultivationSupportFeature.ACTION_WORM_FLIP] = 1
                    val plant = board.creature.firstOrNull { it.id == a.cardId }
                    if (plant != null) {
                        this[CultivationSupportFeature.TARGET_PLANT_COST] = plant.cost
                        putNamed(LearnedCultivationSupportWeights.plantFeature(plant.name), 1.0)
                        putNamed(LearnedCultivationSupportWeights.effectFeature(plant.effect), 1.0)
                    }
                }
                is SupportAction.UseButterfly -> {
                    require(a in request.legalActions)
                    this[CultivationSupportFeature.ACTION_BUTTERFLY] = 1
                    this[CultivationSupportFeature.TARGET_DIE_SIDES] = a.die.sides
                    this[CultivationSupportFeature.TARGET_DIE_VALUE] = a.die.value
                    this[CultivationSupportFeature.EXPECTED_KEEP_BEST_REROLL_GAIN] = DieValueHeuristics.expectedKeepBestRerollGain(a.die.sides, a.die.value)
                }
            }
        }
    }
}
