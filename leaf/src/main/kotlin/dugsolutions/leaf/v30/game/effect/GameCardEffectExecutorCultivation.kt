package dugsolutions.leaf.v30.game.effect

import dugsolutions.leaf.v30.cards.domain.CardEffect
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.GameChronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.chronicle.domain.WarningType
import dugsolutions.leaf.v30.game.effect.details.DoubleOneDie
import dugsolutions.leaf.v30.game.effect.details.FlipDieToOppositeFace
import dugsolutions.leaf.v30.game.effect.details.RaiseDiePlus1AndDoubleMatchingDiceCultivation
import dugsolutions.leaf.v30.game.effect.details.RaiseDiePlus1AndGainWaterCultivation
import dugsolutions.leaf.v30.game.effect.details.RerollDieUntilThreeOrHigherCultivation
import dugsolutions.leaf.v30.game.effect.details.SetDieToMatchAnotherCultivation
import dugsolutions.leaf.v30.game.effect.scope.HandleDieEffectScope
import dugsolutions.leaf.v30.player.Player
import dugsolutions.leaf.v30.player.decision.domain.ExecuteTarget
import dugsolutions.leaf.v30.player.decision.domain.ActionCultivation
import dugsolutions.leaf.v30.random.Randomizer
import dugsolutions.leaf.v30.random.die.Dice
import dugsolutions.leaf.v30.random.die.di.DieFactory
import dugsolutions.leaf.v30.table.Table

