package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
import dugsolutions.leaf.simulation.v35.experiment.plant.PlantExperimentResearchConfig
import dugsolutions.leaf.simulation.v35.experiment.plant.resolveResearchGroveForSample
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.chronicle.domain.*
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.game.*
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.GrovePlantCode
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.PlantValueResolver
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.wisp.domain.WispCard
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.learned.buy.*
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.player.decision.trace.DecisionReasoningSink
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.nio.file.Path
import java.nio.file.Paths

/** Matched held-out evaluation of one trained Buy policy against Human Baseline control. */
fun main(args: Array<String>) {
    val o = EvalOptions.parse(args.toList())
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCards(koin.get(), koin.get(), koin.get(), koin.get(), koin.get(), koin.get())
        val plantManager = koin.get<PlantCardManager>()
        val roundManager = koin.get<RoundCardManager>()
        val wispManager = koin.get<WispCardManager>()
        val allPlants = plantManager.getAllCards().cards
        val defaultGrove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val plantExperiment = PlantExperimentResearchConfig.resolve(o.plantOverridesPath, allPlants)
        val raw = LearnedBuyWeights.load(o.input)
        require(raw.provenance.trainingStatus == "trained") {
            "Held-out evaluation requires a trained policy; ${o.input} has trainingStatus=${raw.provenance.trainingStatus}"
        }
        val weights = LearnedBuyCardCatalog.prepare(
            raw,
            allPlants,
            additionalPlantCosts = plantExperiment.effectiveCosts(allPlants)
        )
        rejectTrainingSeedOverlap(weights, o)
        val factory = koin.get<GameFactory>()
        val runner = koin.get<GameRunner>()
        val plantsByName = allPlants.associateBy { it.name }
        val control = EvalAccumulator()
        val learned = EvalAccumulator()

        println("Learned Buy Policy Held-Out Evaluation")
        println("policy=${o.input}")
        println("matched samples=${o.games}; games run=${o.games * 2}")
        println("evaluation seeds=${o.seed}..${o.seed + o.games - 1}; strategy seeds=${o.strategySeed}..${o.strategySeed + o.games - 1}")
        println("affected role rotates across physical seats; opponents=Human Baseline; ${o.groveDescription()}; rounds=${o.roundLabel}")
        if (o.grovePattern != null) {
            println("Grove zeros are resolved independently once per matched sample using grove seeds=${o.groveSeed}..${o.groveSeed + o.games - 1}; CONTROL and LEARNED share that resolved Grove")
        }
        println("CONTROL=Human Baseline; LEARNED=same role with learned Buy selection only; all other decisions=Human Baseline")
        println("research environment=${o.researchEnvironment}; environment seeds=${o.environmentSeed}..${o.environmentSeed + o.games - 1}")
        println("Round constraints: include=${o.roundIncludes.ifEmpty { setOf("<all>") }.sorted()} exclude=${o.roundExcludes.sorted()}")
        println("Wisp constraints: include=${o.wispIncludes.ifEmpty { setOf("<all>") }.sorted()} exclude=${o.wispExcludes.sorted()}")
        println("training fitness recorded in policy=${weights.provenance.fitness?.let(::pct) ?: "unknown"}; evaluation seeds are required not to overlap its recorded training cohorts")
        if (plantExperiment.isActive) {
            println()
            println(plantExperiment.render(allPlants))
        }
        println()

        repeat(o.games) { sample ->
            val seat = sample % 4
            val mechanicalSeed = o.seed + sample
            val strategySeed = o.strategySeed + sample
            val resolvedGrove = resolveGroveForSample(
                o, sample, plantManager, defaultGrove, allPlants, plantExperiment.values
            )
            val environment = resolveResearchEnvironmentForSample(o, sample, roundManager, wispManager)
            if (sample == 0) {
                println("resolved research environment sample 0 Round deck=${environment.roundCards?.joinToString { it.name } ?: "<normal game setup>"}")
                println("resolved research environment sample 0 Wisp deck=${environment.wispCards?.joinToString { it.name } ?: "<normal game setup>"}")
                println()
            }
            val groveCode = GrovePlantCode.encode(resolvedGrove)
            val controlFactories = List(4) { PlayerDecisionFactory.humanBaseline() }
            val learnedFactories = List(4) { if (it == seat) learnedFactory(weights) else PlayerDecisionFactory.humanBaseline() }
            control.add(runOne(factory, runner, resolvedGrove, groveCode, controlFactories, mechanicalSeed, strategySeed, sample, seat, "CONTROL", o.roundSetup, o.roundLabel, environment, plantExperiment.values), seat, plantsByName, resolvedGrove, plantExperiment.values)
            learned.add(runOne(factory, runner, resolvedGrove, groveCode, learnedFactories, mechanicalSeed, strategySeed, sample, seat, "LEARNED", o.roundSetup, o.roundLabel, environment, plantExperiment.values), seat, plantsByName, resolvedGrove, plantExperiment.values)
        }
        printReport(o, weights, control, learned)
    } finally { app.close() }
}

internal data class CompletedEvalGame(val summary: GameSummary, val entries: List<GameEntry>)

private fun runOne(factory: GameFactory, runner: GameRunner, grove: List<PlantCard>, groveCode: String, decisions: List<PlayerDecisionFactory>, seed: Long, strategySeed: Long, sample: Int, affectedSeat: Int, variant: String, roundSetup: GameRoundSetup, roundLabel: String, environment: ResolvedResearchEnvironment, plantValues: PlantValueResolver): CompletedEvalGame {
    val game = factory(
        evaluationGameConfig(grove, decisions, roundSetup, seed, strategySeed, plantValues),
        exactRoundCards=environment.roundCards,
        exactWispCards=environment.wispCards
    )
    val result = withSimulationFailureDiagnostics(game, SimulationRunContext("evaluate_buy_policy", sample, variant, affectedSeat, seed, strategySeed, groveCode, roundLabel)) { runner.run(game) }
    return CompletedEvalGame(GameSummaryExtractor.extract(game, result), game.chronicle.entries.toList())
}


internal fun evaluationGameConfig(
    grove: List<PlantCard>,
    decisions: List<PlayerDecisionFactory>,
    roundSetup: GameRoundSetup,
    seed: Long,
    strategySeed: Long,
    plantValues: PlantValueResolver
): GameConfig = GameConfig(
    selectedPlantCards = grove,
    playerDecisionFactories = decisions,
    roundSetup = roundSetup,
    seed = seed,
    strategySeed = strategySeed,
    recordDecisionReasoning = false,
    plantValues = plantValues
)

internal class EvalAccumulator {
    var winShare=0.0; var vp=0L; var plants=0L; var plantCost=0L; var dice=0L; var dicePower=0L; var battleVp=0L; var wounds=0L
    var plantPurchases=0L; var diePurchases=0L
    val seatWins=DoubleArray(4); val seatGames=IntArray(4)
    val plantCosts=sortedMapOf<Int,Long>(); val plantTypes=sortedMapOf<String,Long>(); val plantCards=sortedMapOf<String,Long>(); val dieSizes=sortedMapOf<String,Long>(); val finalDiceSizes=sortedMapOf<String,Long>()
    val buyShape = BuyShapeAccumulator()
    val battleShape = BattleShapeAccumulator()
    val strikeResearch = StrikeRowResearchAccumulator()
    val utilization = EffectResourceAccumulator()
    val vpLedger = VpLedgerAccumulator()
    val groveCardGames=mutableMapOf<String,Long>(); val groveCardWins=mutableMapOf<String,Double>()
    val watchedCardCopies=mutableMapOf<String,Long>(); val watchedCardVp=mutableMapOf<String,Long>()

    fun add(
        game: CompletedEvalGame,
        seat: Int,
        plantsByName: Map<String,PlantCard>,
        grove: List<PlantCard>,
        plantValues: PlantValueResolver = PlantValueResolver.CANONICAL
    ) {
        val p=game.summary.players.single { it.seat==seat }
        winShare+=p.winShare; vp+=p.totalVp; plants+=p.finalPlantCount; plantCost+=p.finalPlantPrintedCost; dice+=p.finalDiceCount; dicePower+=p.finalDicePower; battleVp+=p.battleStrikeVp; wounds+=p.woundsTaken
        seatWins[seat]+=p.winShare; seatGames[seat]++
        mapOf("D4" to p.ownedDiceSignature.d4,"D6" to p.ownedDiceSignature.d6,"D8" to p.ownedDiceSignature.d8,"D10" to p.ownedDiceSignature.d10,"D12" to p.ownedDiceSignature.d12,"D20" to p.ownedDiceSignature.d20).forEach { (k,v) -> finalDiceSizes[k]=(finalDiceSizes[k]?:0)+v }
        buyShape.addGame(game.entries, p.playerId)
        battleShape.addGame(game.entries, p.playerId)
        strikeResearch.addGame(game.entries, p.playerId, p.winShare)
        utilization.addGame(game.entries, p.playerId)
        vpLedger.addGame(p, game.entries, plantsByName, plantValues)
        grove.map { it.name }.distinct().forEach { name -> groveCardGames[name]=(groveCardGames[name]?:0)+1; groveCardWins[name]=(groveCardWins[name]?:0.0)+p.winShare }
        WATCHED_CARDS.forEach { name ->
            val copies=p.plantCreatureSignature.cards.count { it.plantName==name }
            if(copies>0) {
                watchedCardCopies[name]=(watchedCardCopies[name]?:0)+copies
                val card=plantsByName[name]
                if(card!=null) scoreWatchedCard(card,copies,p,plantValues)?.let { watchedCardVp[name]=(watchedCardVp[name]?:0)+it.toLong() }
            }
        }
        game.entries.filterIsInstance<GameEntry.Purchase>().filter { it.playerId==p.playerId }.forEach { purchase ->
            when(purchase.kind) {
                PurchaseKind.PLANT -> { plantPurchases++; plantCosts.bump(purchase.cost); plantCards.bump(purchase.itemName); plantTypes.bump(plantsByName[purchase.itemName]?.type?.name ?: "UNKNOWN") }
                PurchaseKind.DIE -> { diePurchases++; dieSizes.bump(purchase.itemName) }
            }
        }
    }
    private fun <K> MutableMap<K,Long>.bump(key:K) { this[key]=(this[key]?:0L)+1L }
}

