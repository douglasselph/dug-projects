package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * C04 reviewed preferences, from the author's 36-case blind review.
 *
 * These are REAL, explicitly populated three-row decision views: every player
 * has >= 3 placed dice, with no invented enemy row totals. The Main scoring
 * entry point is production BattleMainPriority. Legality through the complete
 * game coordinator has NOT been proven; see doc/C04_REVIEWED_ACCEPTANCE.md.
 *
 * Preferences are diagnostic until the actual legal-choice generator confirms
 * the described candidate AND target; otherwise a Plant may choose another die.
 * Other cases are diagnostic: they print the disagreement for calibration,
 * without silently rewriting the author's judgment as a passing assertion.
 */
class C04ReviewedPreferenceAcceptanceTest {
    private val a = PlayerId(0)
    private val b = PlayerId(1)
    private val c = PlayerId(2)
    private val d = PlayerId(3)
    private val root = CreatureCard(
        id = CreatureCardId(641),
        card = PlantCard(6, "Root_05_02", "Root Four More", PlantType.ROOT, 5,
            null, "", "", "", "", "", "", "", GameEffect.RAISE_DIE_PLUS_4,
            PlantScoringRule.Fixed(1)),
        side = CreatureSide.LEFT, position = CreaturePosition(-1, 0),
        facing = CreatureCard.Facing.FACE_UP
    )
    private val round = RoundCard(
        quantity = 1, name = "C04 reviewed preference", type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("VP", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("Wisp", "", "", "", null, GameEffect.GAIN_ONE_WISP),
        backImage = ""
    )
    private data class Positioned(val side: Int, val face: Int)
    private fun die(sides: Int, face: Int) = Positioned(sides, face)
    private fun state(
        players: List<PlayerId>,
        top: List<List<Positioned>>,
        middle: List<List<Positioned>>,
        bottom: List<List<Positioned>>,
        drawSides: Int,
        wisps: Int = 3,
        bees: Int = 3,
        worms: Int = 3
    ): DecisionContext {
        require(players.size in 2..4)
        val positions = listOf(top, middle, bottom)
        require(positions.all { row -> row.size == players.size && row.all { it.size <= 3 } })
        require(players.indices.all { i -> positions.sumOf { row -> row[i].size } >= 3 })
        val rows = listOf(StrikeRow.TOP, StrikeRow.MIDDLE, StrikeRow.BOTTOM).mapIndexed { index, strike ->
            BattleRowView(strike, false, players.mapIndexed { playerIndex, player ->
                val dice = positions[index][playerIndex].mapIndexed { dieIndex, value ->
                    BattleDieView(index * 3 + dieIndex, value.side, value.face)
                }
                val total = dice.sumOf { it.value }
                BattlePlayerRowView(player, strike, dice, emptyList(), total, 0, total, false)
            })
        }
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            grove = DecisionContext.EMPTY.grove.copy(wispDeckRemaining = wisps, bees = bees, worms = worms),
            self = DecisionContext.EMPTY.self.copy(board = DecisionContext.EMPTY.self.board.copy(
                id = a, supply = listOf(DieView(90, drawSides, 1))
            )),
            battle = BattleView(playerOrder = players, rows = rows)
        )
    }
    private fun compare(
        id: String, context: DecisionContext, first: BattleMainAction,
        second: BattleMainAction, human: String, confidence: Int, enforce: Boolean
    ) {
        val x = BattleMainPriority.score(context, round, first).total
        val y = BattleMainPriority.score(context, round, second).total
        val actual = when {
            x > y -> "A"
            y > x -> "B"
            else -> "TIE"
        }
        println("C04_REVIEW|$id|human=$human|confidence=$confidence|baseline=$actual|A=$x|B=$y|agree=${human == actual}")
        if (enforce) assertTrue(actual == human,
            "$id: high-confidence human preference $human; baseline $actual (A=$x, B=$y)")
    }

    @Test fun `V3-02 capped Root cannot beat D10 Draw`() {
        val s = state(listOf(a,b,c),
            top = listOf(listOf(die(6,6)), listOf(die(10,7)), listOf(die(12,4))),
            middle = listOf(listOf(die(10,5),die(6,2)), listOf(die(6,4)), listOf(die(8,3))),
            bottom = listOf(listOf(die(8,4)), listOf(die(12,9),die(6,4)), listOf(die(10,7))),
            drawSides = 10)
        compare("V3-02", s, BattleMainAction.ActivatePlant(root), BattleMainAction.Draw,
            human = "B", confidence = 5, enforce = false)
    }

    @Test fun `V3-05 nearly capped Raise versus reward-bearing D4`() {
        val s = state(listOf(a,b),
            top = listOf(listOf(die(8,7)), listOf(die(8,4),die(8,6))),
            middle = listOf(listOf(die(12,7),die(6,5)), listOf(die(8,5))),
            bottom = listOf(listOf(die(10,4)), listOf(die(10,7),die(6,1))),
            drawSides = 4)
        // Reviewed B confidence 5; the Plant candidate can select a different
        // own die, so this is NOT yet a validated forced-TOP-target comparison.
        compare("V3-05", s, BattleMainAction.ActivatePlant(root), BattleMainAction.Draw,
            human = "B", confidence = 5, enforce = false)
    }

    @Test fun `V3-13 Wisp acquisition versus contested D8 Draw is measured`() {
        val s = state(listOf(a,b),
            top = listOf(listOf(die(8,5)), listOf(die(12,7))),
            middle = listOf(listOf(die(8,6)), listOf(die(8,5),die(6,3))),
            bottom = listOf(listOf(die(10,8)), listOf(die(8,2))),
            drawSides = 8, wisps = 4)
        compare("V3-13", s, BattleMainAction.RoundEffect2, BattleMainAction.Draw,
            human = "B", confidence = 4, enforce = false)
    }

    @Test fun `V3-18 immediate VP versus contested D8 Draw is measured`() {
        val s = state(listOf(a,b,c,d),
            top = listOf(listOf(die(12,4)), listOf(die(6,2)), listOf(die(10,7)), listOf(die(12,4),die(8,5))),
            middle = listOf(listOf(die(8,3)), listOf(die(10,5)), listOf(die(6,4),die(6,4)), listOf(die(8,3))),
            bottom = listOf(listOf(die(10,7),die(6,6)), listOf(die(8,4)), listOf(die(12,9)), listOf(die(10,7),die(6,6))),
            drawSides = 8)
        compare("V3-18", s, BattleMainAction.RoundEffect1, BattleMainAction.Draw,
            human = "B", confidence = 4, enforce = false)
    }

}
