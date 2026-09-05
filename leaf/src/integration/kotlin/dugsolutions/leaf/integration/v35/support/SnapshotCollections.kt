package dugsolutions.leaf.integration.v35.support

import java.util.Collections

internal fun <T> immutableList(values: Iterable<T>): List<T> =
    Collections.unmodifiableList(values.toList())

internal fun <K, V> immutableMap(values: Map<K, V>): Map<K, V> =
    Collections.unmodifiableMap(LinkedHashMap(values))

internal fun <T> immutableSet(values: Iterable<T>): Set<T> =
    Collections.unmodifiableSet(LinkedHashSet(values.toList()))
