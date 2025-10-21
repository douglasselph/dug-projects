package dugsolutions.leaf.cards

import dugsolutions.leaf.common.domain.GameEffect
import dugsolutions.leaf.cards.domain.Cost
import dugsolutions.leaf.cards.domain.FlourishType
import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.cards.domain.MatchWith
import dugsolutions.leaf.cards.domain.Phase

/**
 * Provides predefined test cards for use in tests.
 * Currently only includes Resource cards from Cards-v14.csv.
 */
class FakeCards {

    companion object {
        // Card IDs
        private var ID = 0

        // Resource cards from Cards-v14.csv
        val sunlightCard = GameCard(
            id = ++ID,
            name = "Sunlight",
            type = FlourishType.RESOURCE,
            resilience = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = GameEffect.ADD_TO_DIE,
            primaryValue = 2,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 4,
            notes = null
        )

        val waterCard = GameCard(
            id = ++ID,
            name = "Water",
            type = FlourishType.RESOURCE,
            resilience = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = GameEffect.REROLL_ACCEPT_2ND,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 4,
            notes = null
        )

        val compostCard = GameCard(
            id = ++ID,
            name = "Compost",
            type = FlourishType.RESOURCE,
            resilience = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = GameEffect.UPGRADE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 4,
            notes = "Only if die exists"
        )

        val mulchCard = GameCard(
            id = ++ID,
            name = "Mulch",
            type = FlourishType.RESOURCE,
            resilience = 0,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = GameEffect.GRAFT_DIE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 4,
            notes = null
        )

        // ROOT cards for damage absorption testing
        val weakRootCard = GameCard(
            id = ++ID,
            name = "Weak Root",
            type = FlourishType.ROOT,
            resilience = 1,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 1,
            notes = null
        )

        val strongRootCard = GameCard(
            id = ++ID,
            name = "Strong Root",
            type = FlourishType.ROOT,
            resilience = 3,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 1,
            notes = null
        )

        val massiveRootCard = GameCard(
            id = ++ID,
            name = "Massive Root",
            type = FlourishType.ROOT,
            resilience = 5,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 1,
            notes = null
        )

        val rootCard = strongRootCard

        // VINE cards for damage absorption testing
        val weakVineCard = GameCard(
            id = ++ID,
            name = "Weak Vine",
            type = FlourishType.VINE,
            resilience = 2,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 1,
            notes = null
        )

        val strongVineCard = GameCard(
            id = ++ID,
            name = "Strong Vine",
            type = FlourishType.VINE,
            resilience = 4,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 1,
            notes = null
        )

        val vineCard = strongVineCard

        // FLOWER cards for damage absorption testing
        val weakFlowerCard = GameCard(
            id = ++ID,
            name = "Weak Flower",
            type = FlourishType.FLOWER,
            resilience = 1,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 1,
            notes = null
        )

        val strongFlowerCard = GameCard(
            id = ++ID,
            name = "Strong Flower",
            type = FlourishType.FLOWER,
            resilience = 3,
            cost = Cost.None,
            phase = Phase.Cultivation,
            primaryEffect = null,
            primaryValue = 0,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 1,
            notes = null
        )

        val flowerCard = strongFlowerCard

        // Collections of cards
        val ALL_RESOURCE = listOf(
            sunlightCard,
            waterCard,
            compostCard,
            mulchCard
        )

        val ALL_ROOT = listOf(
            weakRootCard,
            strongRootCard,
            massiveRootCard
        )

        val ALL_VINE = listOf(
            weakVineCard,
            strongVineCard
        )

        val ALL_FLOWER = listOf(
            weakFlowerCard,
            strongFlowerCard
        )

        val ALL_CARDS = ALL_RESOURCE + ALL_ROOT + ALL_VINE + ALL_FLOWER
    }
}
