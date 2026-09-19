package dugsolutions.leaf.v30.table.di

import dugsolutions.leaf.v30.cards.GameCardManager
import dugsolutions.leaf.v30.cards.di.GameCardsFactory
import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.cards.domain.GameCards
import dugsolutions.leaf.v30.grove.domain.GroveCardStackID
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.table.domain.GameLength
import dugsolutions.leaf.v30.table.domain.TableConfig

class TableConfigFactory(
    private val gameCardManager: GameCardManager,
    private val gameCardsFactory: GameCardsFactory,
    private val randomizer: Randomizer
) {
    operator fun invoke(
        cards: GameCards,
        numPlayers: Int,
        gameLength: GameLength
    ): TableConfig {
        return TableConfig(cards, numPlayers, gameLength)
    }

    operator fun invoke(
        cards: GameCards,
        numPlayers: Int,
        numBattle: Int,
        numCultivation: Int
    ): TableConfig {
        return TableConfig(
            cards = cards,
            numPlayers = numPlayers,
            numBattle = numBattle,
            numCultivation = numCultivation
        )
    }

    fun random(
        numPlayers: Int,
        gameLength: GameLength
    ): TableConfig {
        return TableConfig(
            cards = selectRandomGroveCards(),
            numPlayers = numPlayers,
            gameLength = gameLength
        )
    }

    fun random(
        numPlayers: Int,
        numBattle: Int,
        numCultivation: Int
    ): TableConfig {
        return TableConfig(
            cards = selectRandomGroveCards(),
            numPlayers = numPlayers,
            numBattle = numBattle,
            numCultivation = numCultivation
        )
    }

    private fun selectRandomGroveCards(): GameCards {
        val allCards = gameCardManager.getAllCards().cards
        val selected = GroveCardStackID.entries.map { stackId ->
            selectCardForStack(stackId, allCards)
        }
        return gameCardsFactory(selected)
    }

    private fun selectCardForStack(
        stackId: GroveCardStackID,
        allCards: List<GameCard>
    ): GameCard {
        val candidates = allCards.filter { card ->
            card.type == stackId.type && card.cost == stackId.cost
        }
        return randomizer.shuffled(candidates).firstOrNull()
            ?: throw IllegalStateException("No game cards available for grove stack: $stackId")
    }
}
