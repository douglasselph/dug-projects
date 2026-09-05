package dugsolutions.leaf.v35.player.decision.battle

import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.support.HandDieChoice
import dugsolutions.leaf.v35.player.decision.support.SupportAction
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.tokens.Critter

/** Main Actions available during Battle Steps 4 and 5. */
sealed interface BattleMainAction {
    data object Draw : BattleMainAction
    data class ActivatePlant(val card: CreatureCard) : BattleMainAction
    data object RoundEffect1 : BattleMainAction
    data object RoundEffect2 : BattleMainAction
}

/**
 * Battle-only Support Actions layered around the support vocabulary shared with
 * Cultivation.
 */
sealed interface BattleSupportAction {
    data class Shared(val action: SupportAction) : BattleSupportAction

    /** Place one currently uncommitted Critter in one exact Strike Square. */
    data class PlaceCritter(
        val critter: Critter,
        val row: StrikeRow
    ) : BattleSupportAction
}

/** One Step-5 choice: take a Support Action or spend the final Main Action. */
sealed interface BattleTurnAction {
    data class Support(val action: BattleSupportAction) : BattleTurnAction
    data class FinalMain(val action: BattleMainAction) : BattleTurnAction
}

class ChooseBattleFirstMainActionRequest(
    val roundCard: RoundCard,
    legalChoices: List<BattleMainAction>,
    val context: DecisionContext = DecisionContext.EMPTY
) {
    val legalChoices: List<BattleMainAction> = legalChoices.toList()

    init {
        require(this.legalChoices.isNotEmpty()) {
            "Battle first Main Action requires at least one legal choice"
        }
    }
}

class ChooseBattleTurnActionRequest(
    val roundCard: RoundCard,
    val passNumber: Int,
    legalChoices: List<BattleTurnAction>,
    val context: DecisionContext = DecisionContext.EMPTY
) {
    val legalChoices: List<BattleTurnAction> = legalChoices.toList()

    init {
        require(passNumber > 0) {
            "Battle support/final-main pass number must be positive: $passNumber"
        }
        require(this.legalChoices.isNotEmpty()) {
            "Battle Step 5 requires at least one legal choice"
        }
    }
}

enum class BattleDiePlacementReason {
    MAIN_DRAW,
    MULCH,
    EFFECT
}

/**
 * Decision made after a new Battle die has actually been rolled and therefore
 * has a value. The exact die snapshot is included so strategies can choose a
 * Strike Row with full information.
 */
class ChooseBattleDiePlacementRequest(
    val die: HandDieChoice,
    val reason: BattleDiePlacementReason,
    legalRows: List<StrikeRow>,
    val context: DecisionContext = DecisionContext.EMPTY
) {
    val legalRows: List<StrikeRow> = legalRows.toList()

    init {
        require(this.legalRows.isNotEmpty()) {
            "Battle die placement requires at least one legal Strike Row"
        }
    }
}

interface BattleStrategy {
    /** Step 4: every player must take exactly one Main Action. */
    fun chooseFirstMainAction(
        request: ChooseBattleFirstMainActionRequest
    ): BattleMainAction

    /** Step 5: choose one Support Action or the final Main Action. */
    fun chooseTurnAction(
        request: ChooseBattleTurnActionRequest
    ): BattleTurnAction

    /** Choose where a newly added Battle Hand die is placed. */
    fun chooseDiePlacement(
        request: ChooseBattleDiePlacementRequest
    ): StrikeRow
}
