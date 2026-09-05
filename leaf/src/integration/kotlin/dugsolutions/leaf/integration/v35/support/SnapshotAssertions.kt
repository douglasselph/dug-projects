package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameStatus
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.tokens.Critter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Reusable assertions for integration snapshots.
 *
 * These helpers intentionally compare stable, player-facing state rather than
 * implementation object identity. Individual sanity tests can therefore stay
 * short while still producing useful failure messages.
 */
object SnapshotAssertions {

    fun assertReadyGame(
        snapshot: GameSnapshot,
        expectedPlayerCount: Int,
        expectedRoundNames: List<String>
    ) {
        assertEquals(GameStatus.READY, snapshot.status, "game status")
        assertEquals(0, snapshot.roundNumber, "round number")
        assertNull(snapshot.currentRound, "current Round before first reveal")
        assertEquals(expectedRoundNames.size, snapshot.roundCardsRemaining, "Round cards remaining")
        assertEquals(expectedRoundNames, snapshot.roundDrawPile.map { it.name }, "Round draw order")
        assertEquals(expectedPlayerCount, snapshot.players.size, "player count")
        assertEquals(
            (1..expectedPlayerCount).map(::PlayerId).toSet(),
            snapshot.players.keys,
            "player ids"
        )
    }

    fun assertCurrentRound(
        snapshot: GameSnapshot,
        name: String,
        type: RoundCardType,
        firstEffect: GameEffect? = null,
        secondEffect: GameEffect? = null
    ): RoundCardSnapshot {
        assertNotNull(snapshot.currentRound, "Expected a revealed Round")
        val current = requireNotNull(snapshot.currentRound)
        assertEquals(name, current.name, "current Round name")
        assertEquals(type, current.type, "current Round type")
        firstEffect?.let { assertEquals(it, current.firstEffect, "first Round effect") }
        secondEffect?.let { assertEquals(it, current.secondEffect, "second Round effect") }
        return current
    }

    fun assertRoundDrawPile(
        snapshot: GameSnapshot,
        vararg expectedNames: String
    ) {
        assertEquals(expectedNames.toList(), snapshot.roundDrawPile.map { it.name }, "Round draw pile")
        assertEquals(expectedNames.size, snapshot.roundCardsRemaining, "Round cards remaining")
    }

    fun assertInitialPlayer(player: PlayerSnapshot) {
        assertEquals(0, player.vp, "P${player.id.value} VP")
        assertDiceCounts(
            player.supply,
            mapOf(DieSides.D4 to 3, DieSides.D6 to 3),
            "P${player.id.value} Supply"
        )
        assertTrue(player.supply.all { it.value == 1 }, "P${player.id.value} starting dice should show 1")
        assertTrue(player.hand.isEmpty(), "P${player.id.value} Hand should start empty")
        assertTrue(player.discard.isEmpty(), "P${player.id.value} Discard should start empty")
        assertEquals(0, player.bees, "P${player.id.value} Bees")
        assertEquals(0, player.worms, "P${player.id.value} Worms")
        assertEquals(Critter.BEE.baseValue, player.beeValue, "P${player.id.value} Bee value")
        assertEquals(Critter.WORM.baseValue, player.wormValue, "P${player.id.value} Worm value")
        assertEquals(0, player.water, "P${player.id.value} Water")
        assertEquals(0, player.mulch, "P${player.id.value} Mulch")
        assertEquals(0, player.pendingMulch, "P${player.id.value} pending Mulch")
        assertTrue(player.butterflies.isEmpty(), "P${player.id.value} Butterflies should start empty")
        assertTrue(player.wisps.isEmpty(), "P${player.id.value} Wisps should start empty")
        assertTrue(player.plants.isEmpty(), "P${player.id.value} Plant Creature should start empty")
    }

