package dugsolutions.leaf.v35.player.decision.baseline.effect

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
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleMainPriority
import dugsolutions.leaf.v35.player.decision.baseline.battle.BattleOEdelweissAnalyzer
import dugsolutions.leaf.v35.player.decision.context.BattleDieView
import dugsolutions.leaf.v35.player.decision.context.BattlePlayerRowView
import dugsolutions.leaf.v35.player.decision.context.BattleRowView
import dugsolutions.leaf.v35.player.decision.context.BattleView
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.DieView
import dugsolutions.leaf.v35.player.decision.effect.ChooseOEdelweissRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HumanBaselineEffectStrategyOEdelweissBattleAlignmentTest {
    @Test
    fun `O Edelweiss replays spent Plant whose immediate Battle use flips the Strike`() {
        val weak = plant(
            id = 1,
            name = "Root_07_02",
            effect = GameEffect.GAIN_WORM_AND_BOOST_WORMS_THIS_ROUND,
            faceUp = false
        )
        val tactical = plant(
            id = 2,
            name = "Root_05_02",
            effect = GameEffect.RAISE_DIE_PLUS_4,
            faceUp = false
        )
        val context = battleContext(listOf(weak, tactical))

        val chosen = HumanBaselineEffectStrategy().chooseOEdelweiss(
            ChooseOEdelweissRequest(
                effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                choiceNumber = 1,
                legalChoices = listOf(
                    OEdelweissChoice.Play(weak.toChoice()),
                    OEdelweissChoice.Play(tactical.toChoice()),
                    OEdelweissChoice.Done
                ),
                context = context
            )
        )

        val play = chosen as OEdelweissChoice.Play
        assertEquals(tactical.id, play.card.cardId)
    }

    @Test
    fun `O Edelweiss Flip enables strong spent Battle Main but refuses to disable it once face up`() {
        val spent = plant(
            id = 2,
            name = "Root_05_02",
            effect = GameEffect.RAISE_DIE_PLUS_4,
            faceUp = false
        )
        val spentContext = battleContext(listOf(spent))
        val flipUp = OEdelweissChoice.Flip(spent.toChoice())

        assertEquals(
            flipUp,
            HumanBaselineEffectStrategy().chooseOEdelweiss(
                ChooseOEdelweissRequest(
                    effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                    choiceNumber = 1,
                    legalChoices = listOf(OEdelweissChoice.Done, flipUp),
                    context = spentContext
                )
            )
        )

        val ready = spent.copy(facing = CreatureCard.Facing.FACE_UP)
        val readyContext = battleContext(listOf(ready))
        val flipDown = OEdelweissChoice.Flip(ready.toChoice())

        assertEquals(
            OEdelweissChoice.Done,
            HumanBaselineEffectStrategy().chooseOEdelweiss(
                ChooseOEdelweissRequest(
                    effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                    choiceNumber = 2,
                    legalChoices = listOf(flipDown, OEdelweissChoice.Done),
                    context = readyContext
                )
            )
        )
    }

    @Test
    fun `O Edelweiss top-level Battle score uses the same current one-step opportunity`() {
        val sourceId = CreatureCardId(10)
        val sourceView = plant(
            id = sourceId.value,
            name = "Flower_17_03",
            effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
            faceUp = true,
            type = PlantType.FLOWER,
            cost = 17
        )
        val tactical = plant(
            id = 2,
            name = "Root_05_02",
            effect = GameEffect.RAISE_DIE_PLUS_4,
            faceUp = false
        )
        val context = battleContext(listOf(sourceView, tactical))
        val analyzer = BattleOEdelweissAnalyzer()
        val bestCurrentChoice = requireNotNull(analyzer.topLevelPriority(context, sourceId))
        assertTrue(bestCurrentChoice.total > 35)

        val source = CreatureCard(
            id = sourceId,
            card = PlantCard(
                quantity = 6,
                name = "Flower_17_03",
                title = "O Edelweiss",
                type = PlantType.FLOWER,
                cost = 17,
                lineIcon = null,
                vpIcon = "",
                typeIcon = "",
                fgColor = "",
                textColor = "",
                fullImage = "",
                backgroundImage = "",
                cardBackgroundImage = "",
                effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                scoringRule = PlantScoringRule.Fixed(1)
            ),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 1),
            facing = CreatureCard.Facing.FACE_UP
        )

        val topLevel = BattleMainPriority.intrinsicScore(
            context = context,
            roundCard = round(),
            action = BattleMainAction.ActivatePlant(source)
        )

        assertEquals(bestCurrentChoice.total, topLevel.total)
    }

    @Test
    fun `O Edelweiss top-level Battle score is only Done when current first step would disable a ready Plant`() {
        val sourceId = CreatureCardId(10)
        val sourceView = plant(
            id = sourceId.value,
            name = "Flower_17_03",
            effect = GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
            faceUp = true,
            type = PlantType.FLOWER,
            cost = 17
        )
        val ready = plant(
            id = 2,
            name = "Root_05_02",
            effect = GameEffect.RAISE_DIE_PLUS_4,
            faceUp = true
        )
        val context = battleContext(listOf(sourceView, ready))
        val source = CreatureCard(
            id = sourceId,
            card = PlantCard(
                6, "Flower_17_03", "O Edelweiss", PlantType.FLOWER, 17,
                null, "", "", "", "", "", "", "",
                GameEffect.PLAY_OR_FLIP_ANOTHER_CARD_TWICE,
                PlantScoringRule.Fixed(1)
            ),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-1, 1),
            facing = CreatureCard.Facing.FACE_UP
        )

        val topLevel = BattleMainPriority.intrinsicScore(
            context = context,
            roundCard = round(),
            action = BattleMainAction.ActivatePlant(source)
        )

        assertEquals(35, topLevel.total)
    }

    private fun battleContext(creature: List<CreatureCardView>): DecisionContext {
        val actorDie = BattleDieView(handIndex = 0, sides = 6, value = 2)
        return DecisionContext.EMPTY.copy(
            phase = RoundCardType.BATTLE,
            self = DecisionContext.EMPTY.self.copy(
                board = DecisionContext.EMPTY.self.board.copy(
                    id = ACTOR,
                    hand = listOf(DieView(index = 0, sides = 6, value = 2)),
                    creature = creature
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
                                dice = listOf(actorDie),
                                critters = emptyList(),
                                dieTotal = 2,
                                critterTotal = 0,
                                total = 6,
                                withdrawn = false
                            ),
                            BattlePlayerRowView(
                                playerId = OPPONENT,
                                row = StrikeRow.TOP,
                                dice = emptyList(),
                                critters = emptyList(),
                                dieTotal = 0,
                                critterTotal = 0,
                                total = 9,
                                withdrawn = false
                            )
                        )
                    )
                )
            )
        )
    }

    private fun plant(
        id: Int,
        name: String,
        effect: GameEffect,
        faceUp: Boolean,
        type: PlantType = PlantType.ROOT,
        cost: Int = 5
    ): CreatureCardView =
        CreatureCardView(
            id = CreatureCardId(id),
            name = name,
            title = name,
            type = type,
            cost = cost,
            effect = effect,
            scoringRule = PlantScoringRule.Fixed(1),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-id, 0),
            facing = if (faceUp) CreatureCard.Facing.FACE_UP else CreatureCard.Facing.FACE_DOWN,
            isSnippable = true
        )

    private fun CreatureCardView.toChoice(): EffectPlantChoice =
        EffectPlantChoice(
            cardId = id,
            cardName = name,
            isFaceUp = isFaceUp
        )

    private fun round() = RoundCard(
        quantity = 1,
        name = "battle",
        type = RoundCardType.BATTLE,
        firstEffect = RoundCardEffect("x", "", "", "", null, GameEffect.GAIN_ONE_VP),
        secondEffect = RoundCardEffect("y", "", "", "", null, GameEffect.GAIN_ONE_VP),
        backImage = ""
    )

    private companion object {
        val ACTOR = PlayerId(1)
        val OPPONENT = PlayerId(2)
    }
}
