package dugsolutions.leaf.integration.v35.support

import java.util.ArrayDeque

/**
 * Tiny queue used by scripted integration-test strategies.
 *
 * Each queued selector receives the real request produced by production code,
 * so tests choose from the actual legal choices rather than fabricating
 * detached decision objects. When no scripted selector remains the owning
 * strategy delegates to its normal fallback policy.
 */
internal class DecisionScript<Request, Choice>(
    private val label: String
) {
    private val selectors = ArrayDeque<(Request) -> Choice>()

    val pendingCount: Int
        get() = selectors.size

    fun then(selector: (Request) -> Choice): DecisionScript<Request, Choice> {
        selectors.addLast(selector)
        return this
    }

    fun nextOrElse(
        request: Request,
        fallback: (Request) -> Choice
    ): Choice =
        if (selectors.isEmpty()) {
            fallback(request)
        } else {
            selectors.removeFirst()(request)
        }

    fun assertExhausted() {
        check(selectors.isEmpty()) {
            "$label still has ${selectors.size} scripted decision(s) remaining"
        }
    }
}
