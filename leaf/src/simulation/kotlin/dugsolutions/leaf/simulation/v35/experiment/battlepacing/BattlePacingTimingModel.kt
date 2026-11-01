package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.BattleStrikeResolutionResult
import dugsolutions.leaf.v35.battle.DoomResult
import dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.round.battle.BattleActionLoopResult
import dugsolutions.leaf.v35.game.round.battle.BattleCleanupResult
import dugsolutions.leaf.v35.game.round.battle.BattleDecisionPlantSnapshot
import dugsolutions.leaf.v35.game.round.battle.BattleDecisionResourceSnapshot
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.tokens.Critter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

internal enum class PacingRuleSet { A_BASELINE, B_CANDIDATE }

internal data class BattlePacingStepObserved(
    val playerId: PlayerId,
    val legalMainCount: Int,
    val legalSupportCount: Int,
    val unresolvedStrikes: Int,
    val actionKind: String?,
    val pass: Boolean,
    val beforeRows: List<BattleGridRowSnapshot>,
    val afterRows: List<BattleGridRowSnapshot>,
    val beforePlants: List<BattleDecisionPlantSnapshot>,
    val afterPlants: List<BattleDecisionPlantSnapshot>,
    val beforeResources: List<BattleDecisionResourceSnapshot>,
    val afterResources: List<BattleDecisionResourceSnapshot>,
    val chronicleEntries: List<GameEntry>
) {
    val legalChoiceCount: Int get() = legalMainCount + legalSupportCount
    val meaningfulDecision: Boolean get() = legalChoiceCount > 1
    val complexityUnits: Double get() = min(10.0, ln(1.0 + max(1, legalChoiceCount).toDouble()) / ln(2.0) + 0.5 * (unresolvedStrikes - 1))
    val diceRolled: Int get() = chronicleEntries.count { it is GameEntry.DieRolled }
    val dicePlaced: Int get() = boardDelta(beforeRows, afterRows, playerId).placedDice
    val diceMoved: Int get() = boardDelta(beforeRows, afterRows, playerId).movedDice
    val diceValueChanges: Int get() = boardDelta(beforeRows, afterRows, playerId).dieValueChanges
    val diceRerolled: Int get() {
        val rawRolls = chronicleEntries.count { it is GameEntry.DieRolled && it.reason == RollReason.ROLL }
        return max(0, rawRolls - dicePlaced)
    }
    val cardsActivated: Int get() = chronicleEntries.count {
        it is GameEntry.MainAction && it.action == MainActionKind.ACTIVATE_PLANT
    }
    val refreshes: Int get() = chronicleEntries.count { it is GameEntry.Refresh }
    val wounds: Int get() = chronicleEntries.count { it is GameEntry.Wound }
    val plantFacingTransitions: Int get() = plantTransitions(beforePlants, afterPlants)
    val resourcesSpent: Int get() = resourceDelta(beforeResources, afterResources).first
    val resourcesGained: Int get() = resourceDelta(beforeResources, afterResources).second
}

