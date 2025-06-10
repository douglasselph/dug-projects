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
        val fakeNone = GameCard(
            id = ++ID,
            name = "Test None",
            type = FlourishType.NONE,
            resilience = 0,
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

        val fakeSeedling = GameCard(
            id = ++ID,
            name = "Sprouting Seed",
            type = FlourishType.SEEDLING,
            resilience = 2,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = CardEffect.REDUCE_COST_ROOT,
            matchValue = 2,
            trashEffect = CardEffect.GAIN_FREE_ROOT,
            trashValue = 1,
            thorn = 0
        )

        val fakeSeedling2 = GameCard(
            id = ++ID,
            name = "Tiny Sprout",
            type = FlourishType.SEEDLING,
            resilience = 1,
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

        val fakeSeedling3 = GameCard(
            id = ++ID,
            name = "Young Plant",
            type = FlourishType.SEEDLING,
            resilience = 2,
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

        val fakeSeedling4 = GameCard(
            id = ++ID,
            name = "Growing Seedling",
            type = FlourishType.SEEDLING,
            resilience = 3,
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

        val fakeSeedling5 = GameCard(
            id = ++ID,
            name = "Test Seedling",
            type = FlourishType.SEEDLING,
            resilience = 1,
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

        val fakeVine = GameCard(
            id = ++ID,
            name = "Thorny Vine",
            type = FlourishType.VINE,
            resilience = 4,
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

        val fakeVine2 = GameCard(
            id = ++ID,
            name = "Test Vine",
            type = FlourishType.VINE,
            resilience = 4,
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

        val fakeCanopy = GameCard(
            id = ++ID,
            name = "Sheltering Canopy",
            type = FlourishType.CANOPY,
            resilience = 5,
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

        val fakeCanopy2 = GameCard(
            id = ++ID,
            name = "Test Canopy",
            type = FlourishType.CANOPY,
            resilience = 3,
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

        val fakeRoot = GameCard(
            id = ++ID,
            name = "Nourishing Root",
            type = FlourishType.ROOT,
            resilience = 2,
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

        val fakeRoot2 = GameCard(
            id = ++ID,
            name = "Test Root",
            type = FlourishType.ROOT,
            resilience = 2,
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

        val fakeFlower = GameCard(
            id = ++ID,
            name = "Spring Flower",
            type = FlourishType.FLOWER,
            resilience = 10,
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
            thorn = 0
        )

        val fakeFlower2 = GameCard(
            id = ++ID,
            name = "Test Flower",
            type = FlourishType.FLOWER,
            resilience = 10,
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
            thorn = 0
        )

        val fakeFlower3 = GameCard(
            id = ++ID,
            name = "Dark Flower",
            type = FlourishType.FLOWER,
            resilience = 10,
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
            thorn = 0
        )

        val fakeBloom = GameCard(
            id = ++ID,
            name = "Spring Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
            cost = Cost.from(
                listOf(
                    CostElement.FlourishTypePresent(FlourishType.VINE),
                    CostElement.SingleDieMinimum(12),
                    CostElement.TotalDiceMinimum(17)
                )
            ),
            primaryEffect = CardEffect.ADD_TO_TOTAL,
            primaryValue = 3,
            matchWith = MatchWith.Flower(fakeFlower.id),
            matchEffect = CardEffect.ADD_TO_TOTAL,
            matchValue = 1,
            trashEffect = CardEffect.DEFLECT,
            trashValue = 4,
            thorn = 0
        )

        val fakeBloom2 = GameCard(
            id = ++ID,
            name = "Test Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(18),
                CostElement.TotalDiceMinimum(22)
            )),
            primaryEffect = CardEffect.DRAW_DIE,
            primaryValue = 1,
            matchWith = MatchWith.Flower(fakeFlower2.id),
            matchEffect = CardEffect.DRAW_DIE,
            matchValue = 1,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        val fakeBloom3 = GameCard(
            id = ++ID,
            name = "Dark Bloom",
            type = FlourishType.BLOOM,
            resilience = 1,
            cost = Cost.from(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(17),
                CostElement.TotalDiceMinimum(21)
            )),
            primaryEffect = CardEffect.ADJUST_BY,
            primaryValue = 1,
            matchWith = MatchWith.Flower(fakeFlower3.id),
            matchEffect = CardEffect.ADJUST_BY,
            matchValue = 1,
            trashEffect = null,
            trashValue = 0,
            thorn = 0
        )

        // Collections of card
        val ALL_SEEDLINGS = listOf(
            fakeSeedling,
            fakeSeedling2,
            fakeSeedling3,
            fakeSeedling4,
            fakeSeedling5
        )

        val ALL_ROOT = listOf(
            fakeRoot,
            fakeRoot2
        )

        val ALL_CANOPY = listOf(
            fakeCanopy,
            fakeCanopy2
        )

        val ALL_VINE = listOf(
            fakeVine,
            fakeVine2
        )

        val ALL_BLOOM = listOf(
            fakeBloom,
            fakeBloom2,
            fakeBloom3
        )

        val ALL_FLOWER = listOf(
            fakeFlower,
            fakeFlower2,
            fakeFlower3
        )

        val ALL_CARDS = ALL_SEEDLINGS + ALL_ROOT + ALL_CANOPY + ALL_VINE + ALL_BLOOM + ALL_FLOWER
    }
} 
