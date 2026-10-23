package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyItem

/** A purchase identity is the named item, not the temporary Component Studio card ID. */
sealed interface BuySelection {
    data object Done : BuySelection
    data class Plant(val title: String, val currentId: String) : BuySelection
    data class Die(val sides: Int) : BuySelection
}

fun BuyChoice.selection(): BuySelection = when (this) {
    BuyChoice.Done -> BuySelection.Done
    is BuyChoice.Purchase -> when (val item = item) {
        is BuyItem.Plant -> BuySelection.Plant(item.card.title, item.card.name)
        is BuyItem.Die -> BuySelection.Die(item.sides.value)
    }
}

/** A comparison always describes TWO recommendations made for ONE identical Buy request. */
enum class BuyDisagreementKind {
    SAME_ITEM,
    DIFFERENT_ITEM,
    EXPERT_BUYS_BASELINE_DONE,
    BASELINE_BUYS_EXPERT_DONE,
    BOTH_DONE
}

data class BuyRecommendationComparison(
    val baseline: BuySelection,
    val expert: BuySelection,
    val kind: BuyDisagreementKind
) {
    val disagrees: Boolean get() = kind == BuyDisagreementKind.DIFFERENT_ITEM ||
        kind == BuyDisagreementKind.EXPERT_BUYS_BASELINE_DONE ||
        kind == BuyDisagreementKind.BASELINE_BUYS_EXPERT_DONE

    /** Same named Plant with a changed ID remains an item agreement; versioning belongs in the catalog fingerprint. */
    fun involves(cardTitle: String): Boolean =
        (baseline is BuySelection.Plant && baseline.title == cardTitle) ||
            (expert is BuySelection.Plant && expert.title == cardTitle)
}

object BuyDisagreement {
    fun compare(baseline: BuyChoice, expert: BuyChoice): BuyRecommendationComparison {
        val b = baseline.selection()
        val e = expert.selection()
        val kind = when {
            b == BuySelection.Done && e == BuySelection.Done -> BuyDisagreementKind.BOTH_DONE
            b == BuySelection.Done -> BuyDisagreementKind.EXPERT_BUYS_BASELINE_DONE
            e == BuySelection.Done -> BuyDisagreementKind.BASELINE_BUYS_EXPERT_DONE
            sameItem(b, e) -> BuyDisagreementKind.SAME_ITEM
            else -> BuyDisagreementKind.DIFFERENT_ITEM
        }
        return BuyRecommendationComparison(b, e, kind)
    }

    private fun sameItem(a: BuySelection, b: BuySelection): Boolean = when {
        a is BuySelection.Plant && b is BuySelection.Plant -> a.title == b.title
        a is BuySelection.Die && b is BuySelection.Die -> a.sides == b.sides
        else -> a == b
    }
}

/** One observation contributes to exactly one cell; observations are not assumed independent. */
data class NamedCardChoiceCounts(
    val both: Long = 0,
    val baselineOnly: Long = 0,
    val expertOnly: Long = 0,
    val neither: Long = 0
) {
    val total: Long get() = both + baselineOnly + expertOnly + neither
    val either: Long get() = both + baselineOnly + expertOnly
    val baselineRecommendations: Long get() = both + baselineOnly
    val expertRecommendations: Long get() = both + expertOnly
    val disagreementWhenEither: Double? get() = ratio(baselineOnly + expertOnly, either)
    val expertRejectionGivenBaseline: Double? get() = ratio(baselineOnly, baselineRecommendations)
    val expertOnlyGivenExpert: Double? get() = ratio(expertOnly, expertRecommendations)

    fun observe(comparison: BuyRecommendationComparison, cardTitle: String): NamedCardChoiceCounts {
        val b = comparison.baseline is BuySelection.Plant && comparison.baseline.title == cardTitle
        val e = comparison.expert is BuySelection.Plant && comparison.expert.title == cardTitle
        return when {
            b && e -> copy(both = both + 1)
            b -> copy(baselineOnly = baselineOnly + 1)
            e -> copy(expertOnly = expertOnly + 1)
            else -> copy(neither = neither + 1)
        }
    }
}

private fun ratio(n: Long, d: Long): Double? = if (d == 0L) null else n.toDouble() / d
