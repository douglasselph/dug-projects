package dugsolutions.leaf.v30.game.round

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.round.domain.RoundCard
import dugsolutions.leaf.v30.table.Table

class RoundBattle(
    table: Table,
    card: RoundCard,
    chronicle: Chronicle = GameChronicle()
) : RoundBase(table, card, chronicle)