    fun assertInitialGrove(
        grove: GroveSnapshot,
        expectedPlantNames: List<String>,
        expectedWispNames: List<String>
    ) {
        assertEquals(9, grove.plantStacks.size, "Plant stack count")
        assertEquals(expectedPlantNames, grove.plantStacks.map { it.name }, "selected Plant stacks")
        assertTrue(grove.plantStacks.all { it.remaining == 6 }, "Every selected Plant stack should start with 6 cards")

        assertEquals(0, grove.graftBed.getValue(DieSides.D4), "Graft Bed D4 count")
        listOf(DieSides.D6, DieSides.D8, DieSides.D10, DieSides.D12, DieSides.D20).forEach { sides ->
            assertEquals(9, grove.graftBed.getValue(sides), "Graft Bed $sides count")
        }

        assertEquals(9, grove.bees, "Grove Bees")
        assertEquals(9, grove.worms, "Grove Worms")
        assertEquals(9, grove.water, "Grove Water")
        assertEquals(9, grove.mulch, "Grove Mulch")
        assertTrue(grove.mulchTokens.all { it.storedDieSides == null }, "Grove Mulch should start empty")
        assertEquals(4, grove.butterflies.size, "Grove Butterflies")
        assertTrue(grove.butterflies.all { it.faceUp }, "Grove Butterflies should start face up")
        assertEquals(expectedWispNames.size, grove.wispCardsRemaining, "Wisp cards remaining")
        assertEquals(expectedWispNames, grove.wispDrawPile.map { it.name }, "Wisp draw order")
    }

    fun assertDiceCounts(
        actual: List<DieSnapshot>,
        expected: Map<DieSides, Int>,
        label: String = "dice"
    ) {
        val actualCounts = DieSides.entries.associateWith { sides ->
            actual.count { it.dieSides == sides }
        }.filterValues { it > 0 }
        val normalizedExpected = expected.filterValues { it > 0 }
        assertEquals(normalizedExpected, actualCounts, "$label counts")
    }

    fun assertDice(
        actual: List<DieSnapshot>,
        expected: List<DieSnapshot>,
        label: String = "dice"
    ) {
        val order = compareBy<DieSnapshot>({ it.sides }, { it.value })
        assertEquals(expected.sortedWith(order), actual.sortedWith(order), label)
    }

    fun assertPlayerResources(
        player: PlayerSnapshot,
        bees: Int,
        worms: Int,
        water: Int,
        mulch: Int,
        wisps: Int = player.wispCards.size,
        plants: Int = player.plants.size
    ) {
        assertEquals(bees, player.bees, "P${player.id.value} Bees")
        assertEquals(worms, player.worms, "P${player.id.value} Worms")
        assertEquals(water, player.water, "P${player.id.value} Water")
        assertEquals(mulch, player.mulch, "P${player.id.value} Mulch")
        assertEquals(wisps, player.wispCards.size, "P${player.id.value} Wisps")
        assertEquals(plants, player.plants.size, "P${player.id.value} Plants")
    }

    fun assertPlantStack(
        grove: GroveSnapshot,
        name: String,
        remaining: Int
    ): PlantStackSnapshot {
        val stack = grove.plantStacks.singleOrNull { it.name == name }
        assertNotNull(stack, "Expected Plant stack $name")
        val found = requireNotNull(stack)
        assertEquals(remaining, found.remaining, "Plant stack $name remaining")
        return found
    }

    fun assertNoRoundHasBeenRevealed(snapshot: GameSnapshot) {
        assertEquals(0, snapshot.roundNumber)
        assertNull(snapshot.currentRound)
    }

    fun assertRoundWasRevealedWithoutPlayerMutation(
        before: GameSnapshot,
        after: GameSnapshot
    ) {
        assertEquals(before.players, after.players, "Reveal should not mutate player state")
        assertEquals(before.grove, after.grove, "Reveal should not mutate Grove state")
        assertFalse(before.roundNumber == after.roundNumber, "Reveal should advance roundNumber")
    }
}