private val WATCHED_CARDS = listOf("Vine_07_01","Vine_07_02","Vine_07_03","Vine_07_04","Flower_11_01","Flower_14_04","Vine_09_03")

private fun scoreWatchedCard(
    card: PlantCard,
    copies: Int,
    p: dugsolutions.leaf.simulation.v35.analysis.PlayerGameSummary,
    plantValues: PlantValueResolver
): Int? {
    val perCopy = when(val rule=plantValues.scoringRuleFor(card)) {
        is PlantScoringRule.Fixed -> rule.points
        PlantScoringRule.PerGraftedVine -> p.plantCreatureSignature.cards.count { it.plantName.startsWith("Vine_") }
        PlantScoringRule.PerGraftedFlower -> p.plantCreatureSignature.cards.count { it.plantName.startsWith("Flower_") }
        PlantScoringRule.PerButterfly -> return null // reported separately as unavailable from compact final summary
        PlantScoringRule.PerOwnedD4 -> p.ownedDiceSignature.d4
    }
    return copies * perCopy
}

internal class BattleShapeAccumulator {
    var battles=0L
    val battleCount=sortedMapOf<Int,Long>(); val vp=sortedMapOf<Int,Long>(); val wounds=sortedMapOf<Int,Long>()
    val poolDice=sortedMapOf<Int,Long>(); val poolPower=sortedMapOf<Int,Long>(); val poolBySize=mutableMapOf<Int,MutableMap<String,Long>>()
    val usedDice=sortedMapOf<Int,Long>(); val usedPower=sortedMapOf<Int,Long>(); val usedBySize=mutableMapOf<Int,MutableMap<String,Long>>()

    fun addGame(entries: List<GameEntry>, playerId: PlayerId) {
        val reveals=entries.withIndex().filter { (it.value as? GameEntry.RoundRevealed)?.cardType==RoundCardType.BATTLE }
        reveals.forEachIndexed { bi, indexed ->
            val battle=bi+1; battles++; battleCount[battle]=(battleCount[battle]?:0)+1
            val end=entries.indexOfFirstFrom(indexed.index+1) { it is GameEntry.RoundCompleted && it.cardType==RoundCardType.BATTLE }.let { if(it<0) entries.size else it+1 }
            val segment=entries.subList(indexed.index,end)
            vp[battle]=(vp[battle]?:0)+segment.filterIsInstance<GameEntry.StrikeResolved>().sumOf { if(playerId in it.winnerIds) it.vpPerWinner else 0 }
            wounds[battle]=(wounds[battle]?:0)+segment.filterIsInstance<GameEntry.StrikeResolved>().count { playerId in it.woundedPlayerIds }
            val before=entries.subList(0,indexed.index).filterIsInstance<GameEntry.RoundCompleted>().lastOrNull()?.playerSummaries?.singleOrNull { it.playerId==playerId }
            if(before!=null) {
                val ds=before.supplyDice+before.discardDice
                poolDice[battle]=(poolDice[battle]?:0)+ds.size; poolPower[battle]=(poolPower[battle]?:0)+ds.sumOf { it.value }
                ds.forEach { poolBySize.getOrPut(battle){mutableMapOf()}[it.name]=(poolBySize.getOrPut(battle){mutableMapOf()}[it.name]?:0)+1 }
            }
            val preview=segment.filterIsInstance<GameEntry.BattleResolvePreview>().lastOrNull()
            val dice=preview?.rows?.flatMap { row -> row.squares.singleOrNull { it.playerId==playerId }?.dice.orEmpty() }.orEmpty()
            usedDice[battle]=(usedDice[battle]?:0)+dice.size; usedPower[battle]=(usedPower[battle]?:0)+dice.sumOf { it.sides.value }
            dice.forEach { d -> usedBySize.getOrPut(battle){mutableMapOf()}[d.sides.name]=(usedBySize.getOrPut(battle){mutableMapOf()}[d.sides.name]?:0)+1 }
        }
    }
    private fun List<GameEntry>.indexOfFirstFrom(start:Int,p:(GameEntry)->Boolean):Int { for(i in start until size) if(p(this[i])) return i; return -1 }
}


internal data class StrikeQualityBucket(
    var battles: Long = 0,
    var battleVp: Long = 0,
    var rowsWon: Long = 0,
    var rowsLost: Long = 0,
    var wounds: Long = 0,
    var gameWinShare: Double = 0.0
)

/**
 * Row-level Battle research for the affected role. This intentionally consumes
 * immutable StrikeResolved/BattleResolvePreview/RoundCompleted snapshots only;
 * it never feeds information back into gameplay or strategy decisions.
 *
 * "Inferior" means lower total committed die sides than the compared loser.
 * Causal labels are deliberately conservative: only direct dice/critters can be
 * called individually decisive because those are the sources represented by the
 * Strike contribution ledger today. Earlier Plant/Wisp/Round actions that changed
 * a die are not reverse-inferred from the final row snapshot.
 */
internal class StrikeRowResearchAccumulator {
    var winnerLoserComparisons = 0L
    var superiorSideWins = 0L
    var equalSideWins = 0L
    var inferiorSideWins = 0L
    var inferiorSideDeficit = 0L
    var inferiorWonWithHigherRollTotal = 0L
    var inferiorWithDecisiveCritter = 0L
    var inferiorWithDecisiveDie = 0L
    var inferiorWithNoHighDie = 0L

    val committedWinnerDice = sortedMapOf<String,Long>()
    val decisiveWinnerDice = sortedMapOf<String,Long>()
    val entryBuckets = sortedMapOf<String,StrikeQualityBucket>()
    val highDieOwned = sortedMapOf<String,Long>()
    val highDieAvailable = sortedMapOf<String,Long>()
    val highDiePlaced = sortedMapOf<String,Long>()
    val highDieOnWinningRows = sortedMapOf<String,Long>()
    val highDieDecisive = sortedMapOf<String,Long>()
    var battleEntries = 0L

    fun addGame(entries: List<GameEntry>, playerId: PlayerId, gameWinShare: Double) {
        val reveals = entries.withIndex().filter { (it.value as? GameEntry.RoundRevealed)?.cardType == RoundCardType.BATTLE }
        reveals.forEachIndexed { bi, indexed ->
            val battle = bi + 1
            val end = entries.indexOfFirstFrom(indexed.index + 1) { it is GameEntry.RoundCompleted && it.cardType == RoundCardType.BATTLE }
                .let { if (it < 0) entries.size else it + 1 }
            val segment = entries.subList(indexed.index, end)
            val before = entries.subList(0, indexed.index).filterIsInstance<GameEntry.RoundCompleted>()
                .lastOrNull()?.playerSummaries?.singleOrNull { it.playerId == playerId }
            val available = before?.let { it.supplyDice + it.discardDice }.orEmpty()
            val owned = before?.let { it.supplyDice + it.discardDice + it.mulchDice.filterNotNull() }.orEmpty()
            battleEntries++
            addThresholdCounts(highDieAvailable, available.map { it.value })
            addThresholdCounts(highDieOwned, owned.map { it.value })

            val strikes = segment.filterIsInstance<GameEntry.StrikeResolved>()
            // Use resolved row snapshots rather than only the final preview so an
            // early-resolved/closed row (for example Wisp's Last Word) is still
            // represented. A die can occupy only one Strike row.
            val placedDice = strikes.flatMap { strike ->
                strike.rowSnapshot?.squares?.singleOrNull { it.playerId == playerId }?.dice.orEmpty()
            }
            addThresholdCounts(highDiePlaced, placedDice.map { it.sides.value })

            val max = available.maxOfOrNull { it.value } ?: 0
            val bucketName = when {
                available.isEmpty() -> "NO_AVAILABLE_DICE"
                max <= 8 -> "MAX_D8_OR_LOWER"
                max == 10 -> "MAX_D10"
                max == 12 -> "MAX_D12"
                else -> "HAS_D20"
            }
            val bucket = entryBuckets.getOrPut(bucketName) { StrikeQualityBucket() }
            bucket.battles++
            bucket.battleVp += strikes.sumOf { if (playerId in it.winnerIds) it.vpPerWinner else 0 }
            bucket.rowsWon += strikes.count { playerId in it.winnerIds }
            bucket.rowsLost += strikes.count { sr -> playerId !in sr.winnerIds && sr.rowSnapshot?.squares?.any { it.playerId == playerId && !it.withdrawn } == true }
            bucket.wounds += strikes.count { playerId in it.woundedPlayerIds }
            bucket.gameWinShare += gameWinShare

            strikes.forEach { observeStrike(it, playerId) }
        }
    }

    private fun observeStrike(strike: GameEntry.StrikeResolved, affected: PlayerId) {
        val row = strike.rowSnapshot ?: return
        val active = row.squares.filterNot { it.withdrawn }
        val winnerSquares = active.filter { it.playerId in strike.winnerIds }
        val loserSquares = active.filter { it.playerId !in strike.winnerIds }
        val ledger = strike.contributionLedger

        winnerSquares.filter { it.playerId == affected }.forEach { winner ->
            winner.dice.forEach { die ->
                committedWinnerDice.bump(sizeBucket(die.sides.value))
                addThresholdOne(highDieOnWinningRows, die.sides.value)
            }
            ledger?.contributions?.filter { it.playerId == affected && it.individuallyWinnerDecisive }?.forEach { contribution ->
                val die = contribution.source as? dugsolutions.leaf.v35.battle.StrikeContributionSource.Die
                if (die != null) {
                    decisiveWinnerDice.bump(sizeBucket(die.sides))
                    addThresholdOne(highDieDecisive, die.sides)
                }
            }
            loserSquares.forEach { loser ->
                winnerLoserComparisons++
                val ws = winner.dice.sumOf { it.sides.value }
                val ls = loser.dice.sumOf { it.sides.value }
                when {
                    ws > ls -> superiorSideWins++
                    ws == ls -> equalSideWins++
                    else -> {
                        inferiorSideWins++
                        inferiorSideDeficit += (ls - ws)
                        val wr = winner.dice.sumOf { it.value }
                        val lr = loser.dice.sumOf { it.value }
                        if (wr > lr) inferiorWonWithHigherRollTotal++
                        if (winner.dice.none { it.sides.value >= 10 }) inferiorWithNoHighDie++
                        val decisive = ledger?.contributions?.filter { it.playerId == affected && it.individuallyWinnerDecisive }.orEmpty()
                        if (decisive.any { it.source is dugsolutions.leaf.v35.battle.StrikeContributionSource.Critter }) inferiorWithDecisiveCritter++
                        if (decisive.any { it.source is dugsolutions.leaf.v35.battle.StrikeContributionSource.Die }) inferiorWithDecisiveDie++
                    }
                }
            }
        }
    }

