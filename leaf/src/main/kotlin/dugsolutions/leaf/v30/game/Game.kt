package dugsolutions.leaf.v30.game

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.game.round.RoundBase
import dugsolutions.leaf.v30.game.round.RoundBattle
import dugsolutions.leaf.v30.game.round.RoundCultivation
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.table.domain.TableConfig

class Game(
    private val table: Table,
    private val chronicle: Chronicle = GameChronicle()
) {
    fun setup(config: TableConfig) {
        table.setup(config)
    }

    fun run(): RoundBase? {
        val card = table.roundDeck.pull() ?: return null
        val round = when (card.cardType) {
            RoundCardType.BATTLE -> RoundBattle(table, card, chronicle)
            RoundCardType.CULTIVATION -> RoundCultivation(table, card, chronicle)
        }
        round.run()
        return round
    }
}
