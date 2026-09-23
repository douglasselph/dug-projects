package dugsolutions.leaf.v35.player.decision.baseline.battle

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.player.decision.baseline.card.HumanBaselineCardScorerRegistry
import dugsolutions.leaf.v35.player.decision.baseline.scoring.PriorityScore
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.OpponentView
import dugsolutions.leaf.v35.player.decision.context.SelfPlayerView
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * Values one legal opponent-Plant Flip/Snip target from the actor's perspective.
 *
 * Battle target selection stays deliberately one step deep. Face-up Plants are
 * valued partly by the immediate Battle Main they deny. Shift Happens also
 * treats flipping a spent Plant face up as helping that opponent, so it avoids
 * doing so when a less harmful legal target exists. Snipping an already-spent
 * Plant has no immediate activation to deny, so the existing card-loss value
 * remains the simple fallback.
 *
 * Only public opponent board/Battle information is used. Hidden Wisp identity
 * is never reconstructed for the opponent-perspective tactical projection.
 */
class BattleOpponentPlantTargetAnalyzer(
    private val cardScorers: HumanBaselineCardScorerRegistry = HumanBaselineCardScorerRegistry(),
    private val enabledPlantAnalyzer: BattleEnabledPlantAnalyzer = BattleEnabledPlantAnalyzer(cardScorers)
) {
    operator fun invoke(
        context: DecisionContext,
        effect: GameEffect,
        choice: EffectOpponentPlantWoundChoice
    ): PriorityScore? {
        if (context.phase != RoundCardType.BATTLE) return null

        val owner = context.opponents.firstOrNull { it.id == choice.ownerId } ?: return null
        val card = owner.board.creature.firstOrNull { it.id == choice.cardId } ?: return null
        if (card.name != choice.cardName) return null

        val ownerContext = opponentPerspective(context, owner)
        val scorer = cardScorers.forPlant(card)
        val lossValue = scorer.lossValue(ownerContext, card)
        val immediateUse = if (context.battle?.isDone(owner.id) == true) {
            0
        } else {
            enabledPlantAnalyzer(ownerContext, card).priority.total
        }

        return when (effect) {
            GameEffect.FLIP_OWN_PLANT_OR_FLIP_OPPONENT_ROOT_OR_VINE_IN_BATTLE -> {
                if (card.isFaceUp) {
                    PriorityScore(40)
                        .adjusted(immediateUse, "Disable opponent's immediate Battle Plant use")
                } else {
                    PriorityScore(40)
                        .adjusted(-immediateUse, "Avoid enabling opponent's spent Battle Plant")
                }
            }

            GameEffect.WOUND_OPPONENT_PLANT_OF_YOUR_CHOICE,
            GameEffect.FLIP_OWN_PLANT_OR_WOUND_CHOSEN_OPPONENT_CHOOSE_CARD_IN_BATTLE -> {
                var score = PriorityScore(40)
                    .adjusted(lossValue, "Opponent Plant value lost to Wound")

                if (choice is EffectOpponentPlantWoundChoice.Flip && card.isFaceUp) {
                    score = score.adjusted(
                        immediateUse,
                        "Disable opponent's immediate Battle Plant use"
                    )
                }
                if (choice is EffectOpponentPlantWoundChoice.Snip) {
                    score = score.adjusted(20, "Snip removes the spent Plant")
                }
                score
            }

            else -> null
        }
    }

    private fun opponentPerspective(
        context: DecisionContext,
        owner: OpponentView
    ): DecisionContext =
        context.copy(
            self = SelfPlayerView(
                board = owner.board,
                // Opponent Wisp identities are hidden from the acting player.
                wisps = emptyList()
            ),
            opponents = context.opponents
                .filterNot { it.id == owner.id } +
                OpponentView(
                    board = context.self.board,
                    wispCount = context.self.wispCount
                )
        )
}
