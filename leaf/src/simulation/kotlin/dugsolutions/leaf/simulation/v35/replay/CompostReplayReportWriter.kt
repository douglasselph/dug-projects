package dugsolutions.leaf.simulation.v35.replay

import dugsolutions.leaf.v35.chronicle.ChronicleTextRenderer
import dugsolutions.leaf.v35.game.replay.CultivationReplayDecision
import java.nio.file.Files
import java.nio.file.Path

/** Stores both full observed game narratives and machine-readable decision/outcome tables. */
object CompostReplayReportWriter {
    fun write(pair: CompostPairedOutcome, directory: Path) {
        Files.createDirectories(directory)
        writeGame(pair.original, directory.resolve("original"))
        writeGame(pair.alternative, directory.resolve("alternative"))
        val d = pair.deltaForPlayer
        val text = buildString {
            appendLine("metric\talternative_minus_original")
            appendLine("total_vp\t${d.totalVp}")
            appendLine("battle_strike_vp\t${d.battleVp}")
            appendLine("plant_vp\t${d.plantVp}")
            appendLine("unplayed_wisp_vp\t${d.wispVp}")
            appendLine("final_dice_count\t${d.diceCount}")
            appendLine("final_dice_power\t${d.dicePower}")
            appendLine("final_plant_count\t${d.plants}")
            appendLine("wounds_taken\t${d.wounds}")
            appendLine("win_share\t${d.winShare}")
        }
        Files.writeString(directory.resolve("paired-delta.tsv"), text)
        Files.writeString(directory.resolve("fork.txt"), pair.fork.toString() + "\n")
    }

    private fun writeGame(game: RecordedReplayGame, directory: Path) {
        Files.createDirectories(directory)
        Files.writeString(directory.resolve("chronicle.txt"), ChronicleTextRenderer.render(game.chronicle, detail = true))
        Files.writeString(directory.resolve("summary.txt"), game.summary.toString() + "\n")
        Files.writeString(directory.resolve("decisions.tsv"), decisionsTsv(game.decisions))
    }

    private fun decisionsTsv(decisions: List<CultivationReplayDecision>): String = buildString {
        appendLine("round\tplayer\tconsultation\tmains_remaining\tcard\tfirst_effect\tsecond_effect\tlegal\tpolicy\tactual\tplant\tintervened")
        for (d in decisions) {
            appendLine(listOf(
                d.roundNumber, d.playerId, d.consultationIndex, d.mainActionsRemaining,
                d.roundCardName, d.firstEffect, d.secondEffect,
                d.legal.joinToString(";"), d.policyChoice, d.actualChoice,
                d.plantName ?: "", d.intervened
            ).joinToString("\t") { it.toString().replace('\t', ' ').replace('\n', ' ') })
        }
    }
}
