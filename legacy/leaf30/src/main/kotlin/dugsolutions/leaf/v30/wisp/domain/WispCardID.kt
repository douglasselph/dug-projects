package dugsolutions.leaf.v30.wisp.domain

import java.util.Collections

typealias WispCardID = Int

object GenWispCardID {
    private var ID = 0
    private val lock = Any()
    private val nameToIdMap = Collections.synchronizedMap(mutableMapOf<String, WispCardID>())

    fun generateId(name: String): WispCardID = synchronized(lock) {
        nameToIdMap[name]?.let { return it }
        val id = ++ID
        nameToIdMap[name] = id
        return id
    }
}