internal data class BattlePacingWindowObserved(
    val index: Int,
    val playerId: PlayerId,
    val stage: String,
    val row: StrikeRow?,
    val unresolvedStrikes: Int,
    val steps: List<BattlePacingStepObserved>,
    val emptyOpportunity: Boolean,
    val passCount: Int,
    val beforeRows: List<BattleGridRowSnapshot>,
    val afterRows: List<BattleGridRowSnapshot>,
    val beforePlants: List<BattleDecisionPlantSnapshot>,
    val afterPlants: List<BattleDecisionPlantSnapshot>
) {
    var reassessment: BattleBoardDelta = BattleBoardDelta.ZERO
    val legalChoiceCount: Int get() = steps.firstOrNull()?.legalChoiceCount ?: 0
    val meaningfulDecisionWindows: Int get() = if (steps.any { it.meaningfulDecision }) 1 else 0
    val actionCount: Int get() = steps.count { !it.pass && it.actionKind != null }
    val normalMainActions: Int get() = steps.count { it.actionKind?.startsWith("MAIN_") == true }
    val sunlightMainActions: Int get() = steps.count { it.actionKind?.startsWith("SUPPORT_SUNLIGHT+") == true }
    val supportActions: Int get() = steps.count { it.actionKind?.startsWith("SUPPORT_") == true }
    val diceRolled: Int get() = steps.sumOf { it.diceRolled }
    val diceRerolled: Int get() = steps.sumOf { it.diceRerolled }
    val dicePlaced: Int get() = steps.sumOf { it.dicePlaced }
    val diceMoved: Int get() = steps.sumOf { it.diceMoved }
    val diceValueChanges: Int get() = steps.sumOf { it.diceValueChanges }
    val cardsActivated: Int get() = steps.sumOf { it.cardsActivated }
    val refreshes: Int get() = steps.sumOf { it.refreshes }
    val plantFacingTransitions: Int get() = steps.sumOf { it.plantFacingTransitions }
    val resourcesSpent: Int get() = steps.sumOf { it.resourcesSpent }
    val resourcesGained: Int get() = steps.sumOf { it.resourcesGained }
}

internal data class BattlePacingObserved(
    val ruleSet: PacingRuleSet,
    val players: List<PlayerId>,
    val windows: List<BattlePacingWindowObserved>,
    val initialPlacedDice: Int,
    val strikeCount: Int,
    val woundCount: Int,
    val doomDiceCount: Int,
    val cleanupDiceCount: Int,
    val returnedCritters: Int,
    val remainingSupportInventory: Map<PlayerId, Int>
) {
    val handoffs: Int get() = windows.zipWithNext().count { (a, b) -> a.playerId != b.playerId }
    val decisionWindows: Int get() = windows.count { !it.emptyOpportunity }
    val meaningfulDecisionWindows: Int get() = windows.sumOf { it.meaningfulDecisionWindows }
    val emptyOpportunities: Int get() = windows.count { it.emptyOpportunity }
    val passes: Int get() = windows.sumOf { it.passCount }
    val normalMainActions: Int get() = windows.sumOf { it.normalMainActions }
    val sunlightMainActions: Int get() = windows.sumOf { it.sunlightMainActions }
    val supportActions: Int get() = windows.sumOf { it.supportActions }
    /** Opening Draw-and-Roll dice plus all action-phase rolls. */
    val diceRolled: Int get() = initialPlacedDice + windows.sumOf { it.diceRolled }
    val diceRerolled: Int get() = windows.sumOf { it.diceRerolled }
    val dicePlaced: Int get() = initialPlacedDice + windows.sumOf { it.dicePlaced }
    val diceMoved: Int get() = windows.sumOf { it.diceMoved }
    val diceValueChanges: Int get() = windows.sumOf { it.diceValueChanges }
    val cardsActivated: Int get() = windows.sumOf { it.cardsActivated }
    val refreshesDuringActions: Int get() = windows.sumOf { it.refreshes }
    val plantFacingTransitions: Int get() = windows.sumOf { it.plantFacingTransitions }
    val resourcesSpent: Int get() = windows.sumOf { it.resourcesSpent }
    val resourcesGained: Int get() = windows.sumOf { it.resourcesGained }
    val reassessmentWindows: Int get() = windows.count { it.reassessment.reassessmentUnits > 0 }
    val complexityUnits: Double get() = windows.sumOf { w -> w.steps.sumOf { it.complexityUnits } }
    val reassessmentUnits: Int get() = windows.sumOf { it.reassessment.reassessmentUnits }
}

internal data class BattleBoardDelta(
    val changedRows: Int,
    val leaderChanges: Int,
    val marginChanges: Int,
    val placedDice: Int,
    val movedDice: Int,
    val dieValueChanges: Int,
    val plantTransitions: Int
) {
    val diceChanges: Int get() = placedDice + movedDice + dieValueChanges
    val reassessmentUnits: Int get() = min(10,
        changedRows + leaderChanges + marginChanges + min(3, diceChanges) + min(2, plantTransitions))

    companion object { val ZERO = BattleBoardDelta(0, 0, 0, 0, 0, 0, 0) }
}

