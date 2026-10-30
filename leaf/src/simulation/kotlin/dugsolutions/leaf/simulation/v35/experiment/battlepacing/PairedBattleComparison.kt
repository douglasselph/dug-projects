package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.round.battle.BattleActionLoopResult
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.battle.BattleStrikeResolutionResult
import dugsolutions.leaf.v35.battle.DoomResult
import dugsolutions.leaf.v35.game.round.battle.BattleCleanupResult

/** Executable A/B of exactly one legal Battle opening, not a statistical estimate. */
data class PairedBattleComparisonResult(
    val openingSha256: String,
    val baselineActions: BattleActionLoopResult,
    val baselineStrikes: BattleStrikeResolutionResult,
    val baselineDoom: DoomResult,
    val baselineCleanup: BattleCleanupResult,
    val candidate: ThreeEngagementBattleResult,
    val baselineFinalVP: List<Int>,
    val candidateFinalVP: List<Int>
)

class PairedBattleComparison(private val battleRound: BattleRound) {
    fun run(
        paired: PairedBattleOpening,
        candidatePolicy: EngagementPolicy = MechanicalEngagementPolicy
    ): PairedBattleComparisonResult {
        check(BattleOpeningFingerprint.capture(paired.controlGame, paired.controlBattle) == paired.fingerprint)
        check(BattleOpeningFingerprint.capture(paired.candidateGame, paired.candidateBattle) == paired.fingerprint)
        val a = paired.controlGame
        val card = a.config.roundValues.cardFor(requireNotNull(a.currentRound))
        val actions = battleRound.executeActions(a, card, paired.controlBattle)
        val strikes = battleRound.executeStrikes(a, paired.controlBattle)
        val doom = battleRound.executeDoom(a, paired.controlBattle)
        val cleanup = battleRound.executeCleanup(a, paired.controlBattle)
        val candidate = ThreeEngagementBattle(battleRound).execute(
            paired.candidateGame, paired.candidateBattle, candidatePolicy
        )
        return PairedBattleComparisonResult(
            openingSha256 = paired.fingerprint.sha256,
            baselineActions = actions,
            baselineStrikes = strikes,
            baselineDoom = doom,
            baselineCleanup = cleanup,
            candidate = candidate,
            baselineFinalVP = a.players.map { it.vp },
            candidateFinalVP = paired.candidateGame.players.map { it.vp }
        )
    }
}
