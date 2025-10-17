package dugsolutions.leaf.cards.domain

import java.util.Collections

typealias CardID = Int

object GenCardID {
    
    // Keep track of already assigned IDs to prevent collisions
    private var ID = 0
    
    // Map of name to ID for consistent generation
    private val lock = Any()
    private val nameToIdMap = Collections.synchronizedMap(mutableMapOf<String, CardID>())
    
    /**
     * Generate a CardID from a name.
     * - First tries to return a previously generated ID for the same name
     * - Then creates a positive ID that's more manageable than a raw hashCode
     */
    fun generateId(name: String): CardID = synchronized(lock) {
        // Return existing ID if we've seen this name before
        nameToIdMap[name]?.let { return it }
        val id = ++ID
        nameToIdMap[name] = id
        return id
    }

}