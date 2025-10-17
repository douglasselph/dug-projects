package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.domain.Cost
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.cards.domain.Phase

/**
 * Provides predefined test cards for use in tests.
 * Each card has a unique ID, name, and flourish type.
 */
class FakeCards {

    companion object {
        // Card IDs
        private var ID = 0

        // Basic test cards

        val seedlingCard = GameCard(
            id = ++ID,
            name = "Sprouting Seed",
            type = FlourishType.NONE,
            resilience = 0,
            nutrient = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = "seedling_cheap_sprout.png",
            count = 1,
            notes = null
        )

        val seedlingCard2 = GameCard(
            id = ++ID,
            name = "Tiny Sprout",
            type = FlourishType.NONE,
            resilience = 0,
            nutrient = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val seedlingCard3 = GameCard(
            id = ++ID,
            name = "Young Plant",
            type = FlourishType.NONE,
            resilience = 0,
            nutrient = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = "seedling_unearthed.png",
            count = 1,
            notes = null
        )

        val seedlingCard4 = GameCard(
            id = ++ID,
            name = "Growing Seedling",
            type = FlourishType.NONE,
            resilience = 0,
            nutrient = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = "seedling_unsettled_growth.png",
            count = 1,
            notes = null
        )

        val seedlingCard5 = GameCard(
            id = ++ID,
            name = "Test Seedling",
            type = FlourishType.NONE,
            resilience = 0,
            nutrient = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val vineCard = GameCard(
            id = ++ID,
            name = "Thorny Vine",
            type = FlourishType.Vine,
            resilience = 8,
            nutrient = 1,
            cost = Cost.Value(7),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val vineCard2 = GameCard(
            id = ++ID,
            name = "Test Vine",
            type = FlourishType.Vine,
            resilience = 7,
            nutrient = 1,
            cost = Cost.Value(8),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val canopyCard = GameCard(
            id = ++ID,
            name = "Sheltering Canopy",
            type = FlourishType.CANOPY,
            resilience = 18,
            nutrient = 3,
            cost = Cost.Value(14),
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val canopyCard2 = GameCard(
            id = ++ID,
            name = "Test Canopy",
            type = FlourishType.CANOPY,
            resilience = 19,
            nutrient = 3,
            cost = Cost.Value(20),
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val rootCard = GameCard(
            id = ++ID,
            name = "Nourishing Root",
            type = FlourishType.ROOT,
            resilience = 8,
            nutrient = 1,
            cost = Cost.Value(6),
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val rootCard2 = GameCard(
            id = ++ID,
            name = "Test Root",
            type = FlourishType.ROOT,
            resilience = 10,
            nutrient = 1,
            cost = Cost.Value(7),
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val flowerCard = GameCard(
            id = ++ID,
            name = "Spring Flower A",
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 2,
            cost = Cost.Value(17),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val flowerCard2 = GameCard(
            id = ++ID,
            name = "Summer Flower B",
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 2,
            cost = Cost.Value(22),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val flowerCard3 = GameCard(
            id = ++ID,
            name = "Fall Flower C",
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 2,
            cost = Cost.Value(21),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            count = 1,
            notes = null
        )

        val bloomCard = GameCard(
            id = ++ID,
            name = "Spring Bloom",
            type = FlourishType.FLOWER,
            resilience = 1,
            nutrient = 10,
            cost = Cost.Value(17),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 3,
            matchWith = MatchWith.Sap,
            matchEffect = CardEffect.NONE,
            matchValue = 1,
            count = 1,
            notes = null
        )

        val bloomCard2 = GameCard(
            id = ++ID,
            name = "Test Bloom",
            type = FlourishType.FLOWER,
            resilience = 1,
            nutrient = 10,
            cost = Cost.Value(22),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 1,
            matchWith = MatchWith.Bee,
            matchEffect = CardEffect.NONE,
            matchValue = 1,
            count = 1,
            notes = null
        )

        val bloomCard3 = GameCard(
            id = ++ID,
            name = "Dark Bloom",
            type = FlourishType.FLOWER,
            resilience = 1,
            nutrient = 10,
            cost = Cost.Value(21),
            phase = Phase.Cultivation,
            primaryEffect = CardEffect.NONE,
            primaryValue = 1,
            matchWith = MatchWith.End,
            matchEffect = CardEffect.NONE,
            matchValue = 1,
            count = 1,
            notes = null
        )

        // Collections of cards
        val ALL_SEEDLINGS = listOf(
            seedlingCard,
            seedlingCard2,
            seedlingCard3,
            seedlingCard4,
            seedlingCard5
        )

        val ALL_ROOT = listOf(
            rootCard,
            rootCard2
        )

        val ALL_CANOPY = listOf(
            canopyCard,
            canopyCard2
        )

        val ALL_VINE = listOf(
            vineCard,
            vineCard2
        )

        val ALL_BLOOM = listOf(
            bloomCard,
            bloomCard2,
            bloomCard3
        )

        val ALL_FLOWER = listOf(
            flowerCard,
            flowerCard2,
            flowerCard3
        )

        val ALL_CARDS = ALL_SEEDLINGS + ALL_ROOT + ALL_CANOPY + ALL_VINE + ALL_BLOOM + ALL_FLOWER
    }
}
