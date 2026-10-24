package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.orchestration.ExperimentChunk
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.util.Properties

/** Validated, crash-recoverable publication of a single deterministic chunk. */
class BaselineChunkCheckpoint(
    private val directory: File,
    private val fingerprint: String,
    private val rowsPerGame: Int,
    private val header: String = BaselineResyncCardBalanceSession.HEADER
) {
    init {
        require(rowsPerGame > 0)
        require(directory.isDirectory || directory.mkdirs()) { "Cannot create $directory" }
    }

    fun stem(chunk: ExperimentChunk): String =
        "sample-%06d-%06d".format(chunk.firstSample, chunk.endSampleExclusive - 1)

    fun tsv(chunk: ExperimentChunk) = File(directory, "${stem(chunk)}.tsv")
    fun temporary(chunk: ExperimentChunk) = File(directory, "${stem(chunk)}.tsv.tmp")
    fun marker(chunk: ExperimentChunk) = File(directory, "${stem(chunk)}.complete")
    fun log(chunk: ExperimentChunk) = File(directory, "${stem(chunk)}.log")

    fun isComplete(chunk: ExperimentChunk): Boolean {
        val data = tsv(chunk)
        val done = marker(chunk)
        if (!done.isFile || !data.isFile) return false
        return try {
            val props = Properties().apply { done.inputStream().use(::load) }
            props.getProperty("version") == "1" &&
                props.getProperty("fingerprint") == fingerprint &&
                props.getProperty("firstSample") == chunk.firstSample.toString() &&
                props.getProperty("games") == chunk.games.toString() &&
                props.getProperty("sha256") == sha256(data) &&
                validTsv(data, chunk)
        } catch (_: Exception) {
            false
        }
    }

    /** Stream-based verification bounds temporary heap usage for large chunks. */
    fun validTsv(file: File, chunk: ExperimentChunk): Boolean {
        if (!file.isFile) return false
        return try {
            file.bufferedReader().use { reader ->
                if (reader.readLine() != header) return false
                val columns = header.split('\t').size
                var rows = 0
                var sample = chunk.firstSample
                var forSample = 0
                while (true) {
                    val line = reader.readLine() ?: break
                    val fields = line.split('\t')
                    if (fields.size != columns || fields[0].toIntOrNull() != sample) return false
                    rows++
                    forSample++
                    if (forSample == rowsPerGame) {
                        sample++
                        forSample = 0
                    }
                }
                rows == chunk.games * rowsPerGame &&
                    sample == chunk.endSampleExclusive && forSample == 0
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Complete marker is published LAST, after validating and publishing the TSV. */
    fun commit(chunk: ExperimentChunk) {
        val temp = temporary(chunk)
        require(validTsv(temp, chunk)) { "Incomplete or malformed chunk output: $temp" }
        val data = tsv(chunk)
        atomicMove(temp, data)
        val properties = Properties().apply {
            setProperty("version", "1")
            setProperty("fingerprint", fingerprint)
            setProperty("firstSample", chunk.firstSample.toString())
            setProperty("games", chunk.games.toString())
            setProperty("sha256", sha256(data))
        }
        val pending = File(directory, "${stem(chunk)}.complete.tmp")
        pending.outputStream().use { properties.store(it, "Committed ${Instant.now()}") }
        atomicMove(pending, marker(chunk))
    }

    fun logLine(chunk: ExperimentChunk, message: String) {
        log(chunk).appendText("${Instant.now()} $message\n")
    }

    companion object {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(65536)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        fun fingerprint(options: BaselineResyncCardBalanceOptions, chunkSize: Int, sourceIdentity: String): String {
            val signature = listOf(
                "checkpoint-v1", if (sourceIdentity == "unversioned") localSourceHash() else sourceIdentity, options.start, options.games, chunkSize,
                options.players, options.baseSeed, options.strategySeed, options.groveSeed,
                sha256(options.plantFile.toFile()), sha256(options.roundFile.toFile())
            ).joinToString("\n")
            val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }

        private fun localSourceHash(): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val dirs = listOf(File("src/main"), File("src/simulation"))
            require(dirs.all { it.isDirectory }) {
                "Missing source directories; pass --source-identity=<revision-and-source-hash>"
            }
            dirs.flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
                .sortedBy { it.invariantSeparatorsPath }
                .forEach { file ->
                    digest.update(file.invariantSeparatorsPath.toByteArray())
                    file.inputStream().use { stream ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            val n = stream.read(buffer)
                            if (n < 0) break
                            digest.update(buffer, 0, n)
                        }
                    }
                }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private fun atomicMove(source: File, target: File) {
            try {
                Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
                throw IllegalStateException("Atomic rename unsupported for ${source.parentFile}; refusing unsafe checkpoint", e)
            }
        }
    }
}
