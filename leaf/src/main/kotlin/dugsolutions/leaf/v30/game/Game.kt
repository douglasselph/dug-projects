package dugsolutions.leaf.v30.game

import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.game.effect.GameCardEffectExecutor
import dugsolutions.leaf.v30.game.effect.RoundActionExecutor
import dugsolutions.leaf.v30.game.effect.WispCardEffectExecutor
import dugsolutions.leaf.v30.game.round.RoundBase
import dugsolutions.leaf.v30.game.round.RoundBattle
import dugsolutions.leaf.v30.game.round.RoundCultivation
import dugsolutions.leaf.v30.round.domain.RoundCardType
import dugsolutions.leaf.v30.table.Table
import dugsolutions.leaf.v30.table.domain.TableConfig

class Game(
    private val table: Table,
    private val chronicle: Chronicle = GameChronicle(),
    private val roundActionExecutor: RoundActionExecutor = RoundActionExecutor(),
    private val gameCardEffectExecutor: GameCardEffectExecutor = GameCardEffectExecutor(),
    private val wispCardEffectExecutor: WispCardEffectExecutor = WispCardEffectExecutor()
) {
    fun setup(config: TableConfig) {
        table.setup(config)
    }

    fun run(): RoundBase? {
        val card = table.roundDeck.next() ?: return null
        chronicle(Moment.RoundRevealed(card))
        val round = when (card.cardType) {
            RoundCardType.BATTLE -> RoundBattle(
                table = table,
                card = card,
                chronicle = chronicle,
                gameCardEffectExecutor = gameCardEffectExecutor,
                wispCardEffectExecutor = wispCardEffectExecutor
            )
            RoundCardType.CULTIVATION -> RoundCultivation(
                table = table,
                card = card,
                chronicle = chronicle,
                roundActionExecutor = roundActionExecutor,
                gameCardEffectExecutor = gameCardEffectExecutor,
                wispCardEffectExecutor = wispCardEffectExecutor
            )
        }
        round.drawDice()
        round.rollDice()

        if (round is RoundCultivation) {
            round.performMainActions()
            round.performBuy()
        } else if (round is RoundBattle) {
            round.prepare()
            round.performMainActions()
            round.performSupportActions()
            round.resolve()
        }
        round.cleanup()
        return round
    }
}
