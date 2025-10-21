package dugsolutions.leaf.common.domain

enum class GameEffect(
    val match: String,
    val description: String,
) {

    NONE("-", "No effect"),
    ADD_TO_DIE("AddToDie", "Add VALUE to one die, without exceeding MAX"),
    ADD_TO_TOTAL("AddToTotal", "Add VALUE to final total"),
    ADJUST_BY("AdjustBy", "Adjust any die by +/- VALUE, without exceeding 1 or MAX"),
    ADJUST_TO_MAX("AdjustToMax", "Adjust VALUE dice to MAX"),
    ADJUST_TO_MIN_OR_MAX("AdjustToMinOrMax", "Adjust VALUE dice to either MIN or MAX"),
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
    GRAFT_DIE("GraftDie", "Add a grafted die to the Canopy"),
    REROLL_ACCEPT_2ND("RerollAccept2nd", "Reroll VALUE dice, you must accept the second roll"),
    REROLL_ANY("RerollAny", "Reroll one of your own or an opponents die. You must accept the 2nd roll."),
    REROLL_TAKE_BETTER("RerollTakeBetter", "Reroll VALUE dice, you may take the better of the two rolls"),
    RETAIN_CARD("RetainCard", "Retain VALUE cards"),
    RETAIN_DIE("RetainDie", "Retain VALUE die"),
    RETAIN_DIE_REROLL("RetainDieReroll", "Retain VALUE dice with reroll"),
    REUSE_CARD("ReuseCard", "Reuse VALUE cards"),
    REUSE_DIE("ReuseDie", "Reuse VALUE dice, do not reroll"),
    REUSE_DIE_REROLL("ReuseDieReroll", "Reuse VALUE dice, rerolling"),
    REUSE_ANY("ReuseAny", "Reuse VALUE cards or dice"),
    UPGRADE_ETAIN("UpgradeAnyRetain", "Upgrade VALUE dice then use. (D12 -> D20 costs 8)"),
    UPGRADE("Upgrade", "Upgrade VALUE dice then discard."),
    USE_OPPONENT_CARD("UseOpponentCard", "Use opponent's canopy or root cards"),
    USE_OPPONENT_DIE("UseOpponentDie", "Use VALUE of opponent's dice");

    companion object {
        fun from(incoming: String): GameEffect? {
            for (entry in entries) {
                if (entry.match == incoming) {
                    return entry
                }
            }
            return null
        }
    }
} 
