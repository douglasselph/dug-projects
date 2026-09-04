package dugsolutions.leaf.v35.player.decision.effect

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.creature.CreatureCardId
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BaselineEffectStrategyTest {

    @Test
    fun chooseDie_returnsFirstLegalChoice() {
        val first = EffectDieChoice(0, 6, 3)
        val second = EffectDieChoice(1, 8, 5)

        val chosen = BaselineEffectStrategy().chooseDie(
            ChooseEffectDieRequest(
                effect = GameEffect.RAISE_DIE_PLUS_3,
                legalChoices = listOf(first, second)
            )
        )

        assertEquals(first, chosen)
    }

    @Test
    fun chooseOptionalDie_returnsFirstChoiceOrNull() {
        val first = EffectDieChoice(0, 6, 3)
        val strategy = BaselineEffectStrategy()

        assertEquals(
            first,
            strategy.chooseOptionalDie(
                ChooseOptionalEffectDieRequest(
                    effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
                    legalChoices = listOf(first)
                )
            )
        )

        assertNull(
            strategy.chooseOptionalDie(
                ChooseOptionalEffectDieRequest(
                    effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
                    legalChoices = emptyList()
                )
            )
        )
    }


    @Test
    fun combinedTargetChoices_areExplicitAndDeterministic() {
        val first = EffectDieChoice(0, 6, 3)
        val second = EffectDieChoice(1, 8, 5)
        val strategy = BaselineEffectStrategy()

        assertEquals(
            EffectDiceChoice(listOf(first)),
            strategy.chooseDice(
                ChooseEffectDiceRequest(
                    effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
                    legalChoices = listOf(first, second)
                )
            )
        )

        val pair = EffectDiePairChoice(source = first, target = second)
        assertEquals(
            pair,
            strategy.chooseDiePair(
                ChooseEffectDiePairRequest(
                    effect = GameEffect.SET_DIE_TO_MATCH_ANOTHER,
                    legalChoices = listOf(pair)
                )
            )
        )

        val critterDie = EffectCritterDieChoice(Critter.WORM, second)
        assertEquals(
            critterDie,
            strategy.chooseCritterAndDie(
                ChooseEffectCritterDieRequest(
                    effect = GameEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5,
                    legalChoices = listOf(critterDie)
                )
            )
        )


        val petal = PetalToDie4Choice.TrashD4AndRaiseAll(first)
        assertEquals(
            petal,
            strategy.choosePetalToDie4(
                ChoosePetalToDie4Request(
                    effect = GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4,
                    legalChoices = listOf(petal)
                )
            )
        )

        val beeSource =
            EffectBeeSourceChoice.Opponent(PlayerId(2))
        assertEquals(
            beeSource,
            strategy.chooseBeeSource(
                ChooseBeeSourceRequest(
                    effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
                    legalChoices = listOf(beeSource)
                )
            )
        )


        val butterflyTarget =
            EffectButterflyTargetChoice(
                ownerId = PlayerId(2),
                butterfly = Butterfly.GREEN
            )
        assertEquals(
            butterflyTarget,
            strategy.chooseButterflyTarget(
                ChooseEffectButterflyTargetRequest(
                    effect = GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES,
                    legalChoices = listOf(butterflyTarget)
                )
            )
        )

        val plantTarget =
            EffectPlantChoice(
                cardId = CreatureCardId(4),
                cardName = "Vine_A",
                isFaceUp = false
            )
        assertEquals(
            plantTarget,
            strategy.chooseOptionalPlant(
                ChooseOptionalEffectPlantRequest(
                    effect = GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE,
                    legalChoices = listOf(plantTarget)
                )
            )
        )


        val opponentPlantTarget =
            EffectOpponentPlantWoundChoice.Flip(
                ownerId = PlayerId(2),
                cardId = CreatureCardId(5),
                cardName = "Root_B"
            )
        assertEquals(
            opponentPlantTarget,
            strategy.chooseOpponentPlantWound(
                ChooseEffectOpponentPlantWoundRequest(
                    effect = GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE,
                    legalChoices = listOf(opponentPlantTarget)
                )
            )
        )
    }

    @Test
    fun requests_defensivelyCopyLegalChoices() {
        val mutable = mutableListOf(
            EffectDieChoice(0, 6, 3)
        )
        val required = ChooseEffectDieRequest(
            effect = GameEffect.RAISE_DIE_PLUS_3,
            legalChoices = mutable
        )
        val optional = ChooseOptionalEffectDieRequest(
            effect = GameEffect.REROLL_ALL_PLAYERS_DICE_KEEP_ONE_OWN,
            legalChoices = mutable
        )
        val many = ChooseEffectDiceRequest(
            effect = GameEffect.DISCARD_ANY_NUMBER_OF_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE,
            legalChoices = mutable
        )
        val petalMutable =
            mutableListOf<PetalToDie4Choice>(
                PetalToDie4Choice.TrashD4AndRaiseAll(
                    mutable.first()
                )
            )
        val petal = ChoosePetalToDie4Request(
            effect = GameEffect.GAIN_D4_SET_TO_4_OR_TRASH_D4_RAISE_ALL_DICE_PLUS_4,
            legalChoices = petalMutable
        )
        val beeMutable =
            mutableListOf<EffectBeeSourceChoice>(
                EffectBeeSourceChoice.Opponent(
                    PlayerId(2)
                )
            )
        val bee = ChooseBeeSourceRequest(
            effect = GameEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES_THIS_ROUND,
            legalChoices = beeMutable
        )
        val butterflyMutable = mutableListOf(
            EffectButterflyTargetChoice(
                ownerId = PlayerId(2),
                butterfly = Butterfly.GREEN
            )
        )
        val butterfly = ChooseEffectButterflyTargetRequest(
            effect = GameEffect.STEAL_BUTTERFLY_AND_REFRESH_ALL_BUTTERFLIES,
            legalChoices = butterflyMutable
        )
        val plantMutable = mutableListOf(
            EffectPlantChoice(
                cardId = CreatureCardId(1),
                cardName = "Root_A",
                isFaceUp = true
            )
        )
        val plant = ChooseOptionalEffectPlantRequest(
            effect = GameEffect.FLIP_OWN_PLANT_OR_WOUND_EACH_OPPONENT_IN_BATTLE,
            legalChoices = plantMutable
        )
        val opponentPlantMutable =
            mutableListOf<EffectOpponentPlantWoundChoice>(
                EffectOpponentPlantWoundChoice.Flip(
                    ownerId = PlayerId(2),
                    cardId = CreatureCardId(2),
                    cardName = "Root_B"
                )
            )
        val opponentPlant =
            ChooseEffectOpponentPlantWoundRequest(
                effect = GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE,
                legalChoices = opponentPlantMutable
            )

        mutable.clear()
        petalMutable.clear()
        beeMutable.clear()
        butterflyMutable.clear()
        plantMutable.clear()
        opponentPlantMutable.clear()

        assertEquals(1, required.legalChoices.size)
        assertEquals(1, optional.legalChoices.size)
        assertEquals(1, many.legalChoices.size)
        assertEquals(1, petal.legalChoices.size)
        assertEquals(1, bee.legalChoices.size)
        assertEquals(1, butterfly.legalChoices.size)
        assertEquals(1, plant.legalChoices.size)
        assertEquals(1, opponentPlant.legalChoices.size)
    }
}