private data class DiePosition(val playerId: PlayerId, val row: StrikeRow, val value: Int, val sides: Int)

private fun diceMap(rows: List<BattleGridRowSnapshot>): Map<Long, DiePosition> = buildMap {
    rows.forEach { row -> row.squares.forEach { square -> square.dice.forEachIndexed { index, die ->
        // Asset IDs are expected in Task-6 snapshots. A stable negative fallback keeps comparisons deterministic.
        val key = die.assetId ?: -((row.row.ordinal.toLong() + 1L) * 1_000_000L + square.playerId.value.toLong() * 10_000L + index.toLong())
        put(key, DiePosition(square.playerId, row.row, die.value, die.sides.value))
    } } }
}

private fun totals(rows: List<BattleGridRowSnapshot>): Map<StrikeRow, Map<PlayerId, Int>> =
    rows.associate { row -> row.row to row.squares.filterNot { it.withdrawn }.associate { it.playerId to it.total } }

private fun leaders(values: Map<PlayerId, Int>): Set<PlayerId> {
    val high = values.values.maxOrNull() ?: return emptySet()
    return values.filterValues { it == high }.keys
}

private fun margin(values: Map<PlayerId, Int>, playerId: PlayerId): Int? {
    val own = values[playerId] ?: return null
    val other = values.filterKeys { it != playerId }.values.maxOrNull() ?: return null
    return own - other
}

private fun plantTransitions(before: List<BattleDecisionPlantSnapshot>, after: List<BattleDecisionPlantSnapshot>): Int {
    val b = before.associateBy { it.playerId }
    return after.sumOf { a ->
        val old = b[a.playerId] ?: return@sumOf 0
        max(abs(a.faceUp - old.faceUp), abs(a.faceDown - old.faceDown))
    }
}

private fun resourceTotal(r: BattleDecisionResourceSnapshot): Int =
    r.bees + r.worms + r.water + r.sunlight + r.mulch + r.wisps + r.readyButterflies

private fun resourceDelta(before: List<BattleDecisionResourceSnapshot>, after: List<BattleDecisionResourceSnapshot>): Pair<Int, Int> {
    val b = before.associateBy { it.playerId }
    var spent = 0
    var gained = 0
    after.forEach { a ->
        val old = b[a.playerId] ?: return@forEach
        val d = resourceTotal(a) - resourceTotal(old)
        if (d < 0) spent += -d else gained += d
    }
    return spent to gained
}

private fun boardDelta(before: List<BattleGridRowSnapshot>, after: List<BattleGridRowSnapshot>, actor: PlayerId): BattleBoardDelta {
    val bt = totals(before); val at = totals(after)
    var changedRows = 0; var leaderChanges = 0; var marginChanges = 0
    StrikeRow.entries.forEach { row ->
        val b = bt[row].orEmpty(); val a = at[row].orEmpty()
        if (b != a) changedRows++
        if (leaders(b) != leaders(a)) leaderChanges++
        if (margin(b, actor) != margin(a, actor)) marginChanges++
    }
    val bd = diceMap(before); val ad = diceMap(after)
    val placed = ad.keys.count { it !in bd }
    val moved = (bd.keys intersect ad.keys).count { key ->
        bd.getValue(key).let { old -> ad.getValue(key).let { now -> old.playerId != now.playerId || old.row != now.row } }
    }
    val values = (bd.keys intersect ad.keys).count { key -> bd.getValue(key).value != ad.getValue(key).value }
    return BattleBoardDelta(changedRows, leaderChanges, marginChanges, placed, moved, values,
        plantTransitions(emptyList(), emptyList()))
}

