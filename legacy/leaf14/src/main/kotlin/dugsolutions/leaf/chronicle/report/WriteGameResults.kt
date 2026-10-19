package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.local.TestOutputFile

class WriteGameResults(
    private val writeChronicleResults: WriteChronicleResults,
    private val reportGameAnalysis: ReportGameAnalysis,
    private val generateGameSummary: GenerateGameSummary,
    private val reportGameSummary: ReportGameSummary,
    private val testOutputFile: TestOutputFile
) {


    fun update(testDir: String, testName: String) {
        val outputFile = testOutputFile(testDir, testName)
        writeChronicleResults.update(outputFile.absolutePath, testName)
    }

    fun finish(testDir: String, testName: String) {
        val outputFile = testOutputFile(testDir, testName)
        val lines = reportGameAnalysis()
        val summary = generateGameSummary()
        val report = reportGameSummary(summary)

        // Use the WriteResults class to write the chronicle entries to file
        writeChronicleResults.writeTo(outputFile.absolutePath, lines)
        writeChronicleResults.writeTo(outputFile.absolutePath, report)
        writeChronicleResults.clear()
    }

}
