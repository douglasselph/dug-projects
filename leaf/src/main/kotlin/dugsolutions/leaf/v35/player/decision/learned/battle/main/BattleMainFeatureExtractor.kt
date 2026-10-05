package dugsolutions.leaf.v35.player.decision.learned.battle.main

import dugsolutions.leaf.v35.player.decision.baseline.cultivation.DrawPriority
import dugsolutions.leaf.v35.player.decision.battle.*

/** Pure feature extraction from the visible DecisionContext already supplied to Human Baseline. */
object BattleMainFeatureExtractor {
    fun extract(request: ChooseBattleMainActionRequest, action: BattleMainAction): BattleMainFeatureVector =
        BattleMainFeatureVector.build {
            require(action in request.legalActions) { "Cannot extract features for illegal Battle Main action: $action" }
            val c = request.observation.context
            val b = c.self.board
            val battle = requireNotNull(c.battle) { "Battle Main learner requires visible Battle state" }
            val ownId = c.self.id
            val ownRows = battle.rows.mapNotNull { row -> row.forPlayer(ownId)?.let { own -> row to own } }
            var winning = 0
            var tied = 0
            var losing = 0
            var woundExposed = 0
            var openCapacity = 0
            ownRows.filter { !it.first.closed && !it.second.withdrawn }.forEach { (row, own) ->
                val oppMax = row.players.filter { it.playerId != ownId && !it.withdrawn }.maxOfOrNull { it.total } ?: 0
                when {
                    own.total > oppMax -> winning++
                    own.total == oppMax -> tied++
                    else -> losing++
                }
                if (oppMax - own.total >= 5) woundExposed++
                openCapacity += (3 - own.dice.size).coerceAtLeast(0)
            }
            val battlesRemaining = c.progress.battleRoundsRemaining ?: 0
            val gameProgress = if (c.progress.totalRounds > 0) {
                c.progress.roundsCompleted.toDouble() / c.progress.totalRounds.toDouble()
            } else 0.0
            val leaderVp = (listOf(b.vp) + c.opponents.map { it.board.vp }).maxOrNull() ?: b.vp
            val remainingSupportResources =
                b.water + b.sunlight + b.mulch.size + b.worms + b.bees +
                    b.butterflies.count { it.isFaceUp } + c.self.wispCount

            this[BattleMainFeature.BIAS] = 1
            this[BattleMainFeature.STAGE_FIRST] = if (request.observation.stage == BattleMainPolicyStage.FIRST) 1 else 0
            this[BattleMainFeature.STAGE_FINAL] = if (request.observation.stage == BattleMainPolicyStage.FINAL) 1 else 0
            this[BattleMainFeature.BATTLE_NUMBER] = c.progress.currentBattleRoundNumber ?: 0
            this[BattleMainFeature.BATTLES_REMAINING] = battlesRemaining
            this[BattleMainFeature.FINAL_BATTLE] = if (c.progress.isFinalBattleRound) 1 else 0
            this[BattleMainFeature.GAME_PROGRESS] = gameProgress
            this[BattleMainFeature.SELF_VP] = b.vp
            this[BattleMainFeature.RELATIVE_VP_TO_LEADER] = b.vp - leaderVp
            this[BattleMainFeature.SELF_HAND_DICE_COUNT] = b.hand.size
            this[BattleMainFeature.SELF_DICE_POWER] = b.dicePower
            this[BattleMainFeature.SELF_FACE_UP_PLANTS] = b.creature.count { it.isFaceUp }
            this[BattleMainFeature.SELF_FACE_DOWN_PLANTS] = b.creature.count { it.isFaceDown }
            this[BattleMainFeature.SELF_WATER] = b.water
            this[BattleMainFeature.SELF_MULCH] = b.mulch.size
            this[BattleMainFeature.SELF_BUTTERFLY] = b.butterflies.count { it.isFaceUp }
            this[BattleMainFeature.SELF_WORM] = b.worms
            this[BattleMainFeature.SELF_BEE] = b.bees
            this[BattleMainFeature.SELF_WISP] = c.self.wispCount
            this[BattleMainFeature.SELF_SUNLIGHT] = b.sunlight
            this[BattleMainFeature.ROWS_WINNING] = winning
            this[BattleMainFeature.ROWS_TIED] = tied
            this[BattleMainFeature.ROWS_LOSING] = losing
            this[BattleMainFeature.ROWS_WOUND_EXPOSED] = woundExposed
            this[BattleMainFeature.OPEN_ROW_CAPACITY] = openCapacity
            this[BattleMainFeature.OPPONENT_DONE_COUNT] = battle.donePlayerIds.count { it != ownId }
            this[BattleMainFeature.REMAINING_SUPPORT_RESOURCE_COUNT] = remainingSupportResources
            this[BattleMainFeature.HUMAN_REFERENCE_MATCH] = if (action == request.referenceAction) 1 else 0
            if (request.observation.stage == BattleMainPolicyStage.FINAL) {
                this[BattleMainFeature.FINAL_REMAINING_SUPPORT_INTERACTION] = remainingSupportResources
            }

            when (action) {
                BattleMainAction.Draw -> {
                    this[BattleMainFeature.ACTION_DRAW] = 1
                    val draw = DrawPriority.observe(c)
                    this[BattleMainFeature.DRAW_NEXT_DIE_SIDES] = draw.nextDieSides ?: 0
                    this[BattleMainFeature.DRAW_EXPECTED_ROLL] = draw.expectedRoll ?: 0.0
                    this[BattleMainFeature.DRAW_OPEN_CAPACITY_INTERACTION] = openCapacity
                }
                is BattleMainAction.ActivatePlant -> {
                    this[BattleMainFeature.ACTION_PLANT] = 1
                    val view = b.creature.firstOrNull { it.id == action.card.id }
                        ?: b.creature.firstOrNull { it.name == action.card.card.name }
                        ?: error("Visible Plant missing for Battle Main candidate ${action.card.card.name}")
                    putNamed(LearnedBattleMainWeights.plantFeature(view.name), 1.0)
                    putNamed(LearnedBattleMainWeights.effectFeature(view.effect), 1.0)
                }
                BattleMainAction.RoundEffect1 -> {
                    this[BattleMainFeature.ACTION_ROUND_EFFECT_1] = 1
                    putNamed(LearnedBattleMainWeights.effectFeature(request.observation.firstRoundEffect), 1.0)
                }
                BattleMainAction.RoundEffect2 -> {
                    this[BattleMainFeature.ACTION_ROUND_EFFECT_2] = 1
                    putNamed(LearnedBattleMainWeights.effectFeature(request.observation.secondRoundEffect), 1.0)
                }
            }
        }
}