private fun boardDelta(
    beforeRows: List<BattleGridRowSnapshot>, afterRows: List<BattleGridRowSnapshot>, actor: PlayerId,
    beforePlants: List<BattleDecisionPlantSnapshot>, afterPlants: List<BattleDecisionPlantSnapshot>
): BattleBoardDelta {
    val grid = boardDelta(beforeRows, afterRows, actor)
    return grid.copy(plantTransitions = plantTransitions(beforePlants, afterPlants))
}

private fun actionKind(main: BattleMainAction): String = when (main) {
    BattleMainAction.Draw -> "MAIN_DRAW"
    is BattleMainAction.ActivatePlant -> "MAIN_PLANT"
    BattleMainAction.RoundEffect1 -> "MAIN_ROUND"
    BattleMainAction.RoundEffect2 -> "MAIN_ROUND"
}

private fun actionKind(support: BattleSupportAction): String = when (support) {
    is BattleSupportAction.PlaceCritter -> when (support.critter) {
        Critter.BEE -> "SUPPORT_BEE"
        Critter.WORM -> "SUPPORT_WORM"
    }
    is BattleSupportAction.UseSunlight -> "SUPPORT_SUNLIGHT+${actionKind(support.mainAction)}"
    is BattleSupportAction.Shared -> when (support.action) {
        is SupportAction.PlayWisp -> "SUPPORT_WISP"
        is SupportAction.UseWaterReroll -> "SUPPORT_WATER_REROLL"
        SupportAction.UseWaterRefresh -> "SUPPORT_WATER_REFRESH"
        is SupportAction.UseMulch -> "SUPPORT_MULCH"
        is SupportAction.UseWormFlip -> "SUPPORT_WORM_FLIP"
        is SupportAction.UseButterfly -> "SUPPORT_BUTTERFLY"
    }
}

private fun entriesBetween(game: Game, before: Long, after: Long): List<GameEntry> =
    if (after <= before) emptyList() else game.chronicle.entries.filter { it.sequence > before && it.sequence <= after }

internal fun baselinePacingObserved(
    game: Game,
    actions: BattleActionLoopResult,
    strikes: BattleStrikeResolutionResult,
    doom: DoomResult,
    cleanup: BattleCleanupResult,
    remainingSupports: Map<PlayerId, Int>
): BattlePacingObserved {
    require(actions.decisionWindows.isNotEmpty()) { "Task 6 requires canonical A decision telemetry" }
    val windows = actions.decisionWindows.mapIndexed { index, w ->
        val kind = w.chosenMain?.let(::actionKind) ?: w.chosenSupport?.let(::actionKind)
        val step = if (kind == null) emptyList() else listOf(
            BattlePacingStepObserved(
                playerId = w.playerId, legalMainCount = w.legalMainCount, legalSupportCount = w.legalSupportCount,
                unresolvedStrikes = 3, actionKind = kind, pass = false,
                beforeRows = w.beforeRows, afterRows = w.afterRows,
                beforePlants = w.beforePlants, afterPlants = w.afterPlants,
                beforeResources = w.beforeResources, afterResources = w.afterResources,
                chronicleEntries = entriesBetween(game, w.beforeChronicleSequence, w.afterChronicleSequence)
            )
        )
        BattlePacingWindowObserved(
            index = index + 1, playerId = w.playerId, stage = w.stage.name, row = null, unresolvedStrikes = 3,
            steps = step, emptyOpportunity = kind == null, passCount = 0,
            beforeRows = w.beforeRows, afterRows = w.afterRows,
            beforePlants = w.beforePlants, afterPlants = w.afterPlants
        )
    }.toMutableList()
    assignReassessments(windows)
    val first = windows.first().beforeRows
    return BattlePacingObserved(PacingRuleSet.A_BASELINE, game.players.map { it.id }, windows,
        first.sumOf { row -> row.squares.sumOf { it.dice.size } }, strikes.strikes.size, strikes.totalWounds,
        doom.count, cleanup.totalDiscardedDice, cleanup.totalReturnedCritters, remainingSupports)
}

