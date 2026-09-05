package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.player.creature.CreaturePosition
import dugsolutions.leaf.v35.player.creature.CreatureSide
import dugsolutions.leaf.v35.player.creature.GraftPlacement
import dugsolutions.leaf.v35.player.decision.battle.BattleMainAction
import dugsolutions.leaf.v35.player.decision.battle.BattleSupportAction
import dugsolutions.leaf.v35.player.decision.battle.BattleTurnAction
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleFirstMainActionRequest
import dugsolutions.leaf.v35.player.decision.battle.ChooseBattleTurnActionRequest
import dugsolutions.leaf.v35.player.decision.buy.BuyCritterResource
import dugsolutions.leaf.v35.player.decision.buy.BuyDieResource
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.player.decision.cultivation.ChooseCultivationActionRequest
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationAction
import dugsolutions.leaf.v35.player.decision.cultivation.CultivationMainAction
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.placement.ChooseCreaturePlacementRequest
import dugsolutions.leaf.v35.player.decision.reward.ChooseCritterRequest
import dugsolutions.leaf.v35.player.decision.support.ButterflyRollChoice
import dugsolutions.leaf.v35.player.decision.support.ChooseButterflyRollRequest
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.player.decision.wound.ChooseWoundRequest
import dugsolutions.leaf.v35.player.decision.wound.WoundChoice
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardEffect
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * Freezes Strategy Level 0 as a control group.
 *
 * If one of these tests changes, the Mechanical Baseline itself has changed.
 * New strategic ideas should normally be implemented in src/simulation instead.
 */
class MechanicalControlContractTest {

    @Test
    fun specification_coversEveryDecisionArea() {
        assertEquals("Mechanical Control", MechanicalControl.NAME)
        assertEquals(0, MechanicalControl.STRATEGY_LEVEL)
        assertEquals(DecisionArea.entries.toSet(), MechanicalControl.rules.keys)
        assertTrue(MechanicalControl.rules.values.all { it.isNotBlank() })
    }

    @Test
    fun createsFreshIndependentDirectors() {
        val first = DecisionDirector.mechanicalControl()
        val second = DecisionDirector.mechanicalControl()

        assertNotSame(first, second)
        assertNotSame(first.buy, second.buy)
        assertNotSame(first.effect, second.effect)
        assertEquals(first::class, DecisionDirector.mechanicalBaseline()::class)
    }

    @Test
    fun cultivation_prefersDrawThenStopsInsteadOfOptionalSupport() {
        val strategy = DecisionDirector.mechanicalControl().cultivation
        val round = roundCard(RoundCardType.CULTIVATION)

        assertEquals(
            CultivationAction.Main(CultivationMainAction.Draw),
            strategy.chooseAction(
                ChooseCultivationActionRequest(
                    roundCard = round,
                    mainActionsRemaining = 2,
                    legalChoices = listOf(
                        CultivationAction.Main(CultivationMainAction.RoundEffect1),
                        CultivationAction.Main(CultivationMainAction.Draw)
                    )
                )
            )
        )

        assertEquals(
            CultivationAction.Done,
            strategy.chooseAction(
                ChooseCultivationActionRequest(
                    roundCard = round,
                    mainActionsRemaining = 0,
                    legalChoices = listOf(
                        CultivationAction.Support(SupportAction.UseWaterRefresh),
                        CultivationAction.Done
                    )
                )
            )
        )
    }

    @Test
    fun battle_prefersDrawAndFinishesBeforeOptionalSupport() {
        val strategy = DecisionDirector.mechanicalControl().battle
        val round = roundCard(RoundCardType.BATTLE)

        assertEquals(
            BattleMainAction.Draw,
            strategy.chooseFirstMainAction(
                ChooseBattleFirstMainActionRequest(
                    roundCard = round,
                    legalChoices = listOf(
                        BattleMainAction.RoundEffect1,
                        BattleMainAction.Draw
                    )
                )
            )
        )

        val support = BattleTurnAction.Support(
            BattleSupportAction.Shared(SupportAction.UseWaterRefresh)
        )
        assertEquals(
            BattleTurnAction.FinalMain(BattleMainAction.Draw),
            strategy.chooseTurnAction(
                ChooseBattleTurnActionRequest(
                    roundCard = round,
                    passNumber = 1,
                    legalChoices = listOf(
                        support,
                        BattleTurnAction.FinalMain(BattleMainAction.RoundEffect1),
                        BattleTurnAction.FinalMain(BattleMainAction.Draw)
                    )
                )
            )
        )
    }

