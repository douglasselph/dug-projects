package dugsolutions.leaf.simulation.v35.experiment

/**
 * Intentional boundary for the future high-volume simulation runner.
 *
 * The interface is defined now so strategy work can target simulation rather
 * than integration tests. No production batch implementation is supplied yet.
 */
fun interface BatchRunner<R> {
    fun run(matchup: Matchup, config: ExperimentConfig): R
}
