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
        creature: List<CreatureCardView> = emptyList()
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
                    hand = hand,
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