    @Test
    fun buy_choosesFirstItemAndMinimumSufficientPayment() {
        val strategy = DecisionDirector.mechanicalControl().buy
        val first = BuyItem.Die(DieSides.D8)
        val second = BuyItem.Die(DieSides.D6)

        assertEquals(
            first,
            (strategy.choosePurchase(ChoosePurchaseRequest(listOf(first, second))) as dugsolutions.leaf.v35.player.decision.buy.BuyChoice.Purchase).item
        )

        val payment = strategy.choosePayment(
            ChoosePaymentRequest(
                item = first,
                availableDice = listOf(
                    BuyDieResource(6, 6),
                    BuyDieResource(4, 4),
                    BuyDieResource(10, 9)
                ),
                availableCritters = listOf(
                    BuyCritterResource(Critter.BEE, 2),
                    BuyCritterResource(Critter.WORM, 1)
                )
            )
        )

        assertEquals(8, payment.total)
        assertEquals(2, payment.dice.size + payment.critters.size)
    }

    @Test
    fun placementAndWound_chooseFirstLegalChoice() {
        val director = DecisionDirector.mechanicalControl()
        val card = plantCard("Baseline")
        val placements = listOf(
            GraftPlacement(CreatureSide.LEFT, CreaturePosition(-1, 0)),
            GraftPlacement(CreatureSide.RIGHT, CreaturePosition(1, 0))
        )

        assertEquals(
            placements.first(),
            director.placement.choose(
                ChooseCreaturePlacementRequest(card, placements)
            )
        )

        val firstCard = creatureCard(1, "First")
        val secondCard = creatureCard(2, "Second")
        val choices = listOf(
            WoundChoice.Flip(firstCard),
            WoundChoice.Flip(secondCard)
        )
        assertEquals(
            choices.first(),
            director.wound.choose(ChooseWoundRequest(choices))
        )
    }

    @Test
    fun critterReward_choosesFewerOwnedTypeAndBeeOnTie() {
        val strategy = DecisionDirector.mechanicalControl().reward
        val legal = listOf(Critter.BEE, Critter.WORM)

        assertEquals(
            Critter.BEE,
            strategy.chooseCritter(ChooseCritterRequest(legal, emptyList()))
        )
        assertEquals(
            Critter.WORM,
            strategy.chooseCritter(
                ChooseCritterRequest(
                    legalChoices = legal,
                    ownedCritters = listOf(Critter.BEE, Critter.BEE, Critter.WORM)
                )
            )
        )
    }

    @Test
    fun butterfly_keepsHigherValueAndOriginalOnTie() {
        val strategy = DecisionDirector.mechanicalControl().support

        assertEquals(
            ButterflyRollChoice.REROLLED,
            strategy.chooseButterflyRoll(ChooseButterflyRollRequest(6, 2, 5))
        )
        assertEquals(
            ButterflyRollChoice.ORIGINAL,
            strategy.chooseButterflyRoll(ChooseButterflyRollRequest(6, 5, 5))
        )
    }

    @Test
    fun effectTargeting_choosesFirstLegalTarget() {
        val strategy = DecisionDirector.mechanicalControl().effect
        val choices = listOf(
            EffectDieChoice(0, 6, 2),
            EffectDieChoice(1, 6, 5)
        )

        assertEquals(
            choices.first(),
            strategy.chooseDie(
                ChooseEffectDieRequest(
                    effect = GameEffect.RAISE_DIE_PLUS_4,
                    legalChoices = choices
                )
            )
        )
    }

    private fun roundCard(type: RoundCardType): RoundCard =
        RoundCard(
            quantity = 1,
            name = "Baseline_${type.name}",
            type = type,
            firstEffect = roundEffect("First"),
            secondEffect = roundEffect("Second"),
            backImage = ""
        )

    private fun roundEffect(title: String): RoundCardEffect =
        RoundCardEffect(
            title = title,
            backgroundColor = "",
            textColor = "",
            image = "",
            icon = null,
            effect = GameEffect.GAIN_ONE_VP
        )

    private fun creatureCard(id: Int, name: String): CreatureCard =
        CreatureCard(
            id = CreatureCardId(id),
            card = plantCard(name),
            side = CreatureSide.LEFT,
            position = CreaturePosition(-id, 0),
            facing = CreatureCard.Facing.FACE_UP
        )

    private fun plantCard(name: String): PlantCard =
        PlantCard(
            quantity = 1,
            name = name,
            title = name,
            type = PlantType.VINE,
            cost = 7,
            lineIcon = null,
            vpIcon = "",
            typeIcon = "",
            fgColor = "",
            textColor = "",
            fullImage = "",
            backgroundImage = "",
            cardBackgroundImage = "",
            effect = GameEffect.UNKNOWN
        )
}
