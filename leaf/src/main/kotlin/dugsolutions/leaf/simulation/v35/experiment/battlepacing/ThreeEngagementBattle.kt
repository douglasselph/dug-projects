package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.StrikeResolution
import dugsolutions.leaf.v35.battle.StrikeFinishCoordinator
import dugsolutions.leaf.v35.battle.domain.BattleGridSnapshot
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.RefreshResolver
import dugsolutions.leaf.v35.game.round.battle.BattleCleanupCoordinator
import dugsolutions.leaf.v35.game.round.battle.BattleMainActionStage
import dugsolutions.leaf.v35.game.round.battle.BattleDecisionPlantSnapshot
import dugsolutions.leaf.v35.game.round.battle.BattleDecisionResourceSnapshot
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard

/** Explicit experimental policy interface; canonical Human/Battle policies are NOT silently reused. */
sealed interface EngagementChoice {
    data class Main(val action: BattleMainAction) : EngagementChoice
    data class Support(val action: BattleSupportAction) : EngagementChoice
    data object Pass : EngagementChoice
}

data class EngagementDecision(
    val row: StrikeRow,
    val playerId: PlayerId,
    val actionsAlreadyTaken: Int,
    val normalMainsRemaining: Int,
    val legalMains: List<BattleMainAction>,
    val legalSupports: List<BattleSupportAction>,
    val context: DecisionContext,
    val supportSlotsRemaining: Int = 2
)

fun interface EngagementPolicy {
    fun choose(request: EngagementDecision): EngagementChoice
}

/** Deterministic mechanical sanity policy: take one Draw/Main, then pass. No calibration claims. */
object MechanicalEngagementPolicy : EngagementPolicy {
    override fun choose(request: EngagementDecision): EngagementChoice {
        if (request.legalMains.isNotEmpty()) {
            return EngagementChoice.Main(request.legalMains.firstOrNull { it == BattleMainAction.Draw }
                ?: request.legalMains.first())
        }
        return EngagementChoice.Pass
    }
}

/** Reference policy for Support-cap tests; not an expert or Human Baseline model. */
object SupportSpendingEngagementPolicy : EngagementPolicy {
    override fun choose(request: EngagementDecision): EngagementChoice {
        val main = MechanicalEngagementPolicy.choose(request)
        if (main is EngagementChoice.Main) return main
        val critter = request.legalSupports.filterIsInstance<BattleSupportAction.PlaceCritter>()
            .firstOrNull { it.row == request.row }
        return if (critter != null) EngagementChoice.Support(critter) else EngagementChoice.Pass
    }
}


/** A finished Strike is now empty, with its scoring snapshot frozen in BattleState. */
private class LockedStrikeEvidence(battle: BattleState, val row: StrikeRow) {
    private val frozen = requireNotNull(battle.frozenResolvedRow(row))

    fun verify(battle: BattleState) {
        check(battle.grid.isRowClosed(row)) { "Finished Strike reopened: $row" }
        check(battle.frozenResolvedRow(row) === frozen) { "Finished Strike snapshot changed: $row" }
        check(battle.grid.playerIdsInGridOrder.all { battle.grid.square(it, row).isEmpty }) {
            "Finished Strike not cleared: $row"
        }
    }
}

data class EngagementActionRecord(
    val row: StrikeRow,
    val playerId: PlayerId,
    val choice: EngagementChoice,
    val normalMainsRemainingAfter: Int,
    val supportsUsedInThisTurn: Int
)

data class EngagementDecisionStep(
    val choice: EngagementChoice,
    val legalMainCount: Int,
    val legalSupportCount: Int,
    val beforeRows: List<dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot>,
    val afterRows: List<dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot>,
    val beforePlants: List<BattleDecisionPlantSnapshot>,
    val afterPlants: List<BattleDecisionPlantSnapshot>,
    val beforeResources: List<BattleDecisionResourceSnapshot>,
    val afterResources: List<BattleDecisionResourceSnapshot>,
    val beforeChronicleSequence: Long,
    val afterChronicleSequence: Long
)

