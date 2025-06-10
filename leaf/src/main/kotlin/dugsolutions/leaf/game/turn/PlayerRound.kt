package dugsolutions.leaf.game.turn

import dugsolutions.leaf.cards.CardManager
import dugsolutions.leaf.game.turn.handle.HandleAdorn
import dugsolutions.leaf.game.turn.handle.HandleCard
import dugsolutions.leaf.player.Player

/***
 *  Process card effects that can be done immediately before the acquisition or battle phases.
 *
 *  New variable: playedCards.
 *
 *  Plan:
 *     ADD_TO_DIE("Add VALUE to one die, without exceeding MAX") :
 *        If adjusting a die would lose a match, then do not do that die.
 *        If there is a die which can be easily selected then do that.
 *     ADD_TO_TOTAL("Add VALUE to final total") : Always do -- applied to pipModifier
 *     ADJUST_BY("Adjust any die by +/- VALUE, without exceeding 1 or MAX") :
 *        Use same logic as ADD_TO_DIE, but if there are no good choices, select a target die to adjust.
 *     ADJUST_TO_MAX("Adjust VALUE dice to MAX")
 *        As long as match not lost, then select die with largest gap.
 *     ADJUST_TO_MIN_OR_MAX("Adjust VALUE dice to either MIN or MAX")
 *        Between the dice across both players, select the die that yields the largest gap.
 *     ADORN("Play card to Floral Array then draw a card.") : Always do
 *     DEFLECT("Deflects VALUE") : Always do - applied to deflectDamage value
 *     DISCARD("Opponent discards VALUE dice or cards") : Always do.
 *     DISCARD_CARD("Opponent discards VALUE cards") : Always do
 *     DISCARD_DIE("Opponent discards VALUE dice") : Always do
 *     DRAW_CARD("Draw VALUE cards") : Always do
 *     DRAW_CARD_COMPOST("Draw VALUE cards from Compost") : Always do
 *     DRAW_DIE("Draw VALUE lowest-sided dice") : Always do
 *     DRAW_DIE_ANY("Draw VALUE dice from Supply") : Always do
 *     DRAW_DIE_COMPOST("Draw VALUE lowest-sided dice from Compost.") : Always do
 *     DRAW_THEN_DISCARD("Draw VALUE cards, then discard VALUE-1") : Always do
 *     FLOURISH_OVERRIDE("Override the Flourish type for this turn") : Yields AppliedEffect
 *     GAIN_FREE_ROOT("Gain a free Root card") : Always do
 *     GAIN_FREE_CANOPY("Gain a free Canopy card") : Always do
 *     GAIN_FREE_VINE("Gain a free Vine card") : Always do
 *     REDUCE_COST_ROOT("Reduce the cost of a Root card by VALUE") : Yields AppliedEffect
 *     REDUCE_COST_CANOPY("Reduce the cost of a Canopy card by VALUE") : Yields AppliedEffect
 *     REDUCE_COST_VINE("Reduce the cost of a Vine card by VALUE") : Yields AppliedEffect
 *     REROLL_ACCEPT_2ND("Reroll VALUE dice, you must accept the second roll")
 *        Ignore die which are causing match.
 *        Otherwise if a die is low enough do now.
 *     REROLL_ALL_MAX("Force an opponent to reroll a MAX value die"),
 *        (TODO)
 *     REROLL_TAKE_BETTER("Reroll VALUE dice, you may take the better of the two rolls")
 *        Ignore die which are causing match
 *        Otherwise do die that stands to gain largest gap.
 *     REPLAY_VINE("May use VALUE vine effect again immediately")
 *        Basically will copy the effect from the hand of another Vine card
 *        that has been played or is in hand.
 *     RETAIN_CARD("Retain VALUE cards")
 *        This effect is only possible when the decision is true. Baseline is false.
 *        Mark a card to be retained, but adding it to the retainedList.
 *        Cards in this list will not be discarded but also cannot be used this turn.
 *     RETAIN_DIE("Retain VALUE die"),
 *        This effect is only possible when the decision is true. Baseline is false.
 *        Mark a die to be retained, by adding to the retainedList, and not allowing it to be used this turn.
 *     RETAIN_DIE_REROLL("Retain VALUE dice with reroll")
 *        Same as above.
 *     REUSE_CARD("Reuse VALUE cards")
 *        Select card with best score -- add to "reuseList"
 *        Decision needed here
 *     REUSE_DIE("Reuse VALUE dice"),
 *        Select die with highest sides -- add to reuseList.
 *     REUSE_ANY("Reuse VALUE cards or dice"),
 *        Select die or card -- add to reuseList
 *        Decision needed here
 *     UPGRADE_ANY_RETAIN("Upgrade VALUE dice then use. (D12 -> D20 costs 8)") : Always do
 *     UPGRADE_ANY("Upgrade VALUE dice then discard. (D12 -> D20 costs 8)") : Always do
 *     UPGRADE_D4("Upgrade a D4 and retain") : Always do
 *     UPGRADE_D6("Upgrade a D4 or D6 and retain") : Always do
 *     UPGRADE_D8("Upgrade up to a D8 and retain") : Always do
 *     UPGRADE_D10("Upgrade up to a D10 and retain") : Always do
 *     USE_OPPONENT_CARD("Use VALUE of opponent's cards") : Always do -- add to borrowedList
 *     USE_OPPONENT_DIE("Use VALUE of opponent's dice") : Always do -- add to borrowedList
 */
class PlayerRound(
    private val handleCard: HandleCard,
    private val handleAdorn: HandleAdorn
) {
    suspend operator fun invoke(player: Player, target: Player) {
        player.clearEffects()
        player.cardsToPlay.addAll(player.cardsInHand)

        while (player.cardsToPlay.isNotEmpty()) {
            handleAdorn(player)
            if (player.cardsToPlay.isNotEmpty()) { // TODO: How to unit test this line?
                val card = player.cardsToPlay.removeAt(0)
                handleCard(player, target, card)
            }
        }
    }

} 
