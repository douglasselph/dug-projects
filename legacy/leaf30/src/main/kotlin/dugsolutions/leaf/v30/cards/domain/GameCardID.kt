package dugsolutions.leaf.v30.cards.domain

import java.util.Collections

typealias GameCardID = Int

object GenGameCardID {
    
    // Keep track of already assigned IDs to prevent collisions
    private var ID = 0
    
    // Map of name to ID for consistent generation
    private val lock = Any()
    private val nameToIdMap = Collections.synchronizedMap(mutableMapOf<String, GameCardID>())
    
    /**
     * Generate a GameCardID from a name.
     * - First tries to return a previously generated ID for the same name
     * - Then creates a positive ID that's more manageable than a raw hashCode
     */
    fun generateId(name: String): GameCardID = synchronized(lock) {
        // Return existing ID if we've seen this name before
        nameToIdMap[name]?.let { return it }
        val id = ++ID
        nameToIdMap[name] = id
        return id
    }

}