@Suppress("UNUSED_PARAMETER")
open class GameCardEffectExecutorCultivation(
    chronicle: Chronicle = GameChronicle(),
    dieFactory: DieFactory = DieFactory(Randomizer.create())
) : GameCardEffectExecutorBase(chronicle, dieFactory) {

    open operator fun invoke(
        table: Table,
        player: Player,
        action: ActionCultivation.ExecuteCard
    ) {
        when (action.card.effect) {
            CardEffect.UNKNOWN -> unknown(table, player, action)
            CardEffect.PLACE_BULWARK_TOKEN -> ignoreBattleEffect(table, player, action)
            CardEffect.GAIN_WORM_AND_BOOST_WORMS -> gainWormAndBoostWorms(table, player, action)
            CardEffect.MULCH_DIE_FROM_DISCARD -> mulchDieFromDiscard(table, player, action)
            CardEffect.REROLL_DIE_UNTIL_THREE_OR_HIGHER -> rerollDieUntilThreeOrHigher(table, player, action)
            CardEffect.RAISE_DIE_PLUS_1_AND_GAIN_WATER -> raiseDiePlus1AndGainWater(table, player, action)
            CardEffect.RAISE_DIE_PLUS_1_AND_DOUBLE_MATCHING_DICE -> raiseDiePlus1AndDoubleMatchingDice(
                table,
                player,
                action
            )

            CardEffect.DOUBLE_ONE_DIE -> doubleOneDie(table, player, action)
            CardEffect.DOUBLE_ALL_DICE_SHOWING_ONE_TO_FOUR -> doubleAllDiceShowingOneToFour(table, player, action)
            CardEffect.UPGRADE_DIE_AND_USE_NOW -> upgradeDieAndUseNow(table, player, action)
            CardEffect.FLIP_DIE_TO_OPPOSITE_FACE -> flipDieToOppositeFace(table, player, action)
            CardEffect.SET_DIE_TO_MATCH_ANOTHER -> setDieToMatchAnother(table, player, action)
            CardEffect.RAISE_DIE_PLUS_2_PER_WORM_AND_DISCARD_WORM -> raiseDiePlus2PerWormAndDiscardWorm(
                table,
                player,
                action
            )

            CardEffect.GAIN_OR_STEAL_BEE_AND_BOOST_BEES -> gainOrStealBeeAndBoostBees(table, player, action)
            CardEffect.WOUND_WINNER_OF_STRIKE_ROW -> woundWinnerOfStrikeRow(table, player, action)
            CardEffect.GAIN_D4_OR_RETURN_D4_RAISE_DIE_PLUS_4 -> gainD4OrReturnD4RaiseDiePlus4(table, player, action)
            CardEffect.SWAP_TWO_OWN_DICE -> swapTwoOwnDice(table, player, action)
            CardEffect.RAISE_DIE_PLUS_1_PER_GRAFTED_ROOT_OR_VINE -> raiseDiePlus1PerGraftedRootOrVine(
                table,
                player,
                action
            )

            CardEffect.ROLL_EXTRA_FOR_EACH_MAX_DIE -> rollExtraForEachMaxDie(table, player, action)
            CardEffect.REROLL_HIGHER_OPPOSING_DICE_ON_STRIKE_ROW -> rerollHigherOpposingDiceOnStrikeRow(
                table,
                player,
                action
            )

            CardEffect.DRAIN_HIGHER_DICE_AND_RAISE_OWN_DIE -> drainHigherDiceAndRaiseOwnDie(table, player, action)
            CardEffect.DRAW_DIE_FROM_DISCARD -> drawDieFromDiscard(table, player, action)
            CardEffect.FLIP_HIGHER_OPPOSING_DICE_ON_STRIKE_ROW -> flipHigherOpposingDiceOnStrikeRow(
                table,
                player,
                action
            )

            CardEffect.PLAY_UP_TO_TWO_OTHER_CARDS -> playUpToTwoOtherCards(table, player, action)
            CardEffect.DRAW_TWO_DICE -> drawTwoDice(table, player, action)
            CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_2_VP -> raiseDiePlus1AndEndGamePlus2Vp(table, player, action)
            CardEffect.RAISE_DIE_PLUS_1_AND_END_GAME_PLUS_1_VP_PER_FLOWER -> raiseDiePlus1AndEndGamePlus1VpPerFlower(
                table,
                player,
                action
            )

            CardEffect.RAISE_THREE_DICE_PLUS_1 -> raiseThreeDicePlus1(table, player, action)
            CardEffect.RAISE_DIE_PLUS_4 -> raiseDiePlus4(table, player, action)
            CardEffect.RESOLVE_GRAFTED_ROOT_OR_VINE_EFFECT -> resolveGraftedRootOrVineEffect(table, player, action)
            CardEffect.RESOLVE_STRIKE_IMMEDIATELY -> resolveStrikeImmediately(table, player, action)
            CardEffect.GAIN_MULCH_AND_CLEANUP_MULCH_DIE -> gainMulchAndCleanupMulchDie(table, player, action)
            CardEffect.TRASH_CRITTER_TO_RAISE_DIE_PLUS_5 -> trashCritterToRaiseDiePlus5(table, player, action)
            CardEffect.RAISE_DIE_PLUS_2_PER_VINE -> raiseDiePlus2PerVine(table, player, action)
            CardEffect.FLIP_OPPONENT_FACE_UP_VINE_FACE_DOWN -> flipOpponentFaceUpVineFaceDown(table, player, action)
            CardEffect.SET_DIE_UP_TO_D12_TO_MAX -> setDieUpToD12ToMax(table, player, action)
            CardEffect.REDUCE_OPPOSING_DICE_ON_STRIKE_ROW_BY_3 -> reduceOpposingDiceOnStrikeRowBy3(
                table,
                player,
                action
            )
        }
    }

    private fun unknown(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        chronicle(
            Moment.Warning(
                player = player,
                type = WarningType.UNKNOWN_EFFECT,
                card = action.card
            )
        )
    }
    private fun ignoreBattleEffect(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun rerollDieUntilThreeOrHigher(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        RerollDieUntilThreeOrHigherCultivation(chronicle)(player, action.card, action.target)
    }

    private fun raiseDiePlus1AndGainWater(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        RaiseDiePlus1AndGainWaterCultivation(chronicle)(table, player, action.card, action.target)
    }

    private fun raiseDiePlus1AndDoubleMatchingDice(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        val target = action.target as? ExecuteTarget.PlayerDie
        if (target == null) {
            chronicle(
                Moment.Warning(
                    player = player,
                    type = WarningType.DOUBLE_MATCHING_DICE_INVALID_TARGET,
                    card = action.card
                )
            )
            return
        }
        RaiseDiePlus1AndDoubleMatchingDiceCultivation(chronicle)(
            player = player,
            card = action.card,
            target = target.dice
        )
    }

    private fun doubleOneDie(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        DoubleOneDie(chronicle)(
            scope = HandleDieEffectScope(player),
            card = action.card,
            target = action.target
        )
    }
    private fun doubleAllDiceShowingOneToFour(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        val doubled = player.diceHand.dice
            .filter { die -> die.value in DOUBLE_ALL_DICE_RANGE }
            .mapNotNull { die -> player.raiseDie(die, die.value) }
        chronicle(
            Moment.GameCardEffect(
                player = player,
                card = action.card,
                effect = action.card.effect,
                detail = "Doubled all hand dice showing 1 through 4",
                dice = Dice(doubled)
            )
        )
    }

    private fun flipDieToOppositeFace(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        FlipDieToOppositeFace(chronicle)(
            scope = HandleDieEffectScope(player),
            card = action.card,
            target = action.target
        )
    }
    private fun setDieToMatchAnother(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {
        SetDieToMatchAnotherCultivation(chronicle)(player, action.card, action.target)
    }
    private fun raiseDiePlus2PerWormAndDiscardWorm(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun gainOrStealBeeAndBoostBees(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun woundWinnerOfStrikeRow(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun gainD4OrReturnD4RaiseDiePlus4(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun swapTwoOwnDice(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun raiseDiePlus1PerGraftedRootOrVine(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun rollExtraForEachMaxDie(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun rerollHigherOpposingDiceOnStrikeRow(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun drainHigherDiceAndRaiseOwnDie(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun drawDieFromDiscard(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun flipHigherOpposingDiceOnStrikeRow(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun playUpToTwoOtherCards(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun drawTwoDice(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun raiseDiePlus1AndEndGamePlus2Vp(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun raiseDiePlus1AndEndGamePlus1VpPerFlower(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun raiseThreeDicePlus1(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun raiseDiePlus4(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun resolveGraftedRootOrVineEffect(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun resolveStrikeImmediately(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun gainMulchAndCleanupMulchDie(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun trashCritterToRaiseDiePlus5(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun raiseDiePlus2PerVine(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun flipOpponentFaceUpVineFaceDown(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun setDieUpToD12ToMax(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}
    private fun reduceOpposingDiceOnStrikeRowBy3(table: Table, player: Player, action: ActionCultivation.ExecuteCard) {}

    private companion object {
        val DOUBLE_ALL_DICE_RANGE = 1..4
    }
}
    
