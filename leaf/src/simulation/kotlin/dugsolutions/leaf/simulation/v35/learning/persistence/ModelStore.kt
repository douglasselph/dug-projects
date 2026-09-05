package dugsolutions.leaf.simulation.v35.learning.persistence

/** Persistence boundary for future learned model parameters. */
interface ModelStore<T> {
    fun save(name: String, model: T)
    fun load(name: String): T?
}
