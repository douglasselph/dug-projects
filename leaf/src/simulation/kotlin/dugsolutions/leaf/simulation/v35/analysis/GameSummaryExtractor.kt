package dugsolutions.leaf.simulation.v35.analysis

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.chronicle.domain.SunlightTokenChange
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameRunResult
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.SharedTokenResource

/** Collapses one completed production Game into a compact immutable research record. */
object GameSummaryExtractor {

    fun extract(game: Game, runResult: GameRunResult): GameSummary {
        require(game.isComplete) { "Game summary can be extracted only from a completed game" }
        require(runResult.roundsCompleted >= 0) { "Completed round count cannot be negative" }

        val scoresByPlayer = runResult.finalScoring.scores.associateBy { it.playerId }
        val winnerIds = runResult.finalScoring.winnerIds.toList()
        val winnerShare = if (winnerIds.isEmpty()) 0.0 else 1.0 / winnerIds.size.toDouble()
        val entries = game.chronicle.entries

        val playerSummaries = game.players.mapIndexed { seat, player ->
            val score = requireNotNull(scoresByPlayer[player.id]) {
                "Final scoring is missing player ${player.id}"
            }

            PlayerGameSummary(
                seat = seat,
                playerId = player.id,
                won = player.id in winnerIds,
                winShare = if (player.id in winnerIds) winnerShare else 0.0,
                existingVp = score.existingVp,
                plantVp = score.plantVp,
                unplayedWispVp = score.unplayedWispVp,
                totalVp = score.totalVp,
                battleStrikeVp = battleStrikeVp(entries, player.id),
                woundsTaken = woundsTaken(entries, player.id),
                rollRewardWispsGained = rollRewardWispsGained(entries, player.id),
                wispsPlayed = wispsPlayed(entries, player.id),
                finalWispCount = player.wisps.size,
                sunlightGained = sunlightChanges(entries, player.id, SunlightTokenChange.GAINED),
                sunlightSpent = sunlightChanges(entries, player.id, SunlightTokenChange.SPENT),
                finalSunlightCount = player.tokens.sunlightCount,
                sunlightSupportOpportunities = sunlightSupportOpportunities(entries, player.id),
                sunlightSupportUses = sunlightSupportUses(entries, player.id),
                sunlightExtraMainActions = sunlightMainActions(entries, player.id).size,
                sunlightExtraDrawActions = sunlightMainActions(entries, player.id).count { it.action == dugsolutions.leaf.v35.chronicle.domain.MainActionKind.DRAW },
                sunlightExtraPlantActions = sunlightMainActions(entries, player.id).count { it.action == dugsolutions.leaf.v35.chronicle.domain.MainActionKind.ACTIVATE_PLANT },
                sunlightExtraRoundEffectActions = sunlightMainActions(entries, player.id).count {
                    it.action == dugsolutions.leaf.v35.chronicle.domain.MainActionKind.ROUND_EFFECT_1 ||
                        it.action == dugsolutions.leaf.v35.chronicle.domain.MainActionKind.ROUND_EFFECT_2
                },
                sunlightPlantActivationIds = sunlightMainActions(entries, player.id).mapNotNull { it.plantCardId },
                sunlightImmediateStrikeContributions = game.assetProvenance.immediateDieEffects.count {
                    it.playerId == player.id && it.sunlightFunded && it.contributedToResolvedStrike
                },
                sunlightWinningStrikeContributions = game.assetProvenance.immediateDieEffects.count {
                    it.playerId == player.id && it.sunlightFunded && it.contributedToWinningStrike
                },
                sunlightWinnerDecisiveContributions = game.assetProvenance.immediateDieEffects.count {
                    it.playerId == player.id && it.sunlightFunded && it.individuallyWinnerDecisive
                },
                sunlightWoundDecisiveContributions = game.assetProvenance.immediateDieEffects.count {
                    it.playerId == player.id && it.sunlightFunded && it.individuallyWoundDecisive
                },
                sunlightAssociatedBattleVp = game.assetProvenance.immediateDieEffects
                    .filter { it.playerId == player.id && it.sunlightFunded }
                    .sumOf { it.associatedBattleVp },
                finalPlantCount = player.creature.size,
                finalPlantPrintedCost = player.creature.cards.sumOf { it.card.cost },
                plantCreatureSignature = plantCreatureSignature(player),
                finalDiceCount = finalDiceCount(player),
                finalDicePower = finalDicePower(player),
                ownedDiceSignature = ownedDiceSignature(player),
                roundEffectConsequences = roundEffectConsequences(game, player.id)
            )
        }

        return GameSummary(
            mechanicalSeed = game.config.seed,
            strategySeed = game.config.strategySeed,
            roundsCompleted = runResult.roundsCompleted,
            winnerIds = winnerIds,
            players = playerSummaries,
            sharedTokenEconomy = sharedTokenEconomy(game)
        )
    }



