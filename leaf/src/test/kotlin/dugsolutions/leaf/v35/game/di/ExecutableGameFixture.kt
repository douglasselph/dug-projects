package dugsolutions.leaf.v35.game.di

import dugsolutions.leaf.v35.effect.CardEffectReadiness
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.WispCardRegistry
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Test-only, CSV-backed subset for full-game tests whose subject is lifecycle,
 * dependency injection, deterministic replay, or bookkeeping -- not coverage
 * of every revised card effect. Production games always use the full catalog.
 *
 * Do not remove pending effects from CardEffectReadiness to make tests pass.
 * The full official deck is intentionally not executable until NC-05..NC-22.
 */
internal object ExecutableGameFixture {
    private val plantIds = listOf(
        "Root_05_02", "Root_07_02", "Root_09_04",
        "Vine_07_03", "Vine_09_01", "Vine_11_03",
        "zFlower_11_01", "zFlower_14_01", "zFlower_17_04"
    )

    fun plants(registry: PlantCardRegistry): List<PlantCard> =
        plantIds.map { id ->
            requireNotNull(registry.getCard(id)) { "Executable fixture Plant missing: $id" }
        }.also { cards ->
            check(cards.size == 9 && cards.map { it.type to it.cost }.toSet().size == 9) {
                "Executable fixture must fill all nine distinct Grove slots"
            }
            check(cards.none { it.effect in CardEffectReadiness.pending }) {
                "Executable fixture has a Plant with unfinished mechanics"
            }
        }

    /** Expand actual physical Wisp copies, excluding only unfinished/special scheduler effects. */
    fun wisps(registry: WispCardRegistry): List<WispCard> =
        registry.getAllCards().asSequence()
            .filter { it.effect !in CardEffectReadiness.pending }
            // The separate Battle tests cover the early-Resolve Wisp precisely.
            .filter { it.effect != GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW }
            .flatMap { card -> List(card.quantity) { card }.asSequence() }
            .toList().also { cards ->
                check(cards.isNotEmpty()) { "Executable fixture has no usable Wisps" }
            }

    /** Preserve the requested round pattern using CSV-backed, executable physical cards. */
    fun rounds(registry: RoundCardRegistry, setup: GameRoundSetup): List<RoundCard> {
        val available = registry.getAllCards()
            .filter { card ->
                card.firstEffect.effect !in CardEffectReadiness.pending &&
                    card.secondEffect.effect !in CardEffectReadiness.pending
            }
        val cultivation = available.filter { it.type == RoundCardType.CULTIVATION }
            .flatMap { card -> List(card.quantity) { card } }
        val battle = available.filter { it.type == RoundCardType.BATTLE }
            .flatMap { card -> List(card.quantity) { card } }
        check(cultivation.size >= setup.cultivationRounds && battle.size >= setup.battleRounds) {
            "Not enough executable physical Round cards for $setup"
        }
        var c = 0
        var b = 0
        return setup.roundTypes.map { type ->
            when (type) {
                RoundCardType.CULTIVATION -> cultivation[c++]
                RoundCardType.BATTLE -> battle[b++]
            }
        }
    }
}
