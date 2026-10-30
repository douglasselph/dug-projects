package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.battle.HumanBattleMainPolicy
import dugsolutions.leaf.v35.player.decision.battle.HumanBattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy
import kotlin.test.Test
import kotlin.test.assertIs

class BattlePacingHumanWiringTest {
    @Test fun `human baseline factory wires substantive Buy and Battle policies`() {
        val player = PlayerDecisionFactory.humanBaseline().create()
        assertIs<HumanBaselineBuyStrategy>(player.buy)
        assertIs<HumanBaselineBattleStrategy>(player.battle)
        assertIs<HumanBattleMainPolicy>(player.battleMain)
        assertIs<HumanBattleSupportPolicy>(player.battleSupport)
    }

    @Test fun `mechanical control factory does not claim Human Baseline decisions`() {
        val player = PlayerDecisionFactory.mechanicalControl().create()
        assertIs<MechanicalBuyStrategy>(player.buy)
        assertIs<MechanicalBattleStrategy>(player.battle)
    }
}
