package dugsolutions.leaf.v35.player.decision.learned.buy

import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.random.die.DieSides

/** Pure feature extraction: no RNG and no mutable game objects. */
object BuyFeatureExtractor {
    fun extract(context: DecisionContext, action: BuyItem?): BuyFeatureVector = BuyFeatureVector.build {
        fun put(feature: BuyFeature, value: Number) { this[feature] = value.toDouble() }
        val board = context.self.board
        val ownedDice = board.supply + board.hand + board.discard
        fun diceCount(sides: Int) = ownedDice.count { it.sides == sides }
        fun market(sides: DieSides) = context.grove.graftBed[sides] ?: 0

        put(BuyFeature.BIAS, 1)
        put(BuyFeature.SELF_VP, board.vp)
        put(BuyFeature.SELF_PLANT_COUNT, board.plantCount)
        put(BuyFeature.SELF_ROOT_COUNT, board.creature.count { it.type == PlantType.ROOT })
        put(BuyFeature.SELF_VINE_COUNT, board.creature.count { it.type == PlantType.VINE })
        put(BuyFeature.SELF_FLOWER_COUNT, board.creature.count { it.type == PlantType.FLOWER })
        put(BuyFeature.SELF_DICE_COUNT, ownedDice.size)
        put(BuyFeature.SELF_DICE_POWER, board.dicePower)
        put(BuyFeature.SELF_D4_COUNT, diceCount(4)); put(BuyFeature.SELF_D6_COUNT, diceCount(6))
        put(BuyFeature.SELF_D8_COUNT, diceCount(8)); put(BuyFeature.SELF_D10_COUNT, diceCount(10))
        put(BuyFeature.SELF_D12_COUNT, diceCount(12)); put(BuyFeature.SELF_D20_COUNT, diceCount(20))
        put(BuyFeature.SELF_WISP_COUNT, context.self.wispCount)
        put(BuyFeature.SELF_BEE_COUNT, board.bees); put(BuyFeature.SELF_WORM_COUNT, board.worms)
        put(BuyFeature.OPPONENT_MAX_VP, context.opponents.maxOfOrNull { it.board.vp } ?: 0)
        put(BuyFeature.OPPONENT_MAX_PLANT_COUNT, context.opponents.maxOfOrNull { it.board.plantCount } ?: 0)
        put(BuyFeature.OPPONENT_MAX_DICE_POWER, context.opponents.maxOfOrNull { it.board.dicePower } ?: 0)
        val p = context.progress
        this[BuyFeature.GAME_PROGRESS] = if (p.totalRounds > 0) p.roundsCompleted.toDouble() / p.totalRounds else 0.0
        put(BuyFeature.CULTIVATION_ROUNDS_REMAINING, p.cultivationRoundsRemaining ?: 0)
        put(BuyFeature.BATTLE_ROUNDS_REMAINING, p.battleRoundsRemaining ?: 0)
        put(BuyFeature.MARKET_PLANT_STACKS, context.grove.plantStacks.count { it.remaining > 0 })
        put(BuyFeature.MARKET_D4_REMAINING, market(DieSides.D4)); put(BuyFeature.MARKET_D6_REMAINING, market(DieSides.D6))
        put(BuyFeature.MARKET_D8_REMAINING, market(DieSides.D8)); put(BuyFeature.MARKET_D10_REMAINING, market(DieSides.D10))
        put(BuyFeature.MARKET_D12_REMAINING, market(DieSides.D12)); put(BuyFeature.MARKET_D20_REMAINING, market(DieSides.D20))

        when (action) {
            null -> put(BuyFeature.ACTION_DONE, 1)
            is BuyItem.Die -> {
                put(BuyFeature.ACTION_IS_DIE, 1); put(BuyFeature.ACTION_COST, action.cost)
                this[when (action.sides) {
                    DieSides.D4 -> BuyFeature.ACTION_D4; DieSides.D6 -> BuyFeature.ACTION_D6
                    DieSides.D8 -> BuyFeature.ACTION_D8; DieSides.D10 -> BuyFeature.ACTION_D10
                    DieSides.D12 -> BuyFeature.ACTION_D12; DieSides.D20 -> BuyFeature.ACTION_D20
                }] = 1.0
            }
            is BuyItem.Plant -> {
                put(BuyFeature.ACTION_IS_PLANT, 1); put(BuyFeature.ACTION_COST, action.cost)
                putNamed(LearnedBuyWeights.cardFeature(action.card.name), 1.0)
                putNamed(LearnedBuyWeights.costFeature(action.cost), 1.0)
                this[when (action.card.type) {
                    PlantType.ROOT -> BuyFeature.ACTION_IS_ROOT
                    PlantType.VINE -> BuyFeature.ACTION_IS_VINE
                    PlantType.FLOWER -> BuyFeature.ACTION_IS_FLOWER
                }] = 1.0
                val scoring = context.grove.plantStacks
                    .firstOrNull { it.name == action.card.name }
                    ?.scoringRule
                    ?: action.card.scoringRule
                when (scoring) {
                    is PlantScoringRule.Fixed -> put(BuyFeature.ACTION_PLANT_FIXED_VP, scoring.points)
                    else -> put(BuyFeature.ACTION_PLANT_VARIABLE_VP, 1)
                }
            }
        }

        // Buy decisions occur in Cultivation. Each completed Battle separates
        // one Cultivation block from the next, so 3/2/2 and 2/2/3 share the
        // same three strategic stage identities despite different block sizes.
        val stage = (p.battleRoundsCompleted + 1).coerceAtLeast(1)
        val suffix = when (stage) { 1 -> "STAGE_1"; 2 -> "STAGE_2"; 3 -> "STAGE_3"; else -> "STAGE_4_PLUS" }
        val interactionBases = listOf(
            BuyFeature.ACTION_DONE, BuyFeature.ACTION_IS_PLANT, BuyFeature.ACTION_IS_ROOT,
            BuyFeature.ACTION_IS_VINE, BuyFeature.ACTION_IS_FLOWER, BuyFeature.ACTION_IS_DIE,
            BuyFeature.ACTION_D4, BuyFeature.ACTION_D6, BuyFeature.ACTION_D8,
            BuyFeature.ACTION_D10, BuyFeature.ACTION_D12, BuyFeature.ACTION_D20
        )
        interactionBases.forEach { base ->
            val baseValue = this[base] ?: 0.0
            if (baseValue != 0.0) {
                this[BuyFeature.valueOf("${base.name}_$suffix")] = baseValue
            }
        }
        if (action is BuyItem.Plant) putNamed("${LearnedBuyWeights.costFeature(action.cost)}_$suffix", 1.0)
    }
}
