package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.battle.domain.BattleGridRowSnapshot
import java.util.ArrayDeque

/**
 * In-memory Chronicle for one game/simulation.
 *
 * Recording is synchronized so sequence allocation, scoped buffering, Moment
 * transformation, and insertion happen atomically relative to other Chronicle
 * operations. A scope deliberately holds the Chronicle lock while game logic in
 * that scope executes; JVM monitors are re-entrant, so nested record/scoped calls
 * remain safe while unrelated threads cannot interleave Chronicle sequences.
 */
class GameChronicle : Chronicle {

    private data class ScopeBuffer(
        val entries: MutableList<GameEntry> = mutableListOf()
    )

    private val lock = Any()
    private val storedEntries = mutableListOf<GameEntry>()
    private val scopeStack = ArrayDeque<ScopeBuffer>()
    private var nextSequence = 1L

    override fun record(moment: Moment): GameEntry =
        synchronized(lock) {
            recordLocked(moment)
        }

    override fun <T> scoped(
        parent: () -> Moment,
        block: () -> T
    ): T = synchronized(lock) {
        val startSequence = nextSequence
        val parentSequence = nextSequence++
        val parentDepth = scopeStack.size
        val buffer = ScopeBuffer()
        scopeStack.addLast(buffer)
        var scopeIsActive = true

        try {
            val result = block()
            scopeStack.removeLast()
            scopeIsActive = false

            val parentEntry = transform(
                sequence = parentSequence,
                moment = parent(),
                hierarchyDepth = parentDepth
            )
            val committed = buildList {
                add(parentEntry)
                addAll(buffer.entries)
            }

            if (scopeStack.isEmpty()) {
                storedEntries.addAll(committed)
            } else {
                scopeStack.last().entries.addAll(committed)
            }

            result
        } catch (throwable: Throwable) {
            if (scopeIsActive) {
                scopeStack.removeLast()
            }
            nextSequence = startSequence
            throw throwable
        }
    }

    override val entries: List<GameEntry>
        get() = synchronized(lock) {
            storedEntries.toList()
        }

    override fun entriesAfter(sequence: Long): List<GameEntry> {
        require(sequence >= 0) {
            "Sequence cannot be negative: $sequence"
        }

        return synchronized(lock) {
            storedEntries
                .filter { it.sequence > sequence }
                .toList()
        }
    }

    override fun clear() {
        synchronized(lock) {
            check(scopeStack.isEmpty()) {
                "Cannot clear Chronicle while a scoped Chronicle transaction is active"
            }
            storedEntries.clear()
            nextSequence = 1L
        }
    }

    private fun recordLocked(moment: Moment): GameEntry {
        val entry = transform(
            sequence = nextSequence,
            moment = moment,
            hierarchyDepth = scopeStack.size
        )
        nextSequence++

        if (scopeStack.isEmpty()) {
            storedEntries.add(entry)
        } else {
            scopeStack.last().entries.add(entry)
        }

        return entry
    }

