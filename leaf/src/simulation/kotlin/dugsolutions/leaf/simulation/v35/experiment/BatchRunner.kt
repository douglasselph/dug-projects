package dugsolutions.leaf.simulation.v35.experiment

/** Boundary for running one strategy matchup repeatedly under an experiment config. */
fun interface BatchRunner<R> {
    fun run(matchup: Matchup, config: ExperimentConfig): R
}
