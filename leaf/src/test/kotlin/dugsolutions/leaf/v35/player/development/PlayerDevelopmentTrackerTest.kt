package dugsolutions.leaf.v35.player.development

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.die.SampleDie
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PlayerDevelopmentTrackerTest {
    private fun player() = Player(PlayerId(0), mockk<DecisionDirector>())
    private fun root() = PlantCard(
        quantity = 3, name = "Root_05_01", title = "Root Well",
        type = PlantType.ROOT, cost = 5, lineIcon = null,
        vpIcon = "", typeIcon = "", fgColor = "", textColor = "",
        fullImage = "", backgroundImage = "", cardBackgroundImage = "",
        effect = GameEffect.UNKNOWN
    )

    @Test
    fun tracksNamedCopiesAndSnipsIndividually() {
        val player = player()
        val tracker = PlayerDevelopmentTracker.attach(player)
        player.creature.graft(root(), player.creature.legalPlacements(root())[0])
        val second = player.creature.graft(root(), player.creature.legalPlacements(root())[0])
        assertEquals(2, tracker.snapshot().countPlants("Root Well"))
        player.creature.snip(second.id)
        assertEquals(1, tracker.snapshot().countPlants("Root Well", "SNIPPED"))
        assertEquals(1, tracker.snapshot().countPlants("Root Well", "GRAFTED"))
    }

    @Test
    fun tracksDiceAcquisitionDoomAndUpgradeWithoutDoubleCounting() {
        val player = player()
        val tracker = PlayerDevelopmentTracker.attach(player)
        val factory = SampleDie()
        val d4 = factory.d4
        player.dice.addToHand(d4)
        val d8 = factory.d8
        player.dice.inheritStableDieId(d4, d8)
        player.dice.removeExactFromHand(d4)
        player.dice.addToDiscard(d8)
        assertEquals(1, tracker.snapshot().dice.size)
        assertEquals(1, tracker.snapshot().upgradeCount)
        assertEquals(1, tracker.snapshot().countDice(8, "OWNED"))
        player.dice.recordRemoval(d8, "DOOM")
        assertEquals(1, tracker.snapshot().countDice(8, "DOOM"))
    }

    @Test
    fun snapshotsAreIndependentOfLaterChanges() {
        val player = player()
        val tracker = PlayerDevelopmentTracker.attach(player)
        val before = tracker.snapshot()
        player.creature.graft(root(), player.creature.legalPlacements(root())[0])
        assertEquals(0, before.plants.size)
        assertEquals(1, tracker.snapshot().plants.size)
    }
}
