package dugsolutions.leaf.v35.player.decision.baseline.effect

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantScoringRule
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectOpponentPlantWoundRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HumanBaselineEffectStrategyPlantAlignmentTest {
    @Test
    fun `Shift Happens avoids refreshing a spent opponent Plant with strong immediate Battle value`() {
        val dangerousSpent = plant(1, "Root_05_02", GameEffect.RAISE_DIE_PLUS_4, faceUp = false)
        val expendableFaceUp = plant(2, "Root_07_02", GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND, faceUp = true)
        val context = battleContext(
            actorTotal = 10,
            opponentTotal = 6,
            opponentDie = BattleDieView(0, 6, 2),
            opponentCreature = listOf(dangerousSpent, expendableFaceUp)
        )

        val chosen = HumanBaselineEffectStrategy().chooseOpponentPlantWound(
            ChooseEffectOpponentPlantWoundRequest(
                effect = GameEffect.FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE,
                legalChoices = listOf(
                    EffectOpponentPlantWoundChoice.Flip(OPPONENT, dangerousSpent.id, dangerousSpent.name),
                    EffectOpponentPlantWoundChoice.Flip(OPPONENT, expendableFaceUp.id, expendableFaceUp.name)
                ),
                context = context
            )
        )

        assertEquals(expendableFaceUp.id, chosen.cardId)
    }

    @Test
    fun `Parting Thorn wounds opponent Plant whose immediate Battle use can recover the Strike`() {
        val first = plant(1, "Root_07_02", GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND, faceUp = true)
        val tactical = plant(2, "Root_05_02", GameEffect.RAISE_DIE_PLUS_4, faceUp = true)
        val context = battleContext(10, 6, BattleDieView(0, 6, 2), listOf(first, tactical))

        val chosen = HumanBaselineEffectStrategy().chooseOpponentPlantWound(
            ChooseEffectOpponentPlantWoundRequest(
                effect = GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE,
                legalChoices = listOf(
                    EffectOpponentPlantWoundChoice.Flip(OPPONENT, first.id, first.name),
                    EffectOpponentPlantWoundChoice.Flip(OPPONENT, tactical.id, tactical.name)
                ),
                context = context
            )
        )

        assertEquals(tactical.id, chosen.cardId)
    }

    @Test
    fun `Snip Happens target selection also sees immediate Battle Plant value`() {
        val first = plant(1, "Root_07_02", GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND, faceUp = true)
        val tactical = plant(2, "Root_05_02", GameEffect.RAISE_DIE_PLUS_4, faceUp = true)
        val context = battleContext(10, 6, BattleDieView(0, 6, 2), listOf(first, tactical))

        val chosen = HumanBaselineEffectStrategy().chooseOpponentPlantWound(
            ChooseEffectOpponentPlantWoundRequest(
                effect = GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE,
                legalChoices = listOf(
                    EffectOpponentPlantWoundChoice.Flip(OPPONENT, first.id, first.name),
                    EffectOpponentPlantWoundChoice.Flip(OPPONENT, tactical.id, tactical.name)
                ),
                context = context
            )
        )

        assertEquals(tactical.id, chosen.cardId)
    }

    @Test
    fun `Vine and Again reuses spent Plant with best immediate Battle realization rather than first legal`() {
        val first = plant(1, "Root_07_02", GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND, faceUp = false)
        val tactical = plant(2, "Root_05_02", GameEffect.RAISE_DIE_PLUS_4, faceUp = false)
        val context = battleContext(
            actorTotal = 6,
            opponentTotal = 10,
            actorDie = BattleDieView(0, 6, 2),
            selfCreature = listOf(first, tactical)
        )

        val chosen = HumanBaselineEffectStrategy().choosePlantEffect(
            ChooseEffectPlantRequest(
                effect = GameEffect.REUSE_SPENT_ROOT_OR_VINE_EFFECT,
                legalChoices = listOf(
                    EffectPlantChoice(first.id, first.name, isFaceUp = false),
                    EffectPlantChoice(tactical.id, tactical.name, isFaceUp = false)
                ),
                context = context
            )
        )

        assertEquals(tactical.id, chosen.cardId)
    }

    private fun battleContext(
        actorTotal: Int,
        opponentTotal: Int,
        opponentDie: BattleDieView? = null,
        actorDie: BattleDieView? = null,
        opponentCreature: List<CreatureCardView> = emptyList(),
        selfCreature: List<CreatureCardView> = emptyList()
    ): DecisionContext = DecisionContext.EMPTY.copy(
        phase = RoundCardType.BATTLE,
        self = DecisionContext.EMPTY.self.copy(
            board = DecisionContext.EMPTY.self.board.copy(
                id = ACTOR,
                creature = selfCreature
            )
        ),
        opponents = listOf(
            OpponentView(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = OPPONENT,
                    creature = opponentCreature
                ),
                wispCount = 0
            )
        ),
        battle = BattleView(
            playerOrder = listOf(ACTOR, OPPONENT),
            rows = listOf(
                BattleRowView(
                    row = StrikeRow.TOP,
                    closed = false,
                    players = listOf(
                        BattlePlayerRowView(
                            playerId = ACTOR,
                            row = StrikeRow.TOP,
                            dice = listOfNotNull(actorDie),
                            critters = emptyList(),
                            dieTotal = actorDie?.value ?: actorTotal,
                            critterTotal = actorTotal - (actorDie?.value ?: actorTotal),
                            total = actorTotal,
                            withdrawn = false
                        ),
                        BattlePlayerRowView(
                            playerId = OPPONENT,
                            row = StrikeRow.TOP,
                            dice = listOfNotNull(opponentDie),
                            critters = emptyList(),
                            dieTotal = opponentDie?.value ?: opponentTotal,
                            critterTotal = opponentTotal - (opponentDie?.value ?: opponentTotal),
                            total = opponentTotal,
                            withdrawn = false
                        )
                    )
                )
            )
        )
    )

    private fun plant(
        id: Int,
        name: String,
        effect: GameEffect,
        faceUp: Boolean
    ) = CreatureCardView(
        id = CreatureCardId(id),
        name = name,
        title = name,
        type = PlantType.ROOT,
        cost = 7,
        effect = effect,
        scoringRule = PlantScoringRule.Fixed(1),
        side = CreatureSide.LEFT,
        position = CreaturePosition(-id, 0),
        facing = if (faceUp) CreatureCard.Facing.FACE_UP else CreatureCard.Facing.FACE_DOWN,
        isSnippable = true
    )

    private companion object {
        val ACTOR = PlayerId(0)
        val OPPONENT = PlayerId(1)
    }
}
