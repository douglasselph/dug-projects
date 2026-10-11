package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.game.replay.CultivationDecisionReplay
import dugsolutions.leaf.v35.game.replay.CultivationReplayDecision
import dugsolutions.leaf.v35.game.replay.CultivationReplayFork
import dugsolutions.leaf.v35.game.replay.ReplayMainAction
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.round.RoundValueResolver
import dugsolutions.leaf.v35.plant.PlantValueResolver

private fun isCompost(effect: GameEffect) = effect == GameEffect.UPGRADE_DIE_FROM_HAND ||
    effect == GameEffect.UPGRADE_DIE_AND_USE_NOW_NO_REWARDS ||
    effect == GameEffect.UPGRADE_DIE_AND_USE_NOW

/** One completed game, with the historical Chronicle retained only by explicit request. */
data class RecordedReplayGame(
    val summary: GameSummary,
    val decisions: List<CultivationReplayDecision>,
    val chronicle: List<GameEntry>,
    val forkApplied: Boolean,
    val forcedCompostCount: Int
)

data class CompostPairedOutcome(
    val fork: CultivationReplayFork,
    val original: RecordedReplayGame,
    val alternative: RecordedReplayGame,
    val deltaForPlayer: CompostPlayerDelta
)

data class CompostPlayerDelta(
    val totalVp: Int,
    val battleVp: Int,
    val plantVp: Int,
    val wispVp: Int,
    val diceCount: Int,
    val dicePower: Int,
    val plants: Int,
    val wounds: Int,
    val winShare: Double
)

/**
 * Whole-game seeded counterfactual runner. Never tries to clone live mutable
 * objects halfway through a round. Instead replays identical seeds and
 * policy factories, intervening exactly once at the chosen Build consultation.
 *
 * Important: action changes alter the number/order of subsequent RNG draws.
 * Thus one pair is an observed possible continuation, not a proof that the
 * decision itself caused every later difference. Aggregate many seed pairs.
 */
class CompostCounterfactualRunner(
    private val factory: GameFactory,
    private val runner: GameRunner,
    private val plants: List<PlantCard>,
    private val players: List<PlayerDecisionFactory>,
    private val rounds: GameRoundSetup = GameRoundSetup.standard(),
    private val roundValues: RoundValueResolver = RoundValueResolver.CANONICAL,
    private val plantValues: PlantValueResolver = PlantValueResolver.CANONICAL
) {
    fun run(
        mechanicalSeed: Long,
        strategySeed: Long = mechanicalSeed,
        fork: CultivationReplayFork? = null,
        retainChronicle: Boolean = true,
        forceCompostPlayerId: dugsolutions.leaf.v35.player.PlayerId? = null,
        forceCompostLimit: Int = 0
    ): RecordedReplayGame {
        val tracker = CultivationDecisionReplay(fork, forceCompostPlayerId, forceCompostLimit)
        val game = factory(GameConfig(
            selectedPlantCards = plants,
            playerDecisionFactories = players,
            roundSetup = rounds,
            seed = mechanicalSeed,
            strategySeed = strategySeed,
            cultivationDecisionReplay = tracker,
            roundValues = roundValues,
            plantValues = plantValues
        ))
        val result = runner.run(game)
        if (fork != null) require(tracker.forkApplied) { "Requested replay fork was never reached: $fork" }
        return RecordedReplayGame(
            GameSummaryExtractor.extract(game, result),
            tracker.decisions,
            if (retainChronicle) game.chronicle.entries else emptyList(),
            tracker.forkApplied, tracker.forcedCompostCount
        )
    }

    /** Locate real Original-Compost selections in the baseline decision trace. */
    fun compostOpportunities(game: RecordedReplayGame): List<CultivationReplayDecision> =
        game.decisions.filter { d ->
            (d.actualChoice == ReplayMainAction.ROUND_EFFECT_1 && isCompost(d.firstEffect)) ||
                (d.actualChoice == ReplayMainAction.ROUND_EFFECT_2 && isCompost(d.secondEffect))
        }

    fun compare(
        mechanicalSeed: Long,
        strategySeed: Long = mechanicalSeed,
        fork: CultivationReplayFork,
        original: RecordedReplayGame? = null,
        retainChronicle: Boolean = true
    ): CompostPairedOutcome {
        val control = original ?: run(mechanicalSeed, strategySeed, retainChronicle = retainChronicle)
        val decision = control.decisions.singleOrNull { d ->
            d.roundNumber == fork.roundNumber && d.playerId == fork.playerId.value &&
                d.consultationIndex == fork.consultationIndex
        } ?: error("Original trace has no matching decision: $fork")
        check(decision.actualChoice == ReplayMainAction.ROUND_EFFECT_1 && isCompost(decision.firstEffect) ||
            decision.actualChoice == ReplayMainAction.ROUND_EFFECT_2 && isCompost(decision.secondEffect)) {
            "Original choice was not original-rule Compost: $decision"
        }
        check(fork.replacement in decision.legal) {
            "Alternative was not legal in the original decision state: $fork"
        }
        check(fork.replacement != decision.actualChoice) { "Alternative must differ from Compost" }
        val changed = run(mechanicalSeed, strategySeed, fork, retainChronicle)
        val before = control.summary.players.single { it.playerId.value == fork.playerId.value }
        val after = changed.summary.players.single { it.playerId.value == fork.playerId.value }
        return CompostPairedOutcome(fork, control, changed, CompostPlayerDelta(
            totalVp = after.totalVp - before.totalVp,
            battleVp = after.battleStrikeVp - before.battleStrikeVp,
            plantVp = after.plantVp - before.plantVp,
            wispVp = after.unplayedWispVp - before.unplayedWispVp,
            diceCount = after.finalDiceCount - before.finalDiceCount,
            dicePower = after.finalDicePower - before.finalDicePower,
            plants = after.finalPlantCount - before.finalPlantCount,
            wounds = after.woundsTaken - before.woundsTaken,
            winShare = after.winShare - before.winShare
        ))
    }
}