    private fun addThresholdCounts(target: MutableMap<String,Long>, sides: List<Int>) {
        sides.forEach { addThresholdOne(target, it) }
    }
    private fun addThresholdOne(target: MutableMap<String,Long>, sides: Int) {
        if (sides >= 10) target.bump("D10+")
        if (sides >= 12) target.bump("D12+")
        if (sides >= 20) target.bump("D20")
    }
    private fun sizeBucket(sides:Int) = if (sides <= 8) "D8_OR_LOWER" else "D$sides"
    private fun <K> MutableMap<K,Long>.bump(key:K) { this[key]=(this[key]?:0L)+1L }
    private fun List<GameEntry>.indexOfFirstFrom(start:Int,p:(GameEntry)->Boolean):Int { for(i in start until size) if(p(this[i])) return i; return -1 }
}


internal class EffectResourceAccumulator {
    var games = 0L
    var finalWisps = 0L
    var finalWispVp = 0L
    var rollWispsGained = 0L
    var immediateWispsPlayed = 0L
    val roundEffects = sortedMapOf<String, Long>()
    val battleRoundEffects = sortedMapOf<String, Long>()
    val roundCardReveals = sortedMapOf<String, Long>()
    val roundEffectOpportunities = sortedMapOf<String, Long>()
    val roundEffectUses = sortedMapOf<String, Long>()
    val roundCategoryOpportunities = sortedMapOf<String, Long>()
    val roundCategoryUses = sortedMapOf<String, Long>()
    val plantEffects = sortedMapOf<String, Long>()
    val wispEffects = sortedMapOf<String, Long>()
    val wispAcquiredByCard = sortedMapOf<String, Long>()
    val wispRetainedByCard = sortedMapOf<String, Long>()
    val wispAcquisitionSources = sortedMapOf<String, Long>()
    val supportActions = sortedMapOf<String, Long>()
    val upgrades = sortedMapOf<String, Long>()
    val upgradeSources = sortedMapOf<String, Long>()
    // Complete die-development accounting. Purchases are derived from Purchase;
    // non-Buy gains use typed DieGained entries; upgrades reuse typed Upgrade.
    val dieGainsBySourceAndSize = sortedMapOf<String, Long>()
    val upgradesBySourceAndTransition = sortedMapOf<String, Long>()
    val dieDevelopmentPowerBySource = sortedMapOf<String, Long>()
    val wispGainTriggers = sortedMapOf<String, Long>()
    var plantRoundExposure = 0L
    var battlePlantExposure = 0L
    var battleRounds = 0L

    fun addGame(entries: List<GameEntry>, playerId: PlayerId) {
        games++
        entries.filterIsInstance<GameEntry.RoundRevealed>().forEach { revealed ->
            roundCardReveals.bump(revealed.cardName)
        }
        entries.filterIsInstance<GameEntry.RoundEffectOpportunity>()
            .filter { it.playerId == playerId }
            .forEach { opportunity ->
                if (opportunity.firstExecutable) {
                    roundEffectOpportunities.bump(roundEffectKey(opportunity.roundCardName, 1, opportunity.firstEffect))
                    roundCategories(opportunity.firstEffect).forEach { roundCategoryOpportunities.bump(it) }
                }
                if (opportunity.secondExecutable) {
                    roundEffectOpportunities.bump(roundEffectKey(opportunity.roundCardName, 2, opportunity.secondEffect))
                    roundCategories(opportunity.secondEffect).forEach { roundCategoryOpportunities.bump(it) }
                }
            }
        entries.filterIsInstance<GameEntry.MainAction>()
            .filter { it.playerId == playerId && (it.action == MainActionKind.ROUND_EFFECT_1 || it.action == MainActionKind.ROUND_EFFECT_2) }
            .forEach { action ->
                val revealed = entries.filterIsInstance<GameEntry.RoundRevealed>().lastOrNull { it.sequence < action.sequence }
                    ?: return@forEach
                val slot = if (action.action == MainActionKind.ROUND_EFFECT_1) 1 else 2
                val effect = if (slot == 1) revealed.firstEffect else revealed.secondEffect
                roundEffectUses.bump(roundEffectKey(revealed.cardName, slot, effect))
                roundCategories(effect).forEach { roundCategoryUses.bump(it) }
            }
        entries.filterIsInstance<GameEntry.RoundCompleted>().forEach { completed ->
            val ps = completed.playerSummaries.singleOrNull { it.playerId == playerId } ?: return@forEach
            plantRoundExposure += ps.graftedPlantCount
            if (completed.cardType == RoundCardType.BATTLE) { battlePlantExposure += ps.graftedPlantCount; battleRounds++ }
        }
        entries.filterIsInstance<GameEntry.FinalScore>().singleOrNull { it.playerId == playerId }?.let {
            finalWispVp += it.unplayedWispVp
        }
        entries.filterIsInstance<GameEntry.RoundCompleted>().lastOrNull()?.playerSummaries?.singleOrNull { it.playerId == playerId }?.let { finalWisps += it.wispCount }
        entries.filterIsInstance<GameEntry.RollReward>().filter { it.playerId == playerId }.forEach {
            when (it.kind) {
                RollRewardKind.WISP_GAINED -> {
                    rollWispsGained++; wispGainTriggers.bump("Roll reward")
                    it.wispName?.let { name -> wispAcquiredByCard.bump(name) }
                    it.wispName?.let { name -> wispAcquisitionSources.bump("ROLL_REWARD:$name") }
                }
                RollRewardKind.WISP_PLAYED_IMMEDIATELY -> {
                    immediateWispsPlayed++; wispGainTriggers.bump("Roll reward (immediate play)")
                    it.wispName?.let { name -> wispAcquiredByCard.bump(name) }
                    it.wispName?.let { name -> wispAcquisitionSources.bump("ROLL_REWARD:$name") }
                }
                else -> Unit
            }
        }
        entries.filterIsInstance<GameEntry.WispAcquired>().filter { it.playerId == playerId }.forEach { acquired ->
            wispAcquiredByCard.bump(acquired.wispName)
            wispAcquisitionSources.bump("${acquired.sourceKind.name}:${acquired.sourceName}:${acquired.wispName}")
        }
        entries.filterIsInstance<GameEntry.FinalScore>().singleOrNull { it.playerId == playerId }?.unplayedWispNames?.forEach { name -> wispRetainedByCard.bump(name) }
        entries.filterIsInstance<GameEntry.SupportAction>().filter { it.playerId == playerId }.forEach { supportActions.bump(it.action.name) }
        entries.filterIsInstance<GameEntry.EffectResolved>().filter { it.playerId == playerId }.forEach { e ->
            when (e.sourceKind) {
                EffectSourceKind.ROUND -> {
                    val label = "${e.sourceName}: ${e.effect.name}"
                    roundEffects.bump(label)
                    if (e.phase == ChroniclePhase.BATTLE) battleRoundEffects.bump(label)
                }
                EffectSourceKind.PLANT -> plantEffects.bump(e.sourceName)
                EffectSourceKind.WISP -> wispEffects.bump(e.sourceName)
            }
            when (e.effect) {
                GameEffect.GAIN_ONE_WISP -> wispGainTriggers.bump("Gain 1 Wisp effect")
                GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT -> wispGainTriggers.bump("Steal Wisp from one opponent")
                GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS -> wispGainTriggers.bump("Steal Wisp from all opponents")
                else -> Unit
            }
        }
        entries.filterIsInstance<GameEntry.Purchase>()
            .filter { it.playerId == playerId && it.kind == PurchaseKind.DIE }
            .forEach { purchase ->
                val sides = dieSidesFromName(purchase.itemName)
                dieGainsBySourceAndSize.bump("BUY:${sides.name}")
                dieDevelopmentPowerBySource.add("BUY", sides.value.toLong())
            }
        entries.withIndex().filter { it.value is GameEntry.DieGained && (it.value as GameEntry.DieGained).playerId == playerId }.forEach { indexed ->
            val gained = indexed.value as GameEntry.DieGained
            val effect = enclosingEffect(entries, indexed.index)
            val source = developmentSource(effect)
            dieGainsBySourceAndSize.bump("$source:${gained.sides.name}")
            dieDevelopmentPowerBySource.add(sourceCategory(source), gained.sides.value.toLong())
        }
        entries.withIndex().filter { it.value is GameEntry.Upgrade && (it.value as GameEntry.Upgrade).playerId == playerId }.forEach { indexed ->
            val u = indexed.value as GameEntry.Upgrade
            val transition = "${u.from.name}->${u.to.name}"
            upgrades.bump(transition)
            val effect = enclosingEffect(entries, indexed.index)
            val legacySource = effect?.let { "${it.sourceKind.name}:${it.sourceName}" } ?: "unscoped"
            upgradeSources.bump(legacySource)
            val source = developmentSource(effect)
            upgradesBySourceAndTransition.bump("$source:$transition")
            dieDevelopmentPowerBySource.add(sourceCategory(source), (u.to.value - u.from.value).toLong())
        }
    }

    private fun roundEffectKey(cardName: String, slot: Int, effect: GameEffect): String =
        "$cardName / Effect $slot / ${effect.name}"

