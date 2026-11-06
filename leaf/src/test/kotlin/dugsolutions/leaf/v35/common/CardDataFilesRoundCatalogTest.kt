package dugsolutions.leaf.v35.common

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Guard the canonical Round CSV filename and raw definitions, independently of effect conversion. */
class CardDataFilesRoundCatalogTest {

    @Test
    fun currentRoundCatalogHasOneAuthoritativePath() {
        assertEquals("Round_Cards.csv", CardDataFiles.ROUND_CARD_LIST)
        assertTrue(Files.isRegularFile(Path.of(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))))
        assertFalse(Files.exists(Path.of(CardDataFiles.dataPath("Turn_Cards.csv"))))
    }

    @Test
    fun sourceRoundDefinitionsIncludeUpdatedSurgeAndSwell() {
        val content = Files.readString(Path.of(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST)))
        assertTrue(content.contains("Gain up to a D12."))
        assertTrue(content.contains("Gain up to a D8."))
        assertTrue(content.contains("Use it now."))
    }
}
