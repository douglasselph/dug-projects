package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HumanBaselineEffectStrategyBattleSwapPairAlignmentTest {
    private val actor = PlayerId(1)
    private val opponent = PlayerId(2)

    @Test
    fun `optional Tulip swap prefers smaller face difference that improves the complete Battle`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 20, 1)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 15, die(1, 20, 10)), player(opponent, 10)),
            row(StrikeRow.BOTTOM, player(actor, 8, die(2, 8, 3)), player(opponent, 10))
        )
        val largeRawSwap = pair(
            source = EffectDieChoice(0, 20, 1),
            target = EffectDieChoice(1, 20, 10)
        )
        val smallerTacticalSwap = pair(
            source = EffectDieChoice(0, 20, 1),
            target = EffectDieChoice(2, 8, 3)
        )

        val chosen = HumanBaselineEffectStrategy().chooseOptionalDiePair(
            ChooseOptionalEffectDiePairRequest(
                effect = GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
                legalChoices = listOf(largeRawSwap, smallerTacticalSwap),
                context = context
            )
        )

        assertEquals(smallerTacticalSwap, chosen)
    }

    @Test
    fun `optional Tulip swap can decline when a large local gain merely sacrifices another won row`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 20, 1)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 15, die(1, 20, 10)), player(opponent, 10))
        )
        val harmfulSwap = pair(
            source = EffectDieChoice(0, 20, 1),
            target = EffectDieChoice(1, 20, 10)
        )

        val chosen = HumanBaselineEffectStrategy().chooseOptionalDiePair(
            ChooseOptionalEffectDiePairRequest(
                effect = GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
                legalChoices = listOf(harmfulSwap),
                context = context
            )
        )

        assertNull(chosen)
    }

    @Test
    fun `current Tulip chooses swap direction that preserves the other row after raising source plus two`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 20, 1)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 15, die(1, 8, 6)), player(opponent, 10))
        )
        val largerLocalTopGainButLosesMiddle = pair(
            source = EffectDieChoice(1, 8, 6),
            target = EffectDieChoice(0, 20, 1)
        )
        val smallerTopGainPreservesMiddle = pair(
            source = EffectDieChoice(0, 20, 1),
            target = EffectDieChoice(1, 8, 6)
        )

        val chosen = HumanBaselineEffectStrategy().chooseDiePair(
            ChooseEffectDiePairRequest(
                effect = GameEffect.DRAW_ONE_DIE_AND_SWAP_TWO_OWN_DICE_RAISE_ONE_PLUS_2_IN_BATTLE,
                legalChoices = listOf(largerLocalTopGainButLosesMiddle, smallerTopGainPreservesMiddle),
                context = context
            )
        )

        assertEquals(smallerTopGainPreservesMiddle, chosen)
    }

    private fun pair(source: EffectDieChoice, target: EffectDieChoice) =
        EffectDiePairChoice(source = source, target = target)

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
}