internal fun candidatePacingObserved(
    game: Game,
    result: ThreeEngagementBattleResult,
    remainingSupports: Map<PlayerId, Int>
): BattlePacingObserved {
    val windows = mutableListOf<BattlePacingWindowObserved>()
    var index = 0
    result.engagements.forEachIndexed { engagementIndex, engagement ->
        val unresolved = 3 - engagementIndex
        engagement.opportunities.forEach { o ->
            val steps = o.decisionSteps.map { s ->
                val kind = when (val c = s.choice) {
                    is EngagementChoice.Main -> actionKind(c.action)
                    is EngagementChoice.Support -> actionKind(c.action)
                    EngagementChoice.Pass -> null
                }
                BattlePacingStepObserved(
                    playerId = o.playerId, legalMainCount = s.legalMainCount, legalSupportCount = s.legalSupportCount,
                    unresolvedStrikes = unresolved, actionKind = kind, pass = s.choice == EngagementChoice.Pass,
                    beforeRows = s.beforeRows, afterRows = s.afterRows,
                    beforePlants = s.beforePlants, afterPlants = s.afterPlants,
                    beforeResources = s.beforeResources, afterResources = s.afterResources,
                    chronicleEntries = entriesBetween(game, s.beforeChronicleSequence, s.afterChronicleSequence)
                )
            }
            windows += BattlePacingWindowObserved(
                index = ++index, playerId = o.playerId, stage = "ENGAGEMENT", row = engagement.order.row,
                unresolvedStrikes = unresolved, steps = steps,
                emptyOpportunity = o.endedByNoLegalChoices && steps.isEmpty(),
                passCount = steps.count { it.pass }, beforeRows = o.beforeRows, afterRows = o.afterRows,
                beforePlants = o.beforePlants, afterPlants = o.afterPlants
            )
        }
    }
    assignReassessments(windows)
    val first = windows.first().beforeRows
    val wounds = result.engagements.sumOf { it.resolution.wounds.size }
    // Candidate cleanup result exposes only totals today; returned Critters are visible in Chronicle Cleanup entries.
    val returned = game.chronicle.entries.filterIsInstance<GameEntry.Cleanup>().takeLast(game.players.size).sumOf { it.returnedCritters }
    return BattlePacingObserved(PacingRuleSet.B_CANDIDATE, game.players.map { it.id }, windows,
        first.sumOf { row -> row.squares.sumOf { it.dice.size } }, result.engagements.size, wounds,
        result.doomCount, result.cleanupDiceCount, returned, remainingSupports)
}

private fun assignReassessments(windows: MutableList<BattlePacingWindowObserved>) {
    val previous = mutableMapOf<PlayerId, BattlePacingWindowObserved>()
    windows.forEach { w ->
        val prior = previous[w.playerId]
        if (prior != null) {
            w.reassessment = boardDelta(prior.afterRows, w.beforeRows, w.playerId, prior.afterPlants, w.beforePlants)
        }
        previous[w.playerId] = w
    }
}

internal data class BattleTimingProfile(
    val name: String,
    val handoffSeconds: Double,
    val decisionBaseSeconds: Double,
    val choiceComplexitySeconds: Double,
    val laterStrikePlanningSeconds: Double,
    val candidateAllocationPremiumSeconds: Double,
    val reassessmentBaseSeconds: Double,
    val reassessmentUnitSeconds: Double,
    val mainDrawSeconds: Double,
    val mainPlantSeconds: Double,
    val mainRoundSeconds: Double,
    val supportCritterSeconds: Double,
    val supportWispSeconds: Double,
    val supportWaterSeconds: Double,
    val supportMulchSeconds: Double,
    val supportWormFlipSeconds: Double,
    val supportButterflySeconds: Double,
    val supportSunlightSeconds: Double,
    val passSeconds: Double,
    val emptyOpportunitySeconds: Double,
    val openingDrawSeconds: Double,
    val rankSeconds: Double,
    val placementPerPlayerSeconds: Double,
    val placementPerDieSeconds: Double,
    val strikeResolveSeconds: Double,
    val woundSeconds: Double,
    val doomSeconds: Double,
    val doomPerDieSeconds: Double,
    val cleanupSeconds: Double,
    val cleanupPerPlayerSeconds: Double,
    val cleanupPerDieSeconds: Double
)

