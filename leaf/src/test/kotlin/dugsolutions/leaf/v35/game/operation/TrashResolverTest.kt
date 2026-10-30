package dugsolutions.leaf.v35.game.operation

import dugsolutions.leaf.v35.error.InvalidGameStateException
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.TrashDestination
import dugsolutions.leaf.v35.effect.EffectTestFixture
import dugsolutions.leaf.v35.effect.FixedEffectDie
import dugsolutions.leaf.v35.random.die.DieSides
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrashResolverTest {

    private val resolver =
        TrashResolver()

    @Test
    fun freshGameGraftBedStartsWithZeroD4s() {
        val actor =
            EffectTestFixture.player(1)
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        assertEquals(
            0,
            game.grove.graftBed.count(
                DieSides.D4
            )
        )
    }

    @Test
    fun trashD4_removesItFromHandAndReturnsItToGraftBed() {
        val d4 =
            FixedEffectDie(4, 3)
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(d4)
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val result =
            resolver.trashDieFromHand(
                game = game,
                player = actor,
                die = d4
            )

        assertTrue(actor.dice.hand.isEmpty())
        assertEquals(
            1,
            game.grove.graftBed.count(
                DieSides.D4
            )
        )
        assertEquals(
            DieSides.D4,
            result.sides
        )
        assertTrue(result.returnedToGraftBed)
        val event = game.chronicle.entries.filterIsInstance<GameEntry.TrashDie>().single()
        assertEquals(TrashDestination.GRAFT_BED, event.destination)
        // The returned die is available to a later Grove acquisition.
        assertTrue(game.grove.graftBed.take(DieSides.D4))
        assertEquals(0, game.grove.graftBed.count(DieSides.D4))
    }

    @Test
    fun trashD6Plus_removesItFromHandAndLeavesGame() {
        val d6 =
            FixedEffectDie(6, 5)
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(d6)
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val beforeD6 =
            game.grove.graftBed.count(
                DieSides.D6
            )

        val result =
            resolver.trashDieFromHand(
                game = game,
                player = actor,
                die = d6
            )

        assertTrue(actor.dice.hand.isEmpty())
        assertEquals(
            beforeD6,
            game.grove.graftBed.count(
                DieSides.D6
            )
        )
        assertEquals(
            0,
            game.grove.graftBed.count(
                DieSides.D4
            )
        )
        assertEquals(DieSides.D6, result.sides)
        assertFalse(result.returnedToGraftBed)
        val event = game.chronicle.entries.filterIsInstance<GameEntry.TrashDie>().single()
        assertEquals(TrashDestination.OUT_OF_GAME, event.destination)
    }

    @Test
    fun trashD20_alsoLeavesGameWithoutChangingGraftBed() {
        val d20 =
            FixedEffectDie(20, 17)
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(d20)
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )

        val before =
            game.grove.graftBed.counts

        resolver.trashDieFromHand(
            game = game,
            player = actor,
            die = d20
        )

        assertEquals(
            before,
            game.grove.graftBed.counts
        )
    }

    @Test
    fun everyLargerDieSizeLeavesGameWithoutChangingGraftBed() {
        for (sides in listOf(6, 8, 10, 12, 20)) {
            val die = FixedEffectDie(sides, 1)
            val actor = EffectTestFixture.player(id = 1, hand = listOf(die))
            val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))
            val before = game.grove.graftBed.counts

            val result = resolver.trashDieFromHand(game, actor, die)

            assertTrue(actor.dice.hand.isEmpty(), "D$sides")
            assertFalse(result.returnedToGraftBed, "D$sides")
            assertEquals(before, game.grove.graftBed.counts, "D$sides")
            val event = game.chronicle.entries.filterIsInstance<GameEntry.TrashDie>().single()
            assertEquals(TrashDestination.OUT_OF_GAME, event.destination, "D$sides")
        }
    }

    @Test
    fun trashingTwoD4sReturnsBothToGraftBed() {
        val first = FixedEffectDie(4, 1)
        val second = FixedEffectDie(4, 2)
        val actor = EffectTestFixture.player(id = 1, hand = listOf(first, second))
        val game = EffectTestFixture.game(actor, EffectTestFixture.player(2))

        resolver.trashDieFromHand(game, actor, first)
        resolver.trashDieFromHand(game, actor, second)

        assertTrue(actor.dice.hand.isEmpty())
        assertEquals(2, game.grove.graftBed.count(DieSides.D4))
        assertEquals(
            listOf(TrashDestination.GRAFT_BED, TrashDestination.GRAFT_BED),
            game.chronicle.entries.filterIsInstance<GameEntry.TrashDie>().map { it.destination }
        )
    }

    @Test
    fun dieNotInHand_isRejectedBeforeMutation() {
        val owned =
            FixedEffectDie(4, 2)
        val notOwned =
            FixedEffectDie(6, 3)
        val actor =
            EffectTestFixture.player(
                id = 1,
                hand = listOf(owned)
            )
        val game =
            EffectTestFixture.game(
                actor,
                EffectTestFixture.player(2)
            )
        val before =
            game.grove.graftBed.counts

        assertFailsWith<
            InvalidGameStateException
        > {
            resolver.trashDieFromHand(
                game = game,
                player = actor,
                die = notOwned
            )
        }

        assertEquals(
            listOf(owned),
            actor.dice.hand
        )
        assertEquals(
            before,
            game.grove.graftBed.counts
        )
    }
    @Test
    fun trashUsesExactIdentityWhenEquivalentDiceAreInHand() {
        val first = FixedEffectDie(6, 4)
        val second = FixedEffectDie(6, 4)
        val actor = EffectTestFixture.player(
            id = 1,
            hand = listOf(first, second)
        )
        val game = EffectTestFixture.game(
            actor,
            EffectTestFixture.player(2)
        )

        resolver.trashDieFromHand(
            game = game,
            player = actor,
            die = second
        )

        assertEquals(1, actor.dice.handSize)
        assertTrue(actor.dice.hand.single() === first)
        assertFalse(actor.dice.hand.any { it === second })
    }

}
