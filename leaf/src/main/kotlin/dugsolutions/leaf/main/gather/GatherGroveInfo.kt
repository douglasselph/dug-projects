package dugsolutions.leaf.main.gather

import dugsolutions.leaf.cards.domain.GameCard
import dugsolutions.leaf.game.battle.MatchingBloomCard
import dugsolutions.leaf.random.die.Die
import dugsolutions.leaf.game.domain.GamePhase
import dugsolutions.leaf.game.domain.GameTime
import dugsolutions.leaf.game.turn.select.SelectAllDice
import dugsolutions.leaf.grove.Grove
import dugsolutions.leaf.grove.domain.MarketStackID
import dugsolutions.leaf.grove.domain.MarketStackType
import dugsolutions.leaf.main.domain.CardInfo
import dugsolutions.leaf.main.domain.DiceInfo
import dugsolutions.leaf.main.domain.DieInfo
import dugsolutions.leaf.main.domain.GroveInfo
import dugsolutions.leaf.main.domain.HighlightInfo
import dugsolutions.leaf.main.domain.CardStackInfo
import dugsolutions.leaf.player.Player
import kotlinx.coroutines.flow.flow

class GatherGroveInfo(
    private val grove: Grove,
    private val gatherCardInfo: GatherCardInfo,
    private val selectAllDice: SelectAllDice,
    private val bestMatchingBloomCard: MatchingBloomCard,
    private val gameTime: GameTime
) {

    operator fun invoke(
        highlightCard: List<GameCard> = emptyList(),
        highlightDie: List<Die> = emptyList(),
        selectForPlayer: Player? = null
    ): GroveInfo? {
        if (gameTime.phase == GamePhase.BATTLE) {
            return null
        }
        return GroveInfo(
            stacks = MarketStackID.entries.mapIndexed { index, stack -> get(index, stack, highlightCard) },
            blooms = MarketStackID.entries.filter { it.type == MarketStackType.FLOWER }.mapIndexedNotNull { index, stack -> bloomFor(index, stack) },
            dice = get(highlightDie),
            instruction = selectForPlayer?.let { it.name + " PIPS " + it.pipTotal },
            quantities = selectAllDice().toString()
        )
    }

    private fun get(index: Int, stack: MarketStackID, highlight: List<GameCard> = emptyList()): CardStackInfo {
        val card = grove.getCardsFor(stack)?.getCard(0)
        val highlightCard = if (highlight.any { it.id == card?.id }) HighlightInfo.SELECTABLE else HighlightInfo.NONE
        return CardStackInfo(
            stack = stack,
            topCard = card?.let { gatherCardInfo(index = index, card = it, highlight = highlightCard) },
            numCards = grove.getCardsFor(stack)?.size ?: 0
        )
    }

    private fun get(highlightDie: List<Die>): DiceInfo {
        if (highlightDie.isEmpty()) {
            return DiceInfo()
        }
        return DiceInfo(
            values = highlightDie.mapIndexed { index, die ->
                DieInfo(
                    index,
                    value = die.sides.toString(),
                    highlight = HighlightInfo.SELECTABLE,
                    backingDie = die
                )
            }
        )
    }

    private fun bloomFor(index: Int, stack: MarketStackID): CardInfo? {
        val flowerCard = grove.getCardsFor(stack)?.getCard(0) ?: return null
        bestMatchingBloomCard(flowerCard)?.let { bloomCard ->
            return gatherCardInfo(index = index, card = bloomCard)
        }
        return null
    }
}
