package dugsolutions.leaf.v35.player.decision.buy.diagnostics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class BuyDisagreementReportsTest {
    private fun event(
        baseline: BuySelection, expert: BuySelection,
        market: List<BuySelection> = emptyList(), players: Int = 2
    ): BuyDecisionEvent {
        val kind = when {
            baseline == BuySelection.Done && expert == BuySelection.Done -> BuyDisagreementKind.BOTH_DONE
            baseline == BuySelection.Done -> BuyDisagreementKind.EXPERT_BUYS_BASELINE_DONE
            expert == BuySelection.Done -> BuyDisagreementKind.BASELINE_BUYS_EXPERT_DONE
            baseline == expert -> BuyDisagreementKind.SAME_ITEM
            else -> BuyDisagreementKind.DIFFERENT_ITEM
        }
        return BuyDecisionEvent(1, 0, 1, 1, 0, players, "CULTIVATION", baseline, expert, kind,
            emptyList(), market.map { BuyOptionObservation(it, 11) }, emptyList(), 0, 0, 2, 1)
    }

    @Test fun `same-state aggregate reports direction, done and zero recommendation cards`() {
        val queen = BuySelection.Plant("Queen's Blossom", "Flower_17_01")
        val root = BuySelection.Plant("Root Well", "Root_05_01")
        val never = BuySelection.Plant("Never Bought", "Flower_11_04")
        val events = listOf(
            event(queen, queen, listOf(queen, root, never)),
            event(queen, root, listOf(queen, root, never)),
            event(root, queen, listOf(queen, root, never)),
            event(queen, BuySelection.Done, listOf(queen, root, never)),
            event(BuySelection.Done, queen, listOf(queen, root, never)),
            event(BuySelection.Done, BuySelection.Done, listOf(queen, root, never))
        )
        val reports = BuyDisagreementReports.generate(events).associate { it.fileName to it.tsv }
        val overall = reports.getValue("overall-disagreement.tsv")
        assertTrue(overall.contains("2\tALL\tALL\t6\t1\t2\t1\t1\t1\t4\t66.6667"))
        val card = reports.getValue("card-disagreement.tsv")
        assertTrue(card.contains("Queen's Blossom\tFlower_17_01\t6\t1\t2\t2\t1\t3\t3\t5\t80.0000\t66.6667\t66.6667"))
        assertTrue(card.contains("Never Bought\tFlower_11_04\t6\t0\t0\t0\t6\t0\t0\t0\t\t\t"))
        val substitutions = reports.getValue("purchase-substitutions.tsv")
        assertTrue(substitutions.contains("Plant: Queen's Blossom\tPlant: Root Well\t1\t25.0000"))
        assertTrue(substitutions.contains("DONE\tPlant: Queen's Blossom\t1\t25.0000"))
    }

    @Test fun `empty events produce only headers and writer creates three files`() {
        val path = Files.createTempDirectory("d6-buy-reports")
        try {
            val written = BuyDisagreementReports.write(emptyList(), path)
            assertEquals(3, written.size)
            assertTrue(written.all { Files.readString(it).lines().first().isNotEmpty() })
        } finally {
            Files.list(path).use { stream -> stream.forEach(Files::delete) }
            Files.delete(path)
        }
    }
}
