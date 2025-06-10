package dugsolutions.leaf.cards.domain

typealias CardID = Int

object GenCardID {
    
    // Keep track of already assigned IDs to prevent collisions
    private var ID = 0
    
    // Map of name to ID for consistent generation
    private val nameToIdMap = mutableMapOf<String, CardID>()
    
    /**
     * Generate a CardID from a name.
     * - First tries to return a previously generated ID for the same name
     * - Then creates a positive ID that's more manageable than a raw hashCode
     */
    fun generateId(name: String): CardID {
        // Return existing ID if we've seen this name before
        nameToIdMap[name]?.let { return it }
        val id = ++ID
        nameToIdMap[name] = id
        return id
    }

}
