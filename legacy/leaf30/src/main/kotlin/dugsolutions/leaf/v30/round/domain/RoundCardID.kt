package dugsolutions.leaf.v30.round.domain

import java.util.Collections

typealias RoundCardID = Int

object GenRoundCardID {
    private var ID = 0
    private val lock = Any()
    private val nameToIdMap = Collections.synchronizedMap(mutableMapOf<String, RoundCardID>())

    fun generateId(name: String): RoundCardID = synchronized(lock) {
        nameToIdMap[name]?.let { return it }
        val id = ++ID
        nameToIdMap[name] = id
        return id
    }
}
