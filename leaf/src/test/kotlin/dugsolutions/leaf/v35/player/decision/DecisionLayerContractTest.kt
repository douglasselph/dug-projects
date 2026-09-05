package dugsolutions.leaf.v35.player.decision

import dugsolutions.leaf.v35.player.decision.baseline.battle.HumanBaselineBattleStrategy
import dugsolutions.leaf.v35.player.decision.baseline.buy.HumanBaselineBuyStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.battle.MechanicalBattleStrategy
import dugsolutions.leaf.v35.player.decision.mechanical.buy.MechanicalBuyStrategy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class DecisionLayerContractTest {

    @Test
    fun `mechanical control and human baseline are distinct core layers`() {
        val mechanical = DecisionDirector.mechanicalControl()
        val human = DecisionDirector.humanBaseline()

        assertIs<MechanicalBattleStrategy>(mechanical.battle)
        assertIs<MechanicalBuyStrategy>(mechanical.buy)
        assertIs<HumanBaselineBattleStrategy>(human.battle)
        assertIs<HumanBaselineBuyStrategy>(human.buy)
        assertNotSame(mechanical.battle, human.battle)
        assertNotSame(mechanical.buy, human.buy)
    }

    @Test
    fun `baseline alias now selects human baseline`() {
        val baseline = DecisionDirector.baseline()

        assertIs<HumanBaselineBattleStrategy>(baseline.battle)
        assertIs<HumanBaselineBuyStrategy>(baseline.buy)
    }

    @Test
    fun `human baseline explicitly reports heuristic implementation pending`() {
        assertTrue(HumanBaseline.NAME.isNotBlank())
        assertFalse(HumanBaseline.HEURISTICS_IMPLEMENTED)
    }
}
