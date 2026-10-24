package dugsolutions.leaf.simulation.v35.experiment.baseline

/** Legacy single-output entry point, retained for existing Bash and Gradle callers. */
class BaselineResyncCardBalanceExperiment(
    private val reporter: BaselineResyncCardBalanceReporter = BaselineResyncCardBalanceReporter()
) {
    fun execute(args: Array<String>) = execute(BaselineResyncCardBalanceOptions.parse(args))

    fun execute(config: BaselineResyncCardBalanceOptions) {
        config.output.parentFile?.mkdirs()
        BaselineResyncCardBalanceSession(config, reporter).use { session ->
            config.output.printWriter().use { writer ->
                writer.println(BaselineResyncCardBalanceSession.HEADER)
                repeat(config.games) { index ->
                    val sample = config.start + index
                    session.runSample(writer, sample)
                    if ((index + 1) % 25 == 0) {
                        writer.flush()
                        println("Baseline resync completed ${index + 1}/${config.games} games; sample=$sample")
                    }
                }
            }
            println("Wrote ${config.games} games / ${session.plantCount} Plant identities to ${config.output.absolutePath}")
        }
    }
}
