package dugsolutions.leaf.v35.player.development

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * D7: read-only final-development reporting. One record per observed player/game.
 * The caller identifies the policy and learner; never pool distinct policies implicitly.
 * `won` can be null for an unfinished game. Wins/ties are supplied by the evaluator.
 */
data class PlayerDevelopmentResult(
    val gameId: String,
    val playerId: String,
    val playerCount: Int,
    val policy: String,
    val learner: String,
    val won: Boolean?,
    val finalVp: Int?,
    val strikeWins: Int?,
    val snapshot: PlayerDevelopmentSnapshot
)

/** All reports are derived from immutable snapshots and require no game mutation. */
data class DevelopmentPlantIdentity(val name: String, val currentId: String, val category: String, val cost: Int)

object PlayerDevelopmentReports {
    data class Report(val fileName: String, val tsv: String)

    fun generate(results: List<PlayerDevelopmentResult>, catalog: List<DevelopmentPlantIdentity> = emptyList()): List<Report> {
        validate(results)
        return listOf(
            Report("development-players.tsv", playerRows(results)),
            Report("development-plants.tsv", plantRows(results)),
            Report("development-dice.tsv", dieRows(results)),
            Report("development-card-summary.tsv", cardSummary(results, catalog)),
            Report("development-dice-summary.tsv", dieSummary(results)),
            Report("development-shape-summary.tsv", shapeSummary(results))
        )
    }

    fun write(results: List<PlayerDevelopmentResult>, directory: Path, catalog: List<DevelopmentPlantIdentity> = emptyList()): List<Path> {
        Files.createDirectories(directory)
        return generate(results, catalog).map { report ->
            val target = directory.resolve(report.fileName)
            Files.write(target, report.tsv.toByteArray(StandardCharsets.UTF_8))
            target
        }
    }

    private fun validate(results: List<PlayerDevelopmentResult>) {
        require(results.all { it.playerCount in 2..4 }) { "playerCount must be 2, 3, or 4" }
        require(results.all { it.gameId.isNotBlank() && it.playerId.isNotBlank() && it.policy.isNotBlank() })
        require(results.map { Triple(it.gameId, it.playerId, it.policy) }.distinct().size == results.size) {
            "Duplicate player/game/policy record"
        }
    }

    private fun render(rows: List<List<Any?>>): String = rows.joinToString("\n", postfix = "\n") { row ->
        row.joinToString("\t") { value -> value?.toString()?.replace('\t', ' ')?.replace('\n', ' ') ?: "" }
    }
    private fun fmt(n: Double): String = String.format(Locale.ROOT, "%.4f", n)
    private fun avg(numbers: List<Int>): String = if (numbers.isEmpty()) "" else fmt(numbers.average())
    private fun percent(n: Int, d: Int): String = if (d == 0) "" else fmt(100.0 * n / d)
    private fun outcome(won: Boolean?): String = when (won) { true -> "WIN"; false -> "LOSS"; null -> "UNKNOWN" }
    private fun base(r: PlayerDevelopmentResult) = listOf(r.gameId, r.playerId, r.playerCount, r.policy, r.learner, outcome(r.won))
    private val baseHeaders = listOf("game_id", "player_id", "players", "policy", "learner", "outcome")

    private fun playerRows(results: List<PlayerDevelopmentResult>): String {
        val rows = mutableListOf<List<Any?>>(baseHeaders + listOf(
            "final_vp", "strike_wins", "plants_acquired", "plants_grafted_at_end", "plants_snipped",
            "dice_ownership_records", "dice_owned_at_end", "dice_doomed", "dice_trashed", "upgrades"
        ))
        results.sortedWith(compareBy({ it.gameId }, { it.policy }, { it.playerId })).forEach { r ->
            val s = r.snapshot
            rows += base(r) + listOf(r.finalVp, r.strikeWins, s.plants.size,
                s.plants.count { it.fate == "GRAFTED" }, s.plants.count { it.fate == "SNIPPED" },
                s.dice.size, s.dice.count { it.fate == "OWNED" },
                s.dice.count { it.fate == "DOOM" }, s.dice.count { it.fate == "TRASH" }, s.upgradeCount)
        }
        return render(rows)
    }

    private fun plantRows(results: List<PlayerDevelopmentResult>): String {
        val rows = mutableListOf<List<Any?>>(baseHeaders + listOf(
            "copy_id", "card_name", "current_card_id", "category", "cost", "fate", "side", "x", "y"
        ))
        results.sortedWith(compareBy({ it.gameId }, { it.policy }, { it.playerId })).forEach { r ->
            r.snapshot.plants.sortedWith(compareBy({ it.cardName }, { it.copyId })).forEach { p ->
                rows += base(r) + listOf(p.copyId, p.cardName, p.cardId, p.category, p.cost, p.fate, p.side, p.x, p.y)
            }
        }
        return render(rows)
    }

    private fun dieRows(results: List<PlayerDevelopmentResult>): String {
        val rows = mutableListOf<List<Any?>>(baseHeaders + listOf(
            "asset_id", "acquired_sides", "final_sides", "fate", "upgrades"
        ))
        results.sortedWith(compareBy({ it.gameId }, { it.policy }, { it.playerId })).forEach { r ->
            r.snapshot.dice.sortedBy { it.assetId }.forEach { d ->
                rows += base(r) + listOf(d.assetId, d.acquiredSides, d.finalSides, d.fate, d.upgrades)
            }
        }
        return render(rows)
    }