    private fun roundEffectConsequences(game: Game, playerId: PlayerId): List<RoundEffectConsequenceSummary> =
        game.assetProvenance.roundEffectConsequences
            .filter { it.playerId == playerId }
            .map { lineage ->
                val linked = game.assetProvenance.immediateDieEffects.filter {
                    it.playerId == playerId && it.roundEffectLineageId == lineage.lineageId
                }
                RoundEffectConsequenceSummary(
                    lineageId = lineage.lineageId,
                    effect = lineage.effect,
                    roundCardName = lineage.roundCardName,
                    roundSlot = lineage.roundSlot,
                    spentOrUsed = lineage.spentOrUsed,
                    useKind = lineage.useKind,
                    fundedMainAction = lineage.fundedMainAction,
                    fundedPlantName = lineage.fundedPlantName,
                    sourceDieSides = lineage.sourceDieSides,
                    sourceDieValue = lineage.sourceDieValue,
                    resultDieSides = lineage.resultDieSides,
                    resultDieValue = lineage.resultDieValue,
                    waterRerollDelta = if (lineage.waterRerollBefore != null && lineage.waterRerollAfter != null)
                        lineage.waterRerollAfter!! - lineage.waterRerollBefore!! else null,
                    waterRefreshPlants = lineage.waterRefreshPlants,
                    waterRefreshButterflies = lineage.waterRefreshButterflies,
                    placedInBattle = lineage.placedInBattle || linked.any { it.placedInBattle },
                    contributedToWinningStrike = lineage.contributedToWinningStrike || linked.any { it.contributedToWinningStrike },
                    individuallyWinnerDecisive = lineage.individuallyWinnerDecisive || linked.any { it.individuallyWinnerDecisive },
                    individuallyWoundDecisive = lineage.individuallyWoundDecisive || linked.any { it.individuallyWoundDecisive },
                    associatedBattleVp = maxOf(
                        lineage.associatedBattleVp,
                        linked.sumOf { it.associatedBattleVp }
                    ),
                    linkedImmediateEffects = linked.size,
                    linkedImmediateDelta = linked.sumOf { it.magnitude },
                    linkedImmediateWinningContributions = linked.count { it.contributedToWinningStrike },
                    linkedImmediateWinnerDecisive = linked.count { it.individuallyWinnerDecisive },
                    linkedImmediateWoundDecisive = linked.count { it.individuallyWoundDecisive }
                )
            }

    private fun sharedTokenEconomy(game: Game): List<SharedTokenEconomySummary> =
        game.grove.sharedTokenEconomy.snapshots().map { snapshot ->
            val actualFinalGroveSupply = groveSupply(game, snapshot.resource)
            require(snapshot.trackedFinalGroveSupply == actualFinalGroveSupply) {
                "Shared-token tracker drift for ${snapshot.resource}: tracked=${snapshot.trackedFinalGroveSupply}, actual=$actualFinalGroveSupply"
            }
            SharedTokenEconomySummary(
                resource = snapshot.resource,
                startingGroveSupply = snapshot.startingSupply,
                gainAttempts = snapshot.gainAttempts,
                successfulGains = snapshot.successfulGains,
                failedGainsEmptyGrove = snapshot.failedEmptyGains,
                spendsOrUses = snapshot.returnsToGrove,
                returnsToGrove = snapshot.returnsToGrove,
                finalGroveSupply = actualFinalGroveSupply,
                finalHeldByPlayers = game.players.sumOf { player ->
                    when (snapshot.resource) {
                        SharedTokenResource.WATER -> player.tokens.waterCount
                        SharedTokenResource.SUNLIGHT -> player.tokens.sunlightCount
                        SharedTokenResource.MULCH -> player.tokens.mulchCount + player.tokens.pendingMulchCount
                        SharedTokenResource.BEE -> player.critters.count(Critter.BEE)
                        SharedTokenResource.WORM -> player.critters.count(Critter.WORM)
                        SharedTokenResource.BUTTERFLY -> player.butterflies.size
                    }
                },
                minimumGroveSupply = snapshot.minimumGroveSupply,
                maximumOutsideGrove = snapshot.maximumOutsideGrove,
                reachedZero = snapshot.reachedZero,
                timesReachedZero = snapshot.timesReachedZero,
                emptySupplyObservations = snapshot.emptySupplyObservations
            )
        }

