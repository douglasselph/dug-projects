package dugsolutions.leaf.v30.player.decision.baseline

import dugsolutions.leaf.v30.common.Critter
import dugsolutions.leaf.v30.player.decision.domain.Decision
import dugsolutions.leaf.v30.player.decision.domain.DecisionDirector
import dugsolutions.leaf.v30.player.decision.domain.MainAction

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

    override fun chooseMainAction(input: Decision.ChooseMainAction): MainAction {
        return MainAction.PullDie
    }
}
