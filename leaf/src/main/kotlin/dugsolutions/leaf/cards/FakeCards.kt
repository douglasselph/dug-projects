package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.CardEffect
import dugsolutions.leaf.cards.cost.Cost
import dugsolutions.leaf.cards.cost.CostElement
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith

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
            type = FlourishType.SEEDLING,
            resilience = 0,
            nutrient = 0,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = CardEffect.REDUCE_COST_ROOT,
            matchValue = 2,
            trashEffect = CardEffect.GAIN_FREE_ROOT,
            trashValue = 1,
            thorn = 0,
            image = "seedling_cheap_sprout.png"
        )

        val seedlingCard2 = GameCard(
            id = ++ID,
            name = "Tiny Sprout",
            type = FlourishType.SEEDLING,
            resilience = 0,
            nutrient = 0,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val seedlingCard3 = GameCard(
            id = ++ID,
            name = "Young Plant",
            type = FlourishType.SEEDLING,
            resilience = 0,
            nutrient = 0,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0,
            image = "seedling_unearthed.png"
        )

        val seedlingCard4 = GameCard(
            id = ++ID,
            name = "Growing Seedling",
            type = FlourishType.SEEDLING,
            resilience = 0,
            nutrient = 0,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val seedlingCard5 = GameCard(
            id = ++ID,
            name = "Test Seedling",
            type = FlourishType.SEEDLING,
            resilience = 0,
            nutrient = 0,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val vineCard = GameCard(
            id = ++ID,
            name = "Thorny Vine",
            type = FlourishType.VINE,
            resilience = 8,
            nutrient = 1,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.CANOPY),
                CostElement.SingleDieMinimum(7),
                CostElement.TotalDiceMinimum(9)
            )),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = CardEffect.REUSE_DIE,
            trashValue = 1,
            thorn = 2
        )

        val vineCard2 = GameCard(
            id = ++ID,
            name = "Test Vine",
            type = FlourishType.VINE,
            resilience = 7,
            nutrient = 1,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.CANOPY),
                CostElement.SingleDieMinimum(8),
                CostElement.TotalDiceMinimum(10)
            )),
            primaryEffect = CardEffect.GAIN_FREE_ROOT,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val canopyCard = GameCard(
            id = ++ID,
            name = "Sheltering Canopy",
            type = FlourishType.CANOPY,
            resilience = 18,
            nutrient = 3,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.SingleDieMinimum(6),
                CostElement.TotalDiceMinimum(14)
            )),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val canopyCard2 = GameCard(
            id = ++ID,
            name = "Test Canopy",
            type = FlourishType.CANOPY,
            resilience = 19,
            nutrient = 3,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.ROOT),
                CostElement.SingleDieMinimum(9),
                CostElement.TotalDiceMinimum(20)
            )),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val rootCard = GameCard(
            id = ++ID,
            name = "Nourishing Root",
            type = FlourishType.ROOT,
            resilience = 8,
            nutrient = 1,
            cost = Cost.from(listOf(
                CostElement.SingleDieMinimum(2),
                CostElement.TotalDiceMinimum(6)
            )),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val rootCard2 = GameCard(
            id = ++ID,
            name = "Test Root",
            type = FlourishType.ROOT,
            resilience = 10,
            nutrient = 1,
            cost = Cost.from(listOf(
                CostElement.SingleDieMinimum(3),
                CostElement.TotalDiceMinimum(7)
            )),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val flowerCard = GameCard(
            id = ++ID,
            name = "Spring Flower A",
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 2,
            cost = Cost.from(
                listOf(
                    CostElement.FlourishTypePresent(FlourishType.VINE),
                    CostElement.SingleDieMinimum(12),
                    CostElement.TotalDiceMinimum(17)
                )
            ),
            primaryEffect = CardEffect.ADORN,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 10,
            thorn = 1
        )

        val flowerCard2 = GameCard(
            id = ++ID,
            name = "Summer Flower B",
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 2,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(18),
                CostElement.TotalDiceMinimum(22)
            )),
            primaryEffect = CardEffect.ADORN,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 10,
            thorn = 1
        )

        val flowerCard3 = GameCard(
            id = ++ID,
            name = "Fall Flower C",
            type = FlourishType.FLOWER,
            resilience = 10,
            nutrient = 2,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(17),
                CostElement.TotalDiceMinimum(21)
            )),
            primaryEffect = CardEffect.ADORN,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 10,
            thorn = 1
        )

        val bloomCard = GameCard(
            id = ++ID,
            name = "Spring Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
            nutrient = 10,
            cost = Cost.from(
                listOf(
                    CostElement.FlourishTypePresent(FlourishType.VINE),
                    CostElement.SingleDieMinimum(12),
                    CostElement.TotalDiceMinimum(17)
                )
            ),
            primaryEffect = CardEffect.ADD_TO_TOTAL,
            primaryValue = 3,
            matchWith = MatchWith.Flower(flowerCard.id),
            matchEffect = CardEffect.ADD_TO_TOTAL,
            matchValue = 1,
            trashEffect = CardEffect.DEFLECT,
            trashValue = 4,
            thorn = 0
        )

        val bloomCard2 = GameCard(
            id = ++ID,
            name = "Test Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
            nutrient = 10,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(18),
                CostElement.TotalDiceMinimum(22)
            )),
            primaryEffect = CardEffect.DRAW_DIE,
            primaryValue = 1,
            matchWith = MatchWith.Flower(flowerCard2.id),
            matchEffect = CardEffect.DRAW_DIE,
            matchValue = 1,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val bloomCard3 = GameCard(
            id = ++ID,
            name = "Dark Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
            nutrient = 10,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(17),
                CostElement.TotalDiceMinimum(21)
            )),
            primaryEffect = CardEffect.ADJUST_BY,
            primaryValue = 1,
            matchWith = MatchWith.Flower(flowerCard3.id),
            matchEffect = CardEffect.ADJUST_BY,
            matchValue = 1,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        // Collections of card
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
