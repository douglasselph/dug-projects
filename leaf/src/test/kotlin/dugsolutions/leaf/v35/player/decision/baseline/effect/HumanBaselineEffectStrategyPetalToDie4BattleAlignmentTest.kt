package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattlePetalToDie4Analyzer
import dugsolutions.leaf.v35.player.decision.baseline.card.CardPhase
import dugsolutions.leaf.v35.player.decision.baseline.card.CardScoringHelpers
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalToDie4Request
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.roundToInt

class HumanBaselineEffectStrategyPetalToDie4BattleAlignmentTest {
    private val actor = PlayerId(1)
    private val opponent = PlayerId(2)

    @Test
    fun `Petal prefers GainD4 when trash branch has larger raw pip movement but misses the tactical flip`() {
        val context = context(
            1,
            row(
                StrikeRow.TOP,
                player(actor, 9, die(0, 4, 4), die(1, 6, 5)),
                player(opponent, 10)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 1, die(2, 20, 1)),
                player(opponent, 20)
            ),
            row(
                StrikeRow.BOTTOM,
                player(actor, 1, die(3, 20, 1)),
                player(opponent, 20)
            )
        )
        val trash = PetalToDie4Choice.TrashD4AndRaiseAll(
            EffectDieChoice(index = 0, sides = 4, value = 4)
        )
        val analyzer = BattlePetalToDie4Analyzer()
        val gainAnalysis = requireNotNull(analyzer(context, PetalToDie4Choice.GainD4))
        val trashAnalysis = requireNotNull(analyzer(context, trash))

        // Trash removes 4, then gains 1 + 4 + 4 = 9: net +5 raw pips versus GainD4's +4.
        assertEquals(5.0, trashAnalysis.swing.rowSwings.sumOf { it.rawSwing })
        assertEquals(4.0, gainAnalysis.swing.rowSwings.sumOf { it.rawSwing })
        assertTrue(gainAnalysis.tacticalValue > trashAnalysis.tacticalValue)

        val chosen = HumanBaselineEffectStrategy().choosePetalToDie4(
            ChoosePetalToDie4Request(
                effect = GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4,
                legalChoices = listOf(trash, PetalToDie4Choice.GainD4),
                context = context
            )
        )

        assertEquals(PetalToDie4Choice.GainD4, chosen)
    }

    @Test
    fun `Petal top-level Battle score uses the same best complete branch realization`() {
        val context = context(
            1,
            row(
                StrikeRow.TOP,
                player(actor, 9, die(0, 4, 4), die(1, 6, 5)),
                player(opponent, 10)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 1, die(2, 20, 1)),
                player(opponent, 20)
            ),
            row(
                StrikeRow.BOTTOM,
                player(actor, 1, die(3, 20, 1)),
                player(opponent, 20)
            )
        )
        val analyzer = BattlePetalToDie4Analyzer()
        val best = analyzer.evaluateAll(context).maxBy { it.tacticalValue }

        val topLevel = CardScoringHelpers.playScore(
            context = context,
            phase = CardPhase.BATTLE,
            effect = GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4,
            cardName = "Flower_14_04",
            base = 65
        )

        assertEquals(65 + best.tacticalValue.roundToInt(), topLevel.total)
        assertEquals(PetalToDie4Choice.GainD4, best.realization)
    }

    private fun context(
        graftBedD4: Int = 0,
        vararg rows: BattleRowView
    ): DecisionContext {
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
            grove = DecisionContext.EMPTY.grove.copy(
                graftBed = if (graftBedD4 > 0) mapOf(DieSides.D4 to graftBedD4) else emptyMap()
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
            dieTotal = dice.sumOf { it.value },
            critterTotal = 0,
            total = total,
            withdrawn = false
        )

    private fun die(index: Int, sides: Int, value: Int) =
        BattleDieView(index, sides, value)
}