internal object BattleTimingProfiles {
    val defaults = listOf(
        BattleTimingProfile("fast", 1.0, 1.5, 0.7, 0.5, 0.4, 0.5, 0.6,
            2.5, 2.5, 2.0, 1.5, 3.0, 2.0, 2.0, 1.5, 2.5, 3.0,
            0.5, 0.3, 7.0, 2.5, 2.0, 0.7, 2.5, 1.0, 2.0, 0.4, 4.0, 1.0, 0.4),
        BattleTimingProfile("typical", 2.0, 3.0, 1.5, 1.1, 0.9, 1.5, 1.3,
            4.5, 4.5, 3.5, 2.5, 6.0, 4.0, 4.0, 3.0, 5.0, 6.0,
            1.0, 0.6, 12.0, 4.0, 3.5, 1.0, 4.5, 2.0, 3.5, 0.7, 7.0, 1.8, 0.7),
        BattleTimingProfile("analysis-heavy", 3.0, 5.0, 2.7, 2.0, 1.8, 2.5, 2.2,
            6.0, 6.5, 5.0, 3.5, 9.0, 5.5, 5.5, 4.0, 7.0, 9.0,
            1.5, 0.8, 18.0, 6.0, 5.0, 1.3, 6.5, 3.0, 5.0, 1.0, 10.0, 2.5, 1.0)
    )

    val csvHeader = listOf("name","handoffSeconds","decisionBaseSeconds","choiceComplexitySeconds",
        "laterStrikePlanningSeconds","candidateAllocationPremiumSeconds","reassessmentBaseSeconds","reassessmentUnitSeconds",
        "mainDrawSeconds","mainPlantSeconds","mainRoundSeconds","supportCritterSeconds","supportWispSeconds","supportWaterSeconds",
        "supportMulchSeconds","supportWormFlipSeconds","supportButterflySeconds","supportSunlightSeconds","passSeconds",
        "emptyOpportunitySeconds","openingDrawSeconds","rankSeconds","placementPerPlayerSeconds","placementPerDieSeconds",
        "strikeResolveSeconds","woundSeconds","doomSeconds","doomPerDieSeconds","cleanupSeconds","cleanupPerPlayerSeconds","cleanupPerDieSeconds")

    fun load(path: Path?): List<BattleTimingProfile> {
        if (path == null) return defaults
        val lines = Files.readAllLines(path).filter { it.isNotBlank() }
        require(lines.size >= 2) { "Timing model requires header plus at least one profile: $path" }
        require(lines.first().split(',') == csvHeader) { "Timing model header mismatch: $path" }
        return lines.drop(1).map { line ->
            val c = line.split(','); require(c.size == csvHeader.size) { "Bad timing-model row: $line" }
            var i = 0
            val name = c[i++]
            fun d(): Double = c[i++].toDouble()
            BattleTimingProfile(name, d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d(),d())
        }
    }

    fun row(p: BattleTimingProfile): List<Any> = listOf(p.name,p.handoffSeconds,p.decisionBaseSeconds,p.choiceComplexitySeconds,
        p.laterStrikePlanningSeconds,p.candidateAllocationPremiumSeconds,p.reassessmentBaseSeconds,p.reassessmentUnitSeconds,
        p.mainDrawSeconds,p.mainPlantSeconds,p.mainRoundSeconds,p.supportCritterSeconds,p.supportWispSeconds,p.supportWaterSeconds,
        p.supportMulchSeconds,p.supportWormFlipSeconds,p.supportButterflySeconds,p.supportSunlightSeconds,p.passSeconds,
        p.emptyOpportunitySeconds,p.openingDrawSeconds,p.rankSeconds,p.placementPerPlayerSeconds,p.placementPerDieSeconds,
        p.strikeResolveSeconds,p.woundSeconds,p.doomSeconds,p.doomPerDieSeconds,p.cleanupSeconds,p.cleanupPerPlayerSeconds,p.cleanupPerDieSeconds)
}