    private fun transform(
        sequence: Long,
        moment: Moment,
        hierarchyDepth: Int
    ): GameEntry =
        when (moment) {
            is Moment.Marker -> GameEntry.Marker(sequence, moment.message, hierarchyDepth)
            is Moment.RoundRevealed -> GameEntry.RoundRevealed(
                sequence, moment.roundNumber, moment.cardName, moment.cardType,
                moment.firstEffect, moment.secondEffect, hierarchyDepth
            )
            is Moment.RoundCompleted -> GameEntry.RoundCompleted(
                sequence, moment.roundNumber, moment.cardName, moment.cardType,
                moment.playerSummaries.map { summary ->
                    summary.copy(
                        supplyDice = summary.supplyDice.toList(),
                        discardDice = summary.discardDice.toList(),
                        mulchDice = summary.mulchDice.toList(),
                        butterflies = summary.butterflies.toList()
                    )
                }, hierarchyDepth
            )
            is Moment.DieRolled -> GameEntry.DieRolled(
                sequence, moment.playerId, moment.sides, moment.value,
                moment.rewardPolicy, moment.reason, hierarchyDepth
            )
            is Moment.RollReward -> GameEntry.RollReward(
                sequence, moment.playerId, moment.kind, moment.critter, moment.wispName,
                hierarchyDepth
            )
            is Moment.OpeningDrawCompleted -> GameEntry.OpeningDrawCompleted(
                sequence, moment.phase, moment.playerId, moment.count, hierarchyDepth
            )
            is Moment.WispAcquired -> GameEntry.WispAcquired(
                sequence, moment.playerId, moment.wispName, moment.sourceKind,
                moment.sourceName, moment.playedImmediately, hierarchyDepth
            )
            is Moment.WispPlayDecision -> GameEntry.WispPlayDecision(
                sequence = sequence, playerId = moment.playerId, phase = moment.phase,
                legalWispNames = moment.legalWispNames.toList(), selectedWispName = moment.selectedWispName,
                referenceWispName = moment.referenceWispName, supportPassNumber = moment.supportPassNumber,
                mainActionsRemaining = moment.mainActionsRemaining, hierarchyDepth = hierarchyDepth
            )
            is Moment.RoundEffectOpportunity -> GameEntry.RoundEffectOpportunity(
                sequence = sequence,
                playerId = moment.playerId,
                phase = moment.phase,
                roundCardName = moment.roundCardName,
                firstEffect = moment.firstEffect,
                secondEffect = moment.secondEffect,
                firstExecutable = moment.firstExecutable,
                secondExecutable = moment.secondExecutable,
                hierarchyDepth = hierarchyDepth,
                firstBlockedBySharedResource = moment.firstBlockedBySharedResource,
                secondBlockedBySharedResource = moment.secondBlockedBySharedResource
            )
            is Moment.RoundEffectChoice -> GameEntry.RoundEffectChoice(
                sequence = sequence,
                playerId = moment.playerId,
                phase = moment.phase,
                roundCardName = moment.roundCardName,
                firstEffect = moment.firstEffect,
                secondEffect = moment.secondEffect,
                firstExecutable = moment.firstExecutable,
                secondExecutable = moment.secondExecutable,
                legalMainActions = moment.legalMainActions.toList(),
                selectedMainAction = moment.selectedMainAction,
                sunlightHeld = moment.sunlightHeld,
                battlesRemaining = moment.battlesRemaining,
                battleNext = moment.battleNext,
                humanBaselineSelectedMainAction = moment.humanBaselineSelectedMainAction,
                legalPlantCardNames = moment.legalPlantCardNames.toList(),
                selectedPlantCardName = moment.selectedPlantCardName,
                humanBaselineSelectedPlantCardName = moment.humanBaselineSelectedPlantCardName,
                battleStage = moment.battleStage,
                hierarchyDepth = hierarchyDepth
            )
            is Moment.MainAction -> GameEntry.MainAction(
                sequence, moment.playerId, moment.phase, moment.action,
                moment.actionNumber, moment.battleStage, moment.decisionProbabilityPercent,
                hierarchyDepth
            )
            is Moment.SupportAction -> GameEntry.SupportAction(
                sequence, moment.playerId, moment.phase, moment.action, moment.row,
                moment.wispUsePercentage, hierarchyDepth
            )
            is Moment.CultivationSupportDecision -> GameEntry.CultivationSupportDecision(
                sequence = sequence,
                playerId = moment.playerId,
                mainActionsRemaining = moment.mainActionsRemaining,
                legalActionIds = moment.legalActionIds.toList(),
                selectedActionId = moment.selectedActionId,
                referenceActionId = moment.referenceActionId,
                faceDownPlants = moment.faceDownPlants,
                spentButterflies = moment.spentButterflies,
                hierarchyDepth = hierarchyDepth
            )
            is Moment.PlantEffectDecision -> GameEntry.PlantEffectDecision(
                sequence = sequence,
                playerId = moment.playerId,
                phase = moment.phase,
                plantId = moment.plantId,
                effect = moment.effect,
                decisionKind = moment.decisionKind,
                legalChoiceIds = moment.legalChoiceIds.toList(),
                selectedChoiceId = moment.selectedChoiceId,
                referenceChoiceId = moment.referenceChoiceId,
                hierarchyDepth = hierarchyDepth
            )
            is Moment.EffectResolved -> GameEntry.EffectResolved(
                sequence, moment.playerId, moment.effect, moment.sourceKind,
                moment.sourceName, moment.phase, hierarchyDepth
            )
            is Moment.RootAndScootWithdrawal -> GameEntry.RootAndScootWithdrawal(
                sequence = sequence,
                playerId = moment.playerId,
                row = moment.row,
                rowSnapshot = copyGridRow(moment.rowSnapshot),
                hierarchyDepth = hierarchyDepth
            )
            is Moment.DecisionReasoning -> GameEntry.DecisionReasoning(
                sequence = sequence,
                playerId = moment.playerId,
                choiceLabel = moment.choiceLabel,
                baseScore = moment.baseScore,
                adjustments = moment.adjustments.toList(),
                total = moment.total,
                observations = moment.observations.toMap(),
                alternatives = moment.alternatives.toList(),
                hierarchyDepth = hierarchyDepth
            )
            is Moment.BuyOrder -> GameEntry.BuyOrder(
                sequence = sequence,
                order = moment.order.toList(),
                resources = moment.resources.map { resource ->
                    resource.copy(
                        dice = resource.dice.toList(),
                        critters = resource.critters.toList()
                    )
                },
                hierarchyDepth = hierarchyDepth
            )
            is Moment.Purchase -> GameEntry.Purchase(
                sequence, moment.playerId, moment.kind, moment.itemName,
                moment.cost, moment.paymentTotal, hierarchyDepth
            )
            is Moment.Graft -> GameEntry.Graft(
                sequence, moment.playerId, moment.plantName, hierarchyDepth
            )
            is Moment.BattleOrder -> GameEntry.BattleOrder(
                sequence, moment.order.toList(), moment.initialDiceCount,
                moment.highestDice.toList(), hierarchyDepth
            )
            is Moment.BattleGridReport -> GameEntry.BattleGridReport(
                sequence = sequence,
                kind = moment.kind,
                passNumber = moment.passNumber,
                rows = moment.rows.map(::copyGridRow),
                hierarchyDepth = hierarchyDepth
            )
            is Moment.BattleResolvePreview -> GameEntry.BattleResolvePreview(
                sequence = sequence,
                rows = moment.rows.map(::copyGridRow),
                hierarchyDepth = hierarchyDepth
            )
            is Moment.BattleDieRow -> GameEntry.BattleDieRow(
                sequence = sequence,
                rollSequence = moment.rollSequence,
                playerId = moment.playerId,
                sides = moment.sides,
                value = moment.value,
                row = moment.row,
                hierarchyDepth = hierarchyDepth
            )
            is Moment.ButterflyState -> GameEntry.ButterflyState(
                sequence = sequence,
                playerId = moment.playerId,
                butterflies = moment.butterflies.toList(),
                hierarchyDepth = hierarchyDepth
            )
            is Moment.StrikeResolved -> GameEntry.StrikeResolved(
                sequence = sequence,
                row = moment.row,
                totals = moment.totals.toList(),
                rowSnapshot = moment.rowSnapshot?.let(::copyGridRow),
                winnerIds = moment.winnerIds.toList(),
                woundedPlayerIds = moment.woundedPlayerIds.toList(),
                vpPerWinner = moment.vpPerWinner,
                contributionLedger = moment.contributionLedger,
                hierarchyDepth = hierarchyDepth
            )
            is Moment.Wound -> GameEntry.Wound(
                sequence, moment.playerId, moment.kind, moment.plantName, hierarchyDepth
            )
            is Moment.Doom -> GameEntry.Doom(sequence, moment.dice.toList(), hierarchyDepth)
            is Moment.Refresh -> GameEntry.Refresh(sequence, moment.playerId, hierarchyDepth)
            is Moment.Cleanup -> GameEntry.Cleanup(
                sequence, moment.playerId, moment.phase, moment.discardedDice,
                moment.returnedCritters, moment.refreshed, hierarchyDepth
            )
            is Moment.Upgrade -> GameEntry.Upgrade(
                sequence, moment.playerId, moment.from, moment.to, moment.destination,
                moment.fromValue, hierarchyDepth
            )
            is Moment.DieGained -> GameEntry.DieGained(
                sequence, moment.playerId, moment.sides, hierarchyDepth
            )
            is Moment.SunlightTokenChanged -> GameEntry.SunlightTokenChanged(
                sequence, moment.playerId, moment.change, hierarchyDepth
            )
            is Moment.SunlightSupportOpportunity -> GameEntry.SunlightSupportOpportunity(
                sequence, moment.playerId, moment.legalExtraMainActions, hierarchyDepth
            )
            is Moment.SunlightMainAction -> GameEntry.SunlightMainAction(
                sequence, moment.playerId, moment.action, moment.plantCardId,
                moment.plantName, moment.plantEffect, hierarchyDepth
            )
            is Moment.MulchStored -> GameEntry.MulchStored(
                sequence, moment.playerId, moment.sides, moment.value, moment.fromDiscard,
                hierarchyDepth
            )
            is Moment.DieValueChanged -> GameEntry.DieValueChanged(
                sequence, moment.playerId, moment.effect, moment.sides,
                moment.before, moment.after, hierarchyDepth
            )
            is Moment.TrashDie -> GameEntry.TrashDie(
                sequence, moment.playerId, moment.sides, moment.destination, hierarchyDepth
            )
            is Moment.FinalScore -> GameEntry.FinalScore(
                sequence = sequence, playerId = moment.playerId, existingVp = moment.existingVp,
                plantVp = moment.plantVp, unplayedWispVp = moment.unplayedWispVp,
                totalVp = moment.totalVp, graftedPlantCount = moment.graftedPlantCount,
                unplayedWispNames = moment.unplayedWispNames.toList(), hierarchyDepth = hierarchyDepth
            )
            is Moment.FinalWinners -> GameEntry.FinalWinners(
                sequence, moment.winnerIds.toList(), hierarchyDepth
            )
            is Moment.GameCompleted -> GameEntry.GameCompleted(
                sequence, moment.roundsCompleted, hierarchyDepth
            )
        }

    private fun copyGridRow(row: BattleGridRowSnapshot): BattleGridRowSnapshot =
        row.copy(
            squares = row.squares.map { square ->
                square.copy(
                    dice = square.dice.toList(),
                    critters = square.critters.toList()
                )
            }
        )
}
