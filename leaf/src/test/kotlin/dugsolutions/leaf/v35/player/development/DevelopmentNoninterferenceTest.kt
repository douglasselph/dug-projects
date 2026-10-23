package dugsolutions.leaf.v35.player.development

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.random.die.SampleDie
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** D8: attached research observers must not alter game-visible ownership transitions. */
class DevelopmentNoninterferenceTest {
    private fun root() = PlantCard(
        quantity = 3, name = "Root_05_01", title = "Root Well",
        type = PlantType.ROOT, cost = 5, lineIcon = null,
        vpIcon = "", typeIcon = "", fgColor = "", textColor = "",
        fullImage = "", backgroundImage = "", cardBackgroundImage = "",
        effect = GameEffect.UNKNOWN
    )

    private fun execute(tracked: Boolean): Pair<List<String>, PlayerDevelopmentSnapshot?> {
        val player = Player(PlayerId(0), mockk<DecisionDirector>())
        val tracker = if (tracked) PlayerDevelopmentTracker.attach(player) else null
        val die = SampleDie().d4
        val trace = mutableListOf<String>()
        player.dice.addToHand(die)
        trace += "hand:${player.dice.hand.map { it.sides }}"
        val first = player.creature.graft(root(), player.creature.legalPlacements(root()).first())
        trace += "graft:${first.card.title}:${player.creature.cards.size}"
        // Snapshot is passive: requesting one cannot modify or reset the recorder.
        tracker?.snapshot()
        trace += "snip:${player.creature.snip(first.id)?.card?.title}:${player.creature.cards.size}"
        val removed = player.dice.removeExactFromHand(die)
        trace += "removed:${removed != null}:${player.dice.hand.size}"
        if (removed != null) player.dice.recordRemoval(removed, "DOOM")
        return trace to tracker?.snapshot()
    }

    @Test fun `tracking and snapshot reads preserve gameplay transitions`() {
        val (without, _) = execute(false)
        val (with, snapshot) = execute(true)
        assertEquals(without, with)
        assertNotNull(snapshot)
        assertEquals(1, snapshot!!.countPlants("Root Well", "SNIPPED"))
        assertEquals(1, snapshot.countDice(4, "DOOM"))
        assertEquals(0, snapshot.upgradeCount)
    }

    @Test fun `past snapshot remains unchanged after later mutation`() {
        val player = Player(PlayerId(0), mockk<DecisionDirector>())
        val tracker = PlayerDevelopmentTracker.attach(player)
        val before = tracker.snapshot()
        player.creature.graft(root(), player.creature.legalPlacements(root()).first())
        assertTrue(before.plants.isEmpty())
        assertEquals(1, tracker.snapshot().countPlants("Root Well", "GRAFTED"))
    }
}
