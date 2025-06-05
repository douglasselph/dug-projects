package dugsolutions.leaf.main.domain

import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig

/**
 * Object for generating fake DieInfo objects for testing purposes.
 * Uses the kfaker library to generate realistic-looking test data.
 */
object DieInfoFaker {
    private val faker = Faker(fakerConfig { locale = "en" })

    /**
     * Creates a fake DieInfo object with random but realistic values.
     * @param index Optional index value. If not provided, a random index between 0 and 9 will be used.
     * @param highlight Optional highlight value. If not provided, HighlightInfo.NONE will be used.
     * @return A DieInfo object with fake data
     */
    fun create(
        index: Int = faker.random.nextInt(0, 9),
        highlight: HighlightInfo = HighlightInfo.NONE
    ): DieInfo {
        return DieInfo(
            index = index,
            value = faker.random.nextInt(1, 6).toString(),
            highlight = highlight
        )
    }

    /**
     * Creates a list of fake DieInfo objects.
     * @param count Number of DieInfo objects to create
     * @param startIndex Starting index for the dice. Each subsequent die will increment this index.
     * @param highlight Optional highlight value to apply to all dice
     * @return List of DieInfo objects with fake data
     */
    fun createList(
        count: Int,
        startIndex: Int = 0,
        highlight: HighlightInfo = HighlightInfo.NONE
    ): List<DieInfo> {
        return (0 until count).map { i ->
            create(
                index = startIndex + i,
                highlight = highlight
            )
        }
    }

    /**
     * Creates a fake DieInfo object that is marked as selected.
     * @param index Optional index value
     * @return A DieInfo object with HighlightInfo.SELECTED
     */
    fun createSelected(index: Int = faker.random.nextInt(0, 9)): DieInfo {
        return create(index = index, highlight = HighlightInfo.SELECTED)
    }

    /**
     * Creates a list of fake DieInfo objects where some are selected.
     * @param count Total number of dice to create
     * @param selectedCount Number of dice that should be marked as selected
     * @param startIndex Starting index for the dice
     * @return List of DieInfo objects with some marked as selected
     */
    fun createListWithSelected(
        count: Int,
        selectedCount: Int,
        startIndex: Int = 0
    ): List<DieInfo> {
        require(selectedCount <= count) { "selectedCount must be less than or equal to count" }
        
        val selectedIndices = (0 until count).shuffled().take(selectedCount).toSet()
        return (0 until count).map { i ->
            create(
                index = startIndex + i,
                highlight = if (selectedIndices.contains(i)) HighlightInfo.SELECTED else HighlightInfo.NONE
            )
        }
    }

    /**
     * Creates a fake DieInfo object with a specific value.
     * @param value The die value to set
     * @param index Optional index value
     * @return A DieInfo object with the specified value
     */
    fun createWithValue(
        value: String,
        index: Int = faker.random.nextInt(0, 9)
    ): DieInfo {
        return create(index = index).copy(value = value)
    }
} 