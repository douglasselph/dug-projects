package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * D6: deterministic, read-only aggregation of D3 same-state comparisons.
 *
 * Each event is ONE comparison. Grouping by player count does not mean events are
 * statistically independent; reports describe observed decisions, not win impact.
 * The caller must supply homogeneous policy/learner/state-origin cohorts separately.
 */
object BuyDisagreementReports {
    data class Report(val fileName: String, val tsv: String)

    fun generate(events: List<BuyDecisionEvent>): List<Report> = listOf(
        Report("overall-disagreement.tsv", overall(events)),
        Report("card-disagreement.tsv", cards(events)),
        Report("purchase-substitutions.tsv", substitutions(events))
    )

    /** Write complete UTF-8 TSV files; deliberately not wired into evaluation until D9. */
    fun write(events: List<BuyDecisionEvent>, directory: Path): List<Path> {
        Files.createDirectories(directory)
        return generate(events).map { report ->
            val target = directory.resolve(report.fileName)
            Files.write(target, report.tsv.toByteArray(StandardCharsets.UTF_8))
            target
        }
    }

    private fun overall(events: List<BuyDecisionEvent>): String {
        val rows = mutableListOf(listOf(
            "players", "round", "buy_position", "decisions", "same_item", "different_item",
            "expert_buys_baseline_done", "baseline_buys_expert_done", "both_done",
            "disagreements", "disagreement_pct"
        ))
        // An ALL row and separate round/position rows make the denominator explicit.
        for (players in events.map { it.playerCount }.distinct().sorted()) {
            val cohort = events.filter { it.playerCount == players }
            fun emit(round: String, pos: String, group: List<BuyDecisionEvent>) {
                val counts = BuyDisagreementKind.entries.associateWith { kind -> group.count { it.kind == kind } }
                val disagreements = group.count { it.disagrees }
                rows += listOf(players.toString(), round, pos, group.size.toString(),
                    counts.getValue(BuyDisagreementKind.SAME_ITEM).toString(),
                    counts.getValue(BuyDisagreementKind.DIFFERENT_ITEM).toString(),
                    counts.getValue(BuyDisagreementKind.EXPERT_BUYS_BASELINE_DONE).toString(),
                    counts.getValue(BuyDisagreementKind.BASELINE_BUYS_EXPERT_DONE).toString(),
                    counts.getValue(BuyDisagreementKind.BOTH_DONE).toString(),
                    disagreements.toString(), percent(disagreements, group.size))
            }
            emit("ALL", "ALL", cohort)
            cohort.groupBy { it.roundNumber }.toSortedMap().forEach { (round, group) ->
                emit(round.toString(), "ALL", group)
            }
            cohort.groupBy { it.purchasesMadeThisBuy }.toSortedMap().forEach { (position, group) ->
                emit("ALL", position.toString(), group)
            }
        }
        return render(rows)
    }

    private fun cards(events: List<BuyDecisionEvent>): String {
        val rows = mutableListOf(listOf(
            "players", "card_name", "current_ids", "decisions", "both", "baseline_only",
            "expert_only", "neither", "baseline_recommendations", "expert_recommendations",
            "either_recommends", "disagreement_when_either_pct", "expert_rejection_given_baseline_pct",
            "expert_only_given_expert_pct"
        ))
        for (players in events.map { it.playerCount }.distinct().sorted()) {
            val cohort = events.filter { it.playerCount == players }
            // Include every Plant observed in market, even if NEVER recommended.
            val cards = linkedMapOf<String, MutableSet<String>>()
            fun add(selection: BuySelection) {
                if (selection is BuySelection.Plant) cards.getOrPut(selection.title) { sortedSetOf() }.add(selection.currentId)
            }
            cohort.forEach { event ->
                add(event.baseline); add(event.expert)
                event.marketOptions.forEach { add(it.selection) }
                event.legalOptions.forEach { add(it.selection) }
            }
            for ((name, ids) in cards.toSortedMap()) {
                var counts = NamedCardChoiceCounts()
                for (event in cohort) counts = counts.observe(
                    BuyRecommendationComparison(event.baseline, event.expert, event.kind), name
                )
                rows += listOf(players.toString(), name, ids.sorted().joinToString(";"),
                    counts.total.toString(), counts.both.toString(), counts.baselineOnly.toString(),
                    counts.expertOnly.toString(), counts.neither.toString(),
                    counts.baselineRecommendations.toString(), counts.expertRecommendations.toString(),
                    counts.either.toString(), ratioPct(counts.disagreementWhenEither),
                    ratioPct(counts.expertRejectionGivenBaseline), ratioPct(counts.expertOnlyGivenExpert))
            }
        }
        return render(rows)
    }

    private fun substitutions(events: List<BuyDecisionEvent>): String {
        val rows = mutableListOf(listOf(
            "players", "baseline_choice", "expert_choice", "decisions", "of_all_disagreements_pct"
        ))
        for (players in events.map { it.playerCount }.distinct().sorted()) {
            val disagreements = events.filter { it.playerCount == players && it.disagrees }
            val pairs = disagreements.groupingBy { selectionName(it.baseline) to selectionName(it.expert) }.eachCount()
            pairs.toList().sortedWith(compareBy<Pair<Pair<String, String>, Int>> { it.first.first }
                .thenBy { it.first.second }).forEach { (pair, count) ->
                rows += listOf(players.toString(), pair.first, pair.second,
                    count.toString(), percent(count, disagreements.size))
            }
        }
        return render(rows)
    }

    private fun selectionName(selection: BuySelection): String = when (selection) {
        BuySelection.Done -> "DONE"
        is BuySelection.Plant -> "Plant: ${selection.title}"
        is BuySelection.Die -> "Die: D${selection.sides}"
    }

    private fun percent(n: Int, d: Int): String = if (d == 0) "" else
        "%.4f".format(java.util.Locale.ROOT, n.toDouble() * 100.0 / d)

    private fun ratioPct(ratio: Double?): String = if (ratio == null) "" else
        "%.4f".format(java.util.Locale.ROOT, ratio * 100.0)

    private fun render(rows: List<List<String>>): String = rows.joinToString("\n", postfix = "\n") { row ->
        row.joinToString("\t") { field -> field.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ') }
    }
}