    private fun roundCategories(effect: GameEffect): List<String> = buildList {
        when (effect) {
            GameEffect.UPGRADE_DIE_AND_USE_NOW, GameEffect.UPGRADE_DIE_FROM_HAND,
            GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW -> add("Die upgrade")
            GameEffect.GAIN_ANY_DIE_TO_DISCARD, GameEffect.GAIN_D10_TO_DISCARD,
            GameEffect.GAIN_D12_TO_DISCARD, GameEffect.GAIN_D20_TO_DISCARD -> {
                add("Direct die gain")
                if (effect != GameEffect.GAIN_ANY_DIE_TO_DISCARD) add("High-sided die gain")
            }
            GameEffect.MULCH_DIE_FROM_DISCARD, GameEffect.MULCH_DIE_FROM_HAND,
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD -> add("Mulch")
            GameEffect.RAISE_ALL_DICE_PLUS_2, GameEffect.RAISE_ANY_DIE_PLUS_1,
            GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE,
            GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
            GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
            GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE,
            GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
            GameEffect.RAISE_DIE_PLUS_3, GameEffect.RAISE_DIE_PLUS_4 -> add("Raise")
            GameEffect.GAIN_ANY_TWO_CRITTERS, GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
            GameEffect.GAIN_TWO_WORMS, GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND -> add("Critter gain")
            GameEffect.GAIN_ONE_WISP, GameEffect.STEAL_RANDOM_WISP_FROM_ONE_OPPONENT,
            GameEffect.STEAL_RANDOM_WISP_FROM_ALL_OPPONENTS -> add("Wisp gain")
            GameEffect.GAIN_ONE_VP, GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE -> add("VP")
            else -> Unit
        }
    }

    private fun dieSidesFromName(name: String): DieSides =
        DieSides.entries.singleOrNull { it.name == name }
            ?: error("Recorded die purchase has unknown size: $name")

    private fun developmentSource(effect: GameEntry.EffectResolved?): String {
        if (effect == null) return "OTHER:unscoped"
        // Compost is economically important enough to remain visible even when
        // its effect is carried by a Round card rather than a separate source kind.
        if (effect.sourceName.contains("Compost", ignoreCase = true)) return "COMPOST:${effect.sourceName}"
        return when (effect.sourceKind) {
            EffectSourceKind.ROUND -> "ROUND:${effect.sourceName}"
            EffectSourceKind.WISP -> "WISP:${effect.sourceName}"
            EffectSourceKind.PLANT -> "PLANT:${effect.sourceName}"
        }
    }

    private fun sourceCategory(source: String): String = source.substringBefore(':')

    private fun enclosingEffect(entries: List<GameEntry>, index: Int): GameEntry.EffectResolved? {
        val depth = entries[index].hierarchyDepth
        for (i in index - 1 downTo 0) {
            val e = entries[i]
            if (e.hierarchyDepth < depth) return e as? GameEntry.EffectResolved
        }
        return null
    }
    private fun MutableMap<String, Long>.bump(key: String) { this[key] = (this[key] ?: 0L) + 1L }
    private fun MutableMap<String, Long>.add(key: String, amount: Long) { this[key] = (this[key] ?: 0L) + amount }
}


internal class VpLedgerAccumulator {
    var games = 0L
    var finalVp = 0L
    var existingVp = 0L
    var battleStrikeVp = 0L
    var directEffectVp = 0L
    var otherExistingVp = 0L
    var plantVp = 0L
    var unattributedPlantVp = 0L
    var wispVp = 0L
    val directEffectVpBySource = sortedMapOf<String, Long>()
    val plantVpByCard = sortedMapOf<String, Long>()

    fun addGame(
        p: dugsolutions.leaf.simulation.v35.analysis.PlayerGameSummary,
        entries: List<GameEntry>,
        plantsByName: Map<String, PlantCard>,
        plantValues: PlantValueResolver = PlantValueResolver.CANONICAL
    ) {
        games++
        finalVp += p.totalVp
        existingVp += p.existingVp
        battleStrikeVp += p.battleStrikeVp
        plantVp += p.plantVp
        wispVp += p.unplayedWispVp

        val gainOneEvents = entries.filterIsInstance<GameEntry.EffectResolved>()
            .filter { it.playerId == p.playerId && it.effect == GameEffect.GAIN_ONE_VP }
        directEffectVp += gainOneEvents.size
        gainOneEvents.forEach { e ->
            val key = "${e.sourceKind.name}:${e.sourceName}"
            directEffectVpBySource[key] = (directEffectVpBySource[key] ?: 0L) + 1L
        }

        val other = p.existingVp - p.battleStrikeVp - gainOneEvents.size
        require(other >= 0) {
            "VP ledger cannot reconcile existing VP for ${p.playerId}: existing=${p.existingVp}, " +
                "Battle=${p.battleStrikeVp}, recorded GAIN_ONE_VP=${gainOneEvents.size}"
        }
        otherExistingVp += other

        val vineCount = p.plantCreatureSignature.cards.count { it.plantName.startsWith("Vine_") }
        val flowerCount = p.plantCreatureSignature.cards.count { it.plantName.startsWith("Flower_") }
        var attributedPlants = 0
        p.plantCreatureSignature.cards.groupingBy { it.plantName }.eachCount().forEach { (name, copies) ->
            val card = plantsByName[name] ?: return@forEach
            val perCopy = when (val rule = plantValues.scoringRuleFor(card)) {
                is PlantScoringRule.Fixed -> rule.points
                PlantScoringRule.PerGraftedVine -> vineCount
                PlantScoringRule.PerGraftedFlower -> flowerCount
                PlantScoringRule.PerOwnedD4 -> p.ownedDiceSignature.d4
                PlantScoringRule.PerButterfly -> return@forEach
            }
            val vp = copies * perCopy
            attributedPlants += vp
            plantVpByCard[name] = (plantVpByCard[name] ?: 0L) + vp
        }
        val plantResidual = p.plantVp - attributedPlants
        require(plantResidual >= 0) {
            "VP ledger over-attributed Plant VP for ${p.playerId}: final=${p.plantVp}, attributed=$attributedPlants"
        }
        unattributedPlantVp += plantResidual

        val reconciled = p.battleStrikeVp + gainOneEvents.size + other + p.plantVp + p.unplayedWispVp
        require(reconciled == p.totalVp) {
            "VP ledger does not reconcile for ${p.playerId}: components=$reconciled final=${p.totalVp}"
        }
    }
}

internal class BuyShapeAccumulator {
    var phases = 0L
    var purchases = 0L
    var startingPower = 0L
    var spentPower = 0L
    var overpayment = 0L
    var maxPurchases = 0
    val purchaseCount = sortedMapOf<Int, Long>()
    val kindSequences = mutableMapOf<String, Long>()
    val itemSequences = mutableMapOf<String, Long>()
    val plantCostSequences = mutableMapOf<String, Long>()
    val stagePhases = sortedMapOf<Int, Long>()
    val stagePurchases = sortedMapOf<Int, Long>()
    val stagePlants = sortedMapOf<Int, Long>()
    val stageDice = sortedMapOf<Int, Long>()
    val stageSpent = sortedMapOf<Int, Long>()

    fun addGame(entries: List<GameEntry>, playerId: PlayerId) {
        val orders = entries.withIndex().filter { it.value is GameEntry.BuyOrder }
        orders.forEachIndexed { phaseIndex, indexed ->
            val order = indexed.value as GameEntry.BuyOrder
            val start = indexed.index + 1
            val end = orders.getOrNull(phaseIndex + 1)?.index ?: entries.size
            val ps = entries.subList(start, end).filterIsInstance<GameEntry.Purchase>().filter { it.playerId == playerId }
            val power = order.resources.singleOrNull { it.playerId == playerId }?.total ?: 0
            val stage = when (phaseIndex) { 0, 1, 2 -> 1; 3, 4 -> 2; else -> 3 }
            phases++
            startingPower += power
            purchases += ps.size
            maxPurchases = maxOf(maxPurchases, ps.size)
            purchaseCount.bump(ps.size)
            stagePhases.bump(stage)
            stagePurchases.add(stage, ps.size.toLong())
            val spent = ps.sumOf { it.paymentTotal }
            spentPower += spent
            stageSpent.add(stage, spent.toLong())
            overpayment += ps.sumOf { it.overpayment }.toLong()
            stagePlants.add(stage, ps.count { it.kind == PurchaseKind.PLANT }.toLong())
            stageDice.add(stage, ps.count { it.kind == PurchaseKind.DIE }.toLong())
            if (ps.isNotEmpty()) {
                kindSequences.bump(ps.joinToString(" -> ") { if (it.kind == PurchaseKind.PLANT) "Plant" else "Die" })
                itemSequences.bump(ps.joinToString(" -> ") { if (it.kind == PurchaseKind.PLANT) "P${it.cost}" else it.itemName })
                val plantCosts = ps.filter { it.kind == PurchaseKind.PLANT }.map { it.cost }
                if (plantCosts.isNotEmpty()) plantCostSequences.bump(plantCosts.joinToString(" -> "))
            }
        }
    }

    private fun MutableMap<Int, Long>.add(key: Int, value: Long) { this[key] = (this[key] ?: 0L) + value }
    private fun <K> MutableMap<K, Long>.bump(key: K) { this[key] = (this[key] ?: 0L) + 1L }
}

private fun printBuyShape(c: BuyShapeAccumulator, l: BuyShapeAccumulator) {
    fun avg(value: Long, phases: Long) = if (phases == 0L) "0.00" else "%.2f".format(value.toDouble() / phases)
    fun percent(value: Long, phases: Long) = if (phases == 0L) "0.00%" else pct(value.toDouble() / phases)
    println("Buy-phase shape (affected role)")
    println("  Buy phases observed: control=${c.phases} learned=${l.phases}")
    println("  Purchases / phase:   control=${avg(c.purchases,c.phases)} learned=${avg(l.purchases,l.phases)}")
    println("  Max purchases seen:  control=${c.maxPurchases} learned=${l.maxPurchases}")
    println("  Starting power:      control=${avg(c.startingPower,c.phases)} learned=${avg(l.startingPower,l.phases)}")
    println("  Power spent:         control=${avg(c.spentPower,c.phases)} learned=${avg(l.spentPower,l.phases)}")
    println("  Power left:          control=${avg(c.startingPower-c.spentPower,c.phases)} learned=${avg(l.startingPower-l.spentPower,l.phases)}")
    println("  Overpayment / phase: control=${avg(c.overpayment,c.phases)} learned=${avg(l.overpayment,l.phases)}")
    println("  Purchases per Buy phase:")
    val buckets = (c.purchaseCount.keys + l.purchaseCount.keys).toSortedSet()
    buckets.forEach { n -> println("    $n: control=${c.purchaseCount[n]?:0} (${percent(c.purchaseCount[n]?:0,c.phases)}) learned=${l.purchaseCount[n]?:0} (${percent(l.purchaseCount[n]?:0,l.phases)})") }
    println("  By Cultivation stage:")
    (c.stagePhases.keys + l.stagePhases.keys).toSortedSet().forEach { stage ->
        val cp=c.stagePhases[stage]?:0; val lp=l.stagePhases[stage]?:0
        println("    Stage $stage: purchases/phase ${avg(c.stagePurchases[stage]?:0,cp)} -> ${avg(l.stagePurchases[stage]?:0,lp)}; Plants/phase ${avg(c.stagePlants[stage]?:0,cp)} -> ${avg(l.stagePlants[stage]?:0,lp)}; dice/phase ${avg(c.stageDice[stage]?:0,cp)} -> ${avg(l.stageDice[stage]?:0,lp)}; power spent ${avg(c.stageSpent[stage]?:0,cp)} -> ${avg(l.stageSpent[stage]?:0,lp)}")
    }
    printTopSequences("Most common purchase-kind sequences", c.kindSequences, l.kindSequences, c.phases, l.phases)
    printTopSequences("Most common cost/item sequences (P=Plant cost)", c.itemSequences, l.itemSequences, c.phases, l.phases)
    printTopSequences("Most common Plant-cost sequences", c.plantCostSequences, l.plantCostSequences, c.phases, l.phases)
}

