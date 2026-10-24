package dugsolutions.leaf.simulation.v35.experiment.orchestration

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Monotonic durations and wall-clock ETA; skipped chunks do not enter the average. */
class ExperimentTiming(
    private val nanoTime: () -> Long = System::nanoTime,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    private var startedAt: Long? = null
    private var measuredNanos = 0L
    private var measuredChunks = 0

    fun start(): Long {
        check(startedAt == null) { "A timed chunk is already running" }
        return nanoTime().also { startedAt = it }
    }

    fun finish(): Duration {
        val start = checkNotNull(startedAt) { "No timed chunk is running" }
        val elapsed = (nanoTime() - start).coerceAtLeast(0L)
        startedAt = null
        measuredChunks++
        measuredNanos += elapsed
        return Duration.ofNanos(elapsed)
    }

    val samples: Int get() = measuredChunks
    val measured: Duration get() = Duration.ofNanos(measuredNanos)
    val average: Duration get() = Duration.ofNanos(if (measuredChunks == 0) 0 else measuredNanos / measuredChunks)
    fun estimatedRemaining(remainingChunks: Int): Duration {
        require(remainingChunks >= 0)
        val averageNanos = if (measuredChunks == 0) 0L else measuredNanos / measuredChunks
        return Duration.ofNanos(averageNanos * remainingChunks.toLong())
    }
    fun eta(remainingChunks: Int): Instant = clock.instant().plus(estimatedRemaining(remainingChunks))

    companion object {
        private val etaFormat = DateTimeFormatter.ofPattern("EEE MMM dd hh:mm a", Locale.US)
        fun formatDuration(duration: Duration): String {
            val seconds = duration.seconds.coerceAtLeast(0)
            val hours = seconds / 3600
            val minutes = seconds % 3600 / 60
            val remain = seconds % 60
            return when {
                hours > 0 -> "%dh %02dm %02ds".format(hours, minutes, remain)
                minutes > 0 -> "%dm %02ds".format(minutes, remain)
                else -> "${seconds}s"
            }
        }
        fun formatEta(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
            etaFormat.withZone(zone).format(instant)
    }
}