/** One real player opportunity, even if the player elects to pass. */
data class EngagementOpportunity(
    val playerId: PlayerId,
    val actingPosition: Int,
    val beforeTotals: Map<PlayerId, Int>,
    val afterTotals: Map<PlayerId, Int>,
    val actions: List<EngagementChoice>,
    val maxLegalMains: Int,
    val maxLegalSupports: Int,
    val initialLegalMains: Int = 0,
    val initialLegalSupports: Int = 0,
    val endedByPass: Boolean = false,
    val endedByNoLegalChoices: Boolean = false,
    val beforeRows: List<dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot> = emptyList(),
    val afterRows: List<dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot> = emptyList(),
    val beforePlants: List<BattleDecisionPlantSnapshot> = emptyList(),
    val afterPlants: List<BattleDecisionPlantSnapshot> = emptyList(),
    val beforeResources: List<BattleDecisionResourceSnapshot> = emptyList(),
    val afterResources: List<BattleDecisionResourceSnapshot> = emptyList(),
    val beforeChronicleSequence: Long = 0L,
    val afterChronicleSequence: Long = 0L,
    val decisionSteps: List<EngagementDecisionStep> = emptyList()
)

data class EngagementResult(
    val order: EngagementOrder,
    val actions: List<EngagementActionRecord>,
    val resolution: StrikeResolution,
    val initialTotals: Map<PlayerId, Int>,
    val opportunities: List<EngagementOpportunity>,
    /** True when a prior Wisp finished this Strike before its normal Engagement. */
    val preempted: Boolean = false
)

data class ThreeEngagementBattleResult(
    val engagements: List<EngagementResult>,
    val doomCount: Int,
    val cleanupDiceCount: Int,
    val trace: List<String>,
    val returnedCrittersCount: Int = 0
)

/**
 * Adopted v38 Three Engagements. Used by the main game engine and research A/B replays.
 * The existing BattleRound remains the selectable legacy whole-Battle implementation.
 *
 * All chosen actions run through the canonical legal generator and executors;
 * violations of locked-row invariants fail the experiment rather than emit invalid outcomes.
 */
