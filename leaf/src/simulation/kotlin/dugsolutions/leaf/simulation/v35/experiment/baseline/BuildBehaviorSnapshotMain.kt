package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.di.appModules
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.BattleStructure
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.GameRunner
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.di.GameFactory
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import org.koin.dsl.koinApplication
import java.io.File
import java.security.MessageDigest

/**
 * Observation-only Build decisions for the current Human Baseline source revision.
 * No candidate is rescored; the coordinator's actual legal offers and choice,
 * score explanations, and physical resource use are read after each seeded game.
 */
internal object BuildBehaviorSnapshot {
    const val DECISION_HEADER =
        "game\tsequence\tround\tplayer\tround_card\tfirst_effect\tsecond_effect\tlegal_main_actions\tlegal_plants\tselected_main\tselected_plant\treference_main\treference_plant\tfirst_executable\tsecond_executable"
    const val SUPPORT_HEADER =
        "game\tsequence\tround\tplayer\tmains_remaining\tlegal_supports\tselected_support\treference_support"
    const val SCORED_HEADER =
        "game\tsequence\tround\tplayer\tselected_choice\tselected_score\tcandidate\tcandidate_base\tcandidate_score\tcandidate_selected\tadjustments"
    const val RESOURCES_HEADER = "game\tsequence\tround\tplayer\tresource\taction\tdetail"
    const val ROUND_LINEAGE_HEADER =
        "game\tplayer\tresource\tround_card\tslot\tacquired_phase\tused\tuse_phase\tuse_kind\tresult"
    const val INVENTORY_HEADER = "plant_id\tcard_title\tprinted_effect\tscorer_effect\teffect_route\tcultivation_base\tbattle_base"
    const val END_HEADER = "game\tplayer\twater_held\tsunlight_held\tmulch_held\tpending_mulch"

    data class Evidence(
        val decisions: List<String>,
        val supports: List<String>,
        val scoredCandidates: List<String>,
        val resources: List<String>,
        val roundLineages: List<String>,
        val finalInventory: List<String>
    )

    /** Fail on missing or ambiguous identities, not merely on a count of scorer objects. */
    fun plantInventory(cards: List<PlantCard>, scorers: HumanBaselineCardScorerRegistry): List<String> {
        require(cards.size == 36 && cards.map { it.name }.distinct().size == 36) {
            "Expected 36 distinct official Plant definitions; found ${cards.size}"
        }
        return cards.sortedBy { it.name }.map { card ->
            val scorer = requireNotNull(scorers.findByName(card.name)) {
                "Official Plant has no registered Human Baseline scorer: ${card.name}"
            }
            val routed = scorers.forEffect(card.effect).any { card.name in it.cardNames }
            // Capture missing effect-based routes as findings, not a blocker:
            // repairing the scorer registry here could change the old policy
            // whose decisions this step is intended to snapshot unchanged.
            fields(card.name, card.title, card.effect, scorer.effect,
                if (routed) "YES" else "NO", scorer.cultivationPlayBase, scorer.battlePlayBase)
        }
    }

