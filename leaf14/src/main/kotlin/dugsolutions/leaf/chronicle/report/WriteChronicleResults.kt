package dugsolutions.leaf.chronicle.report

import dugsolutions.leaf.chronicle.GameChronicle
import dugsolutions.leaf.chronicle.domain.ChronicleEntry

class WriteChronicleResults(
    private val chronicle: GameChronicle,
    private val writeToFile: WriteToFile
) {
    private var lastFilename: String? = null
    private var entriesWritten: Int = 0

    /**
     * Updates a file with new entries that haven't been written yet.
     * If this is the first call or if the filename has changed, it performs a full write.
     * Otherwise, it appends only the new entries.
     * @param filename The path to the file where entries will be written or updated
     * @param testName Optional test name text to include at the top of the file
     */
    fun update(filename: String, testName: String? = null) {
        val entries = chronicle.getEntries()
        
        // If filename changed or this is the first call, do a full write
        if (filename != lastFilename || entriesWritten == 0) {
            write(filename, header(testName))
            return
        }
        
        // If there are new entries, append them
        if (entries.size > entriesWritten) {
            val lines = convert(entries, startIndex = entriesWritten)
            writeToFile(filename, lines)
            entriesWritten = entries.size
        }
    }

    fun writeTo(filename: String, lines: List<String>) {
        writeToFile(filename, lines)
    }

    fun clear() {
        lastFilename = null
        entriesWritten = 0
    }

    /**
     * Writes all chronicle entries to the specified file, overwriting any existing content.
     * @param filename The path to the file where entries will be written
     * @param testName Optional test name text to include at the top of the file
     */
    private fun write(filename: String, headerText: String? = null) {
        val entries = chronicle.getEntries()
        val lines = convert(entries, startIndex = 0)
        writeToFile(filename, lines, headerText, false)
        lastFilename = filename
        entriesWritten = entries.size
    }
    
    private fun convert(entries: List<ChronicleEntry>, startIndex: Int): List<String> {
        val lines = mutableListOf<String>()
        for (i in startIndex until entries.size) {
            lines.add("  ${entries[i]}")
        }
        return lines
    }

    private fun header(testName: String?): String? {
        return testName?.let { "=== Test Results: $it ===" }
    }
}