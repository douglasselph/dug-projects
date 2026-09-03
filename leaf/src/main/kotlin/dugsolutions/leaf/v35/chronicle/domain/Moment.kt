package dugsolutions.leaf.v35.chronicle.domain

/**
 * Input events accepted by Chronicle.
 *
 * Future Moment variants may temporarily reference live domain objects.
 * GameChronicle must snapshot any such values into immutable GameEntry data
 * before storing them.
 *
 * Marker is intentionally the only initial event. It lets the Chronicle
 * infrastructure exist and be tested before the v35 gameplay event vocabulary
 * is defined.
 */
sealed interface Moment {

    data class Marker(
        val message: String
    ) : Moment
}
