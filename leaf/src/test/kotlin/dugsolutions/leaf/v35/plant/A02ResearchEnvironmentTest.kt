package dugsolutions.leaf.v35.plant

import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A02: protect the 3/2/2 research default and the nine legal Plant slots. */
class A02ResearchEnvironmentTest {
    private val requiredSlots = listOf(
        PlantType.ROOT to 5, PlantType.ROOT to 7, PlantType.ROOT to 9,
        PlantType.VINE to 7, PlantType.VINE to 9, PlantType.VINE to 11,
        PlantType.FLOWER to 11, PlantType.FLOWER to 14, PlantType.FLOWER to 17
    )

    private fun catalog(): PlantCardManager {
        val registry = PlantCardRegistry()
        registry.loadFromCsv(
            CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
            CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
        )
        return PlantCardManager().also { it.loadCards(registry) }
    }

    @Test
    fun `standard research rounds are three two two in the correct order`() {
        val rounds = GameRoundSetup.standard()
        val c = RoundCardType.CULTIVATION
        val b = RoundCardType.BATTLE
        assertEquals(listOf(c, c, c, b, c, c, b, c, c, b), rounds.roundTypes)
        assertEquals(7, rounds.cultivationRounds)
        assertEquals(3, rounds.battleRounds)
        // An explicitly requested alternative is allowed; do not hard-lock this globally.
        assertEquals(listOf(2, 2, 3), (GameRoundSetup.patterned(2, 2, 3) as GameRoundSetup.Patterned).cultivationBlocks)
    }

    @Test
    fun `canonical CSV contains exactly four distinct cards per each of nine legal slots`() {
        val all = catalog().getAllCards().cards
        assertEquals(36, all.size)
        assertEquals(36, all.map { it.name }.toSet().size)
        assertEquals(requiredSlots.toSet(), all.map { it.type to it.cost }.toSet())
        requiredSlots.forEach { slot ->
            val members = all.filter { (it.type to it.cost) == slot }
            assertEquals(4, members.size, "catalog slot $slot")
        }
    }

    @Test
    fun `default and generated Groves choose one card from each slot`() {
        val cards = catalog()
        val resolver = GrovePlantResolver(cards)
        val fixed = FirstGameDefault.PLANT_NAMES.map { requireNotNull(cards.getCard(it)) }
        val resolvedDefault = resolver.resolve(fixed, Randomizer.create(300L))
        assertEquals(fixed.map { it.name }, resolvedDefault.map { it.name })
        for (seed in 0L until 30L) {
            val resolved = resolver.resolve(emptyList(), Randomizer.create(seed))
            assertEquals(9, resolved.size)
            assertEquals(9, resolved.map { it.name }.toSet().size)
            assertEquals(requiredSlots.toSet(), resolved.map { it.type to it.cost }.toSet())
            assertTrue(resolved.all { cards.getCard(it.name) != null })
        }
    }
}
