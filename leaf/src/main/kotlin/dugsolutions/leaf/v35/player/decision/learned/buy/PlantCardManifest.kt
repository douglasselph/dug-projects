package dugsolutions.leaf.v35.player.decision.learned.buy

import dugsolutions.leaf.v35.plant.domain.PlantCard
import java.security.MessageDigest

/**
 * Deterministic AI/research snapshot of strategically meaningful Plant definitions.
 * The normal card CSVs remain the sole source of truth; this is generated from parsed PlantCard objects.
 */
data class PlantCardManifestEntry(
    val cardId: String,
    val title: String,
    val quantity: Int,
    val type: String,
    val cost: Int,
    val effect: String,
    val scoringRule: String,
    val fingerprint: String
)

data class PlantCardManifest(val entries: List<PlantCardManifestEntry>) {
    val catalogFingerprint: String = sha256(entries.sortedBy { it.cardId }.joinToString("\n") { "${it.cardId}=${it.fingerprint}" })
    fun byId(): Map<String, PlantCardManifestEntry> = entries.associateBy { it.cardId }

    companion object {
        const val FORMAT_VERSION = 1

        fun from(cards: Collection<PlantCard>): PlantCardManifest {
            val entries = cards.sortedBy { it.name }.map { card ->
                val canonical = listOf(
                    "id=${card.name}", "title=${card.title}", "quantity=${card.quantity}",
                    "type=${card.type.name}", "cost=${card.cost}", "effect=${card.effect.name}",
                    "scoringRule=${card.scoringRule}"
                ).joinToString("|")
                PlantCardManifestEntry(card.name, card.title, card.quantity, card.type.name, card.cost,
                    card.effect.name, card.scoringRule.toString(), sha256(canonical))
            }
            require(entries.map { it.cardId }.distinct().size == entries.size) { "Duplicate Plant card IDs in AI manifest" }
            return PlantCardManifest(entries)
        }
    }
}

private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