class ThreeEngagementBattle(
    private val battleRound: BattleRound,
    private val starterRotation: Int = 0
) {
    init { require(starterRotation >= 0) }
    fun execute(
        game: Game,
        battle: BattleState,
        policy: EngagementPolicy = MechanicalEngagementPolicy
    ): ThreeEngagementBattleResult {
        val rawCard = requireNotNull(game.currentRound)
        val card = game.config.roundValues.cardFor(rawCard)
        require(game.players.map { it.id }.toSet() == battle.playerIdsInBattleOrder.toSet())
        battle.activateThreeEngagements()
        // Full v38 Wisp deck is legal. Historical A/B runners can still opt into
        // BattlePacingWispDeck.withoutResolve() for their old controlled cohort.

        val actions = battleRound.researchActionCoordinator(game, battle)
        val normalUsed = game.players.associate { it.id to 0 }.toMutableMap()
        val locked = mutableListOf<LockedStrikeEvidence>()
        val trace = mutableListOf<String>()
        fun verifyFinished() {
            StrikeRow.entries.filter { battle.finishedStrike(it) != null }.forEach { settled ->
                if (locked.none { it.row == settled }) locked += LockedStrikeEvidence(battle, settled)
            }
            locked.forEach { it.verify(battle) }
        }
        fun currentTotals(row: StrikeRow): Map<PlayerId, Int> =
            BattleGridSnapshot.row(battle, row).squares
                .filterNot { it.withdrawn }
                .associate { it.playerId to it.total }
        fun chronicleSequence(): Long = game.chronicle.entries.lastOrNull()?.sequence ?: 0L
        fun gridSnapshot() = StrikeRow.entries.map { snapshotRow ->
            BattleGridSnapshot.row(battle, snapshotRow, game.assetProvenance)
        }
        fun plantSnapshot() = game.players.map { observed ->
            BattleDecisionPlantSnapshot(observed.id,
                observed.creature.cards.count { it.isFaceUp },
                observed.creature.cards.count { it.isFaceDown })
        }
        fun resourceSnapshot() = game.players.map { observed ->
            BattleDecisionResourceSnapshot(
                playerId = observed.id,
                bees = observed.critters.count(dugsolutions.leaf.v35.tokens.Critter.BEE),
                worms = observed.critters.count(dugsolutions.leaf.v35.tokens.Critter.WORM),
                water = observed.tokens.waterCount, sunlight = observed.tokens.sunlightCount,
                mulch = observed.tokens.mulchTokens.size, wisps = observed.wisps.cards.cards.size,
                readyButterflies = observed.butterflies.all.count { observed.butterflies.isFaceUp(it) }
            )
        }
        val engagements = StrikeRow.entries.map { row ->
            verifyFinished()
            if (battle.grid.isRowClosed(row)) {
                val finished = requireNotNull(battle.finishedStrike(row)) {
                    "Strike $row was closed without full Resolve/Doom/Cleanup"
                }
                trace += "ENGAGEMENT_PREEMPTED,row=$row,reason=WISP_RESOLVE"
                return@map EngagementResult(
                    EngagementOrderSelector.selectSettled(battle, game.players.map { it.id }, row),
                    emptyList(), finished.resolution, emptyMap(), emptyList(), preempted = true
                )
            }
            val selected = EngagementOrderSelector.select(battle, game.players.map { it.id }, row)
            val shifted = starterRotation % selected.clockwiseActors.size
            val rotated = selected.clockwiseActors.drop(shifted) + selected.clockwiseActors.take(shifted)
            val order = if (shifted == 0) selected else selected.copy(
                starter = rotated.first(), clockwiseActors = rotated
            )
            trace += order.traceLines()
            val records = mutableListOf<EngagementActionRecord>()
            val initialTotals = currentTotals(row)
            val opportunities = mutableListOf<EngagementOpportunity>()
            for ((position, id) in order.clockwiseActors.withIndex()) {
                if (battle.grid.isRowClosed(row)) break // Wisp finished active Strike early
                val before = currentTotals(row)
                val opportunityBeforeRows = gridSnapshot()
                val opportunityBeforePlants = plantSnapshot()
                val opportunityBeforeResources = resourceSnapshot()
                val opportunityBeforeSequence = chronicleSequence()
                val startIndex = records.size
                var maxMains = 0
                var maxSupports = 0
                var initialMains = 0
                var initialSupports = 0
                var endedByPass = false
                var endedByNoLegal = false
                val steps = mutableListOf<EngagementDecisionStep>()
                val player = battle.player(id)
                var normalThisTurn = false
                var supportsThisTurn = 0
                var actionCount = 0
                while (!battle.grid.isRowClosed(row)) {
                    val remaining = 2 - normalUsed.getValue(id)
                    val mains = if (remaining > 0 && !normalThisTurn) {
                        actions.researchLegalMains(game, player, card, battle)
                    } else emptyList()
                    val supports = if (supportsThisTurn < 2) {
                        actions.researchLegalSupports(game, player, card, battle)
                    } else emptyList()
                    if (actionCount == 0) { initialMains = mains.size; initialSupports = supports.size }
                    maxMains = maxOf(maxMains, mains.size)
                    maxSupports = maxOf(maxSupports, supports.size)
                    if (mains.isEmpty() && supports.isEmpty()) { endedByNoLegal = true; break }
                    val stepBeforeRows = gridSnapshot()
                    val stepBeforePlants = plantSnapshot()
                    val stepBeforeResources = resourceSnapshot()
                    val stepBeforeSequence = chronicleSequence()
                    val next = policy.choose(
                        EngagementDecision(row, id, actionCount, remaining, mains, supports,
                            battleRound.researchDecisionContext(game, player, battle),
                            supportSlotsRemaining = 2 - supportsThisTurn)
                    )
                    when (next) {
                        EngagementChoice.Pass -> {
                            endedByPass = true
                            steps += EngagementDecisionStep(
                                choice = next, legalMainCount = mains.size, legalSupportCount = supports.size,
                                beforeRows = stepBeforeRows, afterRows = gridSnapshot(),
                                beforePlants = stepBeforePlants, afterPlants = plantSnapshot(),
                                beforeResources = stepBeforeResources, afterResources = resourceSnapshot(),
                                beforeChronicleSequence = stepBeforeSequence, afterChronicleSequence = chronicleSequence()
                            )
                            break
                        }
                        is EngagementChoice.Main -> {
                            require(next.action in mains) { "Illegal or extra normal Main in $row: $next" }
                            actions.researchTakeMain(game, player, card, battle, next.action,
                                if (normalUsed.getValue(id) == 0) BattleMainActionStage.FIRST else BattleMainActionStage.FINAL)
                            normalUsed[id] = normalUsed.getValue(id) + 1
                            normalThisTurn = true
                        }
                        is EngagementChoice.Support -> {
                            require(next.action in supports) { "Illegal or extra Support in $row: $next" }
                            actions.researchTakeSupport(game, player, card, battle, next.action)
                            supportsThisTurn++
                        }
                    }
                    verifyFinished()
                    steps += EngagementDecisionStep(
                        choice = next, legalMainCount = mains.size, legalSupportCount = supports.size,
                        beforeRows = stepBeforeRows, afterRows = gridSnapshot(),
                        beforePlants = stepBeforePlants, afterPlants = plantSnapshot(),
                        beforeResources = stepBeforeResources, afterResources = resourceSnapshot(),
                        beforeChronicleSequence = stepBeforeSequence, afterChronicleSequence = chronicleSequence()
                    )
                    actionCount++
                    records += EngagementActionRecord(row, id, next, 2 - normalUsed.getValue(id), supportsThisTurn)
                    trace += "ENGAGEMENT_ACTION,row=$row,player=${id.value},choice=$next,normalUsed=${normalUsed.getValue(id)},supportsThisRow=$supportsThisTurn"
                    check(actionCount <= 3) { "Engagement exceeded its one-normal/two-Support allowance" }
                }
                if (policy is EngagementMainPlanSource) {
                    val plan = policy.mainPlan(id, row)
                    trace += plan?.trace(id.value)
                        ?: "ENGAGEMENT_MAIN_PLAN,player=${id.value},row=$row,spend=false,reason=NO_MAIN_DECISION"
                }
                opportunities += EngagementOpportunity(
                    playerId = id, actingPosition = position + 1,
                    beforeTotals = before, afterTotals = currentTotals(row),
                    actions = records.drop(startIndex).map { it.choice },
                    maxLegalMains = maxMains, maxLegalSupports = maxSupports,
                    initialLegalMains = initialMains, initialLegalSupports = initialSupports,
                    endedByPass = endedByPass, endedByNoLegalChoices = endedByNoLegal,
                    beforeRows = opportunityBeforeRows, afterRows = gridSnapshot(),
                    beforePlants = opportunityBeforePlants, afterPlants = plantSnapshot(),
                    beforeResources = opportunityBeforeResources, afterResources = resourceSnapshot(),
                    beforeChronicleSequence = opportunityBeforeSequence, afterChronicleSequence = chronicleSequence(),
                    decisionSteps = steps.toList()
                )
            }
            val finished = battle.finishedStrike(row) ?: StrikeFinishCoordinator().finish(
                game, battle, row,
                decisionContext = { player -> battleRound.researchDecisionContext(game, player, battle) }
            )
            verifyFinished()
            val resolution = finished.resolution
            trace += "ENGAGEMENT_RESOLVE,row=$row,winners=${resolution.winnerIds.joinToString("|") { it.value.toString() }},wounds=${resolution.wounds.size},vpEach=${resolution.vpPerWinner},doom=${finished.doom.count},discard=${finished.cleanup.totalDiscardedDice}"
            EngagementResult(order, records.toList(), resolution, initialTotals, opportunities.toList())
        }
        verifyFinished()
        val completed = StrikeRow.entries.map { row ->
            requireNotNull(battle.finishedStrike(row)) { "Strike $row was not completed" }
        }
        BattleCleanupCoordinator(RefreshResolver(game.chronicle)).executeFinalRefresh(game, battle)
        return ThreeEngagementBattleResult(
            engagements, completed.sumOf { it.doom.count },
            completed.sumOf { it.cleanup.totalDiscardedDice }, trace,
            completed.sumOf { it.cleanup.totalReturnedCritters }
        )
    }
}
