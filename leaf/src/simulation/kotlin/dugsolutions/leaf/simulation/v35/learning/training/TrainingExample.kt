package dugsolutions.leaf.simulation.v35.learning.training

import dugsolutions.leaf.simulation.v35.learning.features.FeatureVector

/** One future supervised/reinforcement-style observation derived from a completed simulation. */
data class TrainingExample(
    val features: FeatureVector,
    val outcome: Double
)
