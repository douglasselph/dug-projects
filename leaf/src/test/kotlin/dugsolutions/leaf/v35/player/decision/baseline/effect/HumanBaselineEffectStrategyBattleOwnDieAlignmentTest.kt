package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.baseline.scoring.BaselineScoreEngine
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.random.StrategyRandomizer
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyBattleOwnDieAlignmentTest {
    private val actor = PlayerId(1)
    private val opponent = PlayerId(2)

    @Test
    fun `plus four chooses smaller capped gain that flips a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 9, die(0, 6, 4)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 20, 1)), player(opponent, 10))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_4,
                context,
                EffectDieChoice(1, 20, 1), // raw +4, but remains a loss
                EffectDieChoice(0, 6, 4)   // capped raw +2, but flips the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `plus one chooses the die whose row flips from tied no-winner to win`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 5)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 4, die(1, 20, 4)), player(opponent, 10))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_ANY_DIE_PLUS_1,
                context,
                EffectDieChoice(1, 20, 4),
                EffectDieChoice(0, 6, 5)
            )
        )

        assertEquals(0, chosen.index)
    }


    @Test
    fun `plus three chooses capped smaller gain that wins a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 5)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 20, 1)), player(opponent, 10))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_3,
                context,
                EffectDieChoice(1, 20, 1), // raw +3, but remains a loss
                EffectDieChoice(0, 6, 5)   // capped raw +1, but wins the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `vine or flower count raise uses current creature count and Battle consequence`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 5)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 20, 1)), player(opponent, 10)),
            creature = listOf(creature(1, PlantType.VINE), creature(2, PlantType.FLOWER))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_VINE_OR_FLOWER,
                context,
                EffectDieChoice(1, 20, 1), // count 2 => raw +2, remains a loss
                EffectDieChoice(0, 6, 5)   // count 2 => capped +1, wins the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `root or vine count raise uses current creature count and Battle consequence`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 5)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 20, 1)), player(opponent, 10)),
            creature = listOf(creature(1, PlantType.ROOT), creature(2, PlantType.VINE))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_1_PER_ROOT_OR_VINE,
                context,
                EffectDieChoice(1, 20, 1), // count 2 => raw +2, remains a loss
                EffectDieChoice(0, 6, 5)   // count 2 => capped +1, wins the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `double chooses smaller raw gain that flips a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 9, die(0, 6, 3)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 5, die(1, 20, 5)), player(opponent, 20))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.DOUBLE_ONE_DIE,
                context,
                EffectDieChoice(1, 20, 5), // raw +5, but remains a loss
                EffectDieChoice(0, 6, 3)   // raw +3, but flips the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `opposite face chooses smaller positive change that flips a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 8, die(0, 6, 2)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 20, 1)), player(opponent, 25))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.FLIP_OWN_DIE_TO_OPPOSITE_FACE,
                context,
                EffectDieChoice(1, 20, 1), // raw +18, but remains a loss
                EffectDieChoice(0, 6, 2)   // raw +3, but flips the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `set eligible die to maximum chooses smaller raw gain that flips a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 9, die(0, 6, 4)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 12, 1)), player(opponent, 20))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.SET_DIE_UP_TO_D12_TO_MAX,
                context,
                EffectDieChoice(1, 12, 1), // raw +11, but remains a loss
                EffectDieChoice(0, 6, 4)   // raw +2, but flips the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `set lowest-value die to maximum chooses the tied-low die with better Battle consequence`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 6, die(0, 6, 1), die(1, 8, 5)),
                player(opponent, 10)
            ),
            row(StrikeRow.MIDDLE, player(actor, 1, die(2, 12, 1)), player(opponent, 20))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.SET_LOWEST_VALUE_DIE_TO_MAX,
                context,
                EffectDieChoice(2, 12, 1), // tied lowest, raw +11, remains a loss
                EffectDieChoice(0, 6, 1)   // tied lowest, raw +5, flips the Strike
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `set to one chooses larger raw sacrifice when it preserves the stronger Battle outcome`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 10, die(0, 6, 2), die(2, 4, 1)),
                player(opponent, 10)
            ),
            row(StrikeRow.MIDDLE, player(actor, 20, die(1, 8, 6)), player(opponent, 10))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE,
                context,
                EffectDieChoice(0, 6, 2), // raw loss 1, but turns a tie into a loss
                EffectDieChoice(1, 8, 6)  // raw loss 5, but still leaves the Strike won
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `set to one target comparison is tactical because VP reward is identical for every legal target`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 11, die(0, 6, 2), die(2, 4, 1)),
                player(opponent, 10)
            ),
            row(StrikeRow.MIDDLE, player(actor, 18, die(1, 8, 4)), player(opponent, 10))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.SET_DIE_SHOWING_2_PLUS_TO_1_AND_GAIN_VP_PER_ONE,
                context,
                EffectDieChoice(0, 6, 2), // both targets create the same extra showing-1 for VP
                EffectDieChoice(1, 8, 4)  // larger sacrifice, but does not surrender the TOP win
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `sapping snapdragon chooses row where opposing reduction completes the win`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 8, die(0, 6, 5)),
                player(opponent, 10, die(2, 6, 6), die(3, 6, 4))
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 8, die(1, 20, 1)),
                player(opponent, 10)
            )
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_IN_STRIKE_ROW,
                context,
                EffectDieChoice(1, 20, 1), // +2 alone reaches only a tie
                EffectDieChoice(0, 6, 5)   // capped own gain, but row drain flips the Strike to a win
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `bloom backflip avoids row where flipping a higher opponent die helps them`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 10, die(0, 6, 5)),
                player(opponent, 10, die(2, 20, 7))
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 9, die(1, 6, 5)),
                player(opponent, 10, die(3, 6, 6))
            )
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_1_AND_FLIP_HIGHER_OPPOSING_DICE_IN_STRIKE_ROW,
                context,
                EffectDieChoice(0, 6, 5), // +1 alone would win, but D20=7 flips up to 14
                EffectDieChoice(1, 6, 5)   // +1 reaches a tie and D6=6 is not higher after the raise
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `bursting blossom prefers target that creates an additional maximum die and Draw`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 5, die(0, 20, 5)), player(opponent, 20)),
            row(StrikeRow.MIDDLE, player(actor, 5, die(1, 6, 5)), player(opponent, 20)),
            supply = listOf(DieView(2, 4, 1))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE,
                context,
                EffectDieChoice(0, 20, 5),
                EffectDieChoice(1, 6, 5) // same +1 row swing, but creates a max and another Draw
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `root and scoot chooses plus one target by immediate Strike consequence without choosing withdrawal row early`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 5)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 2, die(1, 20, 2)), player(opponent, 10))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_1_AND_WITHDRAW_FROM_STRIKE_SQUARE,
                context,
                EffectDieChoice(1, 20, 2),
                EffectDieChoice(0, 6, 5) // +1 flips tied no-winner to a win
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `bursting blossom does not invent a Draw bonus when no die can be drawn`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 20, 5)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 5, die(1, 6, 5)), player(opponent, 20))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.RAISE_DIE_PLUS_1_AND_DRAW_ONE_PER_MAX_DIE,
                context,
                EffectDieChoice(1, 6, 5),
                EffectDieChoice(0, 20, 5) // wins TOP; no Supply/Discard means no extra Draw value
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `transplant tulip discards from safer row instead of simply discarding the lower die`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 10, die(0, 6, 5)),
                player(opponent, 10)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 30, die(1, 20, 10)),
                player(opponent, 10)
            ),
            supply = listOf(DieView(2, 4, 1))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.DISCARD_ONE_DIE_DRAW_ONE_AND_SWAP_TWO_OWN_DICE_IN_BATTLE,
                context,
                EffectDieChoice(0, 6, 5),  // lower die, but expected D4 replacement surrenders the tied row
                EffectDieChoice(1, 20, 10) // larger discard, but the expected replacement leaves a safe win
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `reap forced-row source choice uses expected replacement without choosing the later placement`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 10, die(0, 6, 5)),
                player(opponent, 10)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 30, die(1, 20, 10)),
                player(opponent, 10)
            ),
            supply = listOf(DieView(2, 4, 1), DieView(3, 4, 1))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.DISCARD_ONE_DIE_DRAW_TWO_AND_PLACE_DRAWN_DIE_IN_STRIKE_SQUARE,
                context,
                EffectDieChoice(0, 6, 5),  // expected forced replacement would surrender TOP
                EffectDieChoice(1, 20, 10) // expected forced replacement preserves the large MIDDLE lead
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `draw two source choice protects the tactically important row and leaves later placements fresh`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 10, die(0, 6, 2)),
                player(opponent, 10)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 30, die(1, 20, 8)),
                player(opponent, 10)
            ),
            supply = listOf(DieView(2, 4, 1), DieView(3, 4, 1))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.DISCARD_ONE_DIE_DRAW_TWO,
                context,
                EffectDieChoice(0, 6, 2), // numerically cheapest discard, but it breaks a tied Strike
                EffectDieChoice(1, 20, 8) // larger discard from a row that remains securely won
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `forget me not chooses discard die by expected rolled Battle placement rather than current face`() {
        val context = context(
            row(
                StrikeRow.TOP,
                player(actor, 5),
                player(opponent, 8)
            ),
            row(
                StrikeRow.MIDDLE,
                player(actor, 20),
                player(opponent, 10)
            ),
            discard = listOf(DieView(0, 4, 4), DieView(1, 20, 1))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND,
                context,
                EffectDieChoice(0, 4, 4),  // current face is high, but expected reroll is only 2.5
                EffectDieChoice(1, 20, 1)  // current face is low, but expected reroll can flip TOP
            )
        )

        assertEquals(1, chosen.index)
    }

    @Test
    fun `forget me not equal die sizes ignore current discard face and preserve StrategyRandomizer tie breaking`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = context(
            row(StrikeRow.TOP, player(actor, 5), player(opponent, 8)),
            discard = listOf(DieView(0, 8, 1), DieView(1, 8, 8))
        )

        val chosen = strategy.chooseDie(
            request(
                GameEffect.ROLL_DIE_FROM_DISCARD_INTO_HAND,
                context,
                EffectDieChoice(0, 8, 1),
                EffectDieChoice(1, 8, 8)
            )
        )

        assertEquals(1, chosen.index)
        assertEquals(listOf(2), randomizer.bounds)
    }

    @Test
    fun `reroll until three plus prefers smaller expected gain that flips a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 2)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 20, 1)), player(opponent, 15))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.REROLL_DIE_UNTIL_3_PLUS_IGNORE_ROLL_REWARDS,
                context,
                EffectDieChoice(1, 20, 1), // expected +10.5, but still loses the Strike
                EffectDieChoice(0, 6, 2)   // expected +2.5, but flips tied/no-winner to win
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `Root Recall Battle reroll prefers smaller expected gain that flips a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 2)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 20, 1)), player(opponent, 15))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
                context,
                EffectDieChoice(1, 20, 1), // expected +9.5, but still loses the Strike
                EffectDieChoice(0, 6, 2)   // expected +1.5, but flips tied/no-winner to win
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `Root Awakening chooses smaller expected upgrade gain that flips a Strike`() {
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 4, 2)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 1, die(1, 12, 1)), player(opponent, 20))
        )

        val chosen = HumanBaselineEffectStrategy().chooseDie(
            request(
                GameEffect.UPGRADE_DIE_AND_USE_NOW,
                context,
                EffectDieChoice(1, 12, 1), // D20 expected +9.5, but still loses the Strike
                EffectDieChoice(0, 4, 2)   // D6 expected +1.5, but flips tied/no-winner to win
            )
        )

        assertEquals(0, chosen.index)
    }

    @Test
    fun `Root Awakening exact expected tactical tie uses StrategyRandomizer`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = context(
            row(StrikeRow.TOP, player(actor, 9, die(0, 4, 1)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 9, die(1, 4, 1)), player(opponent, 10))
        )

        val chosen = strategy.chooseDie(
            request(
                GameEffect.UPGRADE_DIE_AND_USE_NOW,
                context,
                EffectDieChoice(0, 4, 1),
                EffectDieChoice(1, 4, 1)
            )
        )

        assertEquals(1, chosen.index)
        assertEquals(listOf(2), randomizer.bounds)
    }

    @Test
    fun `simple Battle reroll exact tactical tie uses StrategyRandomizer`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = context(
            row(StrikeRow.TOP, player(actor, 9, die(0, 8, 2)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 9, die(1, 8, 2)), player(opponent, 10))
        )

        val chosen = strategy.chooseDie(
            request(
                GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
                context,
                EffectDieChoice(0, 8, 2),
                EffectDieChoice(1, 8, 2)
            )
        )

        assertEquals(1, chosen.index)
        assertEquals(listOf(2), randomizer.bounds)
    }

    @Test
    fun `exact double Battle tie uses StrategyRandomizer`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = context(
            row(StrikeRow.TOP, player(actor, 9, die(0, 6, 3)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 9, die(1, 8, 3)), player(opponent, 10))
        )

        val chosen = strategy.chooseDie(
            request(
                GameEffect.DOUBLE_ONE_DIE,
                context,
                EffectDieChoice(0, 6, 3),
                EffectDieChoice(1, 8, 3)
            )
        )

        assertEquals(1, chosen.index)
        assertEquals(listOf(2), randomizer.bounds)
    }

    @Test
    fun `exact fixed raise Battle tie uses StrategyRandomizer`() {
        val randomizer = RecordingRandomizer(1)
        val strategy = HumanBaselineEffectStrategy(
            scoreEngine = BaselineScoreEngine(randomizer)
        )
        val context = context(
            row(StrikeRow.TOP, player(actor, 10, die(0, 6, 5)), player(opponent, 10)),
            row(StrikeRow.MIDDLE, player(actor, 10, die(1, 8, 5)), player(opponent, 10))
        )

        val chosen = strategy.chooseDie(
            request(
                GameEffect.RAISE_ANY_DIE_PLUS_1,
                context,
                EffectDieChoice(0, 6, 5),
                EffectDieChoice(1, 8, 5)
            )
        )

        assertEquals(1, chosen.index)
        assertEquals(listOf(2), randomizer.bounds)
    }

    private fun request(
        effect: GameEffect,
        context: DecisionContext,
        vararg choices: EffectDieChoice
    ) = ChooseEffectDieRequest(effect, choices.toList(), context)

    private fun context(
        vararg rows: BattleRowView,
        creature: List<CreatureCardView> = emptyList(),
        supply: List<DieView> = emptyList(),
        discard: List<DieView> = emptyList()
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
                    supply = supply,
                    hand = hand,
                    discard = discard,
                    creature = creature
                )
            ),
            opponents = listOf(
                OpponentView(board = DecisionContext.EMPTY.self.board.copy(id = opponent), wispCount = 0)
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


    private fun creature(id: Int, type: PlantType) = CreatureCardView(
        id = CreatureCardId(id),
        name = "Plant_$id",
        title = "Plant $id",
        type = type,
        cost = 5,
        effect = GameEffect.GAIN_ONE_VP,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = CreatureCard.Facing.FACE_UP,
        isSnippable = true
    )

    private class RecordingRandomizer(private val result: Int) : StrategyRandomizer {
        val bounds = mutableListOf<Int>()

        override fun nextInt(until: Int): Int {
            bounds += until
            return result
        }
    }
}
