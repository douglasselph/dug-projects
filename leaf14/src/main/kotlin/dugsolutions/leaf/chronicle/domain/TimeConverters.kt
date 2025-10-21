package dugsolutions.leaf.chronicle.domain

import kotlin.math.round

object TimeConverters {

    fun secondsToMinutes(secs: Int): String {
        val minutes = round(secs / 60f).toInt()
        return "$minutes minutes"
    }
}
