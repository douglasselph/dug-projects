package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseRootWellBattleRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBattleDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.RootWellBattleChoice
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyRootWellBattleAlignmentTest {
    private val actor = PlayerId(1)
    private val opponent = PlayerId(2)

    @Test
    fun `Root Well chooses opponent reroll when smaller raw movement flips the Strike`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 19),
                player(opponent, 20, die(4, 20, 20))
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 1, die(0, 20, 1), die(1, 12, 1)),
                player(opponent, 25)
            )
        )
        val ownTwo = RootWellBattleChoice.OwnDice(
            battleChoice(actor, StrikeRow.MIDDLE, 0, 20, 1),
            battleChoice(actor, StrikeRow.MIDDLE, 1, 12, 1)
        ) // expected own gain +15, but still loses
        val opponentOne = RootWellBattleChoice.OpponentDie(
            battleChoice(opponent, StrikeRow.TOP, 4, 20, 20)
        ) // expected opponent change -9.5, but flips TOP to a win

        val chosen = HumanBaselineEffectStrategy().chooseRootWellBattle(
            request(context, ownTwo, opponentOne)
        )

        assertEquals(opponentOne, chosen)
    }

    @Test
    fun `Root Well chooses own pair when smaller total expected gain flips the Strike`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 9, die(0, 6, 1), die(1, 6, 1)),
                player(opponent, 10)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 1),
                player(opponent, 20, die(4, 20, 20))
            )
        )
        val opponentOne = RootWellBattleChoice.OpponentDie(
            battleChoice(opponent, StrikeRow.MIDDLE, 4, 20, 20)
        ) // expected opponent reduction 9.5, but actor still loses badly
        val ownTwo = RootWellBattleChoice.OwnDice(
            battleChoice(actor, StrikeRow.TOP, 0, 6, 1),
            battleChoice(actor, StrikeRow.TOP, 1, 6, 1)
        ) // expected own gain +5 flips TOP to a win

        val chosen = HumanBaselineEffectStrategy().chooseRootWellBattle(
            request(context, opponentOne, ownTwo)
        )

        assertEquals(ownTwo, chosen)
    }

    @Test
    fun `Root Well exact expected Battle tie uses StrategyRandomizer`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 9, die(0, 6, 1), die(1, 6, 1)),
                player(opponent, 10)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 9, die(2, 6, 1), die(3, 6, 1)),
                player(opponent, 10)
            )
        )
        val first = RootWellBattleChoice.OwnDice(
            battleChoice(actor, StrikeRow.TOP, 0, 6, 1),
            battleChoice(actor, StrikeRow.TOP, 1, 6, 1)
        )
        val second = RootWellBattleChoice.OwnDice(
            battleChoice(actor, StrikeRow.MIDDLE, 2, 6, 1),
            battleChoice(actor, StrikeRow.MIDDLE, 3, 6, 1)
        )

        val chosen = strategy.chooseRootWellBattle(request(context, first, second))

        assertEquals(second, chosen)
        assertEquals(listOf(2), randomizer.bounds)
    }

    private fun request(
        context: DecisionContext,
        vararg choices: RootWellBattleChoice
    ) = ChooseRootWellBattleRequest(
        effect = GameEffect.GAIN_WATER_AND_SPEND_1_TO_REROLL_TWO_OWN_OR_ONE_OPPONENT_BATTLE_DIE,
        legalChoices = choices.toList(),
        context = context
    )

    private fun battleChoice(
        ownerId: PlayerId,
        row: StrikeRow,
        index: Int,
        sides: Int,
        value: Int
    ) = EffectBattleDieChoice(
        ownerId = ownerId,
        row = row,
        die = EffectDieChoice(index, sides, value)
    )

    private fun context(vararg rows: BattleRowView): DecisionContext {
        val hand = rows.flatMap { it.forPlayer(actor)?.dice.orEmpty() }
            .associateBy { it.handIndex }
            .values
            .sortedBy { it.handIndex }
            .map { DieView(it.handIndex, it.sides, it.value) }
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = actor,
                    hand = hand
                )
            ),
            opponents = listOf(
                OpponentView(
                    board = DecisionContext.EMPTY.self.board.copy(id = opponent),
                    wispCount = 0
                )
            ),
            battle = BattleView(
                playerOrder = listOf(actor, opponent),
                rows = rows.toList()
            )
        )
    }

    private fun row(row: StrikeRow, vararg players: BattlePlayerRowView) =
        BattleRowView(row, false, players.map { it.copy(row = row) })

    private fun player(id: PlayerId, total: Int, vararg dice: BattleDieView) =
        BattlePlayerRowView(
            playerId = id,
            row = StrikeRow.TOP,
            dice = dice.toList(),
            critters = emptyList(),
            dieTotal = total,
            critterTotal = 0,
            total = total,
            withdrawn = false
        )

    private fun die(index: Int, sides: Int, value: Int) =
        BattleDieView(index, sides, value)

    private class RecordingRandomizer(private val result: Int) : StrategyRandomizer {
        val bounds = mutableListOf<Int>()

        override fun nextInt(until: Int): Int {
            bounds += until
            return result
        }
    }
}
