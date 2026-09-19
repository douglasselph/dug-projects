package dugsolutions.leaf.v30.game.effect.details

import dugsolutions.leaf.v30.cards.domain.GameCard
import dugsolutions.leaf.v30.chronicle.Chronicle
import dugsolutions.leaf.v30.chronicle.domain.Moment
import dugsolutions.leaf.v30.game.effect.scope.DieEffectScope
import dugsolutions.leaf.v30.random.die.Dice

class RollExtraForEachMaxDie(
    private val chronicle: Chronicle
) {
    operator fun invoke(
        scopes: List<DieEffectScope>,
        card: GameCard
    ) {
        val boosted = scopes.flatMap { scope ->
            scope.allDice().dice
                .filter { die -> die.value == die.sides }
                .mapNotNull { die ->
                    scope.reroll(die)?.let { rerolled ->
                        rerolled.boost(rerolled.value + rerolled.sides)
                    }
                }
        }
        chronicle(
            Moment.GameCardEffect(
                player = scopes.first().actingPlayer,
                card = card,
                effect = card.effect,
                detail = "Rolled extra for ${boosted.size} max die/dice",
                dice = Dice(boosted)
            )
        )
    }
}
