package dugsolutions.leaf.v35.research.provenance

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.die.Die
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AssetProvenanceTest {
    @Test
    fun forgetMeNotRecordsNaturalRecycleDistanceAndAcceleration() {
        val target = die(20, 7)
        val player = Player(
            PlayerId(1), DecisionDirector.baseline(),
            dice = PlayerDice(
                supply = listOf(die(4, 2), die(6, 3)),
                discard = listOf(die(8, 4), die(12, 6), target)
            )
        )
        val provenance = AssetProvenance()

        provenance.recordForgetMeNot(player, target, "Flower_17_02")

        val record = provenance.forgetMeNot.single()
        assertEquals("Flower_17_02", record.sourceCard)
        assertEquals(20, record.dieSides)
        assertEquals(5, record.estimatedNaturalDrawsUntilAvailable)
        assertEquals(5, record.estimatedDrawsAccelerated)
    }

    private fun die(sides: Int, value: Int): Die = object : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }
}
