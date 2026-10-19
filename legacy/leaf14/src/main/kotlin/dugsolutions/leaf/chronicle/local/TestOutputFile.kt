package dugsolutions.leaf.chronicle.local

import java.io.File

class TestOutputFile {
    companion object {
        private const val OUTPUT_DIR = "output"
    }

    operator fun invoke(testDir: String, testName: String): File {
        val outputDir = File("$OUTPUT_DIR/$testDir")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        // Create the output file path
        return File(outputDir, "$testName.txt")
    }
}
