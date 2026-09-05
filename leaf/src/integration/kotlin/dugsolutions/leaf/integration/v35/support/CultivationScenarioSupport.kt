package dugsolutions.leaf.integration.v35.support

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly
import dugsolutions.leaf.v35.tokens.Critter
import dugsolutions.leaf.v35.tokens.Token
import dugsolutions.leaf.v35.wisp.domain.WispCard

/** Small state-construction helpers used only by deterministic integration scenarios. */
data class DieSpec(
    val sides: DieSides,
    val value: Int = 1
)

fun IntegrationGameHarness.player(id: Int): Player =
    game.players.single { it.id.value == id }

fun IntegrationGameHarness.setPlayerDice(
    playerId: Int,
    supply: List<DieSpec> = emptyList(),
    hand: List<DieSpec> = emptyList(),
    discard: List<DieSpec> = emptyList()
) {
    val player = player(playerId)
    player.dice.clear()
    supply.forEach { player.dice.addToSupply(game.dieFactory(it.sides).adjustTo(it.value)) }
    hand.forEach { player.dice.addToHand(game.dieFactory(it.sides).adjustTo(it.value)) }
    discard.forEach { player.dice.addToDiscard(game.dieFactory(it.sides).adjustTo(it.value)) }
}

fun IntegrationGameHarness.giveWater(playerId: Int, count: Int = 1) {
    repeat(count) {
        val token = checkNotNull(game.grove.tokens.pull(Token.WATER)) {
            "Grove ran out of Water while preparing integration scenario"
        }
        player(playerId).tokens.add(token)
    }
}

fun IntegrationGameHarness.giveCritter(
    playerId: Int,
    critter: Critter,
    count: Int = 1
) {
    repeat(count) {
        check(game.grove.critters.remove(critter)) {
            "Grove ran out of $critter while preparing integration scenario"
        }
        player(playerId).critters.add(critter)
    }
}

fun IntegrationGameHarness.giveButterfly(
    playerId: Int,
    butterfly: Butterfly,
    faceUp: Boolean = true
) {
    check(game.grove.butterflies.remove(butterfly)) {
        "Grove does not contain $butterfly"
    }
    val player = player(playerId)
    player.butterflies.add(butterfly)
    if (!faceUp) {
        check(player.butterflies.faceDown(butterfly))
    }
}

fun IntegrationGameHarness.givePendingMulch(
    playerId: Int,
    sides: DieSides
) {
    val empty = game.grove.tokens.mulchTokens.firstOrNull { it.sides == null }
    checkNotNull(empty) { "Grove has no empty Mulch token" }
    check(game.grove.tokens.pull(empty) != null)
    player(playerId).tokens.add(Token.PENDING_MULCH(sides))
}

fun IntegrationGameHarness.giveStoredMulch(
    playerId: Int,
    sides: DieSides
) {
    val empty = game.grove.tokens.mulchTokens.firstOrNull { it.sides == null }
    checkNotNull(empty) { "Grove has no empty Mulch token" }
    check(game.grove.tokens.pull(empty) != null)
    player(playerId).tokens.add(Token.MULCH(sides))
}

fun IntegrationGameHarness.giveNextWisp(playerId: Int): WispCard {
    val card = checkNotNull(game.grove.wispDeck.draw()) {
        "Wisp deck is empty while preparing integration scenario"
    }
    player(playerId).wisps.add(card)
    return card
}

fun IntegrationGameHarness.graftPlant(
    playerId: Int,
    nameOrTitle: String,
    faceUp: Boolean = false
): CreatureCard {
    val player = player(playerId)
    val card = catalog.requirePlant(nameOrTitle)
    val placement = player.creature.legalPlacements(card).firstOrNull()
    checkNotNull(placement) {
        "No legal placement for $nameOrTitle while preparing integration scenario"
    }
    val grafted = player.creature.graft(card, placement)
    if (faceUp) {
        check(player.creature.faceUp(grafted.id))
    }
    return checkNotNull(player.creature.get(grafted.id))
}
