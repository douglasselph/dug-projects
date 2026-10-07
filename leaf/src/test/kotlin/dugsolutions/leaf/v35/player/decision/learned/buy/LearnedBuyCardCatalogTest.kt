package dugsolutions.leaf.v35.player.decision.learned.buy

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import kotlin.test.*
import java.nio.file.Path

class LearnedBuyCardCatalogTest {
    private fun cards() = PlantCardRegistry().also { r ->
        r.loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST), CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST))
    }.getAllCards()

    @Test fun `checked in AI card feature schema matches current CardDataFiles`() {
        val weights=LearnedBuyWeights.load(Path.of("data/ai/4p/buy-policy-v1.weights"))
        LearnedBuyCardCatalog.validateCurrentSchema(weights,cards())
    }

    @Test fun `manifest is deterministic and changes when strategic card definition changes`() {
        val original=cards(); val first=original.first()
        val a=PlantCardManifest.from(original)
        val b=PlantCardManifest.from(original.shuffled())
        assertEquals(a.catalogFingerprint,b.catalogFingerprint)
        val changed=original.toMutableList().also { it[0]=first.copy(cost=first.cost+2) }
        assertNotEquals(a.catalogFingerprint,PlantCardManifest.from(changed).catalogFingerprint)
    }

    @Test fun `trained policy refuses changed card catalog with actionable exception`() {
        val original=cards(); val manifest=PlantCardManifest.from(original)
        val weights=LearnedBuyCardCatalog.prepare(LearnedBuyWeights.zeros(),original)
            .withProvenance(LearnedBuyProvenance(trainingStatus="trained",cardManifest=manifest))
        val changed=original.toMutableList().also { it[0]=it[0].copy(cost=it[0].cost+2) }
        val e=assertFailsWith<LearnedPolicyCardManifestMismatchException> { LearnedBuyCardCatalog.prepare(weights,changed) }
        assertTrue(e.message!!.contains("Changed:")); assertTrue(e.message!!.contains(original.first().name))
        assertTrue(e.message!!.contains("CSV files are authoritative"))
    }

    @Test fun `prepare adds identity and exact cost weights`() {
        val prepared=LearnedBuyCardCatalog.prepare(LearnedBuyWeights.zeros(),cards())
        assertTrue("CARD_Root_05_01" in prepared.namedWeights())
        assertTrue("ACTION_COST_5" in prepared.namedWeights())
    }
}