private fun printTopSequences(title: String, c: Map<String,Long>, l: Map<String,Long>, cPhases:Long, lPhases:Long, limit:Int=12) {
    println("  $title:")
    val keys = (c.keys + l.keys).sortedWith(compareByDescending<String> { (c[it]?:0)+(l[it]?:0) }.thenBy { it }).take(limit)
    keys.forEach { key ->
        val cv=c[key]?:0; val lv=l[key]?:0
        println("    $key: control=$cv (${if(cPhases==0L) "0.00%" else pct(cv.toDouble()/cPhases)}) learned=$lv (${if(lPhases==0L) "0.00%" else pct(lv.toDouble()/lPhases)})")
    }
}

private fun printBattleShape(c:BattleShapeAccumulator,l:BattleShapeAccumulator) {
    fun av(m:Map<Int,Long>, b:Int, n:Long)=if(n==0L) "0.00" else "%.2f".format((m[b]?:0).toDouble()/n)
    fun avgSize(power:Map<Int,Long>, dice:Map<Int,Long>, b:Int)=if((dice[b]?:0)==0L) "0.00" else "%.2f".format((power[b]?:0).toDouble()/(dice[b]?:0))
    println("Battle dice utilization (affected role)")
    (c.battleCount.keys+l.battleCount.keys).toSortedSet().forEach { b ->
        val cn=c.battleCount[b]?:0; val ln=l.battleCount[b]?:0
        println("  Battle $b: VP ${av(c.vp,b,cn)} -> ${av(l.vp,b,ln)}; wounds ${av(c.wounds,b,cn)} -> ${av(l.wounds,b,ln)}")
        println("    entering pool: dice ${av(c.poolDice,b,cn)} -> ${av(l.poolDice,b,ln)}; avg sides/die ${avgSize(c.poolPower,c.poolDice,b)} -> ${avgSize(l.poolPower,l.poolDice,b)}")
        println("    used on grid:  dice ${av(c.usedDice,b,cn)} -> ${av(l.usedDice,b,ln)}; avg sides/die ${avgSize(c.usedPower,c.usedDice,b)} -> ${avgSize(l.usedPower,l.usedDice,b)}")
        val sizes=listOf("D4","D6","D8","D10","D12","D20")
        println("    pool by size:  "+sizes.joinToString("; ") { s -> "$s ${"%.2f".format((c.poolBySize[b]?.get(s)?:0).toDouble()/cn)} -> ${"%.2f".format((l.poolBySize[b]?.get(s)?:0).toDouble()/ln)}" })
        println("    grid by size:  "+sizes.joinToString("; ") { s -> "$s ${"%.2f".format((c.usedBySize[b]?.get(s)?:0).toDouble()/cn)} -> ${"%.2f".format((l.usedBySize[b]?.get(s)?:0).toDouble()/ln)}" })
    }
}


private fun printStrikeRowResearch(c: StrikeRowResearchAccumulator, l: StrikeRowResearchAccumulator) {
    fun pctPart(x:Long,n:Long)=if(n==0L) "0.00%" else pct(x.toDouble()/n)
    fun avg(x:Long,n:Long)=if(n==0L) "0.00" else "%.2f".format(x.toDouble()/n)
    println("Strike-row quality, opportunity, and decisiveness (affected role)")
    println("  Winner-vs-loser row comparisons: control=${c.winnerLoserComparisons} learned=${l.winnerLoserComparisons}")
    println("    superior committed die-side power: control=${c.superiorSideWins} (${pctPart(c.superiorSideWins,c.winnerLoserComparisons)}) learned=${l.superiorSideWins} (${pctPart(l.superiorSideWins,l.winnerLoserComparisons)})")
    println("    equal committed die-side power:    control=${c.equalSideWins} (${pctPart(c.equalSideWins,c.winnerLoserComparisons)}) learned=${l.equalSideWins} (${pctPart(l.equalSideWins,l.winnerLoserComparisons)})")
    println("    inferior committed die-side power: control=${c.inferiorSideWins} (${pctPart(c.inferiorSideWins,c.winnerLoserComparisons)}) learned=${l.inferiorSideWins} (${pctPart(l.inferiorSideWins,l.winnerLoserComparisons)})")
    println("  Inferior-side wins:")
    println("    avg side deficit: control=${avg(c.inferiorSideDeficit,c.inferiorSideWins)} learned=${avg(l.inferiorSideDeficit,l.inferiorSideWins)}")
    println("    winner had higher rolled die total: control=${c.inferiorWonWithHigherRollTotal} (${pctPart(c.inferiorWonWithHigherRollTotal,c.inferiorSideWins)}) learned=${l.inferiorWonWithHigherRollTotal} (${pctPart(l.inferiorWonWithHigherRollTotal,l.inferiorSideWins)})")
    println("    individually decisive Critter present: control=${c.inferiorWithDecisiveCritter} (${pctPart(c.inferiorWithDecisiveCritter,c.inferiorSideWins)}) learned=${l.inferiorWithDecisiveCritter} (${pctPart(l.inferiorWithDecisiveCritter,l.inferiorSideWins)})")
    println("    individually decisive die present: control=${c.inferiorWithDecisiveDie} (${pctPart(c.inferiorWithDecisiveDie,c.inferiorSideWins)}) learned=${l.inferiorWithDecisiveDie} (${pctPart(l.inferiorWithDecisiveDie,l.inferiorSideWins)})")
    println("    winner used no D10+ on row: control=${c.inferiorWithNoHighDie} (${pctPart(c.inferiorWithNoHighDie,c.inferiorSideWins)}) learned=${l.inferiorWithNoHighDie} (${pctPart(l.inferiorWithNoHighDie,l.inferiorSideWins)})")
    println("    Note: Plant/Wisp/Round causality is not reverse-inferred here; the current Strike ledger directly tests final dice and Critters only.")

    val thresholds=listOf("D10+","D12+","D20")
    println("  High-die opportunity/utilization (dice per Battle entry):")
    thresholds.forEach { k ->
        println("    $k owned incl. Mulch ${avg(c.highDieOwned[k]?:0,c.battleEntries)} -> ${avg(l.highDieOwned[k]?:0,l.battleEntries)}; available Supply+Discard ${avg(c.highDieAvailable[k]?:0,c.battleEntries)} -> ${avg(l.highDieAvailable[k]?:0,l.battleEntries)}; placed ${avg(c.highDiePlaced[k]?:0,c.battleEntries)} -> ${avg(l.highDiePlaced[k]?:0,l.battleEntries)}; on winning rows ${avg(c.highDieOnWinningRows[k]?:0,c.battleEntries)} -> ${avg(l.highDieOnWinningRows[k]?:0,l.battleEntries)}; individually decisive ${avg(c.highDieDecisive[k]?:0,c.battleEntries)} -> ${avg(l.highDieDecisive[k]?:0,l.battleEntries)}")
    }
    println("  Winning-row dice by exact quality:")
    (c.committedWinnerDice.keys+l.committedWinnerDice.keys).toSortedSet().forEach { k -> println("    $k committed: control=${c.committedWinnerDice[k]?:0} learned=${l.committedWinnerDice[k]?:0}; individually decisive control=${c.decisiveWinnerDice[k]?:0} learned=${l.decisiveWinnerDice[k]?:0}") }

    println("  Battle-entry maximum-die buckets:")
    val order=listOf("NO_AVAILABLE_DICE","MAX_D8_OR_LOWER","MAX_D10","MAX_D12","HAS_D20")
    order.forEach { k ->
        fun line(a:StrikeRowResearchAccumulator):String {
            val b=a.entryBuckets[k] ?: return "n=0"
            return "n=${b.battles}, BattleVP=${avg(b.battleVp,b.battles)}, rows won=${avg(b.rowsWon,b.battles)}, rows lost=${avg(b.rowsLost,b.battles)}, wounds=${avg(b.wounds,b.battles)}, eventual game win share=${if(b.battles==0L) "0.00%" else pct(b.gameWinShare/b.battles)}"
        }
        println("    $k: control[${line(c)}] learned[${line(l)}]")
    }
    println("    Bucket 'available' uses the pre-Battle cycling pool (Supply + Discard); 'owned' additionally includes prepared Mulch dice.")
}


