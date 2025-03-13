package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.player.components.PlayersScoreData

class ReportGameBrief(
    private val reportPlayer: ReportPlayer
) {

    operator fun invoke(result: PlayersScoreData): List<String> = with(result) {
        val lines = mutableListOf<String>()
        with(result) {
            val winner = result.winner ?: return emptyList()
            val scorePlayer = winner.player.score
            lines.add("Winner ${winner.player.id} in $turn, Score=[$scorePlayer]")
        }
        for (data in players) {
            lines.add(reportPlayer(data.player))
        }
        return lines
    }
}