    private fun groveSupply(game: Game, resource: SharedTokenResource): Int = when (resource) {
        SharedTokenResource.WATER -> game.grove.tokens.waterCount
        SharedTokenResource.SUNLIGHT -> game.grove.tokens.sunlightCount
        SharedTokenResource.MULCH -> game.grove.tokens.mulchCount
        SharedTokenResource.BEE -> game.grove.critters.count(Critter.BEE)
        SharedTokenResource.WORM -> game.grove.critters.count(Critter.WORM)
        SharedTokenResource.BUTTERFLY -> game.grove.butterflies.size
    }

    private fun battleStrikeVp(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.filterIsInstance<GameEntry.StrikeResolved>()
            .sumOf { strike -> if (playerId in strike.winnerIds) strike.vpPerWinner else 0 }

    private fun woundsTaken(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count { it is GameEntry.Wound && it.playerId == playerId }

    private fun rollRewardWispsGained(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count { entry ->
            entry is GameEntry.RollReward &&
                entry.playerId == playerId &&
                entry.kind in WISP_GAIN_REWARD_KINDS
        }

    private fun wispsPlayed(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count { entry ->
            entry is GameEntry.SupportAction &&
                entry.playerId == playerId &&
                entry.action == SupportActionKind.WISP
        } + entries.count { entry ->
            entry is GameEntry.RollReward &&
                entry.playerId == playerId &&
                entry.kind == RollRewardKind.WISP_PLAYED_IMMEDIATELY
        }


    private fun sunlightChanges(
        entries: List<GameEntry>,
        playerId: PlayerId,
        change: SunlightTokenChange
    ): Int = entries.count { entry ->
        entry is GameEntry.SunlightTokenChanged &&
            entry.playerId == playerId &&
            entry.change == change
    }

    private fun sunlightSupportOpportunities(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count { it is GameEntry.SunlightSupportOpportunity && it.playerId == playerId }

    private fun sunlightSupportUses(entries: List<GameEntry>, playerId: PlayerId): Int =
        entries.count {
            it is GameEntry.SupportAction &&
                it.playerId == playerId &&
                it.action == SupportActionKind.SUNLIGHT
        }

    private fun sunlightMainActions(entries: List<GameEntry>, playerId: PlayerId): List<GameEntry.SunlightMainAction> =
        entries.filterIsInstance<GameEntry.SunlightMainAction>().filter { it.playerId == playerId }

    private fun plantCreatureSignature(player: Player): PlantCreatureSignature =
        PlantCreatureSignature(
            cards = player.creature.cards
                .map { creatureCard ->
                    PlantCreatureCardSignature(
                        plantName = creatureCard.card.name,
                        side = creatureCard.side,
                        x = creatureCard.position.x,
                        y = creatureCard.position.y
                    )
                }
                .sortedWith(
                    compareBy<PlantCreatureCardSignature>(
                        { it.y },
                        { it.x },
                        { it.side.ordinal },
                        { it.plantName }
                    )
                )
        )

    private fun ownedDiceSignature(player: Player): OwnedDiceSignature {
        val sideValues = buildList {
            addAll((player.dice.supply + player.dice.hand + player.dice.discard).map { it.sides })
            addAll(player.tokens.mulchTokens.mapNotNull { it.sides?.value })
            addAll(player.tokens.pendingMulchTokens.mapNotNull { it.sides?.value })
        }

        fun count(sides: DieSides): Int = sideValues.count { it == sides.value }

        return OwnedDiceSignature(
            d4 = count(DieSides.D4),
            d6 = count(DieSides.D6),
            d8 = count(DieSides.D8),
            d10 = count(DieSides.D10),
            d12 = count(DieSides.D12),
            d20 = count(DieSides.D20)
        )
    }

    private fun finalDiceCount(player: Player): Int =
        player.dice.supply.size +
            player.dice.hand.size +
            player.dice.discard.size +
            player.tokens.mulchTokens.count { it.sides != null } +
            player.tokens.pendingMulchTokens.count { it.sides != null }

    private fun finalDicePower(player: Player): Int =
        (player.dice.supply + player.dice.hand + player.dice.discard).sumOf { it.sides } +
            player.tokens.mulchTokens.sumOf { it.sides?.value ?: 0 } +
            player.tokens.pendingMulchTokens.sumOf { it.sides?.value ?: 0 }

    private val WISP_GAIN_REWARD_KINDS = setOf(
        RollRewardKind.WISP_GAINED,
        RollRewardKind.WISP_PLAYED_IMMEDIATELY
    )
}
