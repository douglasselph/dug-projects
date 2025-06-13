package dugsolutions.leaf.main.domain

import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig

/**
 * Object for generating fake CardInfo objects for testing purposes.
 * Uses the kfaker library to generate realistic-looking test data.
 */
object CardInfoFaker {

    private val faker = Faker(fakerConfig {
        locale = "en"
    })

    /**
     * Creates a fake CardInfo object with random but realistic values.
     * @param index Optional index value. If not provided, a random index between 0 and 9 will be used.
     * @param highlight Optional highlight value. If not provided, HighlightInfo.NONE will be used.
     * @return A CardInfo object with fake data
     */
    fun create(
        index: Int = faker.random.nextInt(0, 9),
        highlight: HighlightInfo = HighlightInfo.NONE
    ): CardInfo {
        val type = faker.random.randomValue(listOf("FLOWER", "LEAF", "ROOT", "STEM"))
        val resilience = faker.random.nextInt(1, 20)
        val nutrient = 1
        val thorn = faker.random.nextInt(0, 3)

        // Generate random effects with 50% chance of being null
        val primary = if (faker.random.nextBoolean()) faker.name.name() else null
        val match = if (faker.random.nextBoolean()) faker.name.name() else null
        val trash = if (faker.random.nextBoolean()) faker.name.name() else null

        return CardInfo(
            index = index,
            name = faker.name.name(),
            type = type,
            resilience = resilience,
            nutrient = nutrient,
            cost = "ROOT 6+",
            primary = primary,
            match = match,
            trash = trash,
            thorn = thorn,
            highlight = highlight
        )
    }

    /**
     * Creates a list of fake CardInfo objects.
     * @param count Number of CardInfo objects to create
     * @param startIndex Starting index for the cards. Each subsequent card will increment this index.
     * @param highlight Optional highlight value to apply to all cards
     * @return List of CardInfo objects with fake data
     */
    fun createList(
        count: Int,
        startIndex: Int = 0,
        highlight: HighlightInfo = HighlightInfo.NONE
    ): List<CardInfo> {
        return (0 until count).map { i ->
            create(
                index = startIndex + i,
                highlight = highlight
            )
        }
    }

    /**
     * Creates a fake CardInfo object that is marked as selected.
     * @param index Optional index value
     * @return A CardInfo object with HighlightInfo.SELECTED
     */
    fun createSelected(index: Int = faker.random.nextInt(0, 9)): CardInfo {
        return create(index = index, highlight = HighlightInfo.SELECTED)
    }

    /**
     * Creates a list of fake CardInfo objects where some are selected.
     * @param count Total number of cards to create
     * @param selectedCount Number of cards that should be marked as selected
     * @param startIndex Starting index for the cards
     * @return List of CardInfo objects with some marked as selected
     */
    fun createListWithSelected(
        count: Int,
        selectedCount: Int,
        startIndex: Int = 0
    ): List<CardInfo> {
        require(selectedCount <= count) { "selectedCount must be less than or equal to count" }
        
        val selectedIndices = (0 until count).shuffled().take(selectedCount).toSet()
        return (0 until count).map { i ->
            create(
                index = startIndex + i,
                highlight = if (selectedIndices.contains(i)) HighlightInfo.SELECTED else HighlightInfo.NONE
            )
        }
    }


} 
