package dugsolutions.leaf.main.domain

import dugsolutions.leaf.chronicle.domain.PlayerScore
import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.fakerConfig

/**
 * Object for generating fake PlayerInfo objects for testing purposes.
 * Uses the kfaker library to generate realistic-looking test data.
 */
object PlayerInfoFaker {
    private val faker = Faker(fakerConfig { locale = "en" })

    /**
     * Creates a fake PlayerInfo object with random but realistic values.
     * @param name Optional player name. If not provided, a random name will be generated.
     * @param handCardCount Number of cards in hand
     * @param handDieCount Number of dice in hand
     * @param supplyDieCount Number of dice in supply
     * @param floralCardCount Number of cards in floral array
     * @param supplyCardCount Number of cards in supply
     * @param compostCardCount Number of cards in compost
     * @param compostDieCount Number of dice in compost
     * @param showDrawCount Whether to show draw count
     * @return A PlayerInfo object with fake data
     */
    fun create(
        name: String = faker.name.name(),
        handCardCount: Int = faker.random.nextInt(0, 5),
        handDieCount: Int = faker.random.nextInt(0, 3),
        supplyDieCount: Int = faker.random.nextInt(0, 3),
        floralCardCount: Int = faker.random.nextInt(0, 3),
        supplyCardCount: Int = faker.random.nextInt(0, 10),
        compostCardCount: Int = faker.random.nextInt(0, 5),
        compostDieCount: Int = faker.random.nextInt(0, 3),
        showDrawCount: Boolean = faker.random.nextBoolean()
    ): PlayerInfo {
        return PlayerInfo(
            name = name,
            infoLine = PlayerScore(1, scoreCards = 5, scoreDice = 3).toString(),
            handCards = CardInfoFaker.createList(handCardCount),
            handDice = DiceInfo(DieInfoFaker.createList(handDieCount)),
            supplyDice = DiceInfo(DieInfoFaker.createList(supplyDieCount)),
            floralArray = CardInfoFaker.createList(floralCardCount),
            supplyCardCount = supplyCardCount,
            compostCardCount = compostCardCount,
            compostDice = DiceInfo(DieInfoFaker.createList(compostDieCount)),
            showDrawCount = showDrawCount
        )
    }

    /**
     * Creates a fake PlayerInfo object with all components empty.
     * @param name Optional player name
     * @return A PlayerInfo object with empty components
     */
    fun createEmpty(name: String = faker.name.name()): PlayerInfo {
        return create(
            name = name,
            handCardCount = 0,
            handDieCount = 0,
            supplyDieCount = 0,
            floralCardCount = 0,
            supplyCardCount = 0,
            compostCardCount = 0,
            compostDieCount = 0
        )
    }

    /**
     * Creates a fake PlayerInfo object with all components populated.
     * @param name Optional player name
     * @return A PlayerInfo object with populated components
     */
    fun createPopulated(name: String = faker.name.name()): PlayerInfo {
        return create(
            name = name,
            handCardCount = 5,
            handDieCount = 3,
            supplyDieCount = 3,
            floralCardCount = 3,
            supplyCardCount = 10,
            compostCardCount = 5,
            compostDieCount = 3
        )
    }

    /**
     * Creates a fake PlayerInfo object with selected items.
     * @param name Optional player name
     * @param selectedHandCards Number of selected hand cards
     * @param selectedHandDice Number of selected hand dice
     * @param selectedFloralCards Number of selected floral cards
     * @return A PlayerInfo object with selected items
     */
    fun createWithSelected(
        name: String = faker.name.name(),
        selectedHandCards: Int = 1,
        selectedHandDice: Int = 1,
        selectedFloralCards: Int = 1
    ): PlayerInfo {
        val handCards = CardInfoFaker.createListWithSelected(5, selectedHandCards)
        val handDice = DieInfoFaker.createListWithSelected(3, selectedHandDice)
        val floralCards = CardInfoFaker.createListWithSelected(3, selectedFloralCards)

        return create(
            name = name,
            handCardCount = 5,
            handDieCount = 3,
            supplyDieCount = 3,
            floralCardCount = 3,
            supplyCardCount = 10,
            compostCardCount = 5,
            compostDieCount = 3
        ).copy(
            handCards = handCards,
            handDice = DiceInfo(handDice),
            floralArray = floralCards
        )
    }
} 
