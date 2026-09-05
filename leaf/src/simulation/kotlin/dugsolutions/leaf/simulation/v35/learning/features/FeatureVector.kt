package dugsolutions.leaf.simulation.v35.learning.features

/** Immutable numeric input to a future learned strategy model. */
data class FeatureVector(
    val values: List<Double>
) {
    val size: Int get() = values.size
}