private data class TimelineSegment(
    val start: Double,
    val end: Double,
    val actor: PlayerId?,
    val allEngaged: Boolean,
    val consequential: Boolean,
    val windowIndex: Int? = null
) { val duration: Double get() = end - start }

internal data class PlayerDowntimeEstimate(
    val playerId: PlayerId,
    val totalWaitingSeconds: Double,
    val longestWaitingSeconds: Double,
    val terminalWaitingSeconds: Double,
    val consequentialWatchingSeconds: Double,
    val noMeaningfulLegalActionSeconds: Double
)

internal data class BattleTimeEstimate(
    val profile: String,
    val totalSeconds: Double,
    val playerDowntime: List<PlayerDowntimeEstimate>,
    val totalDecisionSeconds: Double,
    val totalExecutionSeconds: Double,
    val totalHandoffSeconds: Double,
    val totalReassessmentSeconds: Double,
    val candidateLaterStrikeUnits: Int
)

private fun executionSeconds(kind: String, p: BattleTimingProfile): Double = when {
    kind == "MAIN_DRAW" -> p.mainDrawSeconds
    kind == "MAIN_PLANT" -> p.mainPlantSeconds
    kind == "MAIN_ROUND" -> p.mainRoundSeconds
    kind == "SUPPORT_BEE" || kind == "SUPPORT_WORM" -> p.supportCritterSeconds
    kind == "SUPPORT_WISP" -> p.supportWispSeconds
    kind.startsWith("SUPPORT_WATER") -> p.supportWaterSeconds
    kind == "SUPPORT_MULCH" -> p.supportMulchSeconds
    kind == "SUPPORT_WORM_FLIP" -> p.supportWormFlipSeconds
    kind == "SUPPORT_BUTTERFLY" -> p.supportButterflySeconds
    kind.startsWith("SUPPORT_SUNLIGHT+") -> p.supportSunlightSeconds + when {
        kind.endsWith("MAIN_DRAW") -> p.mainDrawSeconds
        kind.endsWith("MAIN_PLANT") -> p.mainPlantSeconds
        else -> p.mainRoundSeconds
    }
    else -> 0.0
}