private fun printEffectResourceUtilization(c: EffectResourceAccumulator, l: EffectResourceAccumulator) {
    fun avg(v: Long, n: Long) = if (n == 0L) "0.00" else "%.2f".format(v.toDouble() / n)
    fun mapLines(title: String, cm: Map<String,Long>, lm: Map<String,Long>) {
        println("  $title:")
        val keys=(cm.keys+lm.keys).sortedWith(compareByDescending<String>{(cm[it]?:0)+(lm[it]?:0)}.thenBy{it})
        if(keys.isEmpty()) println("    none recorded") else keys.forEach { k -> println("    $k: control=${avg(cm[k]?:0,c.games)} learned=${avg(lm[k]?:0,l.games)} per game") }
    }
    println("Effect and resource utilization (affected role)")
    println("  Plant scale/exposure: final Plants are reported above; Plant-round exposure=${avg(c.plantRoundExposure,c.games)} -> ${avg(l.plantRoundExposure,l.games)} card-rounds/game; Battle Plant size=${avg(c.battlePlantExposure,c.battleRounds)} -> ${avg(l.battlePlantExposure,l.battleRounds)}")
    println("  Round card exposure and use:")
    val revealedKeys=(c.roundCardReveals.keys+l.roundCardReveals.keys).toSortedSet()
    revealedKeys.forEach { k -> println("    revealed $k: control=${avg(c.roundCardReveals[k]?:0,c.games)} learned=${avg(l.roundCardReveals[k]?:0,l.games)} per player-game") }
    val effectKeys=(c.roundEffectOpportunities.keys+l.roundEffectOpportunities.keys+c.roundEffectUses.keys+l.roundEffectUses.keys).toSortedSet()
    effectKeys.forEach { k ->
        val co=c.roundEffectOpportunities[k]?:0; val lo=l.roundEffectOpportunities[k]?:0
        val cu=c.roundEffectUses[k]?:0; val lu=l.roundEffectUses[k]?:0
        fun rate(u:Long,o:Long)=if(o==0L) "n/a" else pct(u.toDouble()/o)
        println("    $k: opportunities control=${avg(co,c.games)} learned=${avg(lo,l.games)}; used control=${avg(cu,c.games)} learned=${avg(lu,l.games)}; use/opportunity control=${rate(cu,co)} learned=${rate(lu,lo)}")
    }
    val categoryKeys=(c.roundCategoryOpportunities.keys+l.roundCategoryOpportunities.keys+c.roundCategoryUses.keys+l.roundCategoryUses.keys).toSortedSet()
    if(categoryKeys.isNotEmpty()) {
        println("    Economic categories (decision-point opportunities / uses):")
        categoryKeys.forEach { k ->
            val co=c.roundCategoryOpportunities[k]?:0; val lo=l.roundCategoryOpportunities[k]?:0
            val cu=c.roundCategoryUses[k]?:0; val lu=l.roundCategoryUses[k]?:0
            println("      $k: control=${avg(co,c.games)} / ${avg(cu,c.games)}; learned=${avg(lo,l.games)} / ${avg(lu,l.games)}")
        }
    }
    println("    Note: an opportunity is one Main-Action decision point where that Round effect was executable; the same revealed effect can create more than one opportunity if it remains legal across later decisions.")
    mapLines("Cultivation/Battle round effects actually resolved", c.roundEffects, l.roundEffects)
    mapLines("Battle round effects actually resolved", c.battleRoundEffects, l.battleRoundEffects)
    mapLines("Support actions", c.supportActions, l.supportActions)
    mapLines("Compost/other die upgrades by step", c.upgrades, l.upgrades)
    mapLines("Upgrade source", c.upgradeSources, l.upgradeSources)
    println("  Die development provenance (events per player-game):")
    mapLines("Direct/new die gains by source and resulting size", c.dieGainsBySourceAndSize, l.dieGainsBySourceAndSize)
    mapLines("Upgrades by source and transition", c.upgradesBySourceAndTransition, l.upgradesBySourceAndTransition)
    println("  Die-side power added by development source (sides gained for new dice; incremental sides for upgrades):")
    val powerKeys=(c.dieDevelopmentPowerBySource.keys+l.dieDevelopmentPowerBySource.keys).toSortedSet()
    if(powerKeys.isEmpty()) println("    none recorded") else powerKeys.forEach { k -> println("    $k: control=${avg(c.dieDevelopmentPowerBySource[k]?:0,c.games)} learned=${avg(l.dieDevelopmentPowerBySource[k]?:0,l.games)} per game") }
    println("  Note: development power is event-flow accounting, not a claim that all added sides survive to the final pool. Upgrades contribute only their incremental side increase, preventing the replacement die from being double-counted as a new gain.")
    println("  Wisps:")
    println("    roll-reward Wisps gained: control=${avg(c.rollWispsGained,c.games)} learned=${avg(l.rollWispsGained,l.games)} per game")
    println("    immediate Wisps played from roll reward: control=${avg(c.immediateWispsPlayed,c.games)} learned=${avg(l.immediateWispsPlayed,l.games)} per game")
    println("    final unplayed Wisp count: control=${avg(c.finalWisps,c.games)} learned=${avg(l.finalWisps,l.games)} per game")
    println("    final unplayed Wisp VP: control=${avg(c.finalWispVp,c.games)} learned=${avg(l.finalWispVp,l.games)} per game")
    mapLines("Wisp acquisition triggers", c.wispGainTriggers, l.wispGainTriggers)
    mapLines("Exact Wisp acquisitions by card", c.wispAcquiredByCard, l.wispAcquiredByCard)
    mapLines("Wisp acquisition source and card", c.wispAcquisitionSources, l.wispAcquisitionSources)
    println("    Wisp acquisition/use/retention by card (per player-game; use rate is uses / acquisitions):")
    (c.wispAcquiredByCard.keys + l.wispAcquiredByCard.keys + c.wispEffects.keys + l.wispEffects.keys + c.wispRetainedByCard.keys + l.wispRetainedByCard.keys).toSortedSet().forEach { name ->
        val ca=c.wispAcquiredByCard[name]?:0; val la=l.wispAcquiredByCard[name]?:0
        val cu=c.wispEffects[name]?:0; val lu=l.wispEffects[name]?:0
        val cr=c.wispRetainedByCard[name]?:0; val lr=l.wispRetainedByCard[name]?:0
        fun rate(used:Long, acquired:Long)=if(acquired==0L) "n/a" else "%.1f%%".format(100.0*used/acquired)
        println("      $name: control acquired=${avg(ca,c.games)} used=${avg(cu,c.games)} rate=${rate(cu,ca)} retained=${avg(cr,c.games)}; learned acquired=${avg(la,l.games)} used=${avg(lu,l.games)} rate=${rate(lu,la)} retained=${avg(lr,l.games)}")
    }
    mapLines("Wisp effects actually played/resolved (by card)", c.wispEffects, l.wispEffects)
    mapLines("Plant effects actually resolved (by card)", c.plantEffects, l.plantEffects)
    println("  Note: exact Wisp acquisitions include Roll Rewards, effect draws, and steals. Retained-by-card is the exact final hand snapshot. Existing Upgrade source reporting shows resulting Wisp-driven die upgrades by card.")
    println("  Plant-round exposure sums the player's grafted Plant count at every completed round; it helps distinguish equal resource-use counts applied to differently sized Plant creatures.")
}


private fun printVpLedger(c: VpLedgerAccumulator, l: VpLedgerAccumulator) {
    fun avg(v: Long, n: Long) = if (n == 0L) "0.00" else "%.2f".format(v.toDouble() / n)
    fun delta(cv: Long, cn: Long, lv: Long, ln: Long): String {
        val cAvg = if (cn == 0L) 0.0 else cv.toDouble() / cn
        val lAvg = if (ln == 0L) 0.0 else lv.toDouble() / ln
        return "%+.2f".format(lAvg - cAvg)
    }
    fun line(label: String, cv: Long, lv: Long) =
        println("  ${label.padEnd(30)} control=${avg(cv,c.games)} learned=${avg(lv,l.games)} delta=${delta(cv,c.games,lv,l.games)}")

    println("VP ledger (affected role; exact reconciliation to FinalScore)")
    line("Final VP", c.finalVp, l.finalVp)
    line("Battle Strike VP", c.battleStrikeVp, l.battleStrikeVp)
    line("Direct GAIN_ONE_VP effects", c.directEffectVp, l.directEffectVp)
    line("Other in-play VP", c.otherExistingVp, l.otherExistingVp)
    line("Plant end-game VP", c.plantVp, l.plantVp)
    line("Unplayed Wisp VP", c.wispVp, l.wispVp)
    println("  Existing/in-play VP check: control=${avg(c.existingVp,c.games)} learned=${avg(l.existingVp,l.games)} (Battle + direct effects + other in-play)")
    println("  Reconciliation: Final VP = Battle Strike VP + direct GAIN_ONE_VP + other in-play VP + Plant end-game VP + unplayed Wisp VP")

    val effectKeys=(c.directEffectVpBySource.keys+l.directEffectVpBySource.keys).sortedWith(compareByDescending<String>{(c.directEffectVpBySource[it]?:0)+(l.directEffectVpBySource[it]?:0)}.thenBy{it})
    println("  Direct GAIN_ONE_VP by source:")
    if(effectKeys.isEmpty()) println("    none recorded") else effectKeys.forEach { k ->
        println("    $k: control=${avg(c.directEffectVpBySource[k]?:0,c.games)} learned=${avg(l.directEffectVpBySource[k]?:0,l.games)} per game")
    }

    val plantKeys=(c.plantVpByCard.keys+l.plantVpByCard.keys).sortedWith(compareByDescending<String>{(c.plantVpByCard[it]?:0)+(l.plantVpByCard[it]?:0)}.thenBy{it})
    println("  Plant end-game VP by card identity:")
    if(plantKeys.isEmpty()) println("    none recorded") else plantKeys.forEach { k ->
        println("    $k: control=${avg(c.plantVpByCard[k]?:0,c.games)} learned=${avg(l.plantVpByCard[k]?:0,l.games)} per game")
    }
    if(c.unattributedPlantVp != 0L || l.unattributedPlantVp != 0L) {
        println("    Butterfly-dependent Plant VP (identity not attributable from compact final state): control=${avg(c.unattributedPlantVp,c.games)} learned=${avg(l.unattributedPlantVp,l.games)} per game")
    }
    println("  Note: 'Other in-play VP' is the exact FinalScore existing-VP remainder after recorded Battle Strike VP and GAIN_ONE_VP effects; it includes variable VP effects whose awarded amount is not carried by EffectResolved.")
}

private fun printWatchedCards(c:EvalAccumulator,l:EvalAccumulator) {
    println("Grove sensitivity and notable VP cards")
    WATCHED_CARDS.forEach { name ->
        val n=c.groveCardGames[name]?:0; if(n==0L) return@forEach
        val cw=(c.groveCardWins[name]?:0.0)/n; val lw=(l.groveCardWins[name]?:0.0)/n
        val cvp=if(name=="Flower_11_01") "n/a" else "${c.watchedCardVp[name]?:0}"; val lvp=if(name=="Flower_11_01") "n/a" else "${l.watchedCardVp[name]?:0}"
        println("  $name present (n=$n): win share ${pct(cw)} -> ${pct(lw)} delta=${signedPct(lw-cw)}; final copies ${c.watchedCardCopies[name]?:0} -> ${l.watchedCardCopies[name]?:0}; attributed Plant VP $cvp -> $lvp")
    }
    println("  Note: conditional Grove win shares are exploratory associations, not isolated causal effects, because the other eight Grove slots also vary.")
    println("  Attributed Plant VP follows the production scoring rule; Butterfly-based scoring is omitted from this compact attribution because final Butterfly count is not retained in GameSummary.")
}

