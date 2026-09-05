package dugsolutions.leaf.v35.common

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

class CardCsvInlineTagSpacingTest {

    @Test
    fun inlineComponentTags_areSeparatedFromAdjacentLettersAndNumbers() {
        val files = listOf(
            CardDataFiles.ROOT_CARD_LIST,
            CardDataFiles.VF_CARD_LIST,
            CardDataFiles.WISP_LIST,
            CardDataFiles.ROUND_CARD_LIST
        )
        val tag = Regex("<[^>]+/>")
        val problems = mutableListOf<String>()

        files.forEach { fileName ->
            val path = Path.of("data", "v35", fileName)
            Files.readAllLines(path).forEachIndexed { index, line ->
                tag.findAll(line).forEach { match ->
                    val before = line.getOrNull(match.range.first - 1)
                    val after = line.getOrNull(match.range.last + 1)
                    if (before?.isLetterOrDigit() == true || after?.isLetterOrDigit() == true) {
                        problems += "$fileName:${index + 1}: ${match.value} in $line"
                    }
                }
            }
        }

        assertTrue(
            problems.isEmpty(),
            "Inline Component Studio tags must have whitespace when adjacent to " +
                "letters/numbers. Punctuation may remain adjacent. Problems:\n" +
                problems.joinToString("\n")
        )
    }
}
