package dugsolutions.leaf.v35.player.decision.learned.battle

import dugsolutions.leaf.v35.player.decision.battle.*
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter
import kotlin.math.max

object BattleSupportFeatureExtractor {
    fun extract(request: ChooseBattleSupportActionRequest, action: BattleTurnAction): BattleSupportFeatureVector =
        BattleSupportFeatureVector.build {
            require(action in request.legalActions) { "Cannot extract features for illegal Battle Support action: $action" }
            val c = request.observation.context
            val b = c.self.board
            val battle = requireNotNull(c.battle) { "Battle Support learner requires visible Battle state" }
            val ownId = c.self.id
            val rows = battle.rows.mapNotNull { row -> row.forPlayer(ownId)?.let { own -> row to own } }
            var winning = 0; var tied = 0; var losing = 0; var woundExposed = 0
            rows.filter { !it.first.closed && !it.second.withdrawn }.forEach { (row, own) ->
                val oppMax = row.players.filter { it.playerId != ownId && !it.withdrawn }.maxOfOrNull { it.total } ?: 0
                when { own.total > oppMax -> winning++; own.total == oppMax -> tied++; else -> losing++ }
                if (oppMax - own.total >= 5) woundExposed++
            }
            val battlesRemaining = c.progress.battleRoundsRemaining ?: 0
            this[BattleSupportFeature.BIAS] = 1
            this[BattleSupportFeature.PASS_NUMBER] = request.observation.passNumber
            this[BattleSupportFeature.BATTLE_NUMBER] = c.progress.currentBattleRoundNumber ?: 0
            this[BattleSupportFeature.BATTLES_REMAINING] = battlesRemaining
            this[BattleSupportFeature.FINAL_BATTLE] = if (c.progress.isFinalBattleRound) 1 else 0
            this[BattleSupportFeature.FINAL_MAIN_AVAILABLE] = if (request.legalActions.any { it is BattleTurnAction.FinalMain }) 1 else 0
            this[BattleSupportFeature.SELF_FACE_UP_PLANTS] = b.creature.count { it.isFaceUp }
            this[BattleSupportFeature.SELF_HAND_DICE_COUNT] = b.hand.size
            this[BattleSupportFeature.SELF_DICE_POWER] = b.dicePower
            this[BattleSupportFeature.SELF_WATER] = b.water
            this[BattleSupportFeature.SELF_SUNLIGHT] = b.sunlight
            this[BattleSupportFeature.SELF_MULCH] = b.mulch.size
            this[BattleSupportFeature.SELF_BEE] = b.bees
            this[BattleSupportFeature.SELF_WORM] = b.worms
            this[BattleSupportFeature.SELF_BUTTERFLY] = b.butterflies.count { it.isFaceUp }
            this[BattleSupportFeature.SELF_WISP] = c.self.wispCount
            this[BattleSupportFeature.ROWS_WINNING] = winning
            this[BattleSupportFeature.ROWS_TIED] = tied
            this[BattleSupportFeature.ROWS_LOSING] = losing
            this[BattleSupportFeature.ROWS_WOUND_EXPOSED] = woundExposed
            this[BattleSupportFeature.OPPONENT_DONE_COUNT] = battle.donePlayerIds.count { it != ownId }
            this[BattleSupportFeature.HUMAN_REFERENCE_MATCH] = if (action == request.referenceAction) 1 else 0

            when (action) {
                is BattleTurnAction.FinalMain -> {
                    this[BattleSupportFeature.ACTION_FINAL_MAIN] = 1
                    addMainNamed(action.action, request)
                }
                is BattleTurnAction.Support -> when (val s = action.action) {
                    is BattleSupportAction.UseSunlight -> {
                        this[BattleSupportFeature.ACTION_SUNLIGHT] = 1
                        this[BattleSupportFeature.SUNLIGHT_HELD_INTERACTION] = b.sunlight
                        this[BattleSupportFeature.SUNLIGHT_BATTLES_REMAINING_INTERACTION] = battlesRemaining
                        when (s.mainAction) {
                            BattleMainAction.Draw -> this[BattleSupportFeature.SUNLIGHT_FUNDED_DRAW] = 1
                            is BattleMainAction.ActivatePlant -> this[BattleSupportFeature.SUNLIGHT_FUNDED_PLANT] = 1
                            BattleMainAction.RoundEffect1, BattleMainAction.RoundEffect2 -> this[BattleSupportFeature.SUNLIGHT_FUNDED_ROUND] = 1
                        }
                        addMainNamed(s.mainAction, request)
                    }
                    is BattleSupportAction.PlaceCritter -> when (s.critter) {
                        Critter.BEE -> this[BattleSupportFeature.ACTION_BEE] = 1
                        Critter.WORM -> this[BattleSupportFeature.ACTION_WORM] = 1
                    }
                    is BattleSupportAction.Shared -> when (val shared = s.action) {
                        is SupportAction.PlayWisp -> { this[BattleSupportFeature.ACTION_WISP] = 1; putNamed(LearnedBattleSupportWeights.wispFeature(shared.card.name), 1.0); putNamed(LearnedBattleSupportWeights.effectFeature(shared.card.effect), 1.0) }
                        is SupportAction.UseWaterReroll, SupportAction.UseWaterRefresh -> this[BattleSupportFeature.ACTION_WATER] = 1
                        is SupportAction.UseMulch -> this[BattleSupportFeature.ACTION_MULCH] = 1
                        is SupportAction.UseWormFlip -> this[BattleSupportFeature.ACTION_WORM_FLIP] = 1
                        is SupportAction.UseButterfly -> this[BattleSupportFeature.ACTION_BUTTERFLY] = 1
                    }
                }
            }
        }

    private fun BattleSupportFeatureVector.Builder.addMainNamed(main: BattleMainAction, request: ChooseBattleSupportActionRequest) {
        when (main) {
            BattleMainAction.Draw -> Unit
            is BattleMainAction.ActivatePlant -> {
                val view = request.observation.context.self.board.creature.firstOrNull { it.id == main.card.id }
                    ?: error("Visible Plant missing for Battle Support candidate ${main.card.card.name}")
                putNamed(LearnedBattleSupportWeights.plantFeature(view.name), 1.0)
                putNamed(LearnedBattleSupportWeights.effectFeature(view.effect), 1.0)
            }
            BattleMainAction.RoundEffect1 -> putNamed(LearnedBattleSupportWeights.effectFeature(request.observation.firstRoundEffect), 1.0)
            BattleMainAction.RoundEffect2 -> putNamed(LearnedBattleSupportWeights.effectFeature(request.observation.secondRoundEffect), 1.0)
        }
    }
}
