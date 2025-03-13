package dugsolutions.leaf.cards

import dugsolutions.leaf.components.CardEffect
import dugsolutions.leaf.components.Cost
import dugsolutions.leaf.components.CostElement
import dugsolutions.leaf.components.FlourishType
import dugsolutions.leaf.components.GameCard
import dugsolutions.leaf.components.MatchWith

/**
 * Provides predefined test cards for use in tests.
 * Each card has a unique ID, name, and flourish type.
 */
class FakeCards {
    companion object {
        // Card IDs
        private const val CARD_ID_1 = 1
        private const val CARD_ID_2 = 2
        private const val CARD_ID_3 = 3
        private const val CARD_ID_4 = 4
        private const val CARD_ID_5 = 5
        private const val CARD_ID_6 = 6
        private const val CARD_ID_7 = 7
        private const val CARD_ID_8 = 8
        private const val CARD_ID_9 = 9
        private const val CARD_ID_10 = 10
        private const val CARD_ID_11 = 11
        private const val CARD_ID_12 = 12
        private const val CARD_ID_13 = 13
        private const val CARD_ID_14 = 14
        private const val CARD_ID_15 = 15

        // Card names
        private const val CARD_NAME_1 = "Test Seedling"
        private const val CARD_NAME_2 = "Test Root"
        private const val CARD_NAME_3 = "Test Canopy"
        private const val CARD_NAME_4 = "Test Vine"
        private const val CARD_NAME_5 = "Test Bloom"
        private const val CARD_NAME_6 = "Test None"
        private const val CARD_NAME_7 = "Spring Bloom"
        private const val CARD_NAME_8 = "Sprouting Seed"
        private const val CARD_NAME_9 = "Tiny Sprout"
        private const val CARD_NAME_10 = "Young Plant"
        private const val CARD_NAME_11 = "Growing Seedling"
        private const val CARD_NAME_12 = "Thorny Vine"
        private const val CARD_NAME_13 = "Sheltering Canopy"
        private const val CARD_NAME_14 = "Nourishing Root"
        private const val CARD_NAME_15 = "Dark Bloom"


        // Basic test cards

        val fakeNone = GameCard(
            id = CARD_ID_6,
            name = CARD_NAME_6,
            type = FlourishType.NONE,
            resilience = 0,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeBloom = GameCard(
            id = CARD_ID_7,
            name = CARD_NAME_7,
            type = FlourishType.BLOOM,
            resilience = 1,
            cost = Cost(
                listOf(
                    CostElement.FlourishTypePresent(FlourishType.VINE),
                    CostElement.SingleDieMinimum(12),
                    CostElement.TotalDiceMinimum(17)
                )
            ),
            primaryEffect = CardEffect.DRAW_DIE,
            primaryValue = 3,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = CardEffect.UPGRADE_D6,
            trashValue = 0
        )

        val fakeSeedling = GameCard(
            id = CARD_ID_8,
            name = CARD_NAME_8,
            type = FlourishType.SEEDLING,
            resilience = 2,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = CardEffect.REDUCE_COST_ROOT,
            matchValue = 2,
            trashEffect = CardEffect.GAIN_FREE_ROOT,
            trashValue = 1
        )

        val fakeSeedling2 = GameCard(
            id = CARD_ID_9,
            name = CARD_NAME_9,
            type = FlourishType.SEEDLING,
            resilience = 1,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeSeedling3 = GameCard(
            id = CARD_ID_10,
            name = CARD_NAME_10,
            type = FlourishType.SEEDLING,
            resilience = 2,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeSeedling4 = GameCard(
            id = CARD_ID_11,
            name = CARD_NAME_11,
            type = FlourishType.SEEDLING,
            resilience = 3,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeSeedling5 = GameCard(
            id = CARD_ID_1,
            name = CARD_NAME_1,
            type = FlourishType.SEEDLING,
            resilience = 1,
            cost = Cost(emptyList()),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeVine = GameCard(
            id = CARD_ID_12,
            name = CARD_NAME_12,
            type = FlourishType.VINE,
            resilience = 4,
            cost = Cost(listOf(
                CostElement.FlourishTypePresent(FlourishType.CANOPY),
                CostElement.SingleDieMinimum(7),
                CostElement.TotalDiceMinimum(9)
            )),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = CardEffect.THORN,
            trashValue = 2
        )

        val fakeVine2 = GameCard(
            id = CARD_ID_4,
            name = CARD_NAME_4,
            type = FlourishType.VINE,
            resilience = 4,
            cost = Cost(listOf(
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
            trashValue = 0
        )

        val fakeCanopy = GameCard(
            id = CARD_ID_13,
            name = CARD_NAME_13,
            type = FlourishType.CANOPY,
            resilience = 5,
            cost = Cost(listOf(
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
            trashValue = 0
        )

        val fakeCanopy2 = GameCard(
            id = CARD_ID_3,
            name = CARD_NAME_3,
            type = FlourishType.CANOPY,
            resilience = 3,
            cost = Cost(listOf(
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
            trashValue = 0
        )

        val fakeRoot = GameCard(
            id = CARD_ID_14,
            name = CARD_NAME_14,
            type = FlourishType.ROOT,
            resilience = 2,
            cost = Cost(listOf(
                CostElement.SingleDieMinimum(2),
                CostElement.TotalDiceMinimum(6)
            )),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeRoot2 = GameCard(
            id = CARD_ID_2,
            name = CARD_NAME_2,
            type = FlourishType.ROOT,
            resilience = 2,
            cost = Cost(listOf(
                CostElement.SingleDieMinimum(3),
                CostElement.TotalDiceMinimum(7)
            )),
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeBloom2 = GameCard(
            id = CARD_ID_5,
            name = CARD_NAME_5,
            type = FlourishType.BLOOM,
            resilience = 1,
            cost = Cost(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(18),
                CostElement.TotalDiceMinimum(22)
            )),
            primaryEffect = CardEffect.USE_OPPONENT_CARD,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
        )

        val fakeBloom3 = GameCard(
            id = CARD_ID_15,
            name = CARD_NAME_15,
            type = FlourishType.BLOOM,
            resilience = 1,
            cost = Cost(listOf(
                CostElement.FlourishTypePresent(FlourishType.VINE),
                CostElement.SingleDieMinimum(17),
                CostElement.TotalDiceMinimum(21)
            )),
            primaryEffect = CardEffect.DRAW_CARD,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            trashEffect = null,
            trashValue = 0
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

        val ALL_CARDS = ALL_SEEDLINGS + ALL_ROOT + ALL_CANOPY + ALL_VINE + ALL_BLOOM
    }
} 
