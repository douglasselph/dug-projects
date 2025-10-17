package dugsolutions.leaf.cards

import dugsolutions.leaf.cards.domain.CardEffect
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
            primaryEffect = CardEffect.ADD_TO_DIE,
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
            primaryEffect = CardEffect.REROLL_ACCEPT_2ND,
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
            primaryEffect = CardEffect.UPGRADE,
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
            primaryEffect = CardEffect.GRAFT_DIE,
            primaryValue = 1,
            matchWith = MatchWith.None,
            matchEffect = null,
            matchValue = 0,
            image = null,
            count = 4,
            notes = null
        )

        // Collections of cards
        val ALL_RESOURCE = listOf(
            sunlightCard,
            waterCard,
            compostCard,
            mulchCard
        )

        val ALL_CARDS = ALL_RESOURCE
    }
}
