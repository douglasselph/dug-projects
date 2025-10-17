package dugsolutions.leaf.tool

import java.io.File
import java.nio.file.Paths

/**
 * Simple utility to view test results stored in a file.
 * This is helpful for viewing test output when it's not easily visible in the IDE.
 */
class TestOutputViewer {
    companion object {
        const val TEST_OUTPUT_DIR = "test-output"
    }

    fun viewFileContents(filePath: String) {
        // Try to resolve the file - first as is, then in test-output directory
        val file = resolveFile(filePath)
        
        if (!file.exists()) {
            println("File not found: $filePath")
            println("Searched in current directory and in $TEST_OUTPUT_DIR/")
            return
        }
        
        println("=== Contents of ${file.absolutePath} (${file.length()} bytes) ===")
        println()
        
        try {
            file.bufferedReader().use { reader ->
                reader.lines().forEach { line ->
                    println(line)
                }
            }
            
            println()
            println("=== End of file contents ===")
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Resolve a file path - if it's a full path use it as is,
     * if it's just a filename, check in test-output directory
     */
    private fun resolveFile(filePath: String): File {
        val file = File(filePath)
        
        // If it exists or contains directory separators, return as is
        if (file.exists() || filePath.contains(File.separator)) {
            return file
        }
        
        // Otherwise try to find it in the test-output directory
        val inTestOutputDir = File(TEST_OUTPUT_DIR, filePath)
        
        // If it's not a full filename (no extension), try adding .txt
        if (!inTestOutputDir.exists() && !filePath.contains(".")) {
            val withTxtExt = File(TEST_OUTPUT_DIR, "$filePath.txt")
            if (withTxtExt.exists()) {
                return withTxtExt
            }
        }
        
        return inTestOutputDir
    }
    
    fun createExampleOutput() {
        val outputDir = File(TEST_OUTPUT_DIR)
        if (!outputDir.exists()) {
            outputDir.mkdir()
        }
        
        val outputFile = File(outputDir, "example_output.txt")
        println("Creating example output in: ${outputFile.absolutePath}")
        
        outputFile.bufferedWriter().use { writer ->
            writer.write("=== Example Test Output ===\n")
            writer.write("This is a sample test output file.\n")
            writer.write("It would contain test results from running an integration test.\n\n")
            
            writer.write("Example entries:\n")
            writer.write("1. DrawCardEntry(playerId=1, gameTurn=GameTurn(number=1, phase=DRAW), cardId=Card_123, cardName=Sap Surge)\n")
            writer.write("2. AddToTotalEntry(playerId=1, gameTurn=GameTurn(number=1, phase=PLAY), amount=2)\n")
            writer.write("3. DrawDieEntry(playerId=1, gameTurn=GameTurn(number=1, phase=DRAW), dieSides=6)\n")
            writer.write("4. AcquireDieEntry(playerId=2, gameTurn=GameTurn(number=2, phase=ACQUIRE), dieSides=8)\n\n")
            
            writer.write("=== End of Example Output ===\n")
        }
        
        // Now view the file we just created
        viewFileContents(outputFile.absolutePath)
    }
    
    fun listTestOutputFiles() {
        val outputDir = File(TEST_OUTPUT_DIR)
        if (!outputDir.exists() || !outputDir.isDirectory) {
            println("Test output directory not found: $TEST_OUTPUT_DIR")
            return
        }
        
        val files = outputDir.listFiles()
        if (files == null || files.isEmpty()) {
            println("No files found in $TEST_OUTPUT_DIR")
            return
        }
        
        println("Available test output files:")
        files.sortedBy { it.lastModified() }.forEachIndexed { index, file ->
            val sizeKb = file.length() / 1024
            val lastModified = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(file.lastModified()))
            println("${index + 1}. ${file.name} (${sizeKb}KB, last modified: $lastModified)")
        }
    }
    
    fun showHelpMessage() {
        println("""
            Test Output Viewer - A simple utility to view test output files
            
            Usage:
              1. Run without arguments to see available test output files
              2. Provide a file name to view a file in the test-output directory
                 - Example: ./gradlew viewTestOutput -Pargs="testBase_2D4_2D6_results"
              3. Provide a full path to view any file
                 - Example: ./gradlew viewTestOutput -Pargs="/path/to/file.txt"
              
            The integration tests write output to the $TEST_OUTPUT_DIR directory.
        """.trimIndent())
    }
}

fun main(args: Array<String>) {
    val viewer = TestOutputViewer()
    
    if (args.isEmpty()) {
        viewer.showHelpMessage()
        println("\nAvailable test output files:")
        viewer.listTestOutputFiles()
    } else {
        val filePath = args[0]
        viewer.viewFileContents(filePath)
    }
} 