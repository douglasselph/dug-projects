package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.orchestration.*
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.io.PrintWriter

/** Persistent JVM, resume-safe chunk execution with unchanged per-game semantics. */
class BaselineResyncCardBalanceChunkedExperiment {
    fun execute(args: Array<String>) {
        val extra = args.associate { arg ->
            require(arg.startsWith("--") && '=' in arg) { "Expected --key=value: $arg" }
            val pair = arg.removePrefix("--").split('=', limit = 2)
            pair[0] to pair[1]
        }
        val legacyArgs = args.filterNot {
            it.startsWith("--chunk=") || it.startsWith("--output-dir=") ||
                it.startsWith("--source-identity=")
        }.toTypedArray()
        val options = BaselineResyncCardBalanceOptions.parse(legacyArgs)
        execute(options, extra["chunk"]?.toInt() ?: 100,
            File(extra["output-dir"] ?: "build/reports/baseline-resync-chunks"),
            extra["source-identity"] ?: "unversioned")
    }

    fun execute(options: BaselineResyncCardBalanceOptions, chunkSize: Int, outputDir: File,
                sourceIdentity: String = "unversioned") {
        require(sourceIdentity.isNotBlank())
        val config = ExperimentConfig(options.start, options.games, chunkSize)
        require(outputDir.isDirectory || outputDir.mkdirs()) { "Cannot create $outputDir" }
        // The fingerprint binds checkpoints to source identity, all seeds, player count,
        // exact input bytes, and sample/chunk boundaries. Bash should supply its Git/source ID.
        val fingerprint = BaselineChunkCheckpoint.fingerprint(options, chunkSize, sourceIdentity)
        FileChannel.open(File(outputDir, ".lock").toPath(),
            StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            val lock = try { channel.tryLock() } catch (_: java.nio.channels.OverlappingFileLockException) { null }
            requireNotNull(lock) { "Another experiment holds the output directory lock: $outputDir" }
            lock.use {
        BaselineResyncCardBalanceSession(options).use { session ->
            val checkpoint = BaselineChunkCheckpoint(outputDir, fingerprint, session.plantCount)
            var writer: PrintWriter? = null
            var current: ExperimentChunk? = null
            var completed = 0
            try {
                val summary = ExperimentRunner(config).run(
                    isComplete = { chunk ->
                        val skip = checkpoint.isComplete(chunk)
                        if (skip) checkpoint.logLine(chunk, "SKIP validated complete checkpoint")
                        skip
                    },
                    beginChunk = { chunk ->
                        current = chunk
                        checkpoint.marker(chunk).delete() // Invalidate any stale marker before rerun.
                        checkpoint.temporary(chunk).delete()
                        checkpoint.log(chunk).writeText("")
                        checkpoint.logLine(chunk, "BEGIN ${checkpoint.stem(chunk)} (${chunk.games} games), fingerprint=$fingerprint")
                        writer = checkpoint.temporary(chunk).printWriter()
                        checkNotNull(writer).println(BaselineResyncCardBalanceSession.HEADER)
                    },
                    runSample = { sample ->
                        session.runSample(checkNotNull(writer), sample)
                        completed++
                        if (completed % 25 == 0) {
                            checkNotNull(writer).flush()
                            check(!checkNotNull(writer).checkError()) { "Error writing sample=$sample" }
                            println("Baseline resync completed $completed/${options.games} new games; sample=$sample")
                        }
                    },
                    commitChunk = { chunk ->
                        val active = checkNotNull(writer)
                        active.flush()
                        check(!active.checkError()) { "Error writing chunk ${chunk.number}" }
                        active.close()
                        writer = null
                        checkpoint.commit(chunk)
                        checkpoint.logLine(chunk, "DONE ${checkpoint.stem(chunk)} sha256=${BaselineChunkCheckpoint.sha256(checkpoint.tsv(chunk))}")
                        current = null
                        println("DONE ${checkpoint.stem(chunk)}")
                    }
                )
                println("Wrote ${summary.completedGames} new games; skipped ${summary.skippedChunks} chunks / " +
                    "${session.plantCount} Plant identities to ${outputDir.absolutePath}")
            } catch (failure: Throwable) {
                writer?.flush()
                writer?.close()
                writer = null
                current?.let { chunk ->
                    checkpoint.marker(chunk).delete()
                    checkpoint.log(chunk).appendText("FAILED: ${failure.stackTraceToString()}\n")
                    System.err.println("FAILED ${checkpoint.stem(chunk)} — see ${checkpoint.log(chunk)}")
                }
                throw failure
            } finally {
                writer?.close()
            }
        }
            }
        }
    }

    internal fun stem(chunk: ExperimentChunk): String =
        "sample-%06d-%06d".format(chunk.firstSample, chunk.endSampleExclusive - 1)
}
