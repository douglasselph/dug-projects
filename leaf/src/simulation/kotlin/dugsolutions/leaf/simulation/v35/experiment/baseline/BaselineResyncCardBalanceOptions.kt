package dugsolutions.leaf.simulation.v35.experiment.baseline

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/** Parsed command-line options. Defaults and accepted arguments preserve the legacy entry point. */
data class BaselineResyncCardBalanceOptions(
    val start: Int,
    val games: Int,
    val players: Int,
    val baseSeed: Long,
    val strategySeed: Long,
    val groveSeed: Long,
    val plantFile: Path,
    val roundFile: Path,
    val output: File
) {
    init {
        require(start >= 0 && games > 0 && players in 2..4)
    }

    companion object {
        fun parse(args: Array<String>): BaselineResyncCardBalanceOptions {
            val options = args.associate { arg ->
                require(arg.startsWith("--") && '=' in arg) { "Expected --key=value: $arg" }
                val parts = arg.removePrefix("--").split('=', limit = 2)
                parts[0] to parts[1]
            }
            fun string(key: String, default: String): String = options[key] ?: default
            return BaselineResyncCardBalanceOptions(
                start = string("start", "0").toInt(),
                games = string("games", "100").toInt(),
                players = string("players", "4").toInt(),
                baseSeed = string("seed", "120000").toLong(),
                strategySeed = string("strategy-seed", "220000").toLong(),
                groveSeed = string("grove-seed", "320000").toLong(),
                plantFile = Paths.get(string("plant-overrides", "data/research/resync/resync-current.csv")),
                roundFile = Paths.get(string("round-overrides", "data/research/resync/round-resync-current.csv")),
                output = File(string("output", "build/reports/baseline-resync-cards.tsv"))
            )
        }
    }
}
