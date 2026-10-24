package dugsolutions.leaf.simulation.v35.experiment.baseline

import dugsolutions.leaf.simulation.v35.experiment.orchestration.*
import java.io.File

/** Persistent-JVM entry point: preserves globally indexed per-sample semantics and TSV schema. */
class BaselineResyncCardBalanceChunkedExperiment {
    fun execute(args: Array<String>) {
        val extra = args.associate { arg ->
            require(arg.startsWith("--") && '=' in arg) { "Expected --key=value: $arg" }
            val pair = arg.removePrefix("--").split('=', limit = 2)
            pair[0] to pair[1]
        }
        val legacyArgs = args.filterNot {
            it.startsWith("--chunk=") || it.startsWith("--output-dir=")
        }.toTypedArray()
        val options = BaselineResyncCardBalanceOptions.parse(legacyArgs)
        val chunkSize = extra["chunk"]?.toInt() ?: 100
        val outputDir = File(extra["output-dir"] ?: "build/reports/baseline-resync-chunks")
        execute(options, chunkSize, outputDir)
    }

    fun execute(options: BaselineResyncCardBalanceOptions, chunkSize: Int, outputDir: File) {
        val config = ExperimentConfig(options.start, options.games, chunkSize)
        outputDir.mkdirs()
        require(outputDir.isDirectory) { "Could not create output directory: $outputDir" }
        BaselineResyncCardBalanceSession(options).use { session ->
            var writer: java.io.PrintWriter? = null
            var temp: File? = null
            var target: File? = null
            var completed = 0
            try {
                val summary = ExperimentRunner(config).run(
                    // Task 7 will add resume validation and atomic publication.
                    beginChunk = { chunk ->
                        val stem = stem(chunk)
                        target = File(outputDir, "$stem.tsv")
                        temp = File(outputDir, "$stem.tsv.tmp")
                        writer = temp!!.printWriter()
                        writer!!.println(BaselineResyncCardBalanceSession.HEADER)
                    },
                    runSample = { sample ->
                        session.runSample(checkNotNull(writer), sample)
                        completed++
                        if (completed % 25 == 0) {
                            writer!!.flush()
                            check(!writer!!.checkError()) { "Error writing sample=$sample" }
                            println("Baseline resync completed $completed/${options.games} games; sample=$sample")
                        }
                    },
                    commitChunk = { chunk ->
                        val active = checkNotNull(writer)
                        active.flush()
                        check(!active.checkError()) { "Error writing chunk ${chunk.number}" }
                        active.close()
                        writer = null
                        val staging = checkNotNull(temp)
                        check(staging.isFile && staging.length() > 0L) { "Missing chunk output $staging" }
                        java.nio.file.Files.move(staging.toPath(), checkNotNull(target).toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                        temp = null
                        println("DONE ${stem(chunk)}")
                    }
                )
                println("Wrote ${summary.completedGames} games / ${session.plantCount} Plant identities to ${outputDir.absolutePath}")
            } finally {
                writer?.close()
                // A failed chunk stays uncommitted and can be inspected at .tmp.
            }
        }
    }

    internal fun stem(chunk: ExperimentChunk): String =
        "sample-%06d-%06d".format(chunk.firstSample, chunk.endSampleExclusive - 1)
}
