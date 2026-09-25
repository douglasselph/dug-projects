package dugsolutions.leaf.v35.chronicle.domain

import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Butterflies

/** Immutable ownership/facing snapshot for one Butterfly. */
data class ButterflyStateSnapshot(
    val butterfly: Butterfly,
    val isFaceUp: Boolean
)

/** Capture owned Butterflies in stable ownership order. */
fun Butterflies.stateSnapshot(): List<ButterflyStateSnapshot> =
    all.map { butterfly ->
        ButterflyStateSnapshot(
            butterfly = butterfly,
            isFaceUp = isFaceUp(butterfly)
        )
    }
