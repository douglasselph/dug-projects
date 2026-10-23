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
    fun crossPlayerSwapRecordsBothDeparturesAndBothArrivals() {
        val a = player()
        val b = Player(PlayerId(1), mockk<DecisionDirector>())
        val aDie = SampleDie().d4
        val bDie = SampleDie().d4
        a.dice.addToHand(aDie)
        b.dice.addToHand(bDie)
        val aTracker = PlayerDevelopmentTracker.attach(a)
        val bTracker = PlayerDevelopmentTracker.attach(b)

        assertEquals(true, a.dice.swapExactHandDieWith(b.dice, aDie, bDie))
        assertEquals(1, aTracker.snapshot().countDice(4, "SWAPPED_OUT"))
        assertEquals(1, bTracker.snapshot().countDice(4, "SWAPPED_OUT"))
        assertEquals(1, aTracker.snapshot().countDice(4, "OWNED"))
        assertEquals(1, bTracker.snapshot().countDice(4, "OWNED"))
        assertEquals(2, aTracker.snapshot().dice.size)
        assertEquals(2, bTracker.snapshot().dice.size)
    }

    @Test
    fun stolenDieRetainsHistoryForFormerAndNewOwner() {
        val former = player()
        val thief = Player(PlayerId(1), mockk<DecisionDirector>())
        val die = SampleDie().d4
        former.dice.addToHand(die)
        val formerTracker = PlayerDevelopmentTracker.attach(former)
        val thiefTracker = PlayerDevelopmentTracker.attach(thief)
        val taken = former.dice.removeExactFromHand(die)!!
        former.dice.recordTransferOut(taken, "STOLEN")
        thief.dice.addToDiscard(taken)

        assertEquals(1, formerTracker.snapshot().countDice(4, "STOLEN"))
        assertEquals(1, thiefTracker.snapshot().countDice(4, "OWNED"))
    }

    @Test
    fun mulchStorageIsRecordedAsRemovalNotUpgrade() {
        val owner = player()
        val die = SampleDie().d8
        owner.dice.addToHand(die)
        val tracker = PlayerDevelopmentTracker.attach(owner)
        val removed = owner.dice.removeExactFromHand(die)!!
        owner.dice.recordRemoval(removed, "MULCH_STORED")
        assertEquals(1, tracker.snapshot().countDice(8, "MULCH_STORED"))
        assertEquals(0, tracker.snapshot().upgradeCount)
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
