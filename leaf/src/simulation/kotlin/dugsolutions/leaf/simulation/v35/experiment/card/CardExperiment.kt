package dugsolutions.leaf.simulation.v35.experiment.card

import dugsolutions.leaf.v35.plant.domain.PlantCard

/** Common boundary for simulation-layer card experiments. */
fun interface CardExperiment {
    fun run(
        spec: CardFocusExperimentSpec,
        selectedPlantCards: List<PlantCard>
    ): CardExperimentResult
}
