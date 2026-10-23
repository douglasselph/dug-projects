package dugsolutions.leaf.simulation.v35.learning.buy

import dugsolutions.leaf.v35.player.decision.buy.diagnostics.BuyDecisionEvent
import dugsolutions.leaf.v35.player.decision.buy.diagnostics.BuyDisagreementReports
import dugsolutions.leaf.v35.player.development.DevelopmentPlantIdentity
import dugsolutions.leaf.v35.player.development.PlayerDevelopmentReports
import dugsolutions.leaf.v35.player.development.PlayerDevelopmentResult
import dugsolutions.leaf.v35.plant.domain.PlantCard
import java.nio.file.Files
import java.nio.file.Path

/** Game-local read-only research data, flushed once per evaluator invocation. */
internal class EvaluationDiagnostics(private val directory: Path, cards: List<PlantCard>) {
    private val events = mutableListOf<BuyDecisionEvent>()
    private val developments = mutableListOf<PlayerDevelopmentResult>()
    private val catalog = cards.map { DevelopmentPlantIdentity(it.title, it.name, it.type.name, it.cost) }

    fun addEvents(variant: String, observations: List<BuyDecisionEvent>) {
        require(variant == "LEARNED") { "Only expert-state shadow comparisons are supported" }
        events.addAll(observations)
    }

    fun addDevelopment(result: PlayerDevelopmentResult) { developments.add(result) }

    fun finish() {
        Files.createDirectories(directory)
        BuyDisagreementReports.write(events, directory)
        PlayerDevelopmentReports.write(developments, directory, catalog)
        Files.writeString(directory.resolve("README-FIRST.txt"),
            "D9 evaluation diagnostics. Disagreement events are LEARNED-state only. " +
            "Development includes CONTROL, LEARNED and other human opponents. " +
            "All summaries are descriptive; they do not establish card causality.\n")
    }
}
