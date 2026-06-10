package dugsolutions.leaf.v30.game

import dugsolutions.leaf.v30.game.round.RoundBase
import dugsolutions.leaf.v30.game.round.RoundBattle
import dugsolutions.leaf.v30.game.round.RoundCultivation
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.table.domain.TableConfig

class Game(
    private val table: Table
) {
    fun setup(config: TableConfig) {
        table.setup(config)
    }

    fun run(): RoundBase? {
        val card = table.roundDeck.pull() ?: return null
        val round = when (card.cardType) {
            RoundCardType.BATTLE -> RoundBattle(table, card)
            RoundCardType.CULTIVATION -> RoundCultivation(table, card)
        }
        round.run()
        return round
    }
}
