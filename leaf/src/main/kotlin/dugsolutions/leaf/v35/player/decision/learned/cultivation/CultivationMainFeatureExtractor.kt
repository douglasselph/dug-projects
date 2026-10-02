package dugsolutions.leaf.v35.player.decision.learned.cultivation

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.baseline.common.PurchaseThresholdHeuristics
import dugsolutions.leaf.v35.player.decision.baseline.cultivation.DrawPriority
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationMainActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.round.domain.RoundCardType

/** Pure, deterministic feature extraction from the same visible snapshot Human Baseline receives. */
object CultivationMainFeatureExtractor {
    fun extract(
        request: ChooseCultivationMainActionRequest,
        action: CultivationMainAction
    ): CultivationMainFeatureVector = CultivationMainFeatureVector.build {
        require(action in request.legalActions) { "Cannot extract features for illegal Cultivation Main action: $action" }

        val context = request.observation.context
        val board = context.self.board
        val progress = context.progress
        val ownedDice = board.supply + board.hand + board.discard
        val purchasingPower = PurchaseThresholdHeuristics.purchasingPower(board)
        val tiers = PurchaseThresholdHeuristics.availableCostTiers(context.grove)
        val affordableTier = PurchaseThresholdHeuristics.bestAffordableTier(purchasingPower, tiers) ?: 0
        val cultivationRemaining = progress.cultivationRoundsRemaining ?: 0
        val battlesRemaining = progress.battleRoundsRemaining
            ?: progress.upcomingRoundTypes.count { it == RoundCardType.BATTLE }
        val battleNext = progress.upcomingRoundTypes.firstOrNull() == RoundCardType.BATTLE
        val gameProgress = if (progress.totalRounds > 0) {
            progress.roundsCompleted.toDouble() / progress.totalRounds.toDouble()
        } else 0.0

        this[CultivationMainFeature.BIAS] = 1
        this[CultivationMainFeature.GAME_PROGRESS] = gameProgress
        this[CultivationMainFeature.CULTIVATION_ROUNDS_REMAINING] = cultivationRemaining
        this[CultivationMainFeature.BATTLE_ROUNDS_REMAINING] = battlesRemaining
        this[CultivationMainFeature.BATTLE_NEXT] = if (battleNext) 1 else 0
        this[CultivationMainFeature.MAIN_ACTIONS_REMAINING] = request.observation.mainActionsRemaining
        this[CultivationMainFeature.SELF_DICE_COUNT] = ownedDice.size +
            board.mulch.count { it.storedDieSides != null } +
            board.pendingMulch.count { it.storedDieSides != null }
        this[CultivationMainFeature.SELF_DICE_POWER] = board.dicePower
        this[CultivationMainFeature.SELF_HAND_DICE_COUNT] = board.hand.size
        this[CultivationMainFeature.SELF_PURCHASING_POWER] = purchasingPower
        this[CultivationMainFeature.SELF_AFFORDABLE_TIER] = affordableTier
        this[CultivationMainFeature.SELF_PLANT_COUNT] = board.plantCount
        this[CultivationMainFeature.SELF_FACE_UP_PLANT_COUNT] = board.creature.count { it.isFaceUp }
        this[CultivationMainFeature.SELF_FACE_DOWN_PLANT_COUNT] = board.creature.count { it.isFaceDown }
        this[CultivationMainFeature.SELF_BEE_COUNT] = board.bees
        this[CultivationMainFeature.SELF_WORM_COUNT] = board.worms
        this[CultivationMainFeature.SELF_WATER_COUNT] = board.water
        this[CultivationMainFeature.SELF_MULCH_COUNT] = board.mulch.size
        this[CultivationMainFeature.SELF_SUNLIGHT_COUNT] = board.sunlight
        this[CultivationMainFeature.SELF_BUTTERFLY_COUNT] = board.butterflies.count { it.isFaceUp }
        this[CultivationMainFeature.GROVE_BEE_COUNT] = context.grove.bees
        this[CultivationMainFeature.GROVE_WORM_COUNT] = context.grove.worms
        this[CultivationMainFeature.GROVE_WATER_COUNT] = context.grove.water
        this[CultivationMainFeature.GROVE_MULCH_COUNT] = context.grove.mulch
        this[CultivationMainFeature.GROVE_SUNLIGHT_COUNT] = context.grove.sunlight
        this[CultivationMainFeature.GROVE_BUTTERFLY_COUNT] = context.grove.butterflies.size

        fun actionInteraction(
            draw: CultivationMainFeature,
            plant: CultivationMainFeature,
            round: CultivationMainFeature,
            value: Double
        ) {
            when (action) {
                CultivationMainAction.Draw -> this[draw] = value
                is CultivationMainAction.ActivatePlant -> this[plant] = value
                CultivationMainAction.RoundEffect1,
                CultivationMainAction.RoundEffect2 -> this[round] = value
            }
        }

        actionInteraction(
            CultivationMainFeature.ACTION_DRAW_BATTLE_NEXT,
            CultivationMainFeature.ACTION_PLANT_BATTLE_NEXT,
            CultivationMainFeature.ACTION_ROUND_BATTLE_NEXT,
            if (battleNext) 1.0 else 0.0
        )
        actionInteraction(
            CultivationMainFeature.ACTION_DRAW_BATTLES_REMAINING,
            CultivationMainFeature.ACTION_PLANT_BATTLES_REMAINING,
            CultivationMainFeature.ACTION_ROUND_BATTLES_REMAINING,
            battlesRemaining.toDouble()
        )
        actionInteraction(
            CultivationMainFeature.ACTION_DRAW_CULTIVATION_REMAINING,
            CultivationMainFeature.ACTION_PLANT_CULTIVATION_REMAINING,
            CultivationMainFeature.ACTION_ROUND_CULTIVATION_REMAINING,
            cultivationRemaining.toDouble()
        )
        actionInteraction(
            CultivationMainFeature.ACTION_DRAW_MAIN_ACTIONS_REMAINING,
            CultivationMainFeature.ACTION_PLANT_MAIN_ACTIONS_REMAINING,
            CultivationMainFeature.ACTION_ROUND_MAIN_ACTIONS_REMAINING,
            request.observation.mainActionsRemaining.toDouble()
        )

        when (action) {
            CultivationMainAction.Draw -> {
                this[CultivationMainFeature.ACTION_DRAW] = 1
                val draw = DrawPriority.observe(context)
                this[CultivationMainFeature.DRAW_NEXT_DIE_SIDES] = draw.nextDieSides ?: 0
                this[CultivationMainFeature.DRAW_EXPECTED_ROLL] = draw.expectedRoll ?: 0.0
                this[CultivationMainFeature.DRAW_ROLL_REWARD_CHANCE] = draw.rollRewardChance ?: 0.0
            }

            is CultivationMainAction.ActivatePlant -> {
                this[CultivationMainFeature.ACTION_PLANT] = 1
                val view = context.self.board.creature.firstOrNull { it.id == action.card.id }
                    ?: context.self.board.creature.firstOrNull { it.name == action.card.card.name }
                    ?: error("Visible Plant view missing for legal Cultivation Main action ${action.card.card.name}")
                this[CultivationMainFeature.ACTION_COST] = view.cost
                this[when (view.type) {
                    PlantType.ROOT -> CultivationMainFeature.ACTION_ROOT
                    PlantType.VINE -> CultivationMainFeature.ACTION_VINE
                    PlantType.FLOWER -> CultivationMainFeature.ACTION_FLOWER
                }] = 1
                putNamed(LearnedCultivationMainWeights.cardFeature(view.name), 1.0)
                putNamed(LearnedCultivationMainWeights.costFeature(view.cost), 1.0)
                addEffectNamedFeatures(
                    effect = view.effect,
                    battleNext = battleNext,
                    battlesRemaining = battlesRemaining,
                    sunlightHeld = board.sunlight,
                    mainActionsRemaining = request.observation.mainActionsRemaining
                )
            }

            CultivationMainAction.RoundEffect1,
            CultivationMainAction.RoundEffect2 -> {
                val slot = if (action == CultivationMainAction.RoundEffect1) 1 else 2
                this[if (slot == 1) CultivationMainFeature.ACTION_ROUND_EFFECT_1 else CultivationMainFeature.ACTION_ROUND_EFFECT_2] = 1
                val effect = if (slot == 1) request.observation.firstRoundEffect else request.observation.secondRoundEffect
                addEffectNamedFeatures(
                    effect = effect,
                    battleNext = battleNext,
                    battlesRemaining = battlesRemaining,
                    sunlightHeld = board.sunlight,
                    mainActionsRemaining = request.observation.mainActionsRemaining
                )
            }
        }
    }

    private fun CultivationMainFeatureVector.Builder.addEffectNamedFeatures(
        effect: GameEffect,
        battleNext: Boolean,
        battlesRemaining: Int,
        sunlightHeld: Int,
        mainActionsRemaining: Int
    ) {
        putNamed(LearnedCultivationMainWeights.effectFeature(effect), 1.0)
        putNamed(LearnedCultivationMainWeights.effectBattleNextFeature(effect), if (battleNext) 1.0 else 0.0)
        putNamed(LearnedCultivationMainWeights.effectBattlesRemainingFeature(effect), battlesRemaining.toDouble())
        putNamed(LearnedCultivationMainWeights.effectSunlightHeldFeature(effect), sunlightHeld.toDouble())
        putNamed(LearnedCultivationMainWeights.effectMainActionsRemainingFeature(effect), mainActionsRemaining.toDouble())
    }
}