    fun collect(game: Game, index: Int): Evidence {
        require(game.config.battleStructure == BattleStructure.THREE_ENGAGEMENTS) {
            "Snapshot requires current Three Engagements Battle rules"
        }
        val decisions = mutableListOf<String>()
        val supports = mutableListOf<String>()
        val scored = mutableListOf<String>()
        val resources = mutableListOf<String>()
        var round = 0
        game.chronicle.entries.forEach { entry ->
            if (entry is GameEntry.RoundRevealed) round = entry.roundNumber
            when (entry) {
                is GameEntry.RoundEffectChoice -> if (entry.phase == ChroniclePhase.CULTIVATION) {
                    val mains = entry.legalMainActions.distinct()
                    require(entry.selectedMainAction == null || entry.selectedMainAction in mains) {
                        "Recorded Build choice was not among legal Main choices"
                    }
                    if (entry.selectedMainAction == MainActionKind.ACTIVATE_PLANT) {
                        require(entry.selectedPlantCardName in entry.legalPlantCardNames) {
                            "Selected Plant is not in recorded legal Plant offers"
                        }
                    }
                    decisions += fields(index, entry.sequence, round, entry.playerId.value,
                        entry.roundCardName, entry.firstEffect, entry.secondEffect,
                        mains.joinToString("|"), entry.legalPlantCardNames.joinToString("|"),
                        entry.selectedMainAction ?: "SUPPORT_OR_DONE", entry.selectedPlantCardName ?: "",
                        entry.humanBaselineSelectedMainAction ?: "", entry.humanBaselineSelectedPlantCardName ?: "",
                        entry.firstExecutable, entry.secondExecutable)
                }
                is GameEntry.CultivationSupportDecision -> supports += fields(
                    index, entry.sequence, round, entry.playerId.value, entry.mainActionsRemaining,
                    entry.legalActionIds.joinToString("|"), entry.selectedActionId ?: "PASS",
                    entry.referenceActionId ?: "PASS"
                )
                is GameEntry.DecisionReasoning -> {
                    // These scored candidates are *not* the entire engine-legal set:
                    // legacy willingness gates may remove a legal candidate first.
                    if (entry.choiceLabel.startsWith("Main(") || entry.choiceLabel.startsWith("FinalMain(")) {
                        entry.alternatives.forEach { alternative ->
                            scored += fields(index, entry.sequence, round, entry.playerId.value,
                                entry.choiceLabel, entry.total, alternative.choiceLabel,
                                alternative.baseScore, alternative.total, alternative.selected,
                                alternative.adjustments.joinToString("|") { "${it.amount}:${it.reason}" })
                        }
                    }
                }
                is GameEntry.EffectResolved -> if (entry.effect == GameEffect.GAIN_WATER_TOKEN) {
                    // The resource handler transfers the token before recording EffectResolved.
                    resources += fields(index, entry.sequence, round, entry.playerId.value,
                        "WATER", "GAIN", "${entry.sourceKind}:${entry.sourceName}")
                }
                is GameEntry.SunlightTokenChanged -> resources += fields(
                    index, entry.sequence, round, entry.playerId.value, "SUNLIGHT", entry.change,
                    "physical_token_transfer")
                is GameEntry.MulchStored -> resources += fields(
                    index, entry.sequence, round, entry.playerId.value, "MULCH", "GAIN",
                    "${entry.sides}:${entry.value};fromDiscard=${entry.fromDiscard}")
                is GameEntry.SupportAction -> when (entry.action) {
                    SupportActionKind.WATER_REFRESH, SupportActionKind.WATER_REROLL ->
                        resources += fields(index, entry.sequence, round, entry.playerId.value,
                            "WATER", "SPENT", entry.action)
                    SupportActionKind.MULCH ->
                        resources += fields(index, entry.sequence, round, entry.playerId.value,
                            "MULCH", "SPENT", entry.phase)
                    // SUNLIGHT is counted from SunlightTokenChanged.SPENT, not twice.
                    else -> Unit
                }
                else -> Unit
            }
        }
        val resourceEffects = setOf(GameEffect.GAIN_WATER_TOKEN, GameEffect.GAIN_SUNLIGHT_TOKEN,
            GameEffect.MULCH_DIE_FROM_HAND, GameEffect.MULCH_DIE_FROM_DISCARD,
            GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD)
        val lineage = game.assetProvenance.roundEffectConsequences
            .filter { it.effect in resourceEffects }
            .map { record -> fields(index, record.playerId.value, record.effect,
                record.roundCardName, record.roundSlot, record.acquiredPhase,
                record.spentOrUsed, record.usePhase ?: "", record.useKind ?: "",
                record.fundedMainAction ?: record.resultDieValue ?: "") }
        val ending = game.players.map { player -> fields(index, player.id.value,
            player.tokens.waterCount, player.tokens.sunlightCount,
            player.tokens.mulchCount, player.tokens.pendingMulchCount) }
        return Evidence(decisions, supports, scored, resources, lineage, ending)
    }

    private fun fields(vararg values: Any): String = values.joinToString("\t") {
        it.toString().replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
    }
}

