package dugsolutions.leaf.v35.player.decision.baseline.wound

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.*
import dugsolutions.leaf.v35.player.decision.context.*
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineWoundStrategyTest {
    @Test
    fun `Flip sacrifices the Plant with the least immediate contribution to the visible Battle`() {
        val tacticallyUseful = creature(1, "Vine_07_01", "Berry Important", GameEffect.RAISE_ANY_DIE_PLUS_1, 7, PlantType.VINE)
        val currentlyQuiet = creature(2, "Flower_17_04", "Queen's Blossom", GameEffect.DRAW_TWO_DICE, 17, PlantType.FLOWER)
        val context = battleContext(listOf(view(tacticallyUseful), view(currentlyQuiet)))

        val chosen = HumanBaselineWoundStrategy().choose(
            ChooseWoundRequest(
                legalChoices = listOf(WoundChoice.Flip(tacticallyUseful), WoundChoice.Flip(currentlyQuiet)),
                context = context
            )
        )

        // Berry Important's +1 flips the visible TOP row from a loss to a win,
        // so the much more expensive Queen's Blossom is the less useful card right now.
        assertEquals(currentlyQuiet.id, chosen.card.id)
    }

    @Test
    fun `Snip sacrifices the legal outer Plant with the lowest permanent preservation value`() {
        val root = creature(1, "Root_05_01", "Double Down", GameEffect.DOUBLE_ONE_DIE, 5, PlantType.ROOT, facing = CreatureCard.Facing.FACE_DOWN)
        val flower = creature(2, "Flower_17_04", "Queen's Blossom", GameEffect.DRAW_TWO_DICE, 17, PlantType.FLOWER, facing = CreatureCard.Facing.FACE_DOWN)
        val context = context(listOf(view(root), view(flower)))

        val chosen = HumanBaselineWoundStrategy().choose(
            ChooseWoundRequest(
                legalChoices = listOf(WoundChoice.Snip(flower), WoundChoice.Snip(root)),
                context = context
            )
        )

        assertEquals(root.id, chosen.card.id)
    }

    @Test
    fun `strategy chooses only among legal Wound choices supplied by the engine`() {
        val legal = creature(1, "Root_05_01", "Double Down", GameEffect.DOUBLE_ONE_DIE, 5, PlantType.ROOT, facing = CreatureCard.Facing.FACE_DOWN)
        val illegalInterior = creature(2, "Flower_17_04", "Queen's Blossom", GameEffect.DRAW_TWO_DICE, 17, PlantType.FLOWER, facing = CreatureCard.Facing.FACE_DOWN)
        val context = context(listOf(view(legal), view(illegalInterior, isSnippable = false)))

        val chosen = HumanBaselineWoundStrategy().choose(
            ChooseWoundRequest(listOf(WoundChoice.Snip(legal)), context)
        )

        assertEquals(legal.id, chosen.card.id)
    }

    private fun battleContext(creature: List<CreatureCardView>): DecisionContext {
        val die = BattleDieView(handIndex = 0, sides = 6, value = 4)
        return context(creature).copy(
            phase = dugsolutions.leaf.v35.round.domain.RoundCardType.BATTLE,
            self = context(creature).self.copy(
                board = context(creature).self.board.copy(hand = listOf(DieView(0, 6, 4)))
            ),
            battle = BattleView(
                playerOrder = listOf(ACTOR, OPPONENT),
                rows = listOf(
                    BattleRowView(
                        row = StrikeRow.TOP,
                        closed = false,
                        players = listOf(
                            BattlePlayerRowView(ACTOR, StrikeRow.TOP, listOf(die), emptyList(), 4, 0, 4, false),
                            BattlePlayerRowView(OPPONENT, StrikeRow.TOP, emptyList(), emptyList(), 5, 0, 5, false)
                        )
                    )
                )
            )
        )
    }

    private fun context(creature: List<CreatureCardView>) = DecisionContext.EMPTY.copy(
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(id = ACTOR, creature = creature)
        )
    )

    private fun creature(
        id: Int,
        name: String,
        title: String,
        effect: GameEffect,
        cost: Int,
        type: PlantType,
        facing: CreatureCard.Facing = CreatureCard.Facing.FACE_UP
    ) = CreatureCard(
        CreatureCardId(id),
        PlantCard(6, name, title, type, cost, null, "", "", "", "", "", "", "", effect, PlantScoringRule.Fixed(1)),
        CreatureSide.LEFT,
        CreaturePosition(-id, 0),
        facing
    )

    private fun view(card: CreatureCard, isSnippable: Boolean = true) = CreatureCardView(
        card.id, card.card.name, card.card.title, card.card.type, card.card.cost,
        card.card.effect, card.card.scoringRule, card.side, card.position, card.facing, isSnippable
    )

    private companion object {
        val ACTOR = PlayerId(0)
        val OPPONENT = PlayerId(1)
    }
}
