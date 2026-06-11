package dugsolutions.leaf.v30.player.decision.baseline

import dugsolutions.leaf.v30.battle.domain.BattleStrikeRow
import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.decision.domain.CardsToRefresh
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.decision.domain.ItemsToBuy
import dugsolutions.leaf.v30.player.decision.domain.MainActionBattle
import dugsolutions.leaf.v30.player.decision.domain.MainActionCultivation
import dugsolutions.leaf.v30.player.domain.CreatureCard

class DecisionDirectorBaseline : DecisionDirector {

    override fun chooseCritter(input: Decision.ChooseCritter): Critter {
        val available = input.availableCritters.toSet()
        val bees = input.player.critters.count { it == Critter.BEE }
        val worms = input.player.critters.count { it == Critter.WORM }

        return when {
            Critter.BEE in available && Critter.WORM in available -> {
                if (bees <= worms) Critter.BEE else Critter.WORM
            }
            Critter.BEE in available -> Critter.BEE
            Critter.WORM in available -> Critter.WORM
            else -> throw IllegalArgumentException("No critters available to choose from")
        }
    }

    override fun chooseMainActionCultivation(input: Decision.ChooseMainActionCultivation): MainActionCultivation {
        return MainActionCultivation.PullDie
    }

    override fun chooseMainActionBattle(input: Decision.ChooseMainActionBattle): MainActionBattle {
        return MainActionBattle.PullDie(BattleStrikeRow.STRIKE_1)
    }

    override fun chooseItemsToBuy(input: Decision.ChooseItemsToBuy): ItemsToBuy {
        return ItemsToBuy()
    }

    override fun chooseCardsToRefreshWithWorms(input: Decision.ChooseCardsToRefreshWithWorms): CardsToRefresh {
        return CardsToRefresh()
    }

    override fun chooseFlipOrSnipCard(input: Decision.ChooseFlipOrSnipCard): CreatureCard {
        return input.creatureCards.firstOrNull { it.isFaceUp }
            ?: input.creatureCards.firstOrNull()
            ?: throw IllegalArgumentException("No creature cards available to flip or snip")
    }
}
