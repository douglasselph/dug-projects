package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.domain.GameSummary
import dugsolutions.leaf.chronicle.domain.TestOutputFile

class WriteGameSummaries(
    private val testOutputFile: TestOutputFile,
    private val generateGameSummaries: GenerateGameSummaries,
    private val reportGameSummaries: ReportGameSummaries,
    private val writeToFile: WriteToFile
) {

    operator fun invoke(testDir: String, testName: String, summaries: List<GameSummary>) {
        val file = testOutputFile(testDir, testName)
        val gameSummaries = generateGameSummaries(summaries)
        val reportLines = reportGameSummaries(gameSummaries)
        val numGames = summaries.size
        writeToFile(file.absolutePath, reportLines, "Summary Report for $numGames games.")
    }
}