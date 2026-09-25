package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.ButterflyStateSnapshot
import dugsolutions.leaf.v35.tokens.Butterfly

/** Shared compact Butterfly-state rendering for Chronicle lines and summaries. */
object ButterflyReport {
    fun render(states: List<ButterflyStateSnapshot>): String =
        states.joinToString(" ") { state ->
            val code = abbreviation(state.butterfly)
            if (state.isFaceUp) code else code.lowercase()
        }

    private fun abbreviation(butterfly: Butterfly): String =
        when (butterfly) {
            Butterfly.GREEN -> "GB"
            Butterfly.YELLOW -> "YB"
            Butterfly.RED -> "RB"
            Butterfly.PURPLE -> "PB"
        }
}
