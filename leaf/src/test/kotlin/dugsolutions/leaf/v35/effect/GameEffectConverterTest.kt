package dugsolutions.leaf.v35.effect

import dugsolutions.leaf.v35.common.CardDataFiles
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEffectConverterTest {

    private val converter = GameEffectConverter()

    @Test
    fun invoke_whenEffectKnown_returnsExpectedGameEffect() {
        // Act
        val result = converter("Gain 1 VP")

        // Assert
        assertEquals(GameEffect.GAIN_ONE_VP, result)
    }

    @Test
    fun invoke_whenFormattingDiffers_stillReturnsExpectedGameEffect() {
        // Arrange
        val effectText = """

            GAIN   1   VP

        """.trimIndent()

        // Act
        val result = converter(effectText)

        // Assert
        assertEquals(GameEffect.GAIN_ONE_VP, result)
    }

    @Test
    fun invoke_whenSmartPunctuationDiffers_stillReturnsExpectedGameEffect() {
        // Arrange
        val effectText = """
            Gain 1 Water.
            <battle/> Spend 1 Water to reroll 2 of your dice, or 1 of your opponent’s.
        """.trimIndent()

        // Act
        val result = converter(effectText)

        // Assert
        assertEquals(
            GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE,
            result
        )
    }

    @Test
    fun invoke_whenSnipHappensUsesCurrentWoundText_returnsExpectedGameEffect() {
        val result = converter(
            """
            Wound 1 card of your choice of an opponent's.
            You must choose a face up card first if there is one.
            """.trimIndent()
        )

        assertEquals(
            GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE,
            result
        )
    }

    @Test
    fun invoke_whenEffectUnknown_returnsUnknownAndReportsSource() {
        // Arrange
        var reportedSource: String? = null
        var reportedText: String? = null
        val converter = GameEffectConverter { sourceName, effectText ->
            reportedSource = sourceName
            reportedText = effectText
        }

        // Act
        val result = converter(
            effectText = "This is not a real Leaf & Let Die effect.",
            sourceName = "Test_Card"
        )

        // Assert
        assertEquals(GameEffect.UNKNOWN, result)
        assertEquals("Test_Card", reportedSource)
        assertEquals("This is not a real Leaf & Let Die effect.", reportedText)
    }

    /**
     * Contract test against the v35 CSV source of truth.
     *
     * This deliberately reads the actual card CSV files rather than duplicating
     * their effect strings in test code. It catches both:
     *
     * 1. A CSV effect whose text no longer maps to a GameEffect.
     * 2. A non-UNKNOWN GameEffect that is no longer represented by any CSV effect.
     */
    @Test
    fun csvSources_andGameEffects_areInSync() {
        // Arrange
        val sourceEffects = readAllSourceEffects()

        // Act
        val converted = sourceEffects.map { source ->
            source to converter(
                effectText = source.effectText,
                sourceName = source.cardName
            )
        }

        // Assert
        val unknown = converted.filter { (_, effect) ->
            effect == GameEffect.UNKNOWN
        }

        assertTrue(
            unknown.isEmpty(),
            buildString {
                appendLine("These CSV effects are not recognized by GameEffectConverter:")
                unknown.forEach { (source, _) ->
                    appendLine(
                        "  ${source.fileName} | ${source.cardName} | " +
                            "${source.columnName}: ${source.effectText.singleLine()}"
                    )
                }
            }
        )

        val representedEffects = converted
            .map { (_, effect) -> effect }
            .toSet()

        val expectedEffects = GameEffect.values()
            .filterNot { it == GameEffect.UNKNOWN }
            .toSet()

        assertEquals(
            expectedEffects,
            representedEffects,
            buildString {
                val missing = expectedEffects - representedEffects
                val unexpected = representedEffects - expectedEffects

                if (missing.isNotEmpty()) {
                    appendLine(
                        "GameEffects not represented by any v35 CSV effect: " +
                            missing.sortedBy { it.name }.joinToString()
                    )
                }
                if (unexpected.isNotEmpty()) {
                    appendLine(
                        "Unexpected converted GameEffects: " +
                            unexpected.sortedBy { it.name }.joinToString()
                    )
                }
            }
        )
    }

    private fun readAllSourceEffects(): List<SourceEffect> {
        val dataDirectory = Path.of("data", "v35")

        assertTrue(
            Files.isDirectory(dataDirectory),
            "Expected v35 card data directory at ${dataDirectory.toAbsolutePath()}"
        )

        return buildList {
            addAll(
                readEffects(
                    dataDirectory.resolve(CardDataFiles.ROOT_CARD_LIST),
                    "effect"
                )
            )
            addAll(
                readEffects(
                    dataDirectory.resolve(CardDataFiles.VF_CARD_LIST),
                    "effect"
                )
            )
            addAll(
                readEffects(
                    dataDirectory.resolve(CardDataFiles.WISP_LIST),
                    "effect"
                )
            )
            addAll(
                readEffects(
                    dataDirectory.resolve(CardDataFiles.ROUND_CARD_LIST),
                    "effect_1_text",
                    "effect_2_text"
                )
            )
        }
    }

    private fun readEffects(
        file: Path,
        vararg effectColumns: String
    ): List<SourceEffect> {
        assertTrue(
            Files.isRegularFile(file),
            "Expected CSV source file at ${file.toAbsolutePath()}"
        )

        val rows = parseCsv(Files.readString(file))

        assertTrue(rows.isNotEmpty(), "CSV file is empty: $file")

        val headers = rows.first()
        val headerIndex = headers
            .mapIndexed { index, name -> name.removePrefix("\uFEFF") to index }
            .toMap()

        val nameIndex = requireNotNull(headerIndex["name"]) {
            "CSV has no 'name' column: $file"
        }

        val effectIndexes = effectColumns.associateWith { column ->
            requireNotNull(headerIndex[column]) {
                "CSV has no '$column' column: $file"
            }
        }

        return rows
            .drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .flatMap { row ->
                val cardName = row.getOrElse(nameIndex) { "" }

                effectIndexes.mapNotNull { (columnName, index) ->
                    val effectText = row.getOrElse(index) { "" }.trim()

                    if (effectText.isEmpty()) {
                        null
                    } else {
                        SourceEffect(
                            fileName = file.fileName.toString(),
                            cardName = cardName,
                            columnName = columnName,
                            effectText = effectText
                        )
                    }
                }
            }
    }

    /**
     * Small RFC-4180-style CSV reader kept in the test so this contract test
     * does not depend on whichever CSV library the production loaders use.
     *
     * It supports quoted fields, commas/newlines inside quoted fields, escaped
     * double quotes, CRLF, and LF line endings.
     */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        fun finishField() {
            row.add(field.toString())
            field.setLength(0)
        }

        fun finishRow() {
            finishField()
            rows.add(row)
            row = mutableListOf()
        }

        while (index < text.length) {
            val char = text[index]

            if (inQuotes) {
                when {
                    char == '"' &&
                        index + 1 < text.length &&
                        text[index + 1] == '"' -> {
                        field.append('"')
                        index++
                    }

                    char == '"' -> {
                        inQuotes = false
                    }

                    else -> {
                        field.append(char)
                    }
                }
            } else {
                when (char) {
                    '"' -> inQuotes = true
                    ',' -> finishField()

                    '\n' -> finishRow()

                    '\r' -> {
                        if (index + 1 < text.length && text[index + 1] == '\n') {
                            index++
                        }
                        finishRow()
                    }

                    else -> field.append(char)
                }
            }

            index++
        }

        check(!inQuotes) {
            "CSV ended while inside a quoted field"
        }

        if (field.isNotEmpty() || row.isNotEmpty()) {
            finishRow()
        }

        return rows
    }

    private fun String.singleLine(): String =
        replace(Regex("""\s+"""), " ").trim()

    private data class SourceEffect(
        val fileName: String,
        val cardName: String,
        val columnName: String,
        val effectText: String
    )
}
