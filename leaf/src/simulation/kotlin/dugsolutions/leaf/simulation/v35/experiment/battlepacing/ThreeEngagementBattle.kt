package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.StrikeResolution
import dugsolutions.leaf.v35.battle.StrikeResolver
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.game.round.battle.BattleMainActionStage
import dugsolutions.leaf.v35.game.round.battle.BattleRound
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.domain.WispCard
import java.util.Random

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
    val context: DecisionContext
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

/** Explicit deck exclusion on BOTH A and B arms, before the first Cultivation round. */
object BattlePacingWispDeck {
    fun withoutResolve(manager: WispCardManager, seed: Long): List<WispCard> {
        val cards = manager.getAllCards().cards.filterNot {
            it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
        }.flatMap { card -> List(card.quantity) { card } }.toMutableList()
        check(cards.isNotEmpty()) { "No experimental Wisps" }
        java.util.Collections.shuffle(cards, Random(seed))
        return cards
    }
}

/** Snapshot uses reference identity, not Die.equals (which compares current value). */
private class LockedStrikeEvidence(battle: BattleState, val row: StrikeRow) {
    private val entries = battle.grid.playerIdsInGridOrder.map { id ->
        val square = battle.grid.square(id, row)
        Triple(id, square.dice.map { it to it.value }, square.critters.toList())
    }

    fun verify(battle: BattleState) {
        check(battle.grid.isRowClosed(row)) { "Locked Strike reopened: $row" }
        entries.forEach { (id, dice, critters) ->
            val square = battle.grid.square(id, row)
            check(square.dice.size == dice.size && square.dice.zip(dice).all { (actual, expected) ->
                actual === expected.first && actual.value == expected.second
            }) { "Locked Strike dice changed: row=$row player=${id.value}" }
            check(square.critters == critters) { "Locked Strike Critters changed: row=$row player=${id.value}" }
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

data class EngagementResult(
    val order: EngagementOrder,
    val actions: List<EngagementActionRecord>,
    val resolution: StrikeResolution
)

data class ThreeEngagementBattleResult(
    val engagements: List<EngagementResult>,
    val doomCount: Int,
    val cleanupDiceCount: Int,
    val trace: List<String>
)

/**
 * Research-only Candidate B. Called on a real, independently replayed Game/Battle opening.
 * The existing BattleRound remains the v37.03 control implementation.
 *
 * All chosen actions run through the canonical legal generator and executors;
 * violations of locked-row invariants fail the experiment rather than emit invalid outcomes.
 */
class ThreeEngagementBattle(private val battleRound: BattleRound) {
    fun execute(
        game: Game,
        battle: BattleState,
        policy: EngagementPolicy = MechanicalEngagementPolicy
    ): ThreeEngagementBattleResult {
        val rawCard = requireNotNull(game.currentRound)
        val card = game.config.roundValues.cardFor(rawCard)
        require(game.players.map { it.id }.toSet() == battle.playerIdsInBattleOrder.toSet())
        check(game.grove.wispDeck.cards.cards.none {
            it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
        } && game.players.all { player -> player.wisps.cards.cards.none {
            it.effect == GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW
        } }) { "Candidate B requires a Resolve-free deck from game creation" }

        val actions = battleRound.researchActionCoordinator(game, battle)
        val normalUsed = game.players.associate { it.id to 0 }.toMutableMap()
        val locked = mutableListOf<LockedStrikeEvidence>()
        val trace = mutableListOf<String>()
        val engagements = StrikeRow.entries.map { row ->
            locked.forEach { it.verify(battle) }
            val order = EngagementOrderSelector.select(battle, game.players.map { it.id }, row)
            trace += order.traceLines()
            val records = mutableListOf<EngagementActionRecord>()
            for (id in order.clockwiseActors) {
                val player = battle.player(id)
                var normalThisTurn = false
                var supportsThisTurn = 0
                var actionCount = 0
                while (true) {
                    val remaining = 2 - normalUsed.getValue(id)
                    val mains = if (remaining > 0 && !normalThisTurn) {
                        actions.researchLegalMains(game, player, card, battle)
                    } else emptyList()
                    val supports = if (supportsThisTurn < 2) {
                        actions.researchLegalSupports(game, player, card, battle)
                    } else emptyList()
                    if (mains.isEmpty() && supports.isEmpty()) break
                    val next = policy.choose(
                        EngagementDecision(row, id, actionCount, remaining, mains, supports,
                            battleRound.researchDecisionContext(game, player, battle))
                    )
                    when (next) {
                        EngagementChoice.Pass -> break
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
                    locked.forEach { it.verify(battle) }
                    actionCount++
                    records += EngagementActionRecord(row, id, next, 2 - normalUsed.getValue(id), supportsThisTurn)
                    trace += "ENGAGEMENT_ACTION,row=$row,player=${id.value},choice=$next,normalUsed=${normalUsed.getValue(id)},supportsThisRow=$supportsThisTurn"
                    check(actionCount <= 3) { "Engagement exceeded its one-normal/two-Support allowance" }
                }
            }
            val resolution = StrikeResolver(WoundResolver(
                grove = game.grove, chronicle = game.chronicle,
                decisionContext = { player -> battleRound.researchDecisionContext(game, player, battle) }
            )).resolveRow(game, battle, row)
            battle.lockResolvedRow(row, game.assetProvenance)
            locked += LockedStrikeEvidence(battle, row)
            trace += "ENGAGEMENT_RESOLVE,row=$row,winners=${resolution.winnerIds.joinToString("|") { it.value.toString() }},wounds=${resolution.wounds.size},vpEach=${resolution.vpPerWinner}"
            EngagementResult(order, records.toList(), resolution)
        }
        locked.forEach { it.verify(battle) }
        // Post-resolution removal permits Doom and Cleanup, never another action.
        battle.grid.beginPostBattleRemoval()
        val doom = battleRound.executeDoom(game, battle)
        val cleanup = battleRound.executeCleanup(game, battle)
        return ThreeEngagementBattleResult(engagements, doom.count, cleanup.totalDiscardedDice, trace)
    }
}
