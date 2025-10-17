package dugsolutions.leaf.chronicle.report

import java.io.File
import java.io.BufferedWriter

class WriteToFile {
    /**
     * Writes or appends content to a file
     *
     * @param filename The path to the file where content will be written
     * @param lines The content to write as a list of strings
     * @param header Optional header text (only used when not appending)
     * @param append Whether to append to the file or overwrite it
     */
    operator fun invoke(
        filename: String,
        lines: List<String>,
        header: String? = null,
        append: Boolean = header == null
    ) {
        val file = File(filename)

        // Ensure parent directory exists
        file.parentFile?.mkdirs()

        // Create FileWriter in appropriate mode, then wrap with BufferedWriter
        BufferedWriter(java.io.FileWriter(file, append)).use { writer ->
            // If header is provided, write it with a newline
            header?.let {
                writer.write(it)
                writer.newLine()
            }

            // Write each line, ensuring it ends with a newline
            for (line in lines) {
                writer.write(line)
                // Only add a newline if the line doesn't already end with one
                if (!line.endsWith("\n")) {
                    writer.newLine()
                }
            }
        }
    }

} 