    private data class Cohort(val players: Int, val policy: String, val learner: String, val outcome: String)
    private fun cohorts(results: List<PlayerDevelopmentResult>): List<Pair<Cohort, List<PlayerDevelopmentResult>>> {
        // ALL means all outcomes, not just wins, and never mixes distinct learner policies.
        val expanded = results.flatMap { r ->
            listOf(Cohort(r.playerCount, r.policy, r.learner, "ALL") to r,
                Cohort(r.playerCount, r.policy, r.learner, outcome(r.won)) to r)
        }
        return expanded.groupBy({ it.first }, { it.second }).toList()
            .sortedWith(compareBy({ it.first.players }, { it.first.policy }, { it.first.learner }, { it.first.outcome }))
    }
    private fun headerCohort() = listOf("players", "policy", "learner", "outcome", "player_games")
    private fun fields(c: Cohort, n: Int) = listOf(c.players, c.policy, c.learner, c.outcome, n)

    private fun cardSummary(results: List<PlayerDevelopmentResult>, inputCatalog: List<DevelopmentPlantIdentity>): String {
        val rows = mutableListOf<List<Any?>>(headerCohort() + listOf(
            "card_name", "current_ids", "category", "cost", "copies_acquired", "players_acquiring",
            "acquired_per_player", "players_acquiring_pct", "grafted_at_end", "snipped", "other_removed"
        ))
        // Include every card observed anywhere in results, even zero purchases for a cohort.
        val catalog = (inputCatalog + results.flatMap { it.snapshot.plants }.map {
            DevelopmentPlantIdentity(it.cardName, it.cardId, it.category, it.cost)
        }).groupBy { it.name }.toSortedMap()
        cohorts(results).forEach { (c, games) ->
            catalog.forEach { (name, definitions) ->
                val byPlayer = games.map { r -> r.snapshot.plants.filter { it.cardName == name } }
                val copies = byPlayer.flatten()
                rows += fields(c, games.size) + listOf(name, definitions.map { it.currentId }.distinct().sorted().joinToString(";"),
                    definitions.first().category, definitions.first().cost, copies.size,
                    byPlayer.count { it.isNotEmpty() }, fmt(copies.size.toDouble() / games.size),
                    percent(byPlayer.count { it.isNotEmpty() }, games.size),
                    copies.count { it.fate == "GRAFTED" }, copies.count { it.fate == "SNIPPED" },
                    copies.count { it.fate != "GRAFTED" && it.fate != "SNIPPED" })
            }
        }
        return render(rows)
    }

    private fun dieSummary(results: List<PlayerDevelopmentResult>): String {
        val rows = mutableListOf<List<Any?>>(headerCohort() + listOf(
            "die_sides", "acquired_at_size", "owned_at_end", "doomed_at_size", "trashed_at_size",
            "other_removed_at_size", "upgrades_to_size", "total_upgrades", "avg_owned_per_player"
        ))
        val sizes = (listOf(4, 6, 8, 10, 12, 20) + results.flatMap { it.snapshot.dice }.flatMap { listOf(it.acquiredSides, it.finalSides) }).distinct().sorted()
        cohorts(results).forEach { (c, games) ->
            val dice = games.flatMap { it.snapshot.dice }
            sizes.forEach { sides ->
                val owned = dice.count { it.finalSides == sides && it.fate == "OWNED" }
                rows += fields(c, games.size) + listOf(sides,
                    dice.count { it.acquiredSides == sides }, owned,
                    dice.count { it.finalSides == sides && it.fate == "DOOM" },
                    dice.count { it.finalSides == sides && it.fate == "TRASH" },
                    dice.count { it.finalSides == sides && it.fate != "OWNED" && it.fate != "DOOM" && it.fate != "TRASH" },
                    // Upgrade history only stores a count and the latest die size: do not mislabel intermediary sizes.
                    "", dice.sumOf { it.upgrades }, fmt(owned.toDouble() / games.size))
            }
        }
        return render(rows)
    }

    private fun shapeSummary(results: List<PlayerDevelopmentResult>): String {
        val rows = mutableListOf<List<Any?>>(headerCohort() + listOf(
            "avg_grafted_roots", "avg_grafted_vines", "avg_grafted_flowers", "avg_grafted_total",
            "avg_acquired_plants", "avg_snipped", "avg_owned_dice", "avg_upgrades", "avg_final_vp", "avg_strike_wins"
        ))
        cohorts(results).forEach { (c, games) ->
            fun count(category: String): List<Int> = games.map { r ->
                r.snapshot.plants.count { it.fate == "GRAFTED" && it.category.equals(category, ignoreCase = true) }
            }
            rows += fields(c, games.size) + listOf(avg(count("ROOT")), avg(count("VINE")), avg(count("FLOWER")),
                avg(games.map { r -> r.snapshot.plants.count { it.fate == "GRAFTED" } }),
                avg(games.map { it.snapshot.plants.size }),
                avg(games.map { r -> r.snapshot.plants.count { it.fate == "SNIPPED" } }),
                avg(games.map { r -> r.snapshot.dice.count { it.fate == "OWNED" } }),
                avg(games.map { it.snapshot.upgradeCount }),
                avg(games.mapNotNull { it.finalVp }), avg(games.mapNotNull { it.strikeWins }))
        }
        return render(rows)
    }
}
