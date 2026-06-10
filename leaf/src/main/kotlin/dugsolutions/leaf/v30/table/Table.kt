package dugsolutions.leaf.v30.table

import dugsolutions.leaf.v30.grove.Grove
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.round.RoundDeck
import dugsolutions.leaf.v30.table.domain.TableConfig

class Table(
    val grove: Grove,
    val roundDeck: RoundDeck
) {
    private val _players = mutableListOf<Player>()

    val players: List<Player>
        get() = _players.toList()

    fun add(player: Player): Table {
        _players.add(player)
        return this
    }

    fun setup(config: TableConfig) {
        grove.reset()
        grove.resetDice(config.numPlayers)
        grove.setCards(config.cards)
        grove.resetWispDeck()
        roundDeck.setup(
            numBattle = config.numBattle,
            numCultivation = config.numCultivation
        )
    }
}
