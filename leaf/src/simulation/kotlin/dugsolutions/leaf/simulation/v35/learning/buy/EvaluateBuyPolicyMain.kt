package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.simulation.v35.analysis.GameSummary
import dugsolutions.leaf.simulation.v35.analysis.GameSummaryExtractor
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.SimulationRunContext
import dugsolutions.leaf.simulation.v35.experiment.diagnostic.withSimulationFailureDiagnostics
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
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.round.domain.RoundCardType
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
        val allPlants = plantManager.getAllCards().cards
        val defaultGrove = FirstGameDefault.PLANT_NAMES.map { requireNotNull(plantManager.getCard(it)) }
        val raw = LearnedBuyWeights.load(o.input)
        require(raw.provenance.trainingStatus == "trained") {
            "Held-out evaluation requires a trained policy; ${o.input} has trainingStatus=${raw.provenance.trainingStatus}"
        }
        val weights = LearnedBuyCardCatalog.prepare(raw, allPlants)
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
        println("affected role rotates across physical seats; opponents=Human Baseline; ${o.groveDescription()}; rounds=3/2/2")
        if (o.grovePattern != null) {
            println("Grove zeros are resolved independently once per matched sample using grove seeds=${o.groveSeed}..${o.groveSeed + o.games - 1}; CONTROL and LEARNED share that resolved Grove")
        }
        println("CONTROL=Human Baseline; LEARNED=same role with learned Buy selection only; all other decisions=Human Baseline")
        println("training fitness recorded in policy=${weights.provenance.fitness?.let(::pct) ?: "unknown"}; evaluation seeds are required not to overlap its recorded training cohorts")
        println()

        repeat(o.games) { sample ->
            val seat = sample % 4
            val mechanicalSeed = o.seed + sample
            val strategySeed = o.strategySeed + sample
            val resolvedGrove = resolveGroveForSample(o, sample, plantManager, defaultGrove)
            val groveCode = GrovePlantCode.encode(resolvedGrove)
            val controlFactories = List(4) { PlayerDecisionFactory.humanBaseline() }
            val learnedFactories = List(4) { if (it == seat) learnedFactory(weights) else PlayerDecisionFactory.humanBaseline() }
            control.add(runOne(factory, runner, resolvedGrove, groveCode, controlFactories, mechanicalSeed, strategySeed, sample, seat, "CONTROL"), seat, plantsByName, resolvedGrove)
            learned.add(runOne(factory, runner, resolvedGrove, groveCode, learnedFactories, mechanicalSeed, strategySeed, sample, seat, "LEARNED"), seat, plantsByName, resolvedGrove)
        }
        printReport(o, weights, control, learned)
    } finally { app.close() }
}

internal data class CompletedEvalGame(val summary: GameSummary, val entries: List<GameEntry>)

private fun runOne(factory: GameFactory, runner: GameRunner, grove: List<PlantCard>, groveCode: String, decisions: List<PlayerDecisionFactory>, seed: Long, strategySeed: Long, sample: Int, affectedSeat: Int, variant: String): CompletedEvalGame {
    val game = factory(GameConfig(selectedPlantCards=grove, playerDecisionFactories=decisions, roundSetup=GameRoundSetup.standard(), seed=seed, strategySeed=strategySeed, recordDecisionReasoning=false))
    val result = withSimulationFailureDiagnostics(game, SimulationRunContext("evaluate_buy_policy", sample, variant, affectedSeat, seed, strategySeed, groveCode, "3/2/2")) { runner.run(game) }
    return CompletedEvalGame(GameSummaryExtractor.extract(game, result), game.chronicle.entries.toList())
}