internal fun estimateBattleTime(observed: BattlePacingObserved, p: BattleTimingProfile): BattleTimeEstimate {
    val segments = mutableListOf<TimelineSegment>()
    var t = 0.0
    var decisionTotal = 0.0; var executionTotal = 0.0; var handoffTotal = 0.0; var reassessTotal = 0.0
    var laterUnits = 0
    fun add(seconds: Double, actor: PlayerId? = null, engaged: Boolean = false, consequential: Boolean = false, window: Int? = null) {
        if (seconds <= 0) return
        segments += TimelineSegment(t, t + seconds, actor, engaged, consequential, window); t += seconds
    }
    // Draw is simultaneous and Rank is table-wide; both contribute duration but not passive waiting.
    add(p.openingDrawSeconds, engaged = true)
    add(p.rankSeconds, engaged = true)
    val perPlayerDice = observed.initialPlacedDice.toDouble() / observed.players.size
    observed.players.forEach { actor -> add(p.placementPerPlayerSeconds + p.placementPerDieSeconds * perPlayerDice, actor = actor) }

    var priorActor: PlayerId? = null
    observed.windows.forEach { w ->
        if (priorActor != null && priorActor != w.playerId) {
            add(p.handoffSeconds, actor = w.playerId, window = w.index); handoffTotal += p.handoffSeconds
        }
        priorActor = w.playerId
        if (w.reassessment.reassessmentUnits > 0) {
            val seconds = p.reassessmentBaseSeconds + p.reassessmentUnitSeconds * w.reassessment.reassessmentUnits
            add(seconds, actor = w.playerId, window = w.index); reassessTotal += seconds
        }
        if (w.steps.isEmpty()) {
            add(p.emptyOpportunitySeconds, actor = w.playerId, window = w.index)
            decisionTotal += p.emptyOpportunitySeconds
        } else {
            w.steps.forEach { step ->
                if (step.pass) {
                    add(p.passSeconds, actor = w.playerId, window = w.index); decisionTotal += p.passSeconds
                } else {
                    val later = max(0, step.unresolvedStrikes - 1)
                    laterUnits += if (observed.ruleSet == PacingRuleSet.B_CANDIDATE) later else 0
                    val decision = p.decisionBaseSeconds + p.choiceComplexitySeconds * step.complexityUnits +
                        p.laterStrikePlanningSeconds * later +
                        if (observed.ruleSet == PacingRuleSet.B_CANDIDATE) p.candidateAllocationPremiumSeconds * later else 0.0
                    add(decision, actor = w.playerId, window = w.index); decisionTotal += decision
                    val execution = step.actionKind?.let { executionSeconds(it, p) } ?: 0.0
                    val consequential = boardDelta(step.beforeRows, step.afterRows, step.playerId,
                        step.beforePlants, step.afterPlants).reassessmentUnits > 0 || step.resourcesSpent > 0 || step.resourcesGained > 0
                    add(execution, actor = w.playerId, consequential = consequential, window = w.index); executionTotal += execution
                }
            }
        }
        // Candidate resolves the row immediately after the last opportunity in each Engagement.
        if (observed.ruleSet == PacingRuleSet.B_CANDIDATE && w.row != null) {
            val sameRowNext = observed.windows.getOrNull(w.index)?.row == w.row // index is 1-based, so getOrNull(index) is next
            if (!sameRowNext) add(p.strikeResolveSeconds, engaged = true, consequential = true)
        }
    }
    if (observed.ruleSet == PacingRuleSet.A_BASELINE) repeat(observed.strikeCount) {
        add(p.strikeResolveSeconds, engaged = true, consequential = true)
    }
    add(p.woundSeconds * observed.woundCount, engaged = true, consequential = true)
    add(p.doomSeconds + p.doomPerDieSeconds * observed.doomDiceCount, engaged = true)
    add(p.cleanupSeconds + p.cleanupPerPlayerSeconds * observed.players.size + p.cleanupPerDieSeconds * observed.cleanupDiceCount, engaged = true)

    val finalTime = t
    val windowsByPlayer = observed.windows.groupBy { it.playerId }
    val downtime = observed.players.map { player ->
        val passive = segments.filter { it.actor != player && !it.allEngaged }
        val totalWaiting = passive.sumOf { it.duration }
        var longest = 0.0; var running = 0.0
        segments.forEach { seg ->
            if (seg.actor != player && !seg.allEngaged) { running += seg.duration; longest = max(longest, running) }
            else running = 0.0
        }
        val ownWindows = windowsByPlayer[player].orEmpty()
        val lastMeaningfulWindow = ownWindows.lastOrNull { w -> w.steps.any { !it.pass && it.actionKind != null } }
        val lastLegalWindow = ownWindows.lastOrNull { w -> w.steps.any { it.legalChoiceCount > 0 } }
        fun windowEnd(index: Int?): Double = if (index == null) 0.0 else segments.filter { it.windowIndex == index }.maxOfOrNull { it.end } ?: 0.0
        val terminal = max(0.0, finalTime - windowEnd(lastMeaningfulWindow?.index))
        val noLegal = max(0.0, finalTime - windowEnd(lastLegalWindow?.index))
        val consequentialWatching = segments.filter { seg ->
            if (seg.actor == null || seg.actor == player || !seg.consequential) false
            else ownWindows.any { it.index > (seg.windowIndex ?: 0) }
        }.sumOf { it.duration }
        PlayerDowntimeEstimate(player, totalWaiting, longest, terminal, consequentialWatching, noLegal)
    }
    return BattleTimeEstimate(p.name, finalTime, downtime, decisionTotal, executionTotal, handoffTotal, reassessTotal, laterUnits)
}
