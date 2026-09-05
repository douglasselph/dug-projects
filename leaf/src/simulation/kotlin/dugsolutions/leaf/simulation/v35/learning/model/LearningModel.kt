package dugsolutions.leaf.simulation.v35.learning.model

import dugsolutions.leaf.simulation.v35.learning.features.FeatureVector

/** Minimal model boundary; concrete learning algorithms belong in simulation, never the rules engine. */
fun interface LearningModel {
    fun score(features: FeatureVector): Double
}