internal class EvalAccumulator {
    var winShare=0.0; var vp=0L; var plants=0L; var plantCost=0L; var dice=0L; var dicePower=0L; var battleVp=0L; var wounds=0L
    var plantPurchases=0L; var diePurchases=0L
    val seatWins=DoubleArray(4); val seatGames=IntArray(4)
    val plantCosts=sortedMapOf<Int,Long>(); val plantTypes=sortedMapOf<String,Long>(); val plantCards=sortedMapOf<String,Long>(); val dieSizes=sortedMapOf<String,Long>(); val finalDiceSizes=sortedMapOf<String,Long>()
    val buyShape = BuyShapeAccumulator()
    val battleShape = BattleShapeAccumulator()
    val utilization = EffectResourceAccumulator()
    val vpLedger = VpLedgerAccumulator()
    val groveCardGames=mutableMapOf<String,Long>(); val groveCardWins=mutableMapOf<String,Double>()
    val watchedCardCopies=mutableMapOf<String,Long>(); val watchedCardVp=mutableMapOf<String,Long>()

    fun add(game: CompletedEvalGame, seat: Int, plantsByName: Map<String,PlantCard>, grove: List<PlantCard>) {
        val p=game.summary.players.single { it.seat==seat }
        winShare+=p.winShare; vp+=p.totalVp; plants+=p.finalPlantCount; plantCost+=p.finalPlantPrintedCost; dice+=p.finalDiceCount; dicePower+=p.finalDicePower; battleVp+=p.battleStrikeVp; wounds+=p.woundsTaken
        seatWins[seat]+=p.winShare; seatGames[seat]++
        mapOf("D4" to p.ownedDiceSignature.d4,"D6" to p.ownedDiceSignature.d6,"D8" to p.ownedDiceSignature.d8,"D10" to p.ownedDiceSignature.d10,"D12" to p.ownedDiceSignature.d12,"D20" to p.ownedDiceSignature.d20).forEach { (k,v) -> finalDiceSizes[k]=(finalDiceSizes[k]?:0)+v }
        buyShape.addGame(game.entries, p.playerId)
        battleShape.addGame(game.entries, p.playerId)
        utilization.addGame(game.entries, p.playerId)
        vpLedger.addGame(p, game.entries, plantsByName)
        grove.map { it.name }.distinct().forEach { name -> groveCardGames[name]=(groveCardGames[name]?:0)+1; groveCardWins[name]=(groveCardWins[name]?:0.0)+p.winShare }
        WATCHED_CARDS.forEach { name ->
            val copies=p.plantCreatureSignature.cards.count { it.plantName==name }
            if(copies>0) {
                watchedCardCopies[name]=(watchedCardCopies[name]?:0)+copies
                val card=plantsByName[name]
                if(card!=null) scoreWatchedCard(card,copies,p)?.let { watchedCardVp[name]=(watchedCardVp[name]?:0)+it.toLong() }
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

private fun scoreWatchedCard(card: PlantCard, copies: Int, p: dugsolutions.leaf.simulation.v35.analysis.PlayerGameSummary): Int? {
    val perCopy = when(val rule=card.scoringRule) {
        is PlantScoringRule.Fixed -> rule.points
        PlantScoringRule.PerGraftedVine -> p.plantCreatureSignature.cards.count { it.plantName.startsWith("Vine_") }
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


internal class EffectResourceAccumulator {
    var games = 0L
    var finalWisps = 0L
    var finalWispVp = 0L
    var rollWispsGained = 0L
    var immediateWispsPlayed = 0L
    val roundEffects = sortedMapOf<String, Long>()
    val battleRoundEffects = sortedMapOf<String, Long>()
    val plantEffects = sortedMapOf<String, Long>()
    val wispEffects = sortedMapOf<String, Long>()
    val supportActions = sortedMapOf<String, Long>()
    val upgrades = sortedMapOf<String, Long>()
    val upgradeSources = sortedMapOf<String, Long>()
    val wispGainTriggers = sortedMapOf<String, Long>()
    var plantRoundExposure = 0L
    var battlePlantExposure = 0L
    var battleRounds = 0L

    fun addGame(entries: List<GameEntry>, playerId: PlayerId) {
        games++
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
                RollRewardKind.WISP_GAINED -> { rollWispsGained++; wispGainTriggers.bump("Roll reward") }
                RollRewardKind.WISP_PLAYED_IMMEDIATELY -> { immediateWispsPlayed++; wispGainTriggers.bump("Roll reward (immediate play)") }
                else -> Unit
            }
        }
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
        entries.withIndex().filter { it.value is GameEntry.Upgrade && (it.value as GameEntry.Upgrade).playerId == playerId }.forEach { indexed ->
            val u = indexed.value as GameEntry.Upgrade
            upgrades.bump("${u.from.name}->${u.to.name}")
            val source = enclosingEffect(entries, indexed.index)
            upgradeSources.bump(source?.let { "${it.sourceKind.name}:${it.sourceName}" } ?: "unscoped")
        }
    }

    private fun enclosingEffect(entries: List<GameEntry>, index: Int): GameEntry.EffectResolved? {
        val depth = entries[index].hierarchyDepth
        for (i in index - 1 downTo 0) {
            val e = entries[i]
            if (e.hierarchyDepth < depth) return e as? GameEntry.EffectResolved
        }
        return null
    }
    private fun MutableMap<String, Long>.bump(key: String) { this[key] = (this[key] ?: 0L) + 1L }
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
        plantsByName: Map<String, PlantCard>
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
        var attributedPlants = 0
        p.plantCreatureSignature.cards.groupingBy { it.plantName }.eachCount().forEach { (name, copies) ->
            val card = plantsByName[name] ?: return@forEach
            val perCopy = when (val rule = card.scoringRule) {
                is PlantScoringRule.Fixed -> rule.points
                PlantScoringRule.PerGraftedVine -> vineCount
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


private fun printEffectResourceUtilization(c: EffectResourceAccumulator, l: EffectResourceAccumulator) {
    fun avg(v: Long, n: Long) = if (n == 0L) "0.00" else "%.2f".format(v.toDouble() / n)
    fun mapLines(title: String, cm: Map<String,Long>, lm: Map<String,Long>) {
        println("  $title:")
        val keys=(cm.keys+lm.keys).sortedWith(compareByDescending<String>{(cm[it]?:0)+(lm[it]?:0)}.thenBy{it})
        if(keys.isEmpty()) println("    none recorded") else keys.forEach { k -> println("    $k: control=${avg(cm[k]?:0,c.games)} learned=${avg(lm[k]?:0,l.games)} per game") }
    }
    println("Effect and resource utilization (affected role)")
    println("  Plant scale/exposure: final Plants are reported above; Plant-round exposure=${avg(c.plantRoundExposure,c.games)} -> ${avg(l.plantRoundExposure,l.games)} card-rounds/game; Battle Plant size=${avg(c.battlePlantExposure,c.battleRounds)} -> ${avg(l.battlePlantExposure,l.battleRounds)}")
    mapLines("Cultivation/Battle round effects actually resolved", c.roundEffects, l.roundEffects)
    mapLines("Battle round effects actually resolved", c.battleRoundEffects, l.battleRoundEffects)
    mapLines("Support actions", c.supportActions, l.supportActions)
    mapLines("Compost/other die upgrades by step", c.upgrades, l.upgrades)
    mapLines("Upgrade source", c.upgradeSources, l.upgradeSources)
    println("  Wisps:")
    println("    roll-reward Wisps gained: control=${avg(c.rollWispsGained,c.games)} learned=${avg(l.rollWispsGained,l.games)} per game")
    println("    immediate Wisps played from roll reward: control=${avg(c.immediateWispsPlayed,c.games)} learned=${avg(l.immediateWispsPlayed,l.games)} per game")
    println("    final unplayed Wisp count: control=${avg(c.finalWisps,c.games)} learned=${avg(l.finalWisps,l.games)} per game")
    println("    final unplayed Wisp VP: control=${avg(c.finalWispVp,c.games)} learned=${avg(l.finalWispVp,l.games)} per game")
    mapLines("Wisp acquisition triggers visible in Chronicle", c.wispGainTriggers, l.wispGainTriggers)
    mapLines("Wisp effects actually played/resolved (by card)", c.wispEffects, l.wispEffects)
    mapLines("Plant effects actually resolved (by card)", c.plantEffects, l.plantEffects)
    println("  Note: Wisp gain triggers are event counts, not an exact acquired-card count: steal-all can transfer multiple Wisps, and current Chronicle does not record the identity of a Wisp drawn by a round/Plant gain effect. Final Wisp count and FinalScore Wisp VP are exact.")
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
    println("  Avg Plant cost:     control=${avg(c.plantCost)} learned=${avg(l.plantCost)} delta=${delta(c.plantCost.toDouble()/o.games,l.plantCost.toDouble()/o.games)}")
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
    printCounts("Plant purchases by printed cost",c.plantCosts,l.plantCosts)
    printCounts("Plant purchases by type",c.plantTypes,l.plantTypes)
    printCounts("Die purchases by size",c.dieSizes,l.dieSizes)
    printCounts("Individual Plant acquisitions",c.plantCards,l.plantCards)
    println()
    printBuyShape(c.buyShape, l.buyShape)
    println()
    printBattleShape(c.battleShape,l.battleShape)
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
    println("Interpretation: this is held-out evidence for this policy with ${o.groveInterpretation()} and 3/2/2, not evidence for other Grove constraints or round structures.")
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
): List<PlantCard> {
    val pattern = options.grovePattern ?: return defaultGrove
    val concreteCode = GrovePlantCode.generate(pattern, Randomizer.create(options.groveSeed + sample))
    return GrovePlantCode.overrideNames(concreteCode).map { name ->
        requireNotNull(plantManager.getCard(name)) { "Unknown Plant card generated for Grove: $name" }
    }
}

private fun rejectTrainingSeedOverlap(weights:LearnedBuyWeights,o:EvalOptions) {
    val n=weights.provenance.gamesPerPolicy ?: return
    fun overlaps(a0:Long,a1:Long,b0:Long,b1:Long)=a0<=b1 && b0<=a1
    weights.provenance.mechanicalSeedStart?.let { start -> require(!overlaps(o.seed,o.seed+o.games-1,start,start+n-1)) { "Evaluation mechanical seeds overlap recorded training seeds $start..${start+n-1}; choose held-out --seed values." } }
    weights.provenance.strategySeedStart?.let { start -> require(!overlaps(o.strategySeed,o.strategySeed+o.games-1,start,start+n-1)) { "Evaluation strategy seeds overlap recorded training seeds $start..${start+n-1}; choose held-out --strategy-seed values." } }
}

private fun learnedFactory(weights:LearnedBuyWeights):PlayerDecisionFactory=object:PlayerDecisionFactory { override fun create()=LearnedBuy.createDirector(weights); override fun create(strategyRandomizer:StrategyRandomizer)=LearnedBuy.createDirector(weights,strategyRandomizer); override fun create(strategyRandomizer:StrategyRandomizer,reasoningSink:DecisionReasoningSink)=LearnedBuy.createDirector(weights,strategyRandomizer) }
private fun loadCards(plantRegistry:PlantCardRegistry,plantManager:PlantCardManager,wispRegistry:WispCardRegistry,wispManager:WispCardManager,roundRegistry:RoundCardRegistry,roundManager:RoundCardManager){ val root=CardDataFiles.dataDirectory(); plantRegistry.clear(); plantRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST,root),CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST,root)); plantManager.loadCards(plantRegistry); wispRegistry.clear(); wispRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.WISP_LIST,root)); wispManager.loadCards(wispRegistry); roundRegistry.clear(); roundRegistry.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST,root)); roundManager.loadCards(roundRegistry) }

internal data class EvalOptions(
    val games: Int,
    val seed: Long,
    val strategySeed: Long,
    val input: Path,
    val grovePattern: String?,
    val groveSeed: Long,
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
                    argument.startsWith("--input") -> input = Paths.get(value(argument))
                    argument.startsWith("--grove-seed") -> groveSeed = value(argument).toLong()
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
            return EvalOptions(games, seed, strategy, input, grovePattern, groveSeed)
        }

        private fun usage() {
            println("evaluate_buy_policy [N|--games N] [--seed N] [--strategy-seed N] [--input PATH] [--grove CODE|--random-grove] [--grove-seed N]")
            println("  --grove 000100000 keeps Vine_07_01 fixed and resolves all zero slots anew for each matched sample.")
            println("  CONTROL and LEARNED always share the same concrete Grove within a matched sample.")
        }
    }
}