private fun printReport(o:EvalOptions, weights:LearnedBuyWeights, c:EvalAccumulator, l:EvalAccumulator) {
    fun avg(x: Long): String = "%.2f".format(x.toDouble() / o.games)
    fun delta(a: Double, b: Double): String = "%+.2f".format(b - a)
    val cw=c.winShare/o.games; val lw=l.winShare/o.games
    println("Held-out result")
    println("  Control win share: ${pct(cw)}")
    println("  Learned win share: ${pct(lw)}")
    println("  Win-share delta:   ${signedPct(lw-cw)}")
    println("  Avg VP:             control=${avg(c.vp)} learned=${avg(l.vp)} delta=${delta(c.vp.toDouble()/o.games,l.vp.toDouble()/o.games)}")
    println("  Avg final Plants:   control=${avg(c.plants)} learned=${avg(l.plants)} delta=${delta(c.plants.toDouble()/o.games,l.plants.toDouble()/o.games)}")
    println("  Avg final Plant printed cost: control=${avg(c.plantCost)} learned=${avg(l.plantCost)} delta=${delta(c.plantCost.toDouble()/o.games,l.plantCost.toDouble()/o.games)}")
    println("  Avg final dice:     control=${avg(c.dice)} learned=${avg(l.dice)}")
    println("  Avg die-side power: control=${avg(c.dicePower)} learned=${avg(l.dicePower)}")
    println("  Avg sides / die:    control=${"%.2f".format(c.dicePower.toDouble()/c.dice)} learned=${"%.2f".format(l.dicePower.toDouble()/l.dice)}")
    println("  Avg Battle VP:      control=${avg(c.battleVp)} learned=${avg(l.battleVp)}")
    println("  Avg Wounds:         control=${avg(c.wounds)} learned=${avg(l.wounds)}")
    println("  Avg final dice by size:")
    listOf("D4","D6","D8","D10","D12","D20").forEach { size -> println("    $size: control=${avg(c.finalDiceSizes[size]?:0)} learned=${avg(l.finalDiceSizes[size]?:0)}") }
    println()
    println("Affected-role win share by physical seat")
    for(s in 0..3) { val n=c.seatGames[s]; println("  Seat ${s+1} (n=$n): control=${pct(c.seatWins[s]/n)} learned=${pct(l.seatWins[s]/n)} delta=${signedPct(l.seatWins[s]/n-c.seatWins[s]/n)}") }
    println()
    println("Buy behavior (affected role; totals across ${o.games} games)")
    println("  Plant purchases: control=${c.plantPurchases} learned=${l.plantPurchases}; avg/game=${avg(c.plantPurchases)} -> ${avg(l.plantPurchases)}")
    println("  Die purchases:   control=${c.diePurchases} learned=${l.diePurchases}; avg/game=${avg(c.diePurchases)} -> ${avg(l.diePurchases)}")
    printCounts("Plant purchases by effective cost",c.plantCosts,l.plantCosts)
    printCounts("Plant purchases by type",c.plantTypes,l.plantTypes)
    printCounts("Die purchases by size",c.dieSizes,l.dieSizes)
    printCounts("Individual Plant acquisitions",c.plantCards,l.plantCards)
    println()
    printBuyShape(c.buyShape, l.buyShape)
    println()
    printBattleShape(c.battleShape,l.battleShape)
    println()
    printStrikeRowResearch(c.strikeResearch,l.strikeResearch)
    println()
    printEffectResourceUtilization(c.utilization,l.utilization)
    println()
    printVpLedger(c.vpLedger,l.vpLedger)
    println()
    printWatchedCards(c,l)
    println()
    println("Policy provenance")
    println("  trained rounds=${weights.provenance.roundPattern}; Grove=${weights.provenance.grove}; generations=${weights.provenance.generations}; games/policy=${weights.provenance.gamesPerPolicy}; training fitness=${weights.provenance.fitness?.let(::pct) ?: "unknown"}")
    println("  training mechanical seed start=${weights.provenance.mechanicalSeedStart}; strategy seed start=${weights.provenance.strategySeedStart}")
    println("  evaluation mechanical seeds=${o.seed}..${o.seed+o.games-1}; strategy seeds=${o.strategySeed}..${o.strategySeed+o.games-1}")
    if (o.grovePattern != null) println("  Grove pattern=${o.grovePattern}; Grove seeds=${o.groveSeed}..${o.groveSeed+o.games-1}; zeros resolved once per matched sample")
    println()
    println("Interpretation: this is held-out evidence for this policy with ${o.groveInterpretation()} and ${o.roundLabel}, not evidence for other Grove constraints or round structures.")
    println("Note: a zero-purchase Buy phase means the player made no recorded purchase in that phase; the Chronicle does not distinguish an explicit Done choice from having no legal purchase.")
}

private fun <K:Comparable<K>> printCounts(title:String,c:Map<K,Long>,l:Map<K,Long>) { println("  $title:"); (c.keys+l.keys).toSortedSet().forEach { k -> println("    $k: control=${c[k]?:0} learned=${l[k]?:0}") } }
private fun pct(x:Double)="%.2f%%".format(x*100.0)
private fun signedPct(x:Double)=(if(x>=0) "+" else "")+pct(x)

internal fun resolveGroveForSample(
    options: EvalOptions,
    sample: Int,
    plantManager: PlantCardManager,
    defaultGrove: List<PlantCard>,
    allPlants: List<PlantCard>,
    plantValues: PlantValueResolver = PlantValueResolver.CANONICAL,
): List<PlantCard> = resolveResearchGroveForSample(
    grovePattern = options.grovePattern,
    groveSeed = options.groveSeed,
    sample = sample,
    plantManager = plantManager,
    defaultGrove = defaultGrove,
    allPlants = allPlants,
    plantValues = plantValues,
    excludedCards = options.excludedCards,
)


internal data class ResolvedResearchEnvironment(
    val roundCards: List<RoundCard>?,
    val wispCards: List<WispCard>?,
)

private val DIE_DEVELOPMENT_EFFECTS = setOf(
    GameEffect.UPGRADE_DIE_FROM_HAND,
    GameEffect.UPGRADE_DIE_AND_USE_NOW,
    GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW,
    GameEffect.GAIN_ANY_DIE_TO_DISCARD,
    GameEffect.GAIN_D10_TO_DISCARD,
    GameEffect.GAIN_D12_TO_DISCARD,
    GameEffect.GAIN_D20_TO_DISCARD,
)

private fun RoundCard.supportsNonBuyDieDevelopment(): Boolean =
    firstEffect.effect in DIE_DEVELOPMENT_EFFECTS || secondEffect.effect in DIE_DEVELOPMENT_EFFECTS

private fun WispCard.supportsNonBuyDieDevelopment(): Boolean = effect in DIE_DEVELOPMENT_EFFECTS

internal fun resolveResearchEnvironmentForSample(
    options: EvalOptions,
    sample: Int,
    roundManager: RoundCardManager,
    wispManager: WispCardManager,
): ResolvedResearchEnvironment {
    val namedEnvironment = options.researchEnvironment != "default"
    val roundControlled = namedEnvironment || options.roundIncludes.isNotEmpty() || options.roundExcludes.isNotEmpty()
    val wispControlled = namedEnvironment || options.wispIncludes.isNotEmpty() || options.wispExcludes.isNotEmpty()
    if (!roundControlled && !wispControlled) return ResolvedResearchEnvironment(null, null)
    val roundRng = Randomizer.create(options.environmentSeed + sample)
    val wispRng = Randomizer.create(options.environmentSeed + sample + 1_000_003L)
    val allRounds = roundManager.getAllCards().cards
    val allWisps = wispManager.getAllCards().cards
    validateNames("Round", options.roundIncludes + options.roundExcludes, allRounds.map { it.name }.toSet())
    validateNames("Wisp", options.wispIncludes + options.wispExcludes, allWisps.map { it.name }.toSet())

    val roundDefinitions = allRounds.filter { card ->
        (options.roundIncludes.isEmpty() || card.name in options.roundIncludes) && card.name !in options.roundExcludes
    }
    val wispDefinitions = allWisps.filter { card ->
        (options.wispIncludes.isEmpty() || card.name in options.wispIncludes) && card.name !in options.wispExcludes
    }
    require(roundDefinitions.isNotEmpty()) { "Round research pool is empty after include/exclude constraints." }
    require(wispDefinitions.isNotEmpty()) { "Wisp research pool is empty after include/exclude constraints." }

    fun expandedRounds(type: RoundCardType): List<RoundCard> = roundDefinitions
        .filter { it.type == type }
        .flatMap { card -> List(card.quantity) { card } }
    fun chooseRounds(type: RoundCardType, count: Int): List<RoundCard> {
        if (count == 0) return emptyList()
        val physical = expandedRounds(type)
        require(physical.size >= count) {
            "Round research pool has too few $type physical cards: required=$count available=${physical.size}. " +
                "Relax --round-include-card/--round-exclude-card constraints."
        }
        val shuffled = roundRng.shuffled(physical)
        return when (options.researchEnvironment) {
            "upgrade-rich" -> shuffled.sortedByDescending { it.supportsNonBuyDieDevelopment() }.take(count)
            "upgrade-poor" -> shuffled.sortedBy { it.supportsNonBuyDieDevelopment() }.take(count)
            else -> shuffled.take(count)
        }
    }
    val cCount = options.roundSetup.roundTypes.count { it == RoundCardType.CULTIVATION }
    val bCount = options.roundSetup.roundTypes.count { it == RoundCardType.BATTLE }
    val cultivation = if (roundControlled) chooseRounds(RoundCardType.CULTIVATION, cCount) else emptyList()
    val battle = if (roundControlled) chooseRounds(RoundCardType.BATTLE, bCount) else emptyList()
    var ci=0; var bi=0
    val exactRounds = if (roundControlled) options.roundSetup.roundTypes.map { if (it == RoundCardType.CULTIVATION) cultivation[ci++] else battle[bi++] } else null

    val expandedWisps = wispDefinitions.flatMap { card -> List(card.quantity) { card } }
    val exactWisps = if (!wispControlled) null else when (options.researchEnvironment) {
        "upgrade-rich" -> {
            val rich = expandedWisps.filter { it.supportsNonBuyDieDevelopment() }
            require(rich.isNotEmpty()) { "upgrade-rich Wisp pool contains no die-development Wisp. Relax Wisp constraints." }
            wispRng.shuffled(rich)
        }
        "upgrade-poor" -> {
            val poor = expandedWisps.filterNot { it.supportsNonBuyDieDevelopment() }
            require(poor.isNotEmpty()) { "upgrade-poor Wisp pool is empty after removing die-development Wisps." }
            wispRng.shuffled(poor)
        }
        else -> wispRng.shuffled(expandedWisps)
    }
    return ResolvedResearchEnvironment(exactRounds, exactWisps)
}

