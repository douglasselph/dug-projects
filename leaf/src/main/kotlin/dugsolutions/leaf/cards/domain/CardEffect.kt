package dugsolutions.leaf.cards.domain

enum class CardEffect(
    val match: String,
    val description: String,
) {
    ADD_TO_DIE("AddToDie", "Add VALUE to one die, without exceeding MAX"),
    ADD_TO_TOTAL("AddToTotal", "Add VALUE to final total"),
    ADJUST_BY("AdjustBy", "Adjust any die by +/- VALUE, without exceeding 1 or MAX"),
    ADJUST_TO_MAX("AdjustToMax", "Adjust VALUE dice to MAX"),
    ADJUST_TO_MIN_OR_MAX("AdjustToMinOrMax", "Adjust VALUE dice to either MIN or MAX"),
    ADORN("Adorn", "Play card to Floral Array then draw a card."),
    DEFLECT("Deflect", "Deflects VALUE"),
    DISCARD("Discard", "Opponent discards VALUE dice or cards"),
    DISCARD_CARD("DiscardCard", "Opponent discards VALUE cards"),
    DISCARD_DIE("DiscardDie", "Opponent discards VALUE dice"),
    DRAW_CARD("DrawCard", "Draw VALUE cards"),
    DRAW_CARD_DISCARD("DrawCardDiscard", "Draw VALUE cards from Discard Patch"),
    DRAW_DIE("DrawDie", "Draw VALUE lowest-sided dice"),
    DRAW_DIE_ANY("DrawDieAny", "Draw VALUE dice from Supply"),
    DRAW_DIE_DISCARD("DrawDieDiscard", "Draw VALUE lowest-sided dice from the Discard Patch."),
    DRAW_ANY("DrawAny", "Draw VALUE cards or dice."),
    FLOURISH_OVERRIDE("FlourishOverride", "Override the Flourish type for this turn"),
    GAIN_D20("GainD20", "Gain a D20 from the Supply if there are any left"),
    GAIN_FREE_ROOT("GainFreeRoot", "Gain a free Root card"),
    GAIN_FREE_CANOPY("GainFreeCanopy", "Gain a free Canopy card"),
    GAIN_FREE_VINE("GainFreeVine", "Gain a free Vine card"),
    REDUCE_COST_ROOT("ReduceCostRoot", "Reduce the cost of a Root card by VALUE"),
    REDUCE_COST_CANOPY("ReduceCostCanopy", "  VALUE"),
    REDUCE_COST_VINE("ReduceCostVine", "Reduce the cost of a Vine card by VALUE"),
    REROLL_ACCEPT_2ND("RerollAccept2nd", "Reroll VALUE dice, you must accept the second roll"),
    REROLL_ALL_MAX("RerollAllMax", "Force an opponent to reroll a MAX value die"),
    REROLL_ANY("RerollAny", "Reroll one of your own or an opponents die. You must accept the 2nd roll."),
    REROLL_TAKE_BETTER("RerollTakeBetter", "Reroll VALUE dice, you may take the better of the two rolls"),
    REPLAY_VINE("ReplayVine", "May use VALUE vine effect again immediately"),
    RETAIN_CARD("RetainCard", "Retain VALUE cards"),
    RETAIN_DIE("RetainDie", "Retain VALUE die"),
    RETAIN_DIE_REROLL("RetainDieReroll", "Retain VALUE dice with reroll"),
    REUSE_CARD("ReuseCard", "Reuse VALUE cards"),
    REUSE_DIE("ReuseDie", "Reuse VALUE dice, do not reroll"),
    REUSE_DIE_REROLL("ReuseDieReroll", "Reuse VALUE dice, rerolling"),
    REUSE_ANY("ReuseAny", "Reuse VALUE cards or dice"),
    UPGRADE_ANY_RETAIN("UpgradeAnyRetain", "Upgrade VALUE dice then use. (D12 -> D20 costs 8)"),
    UPGRADE_ANY("UpgradeAny", "Upgrade VALUE dice then discard. (D12 -> D20 costs 8)"),
    UPGRADE_D4("UpgradeD4", "Upgrade a D4 and retain"),
    UPGRADE_D6("UpgradeD6", "Upgrade a D4 or D6 and retain"),
    UPGRADE_D8("UpgradeD8", "Upgrade up to a D8 and retain"),
    UPGRADE_D10("UpgradeD10", "Upgrade up to a D10 and retain"),
    USE_OPPONENT_CARD("UseOpponentCard", "Use opponent's canopy or root cards"),
    USE_OPPONENT_DIE("UseOpponentDie", "Use VALUE of opponent's dice");

    companion object {
        fun from(incoming: String): CardEffect? {
            for (entry in entries) {
                if (entry.match == incoming) {
                    return entry
                }
            }
            return null
        }
    }
} 