/** Fixed-seed current-policy capture; historical captures remain immutable comparison references. */
fun main(args: Array<String>) {
    val arguments = args.associate { arg ->
        require(arg.startsWith("--") && '=' in arg) { "Expected --key=value: $arg" }
        val pair = arg.removePrefix("--").split('=', limit = 2)
        pair[0] to pair[1]
    }
    require(arguments.keys.all { it in setOf("games", "players", "base-seed", "strategy-seed", "output") }) {
        "Unknown option; use --games, --players, --base-seed, --strategy-seed, --output"
    }
    val games = (arguments["games"] ?: "4").toInt().also { require(it in 1..100) }
    val players = (arguments["players"] ?: "2").toInt().also { require(it in 2..4) }
    val seed = (arguments["base-seed"] ?: "163000").toLong()
    val strategySeed = (arguments["strategy-seed"] ?: "263000").toLong()
    val output = File(arguments["output"] ?: "output/experiments/human-baseline-build/snapshot")
    require(output.isDirectory || output.mkdirs()) { "Unable to create ${output.path}" }
    val app = koinApplication { modules(appModules) }
    try {
        val koin = app.koin
        loadCatalogs(koin.get<PlantCardRegistry>(), koin.get<PlantCardManager>(),
            koin.get<WispCardRegistry>(), koin.get<WispCardManager>(),
            koin.get<RoundCardRegistry>(), koin.get<RoundCardManager>())
        val cards = koin.get<PlantCardManager>().getAllCards().cards
        val inventory = BuildBehaviorSnapshot.plantInventory(cards, HumanBaselineCardScorerRegistry())
        val missingEffectRoutes = inventory.filter { "\tNO\t" in it }
        val grove = FirstGameDefault.PLANT_NAMES.map { name ->
            requireNotNull(koin.get<PlantCardManager>().getCard(name)) { "No Grove card: $name" }
        }
        val all = (1..games).map { gameIndex ->
            val game = koin.get<GameFactory>().invoke(GameConfig(
                selectedPlantCards = grove,
                playerDecisionFactories = List(players) { PlayerDecisionFactory.humanBaseline() },
                roundSetup = GameRoundSetup.standard(),
                battleStructure = BattleStructure.THREE_ENGAGEMENTS,
                seed = seed + gameIndex - 1, strategySeed = strategySeed + gameIndex - 1,
                recordDecisionReasoning = true
            ))
            koin.get<GameRunner>().run(game)
            BuildBehaviorSnapshot.collect(game, gameIndex)
        }
        fun write(name: String, header: String, content: List<String>) {
            File(output, name).writeText((listOf(header) + content).joinToString("\n", postfix = "\n"))
        }
        write("official-plant-scorer-inventory.tsv", BuildBehaviorSnapshot.INVENTORY_HEADER, inventory)
        write("build-choices.tsv", BuildBehaviorSnapshot.DECISION_HEADER, all.flatMap { it.decisions })
        write("support-choices.tsv", BuildBehaviorSnapshot.SUPPORT_HEADER, all.flatMap { it.supports })
        write("scored-main-candidates.tsv", BuildBehaviorSnapshot.SCORED_HEADER, all.flatMap { it.scoredCandidates })
        write("resource-events.tsv", BuildBehaviorSnapshot.RESOURCES_HEADER, all.flatMap { it.resources })
        write("round-resource-lineages.tsv", BuildBehaviorSnapshot.ROUND_LINEAGE_HEADER, all.flatMap { it.roundLineages })
        write("end-inventory.tsv", BuildBehaviorSnapshot.END_HEADER, all.flatMap { it.finalInventory })
        fun sha256(file: File): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
            return bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
        val dataPath = CardDataFiles.dataDirectory()
        val catalogFiles = listOf(CardDataFiles.ROOT_CARD_LIST, CardDataFiles.VF_CARD_LIST,
            CardDataFiles.ROUND_CARD_LIST, CardDataFiles.WISP_LIST)
        val manifest = buildString {
            appendLine("status=OBSERVED_NOT_CERTIFIED")
            appendLine("rules=THREE_ENGAGEMENTS")
            appendLine("round_setup=STANDARD")
            appendLine("grove=${grove.joinToString(",") { it.name }}")
            appendLine("policy=Human Baseline (normal Round Compost dice-lifetime scoring; neutral resource personality)")
            appendLine("games=$games")
            appendLine("players=$players")
            appendLine("base_seed=$seed")
            appendLine("strategy_seed=$strategySeed")
            appendLine("build_decision_events=${all.sumOf { it.decisions.size }}")
            appendLine("round_resource_lineages=${all.sumOf { it.roundLineages.size }}")
            appendLine("unrouted_scorer_effects=${missingEffectRoutes.size}")
            missingEffectRoutes.forEach { appendLine("unrouted=${it.substringBefore('\t')}") }
            catalogFiles.forEach { name ->
                appendLine("catalog_sha256.$name=${sha256(File(CardDataFiles.dataPath(name, dataPath)))}")
            }
            val sourceFiles = listOf(
                "src/main/kotlin/dugsolutions/leaf/v35/game/round/cultivation/CultivationBuildCoordinator.kt",
                "src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/cultivation/HumanBaselineCultivationStrategy.kt",
                "src/main/kotlin/dugsolutions/leaf/v35/player/decision/baseline/card/HumanBaselineCardScorerRegistry.kt",
                "src/main/kotlin/dugsolutions/leaf/v35/effect/GameEffectConverter.kt",
                "src/simulation/kotlin/dugsolutions/leaf/simulation/v35/experiment/baseline/BuildBehaviorSnapshotMain.kt"
            )
            sourceFiles.forEach { name ->
                appendLine("source_sha256.$name=${sha256(File(name))}")
            }
            appendLine("note=Build choices are engine-legal options, including Plant identities. Score alternatives may be narrower due to willingness gates.")
            appendLine("note=Water GAIN is basic GAIN_WATER_TOKEN; compound Water effects are not included in that counter.")
            appendLine("note=The round-resource lineage ledger records only Round-sourced assets; physical events include all sources.")
            appendLine("note=Capture only; no evaluator tuning, learned policy or balance assessment is performed.")
        }
        File(output, "manifest.txt").writeText(manifest)
        println("Build behavior snapshot: ${output.absolutePath}")
        println("Official cards=${inventory.size}; Build decisions=${all.sumOf { it.decisions.size }}; Round-resource lineages=${all.sumOf { it.roundLineages.size }}; missing effect-based scorer routes=${missingEffectRoutes.size}")
    } finally {
        app.close()
    }
}