private fun validateNames(kind:String, requested:Set<String>, known:Set<String>) {
    requested.forEach { require(it in known) { "Unknown $kind research card: $it" } }
}

private fun rejectTrainingSeedOverlap(weights:LearnedBuyWeights,o:EvalOptions) {
    val n=weights.provenance.gamesPerPolicy ?: return
    fun overlaps(a0:Long,a1:Long,b0:Long,b1:Long)=a0<=b1 && b0<=a1
    weights.provenance.mechanicalSeedStart?.let { start -> require(!overlaps(o.seed,o.seed+o.games-1,start,start+n-1)) { "Evaluation mechanical seeds overlap recorded training seeds $start..${start+n-1}; choose held-out --seed values." } }
    weights.provenance.strategySeedStart?.let { start -> require(!overlaps(o.strategySeed,o.strategySeed+o.games-1,start,start+n-1)) { "Evaluation strategy seeds overlap recorded training seeds $start..${start+n-1}; choose held-out --strategy-seed values." } }
}

private fun learnedFactory(weights:LearnedBuyWeights):PlayerDecisionFactory=object:PlayerDecisionFactory { override fun create()=LearnedBuy.createDirector(weights); override fun create(strategyRandomizer:StrategyRandomizer)=LearnedBuy.createDirector(weights,strategyRandomizer); override fun create(strategyRandomizer:StrategyRandomizer,reasoningSink:DecisionReasoningSink)=LearnedBuy.createDirector(weights,strategyRandomizer) }
private fun loadCards(plantRegistry:PlantCardRegistry,plantManager:PlantCardManager,wispRegistry:WispCardRegistry,wispManager:WispCardManager,roundRegistry:RoundCardRegistry,roundManager:RoundCardManager){ val root=CardDataFiles.dataDirectory(); plantRegistry.clear(); plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST,root),CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST,root)); plantManager.loadCards(plantRegistry); wispRegistry.clear(); wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST,root)); wispManager.loadCards(wispRegistry); roundRegistry.clear(); roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST,root)); roundManager.loadCards(roundRegistry) }

internal fun parseRoundSetup(label: String): GameRoundSetup {
    val value = label.trim()
    if ('/' in value) {
        val blocks = value.split('/').map { it.toInt() }
        require(blocks.isNotEmpty() && blocks.all { it > 0 }) { "--rounds must contain positive Cultivation blocks, e.g. 3/2/2" }
        return GameRoundSetup.Patterned(blocks)
    }
    require(value.length == 2 && value.all { it in '1'..'9' }) {
        "Compact --rounds must be two positive digits: cultivation count then battle count, e.g. 32 or 95"
    }
    return GameRoundSetup.Ordered(value[0].digitToInt(), value[1].digitToInt())
}

internal fun normalizedRoundLabel(label: String): String = label.trim()

internal data class EvalOptions(
    val games: Int,
    val seed: Long,
    val strategySeed: Long,
    val input: Path,
    val grovePattern: String?,
    val groveSeed: Long,
    val excludedCards: Set<String>,
    val roundSetup: GameRoundSetup,
    val roundLabel: String,
    val researchEnvironment: String,
    val environmentSeed: Long,
    val roundIncludes: Set<String>,
    val roundExcludes: Set<String>,
    val wispIncludes: Set<String>,
    val wispExcludes: Set<String>,
    val plantOverridesPath: Path?,
) {
    fun groveDescription(): String = grovePattern?.let { "Grove pattern=$it (new resolution per matched sample)" } ?: "Grove=FirstGameDefault"
    fun groveInterpretation(): String = grovePattern?.let { "Grove pattern $it resolved independently per matched sample" } ?: "FirstGameDefault"

    companion object {
        fun parse(args: List<String>): EvalOptions {
            var games = 1000
            var seed = 161000L
            var strategy = 171000L
            var input = Paths.get("output/ai/buy-policy-v1-trained.weights")
            var grovePattern: String? = null
            var groveSeed = 181000L
            val excludedCards = linkedSetOf<String>()
            var roundLabel = "3/2/2"
            var researchEnvironment = "default"
            var environmentSeed = 191000L
            val roundIncludes = linkedSetOf<String>()
            val roundExcludes = linkedSetOf<String>()
            val wispIncludes = linkedSetOf<String>()
            val wispExcludes = linkedSetOf<String>()
            var plantOverridesPath: Path? = null
            var positional = false
            var i = 0

            fun value(argument: String): String {
                return if ('=' in argument) {
                    argument.substringAfter('=')
                } else {
                    i++
                    args[i]
                }
            }

            while (i < args.size) {
                val argument = args[i]
                when {
                    !positional && argument.matches(Regex("[1-9][0-9]*")) -> {
                        games = argument.toInt()
                        positional = true
                    }
                    argument.startsWith("--games") -> games = value(argument).toInt()
                    argument.startsWith("--seed") -> seed = value(argument).toLong()
                    argument.startsWith("--strategy-seed") -> strategy = value(argument).toLong()
                    argument.startsWith("--input") || argument.startsWith("--weights") -> input = Paths.get(value(argument))
                    argument.startsWith("--grove-seed") -> groveSeed = value(argument).toLong()
                    argument.startsWith("--exclude-card") -> value(argument).split(',').filter { it.isNotBlank() }.forEach { excludedCards += it.trim() }
                    argument.startsWith("--research-environment") -> researchEnvironment = value(argument).trim().lowercase()
                    argument.startsWith("--environment-seed") -> environmentSeed = value(argument).toLong()
                    argument.startsWith("--round-include-card") -> value(argument).split(',').filter { it.isNotBlank() }.forEach { roundIncludes += it.trim() }
                    argument.startsWith("--round-exclude-card") -> value(argument).split(',').filter { it.isNotBlank() }.forEach { roundExcludes += it.trim() }
                    argument.startsWith("--wisp-include-card") -> value(argument).split(',').filter { it.isNotBlank() }.forEach { wispIncludes += it.trim() }
                    argument.startsWith("--wisp-exclude-card") -> value(argument).split(',').filter { it.isNotBlank() }.forEach { wispExcludes += it.trim() }
                    argument.startsWith("--plant-overrides") -> plantOverridesPath = Paths.get(value(argument))
                    argument.startsWith("--rounds") -> roundLabel = value(argument).trim()
                    argument.startsWith("--grove") -> grovePattern = GrovePlantCode.validate(value(argument))
                    argument == "--random-grove" -> grovePattern = GrovePlantCode.RANDOM_PATTERN
                    argument == "--help" -> {
                        usage()
                        kotlin.system.exitProcess(0)
                    }
                    else -> error("Unknown argument: $argument")
                }
                i++
            }

            require(games > 0)
            require(researchEnvironment in setOf("default","upgrade-rich","upgrade-poor")) { "--research-environment must be default, upgrade-rich, or upgrade-poor" }
            require(roundIncludes.intersect(roundExcludes).isEmpty()) { "A Round card cannot be both included and excluded: ${roundIncludes.intersect(roundExcludes)}" }
            require(wispIncludes.intersect(wispExcludes).isEmpty()) { "A Wisp card cannot be both included and excluded: ${wispIncludes.intersect(wispExcludes)}" }
            val roundSetup = parseRoundSetup(roundLabel)
            return EvalOptions(games, seed, strategy, input, grovePattern, groveSeed, excludedCards, roundSetup, normalizedRoundLabel(roundLabel), researchEnvironment, environmentSeed, roundIncludes, roundExcludes, wispIncludes, wispExcludes, plantOverridesPath)
        }

        private fun usage() {
            println("evaluate_buy_policy [N|--games N] [--seed N] [--strategy-seed N] [--weights PATH|--input PATH] [--grove CODE|--random-grove] [--exclude-card NAME] [--grove-seed N] [--rounds PATTERN] [--research-environment default|upgrade-rich|upgrade-poor] [--environment-seed N] [--round-include-card NAME] [--round-exclude-card NAME] [--wisp-include-card NAME] [--wisp-exclude-card NAME] [--plant-overrides PATH]")
            println("  --weights PATH selects the frozen learned policy; --input remains a backward-compatible alias.")
            println("  --plant-overrides PATH loads research-only Plant cost, availability, and typed scoring interventions.")
            println("  --grove 000100000 keeps Vine_07_01 fixed and resolves all zero slots anew for each matched sample.")
            println("  --exclude-card Vine_07_04 excludes a Plant from random/zero Grove slots; repeat it or comma-separate names.")
            println("  --rounds 3/2/2 means 3C-B-2C-B-2C-B (Cultivation blocks, existing semantics).")
            println("  --rounds 95 means 9 Cultivation followed by 5 Battle; --rounds 32 means 3 Cultivation followed by 2 Battle.")
            println("  --research-environment upgrade-rich maximizes non-Buy die-development Round cards and uses only die-development Wisps within the constrained pools.")
            println("  --research-environment upgrade-poor minimizes such Round cards (subject to physical inventory/round count) and removes die-development Wisps.")
            println("  Round/Wisp include/exclude options accept repeated or comma-separated exact card names; invalid or insufficient pools fail explicitly.")
            println("  Research environments use a dedicated --environment-seed (default 191000), so environment resolution does not perturb gameplay RNG.")
            println("  CONTROL and LEARNED always share the same concrete Grove, Round deck, and Wisp deck within a matched sample.")
        }
    }
}
