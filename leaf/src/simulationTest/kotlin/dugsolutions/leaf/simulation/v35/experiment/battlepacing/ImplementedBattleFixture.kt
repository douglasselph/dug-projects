package dugsolutions.leaf.simulation.v35.experiment.battlepacing

import dugsolutions.leaf.v35.effect.CardEffectReadiness
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.plant.PlantCardManager
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.domain.RoundCard
import dugsolutions.leaf.v35.round.domain.RoundCardType
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.domain.WispCard

/**
 * Full, legal nine-slot Grove and physical Round/Wisp decks for scheduler tests.
 *
 * These tests assert Battle timing and Support action accounting. They must not
 * accidentally execute *other*, intentionally pending effects while advancing
 * three Cultivation rounds to the requested Battle opening. Separate catalog
 * and readiness tests continue to check the complete official deck.
 *
 * All cards are real definitions loaded from the official CSVs: no synthetic
 * cards, rule changes, or no-op replacements. This is an isolated test fixture.
 */
internal object ImplementedBattleFixture {
    fun plants(manager: PlantCardManager): List<PlantCard> = listOf(
        // PlantCardManager.getCard() is keyed by the CSV `name` (not its display `title`).
        "Root_05_02" to "Root Four More",
        "Root_07_02" to "Root Appreciation", // Free Worm Support
        "Root_09_04" to "Root Recall",
        "Vine_07_03" to "Vine and Punishment",
        "Vine_09_01" to "Low & Behold",
        "Vine_11_03" to "Rise and Vine",
        "zFlower_11_01" to "Alluring Nectar", // Free Butterfly Support
        "zFlower_14_01" to "Bee-loved Bloom", // Free Bee Support
        "zFlower_17_04" to "Queen’s Blossom"
    ).map { (id, expectedTitle) ->
        val card = requireNotNull(manager.getCard(id)) {
            "Fixture Plant absent: $expectedTitle (CSV name $id)"
        }
        check(card.title == expectedTitle) {
            "Fixture Plant $id changed title: expected '$expectedTitle', actual '${card.title}'"
        }
        card
    }
        .also { selected ->
            check(selected.none { it.effect in CardEffectReadiness.pending }) {
                "Scheduler fixture contains an unfinished Plant effect"
            }
        }

    /** Use current CSV-backed physical Wisps, omitting only not-yet-implemented ones. */
    fun wisps(manager: WispCardManager, includeResolve: Boolean = true): List<WispCard> =
        manager.getAllCards().cards.asSequence()
            .filter { it.effect !in CardEffectReadiness.pending }
            .filter { includeResolve || it.effect != GameEffect.RESOLVE_STRIKE_IMMEDIATELY_AND_CLEAR_ROW }
            .flatMap { card -> List(card.quantity) { card }.asSequence() }
            .toList()
            .also { check(it.isNotEmpty()) { "Scheduler fixture has no executable Wisps" } }

    /** A physical 3/2/2 deck that never offers a pending Round effect. */
    fun rounds(manager: RoundCardManager): List<RoundCard> {
        val setup = GameRoundSetup.standard()
        val cultivationNames = listOf(
            "Resource_Compost_Mulch",
            "Resource_Sunlight_Compost",
            "Resource_Sunlight_Mulch",
            "Resource_Sunlight_Water",
            "Resource_Water_Compost",
            "Resource_Water_Mulch",
            "Resource_Compost_Mulch" // second physical copy (quantity 2)
        )
        val battleNames = listOf(
            "Battle_Barkskin_Flow",
            "Battle_Bloom_Burrow",
            "Battle_Whisper_Burst"
        )
        check(setup.cultivationRounds == cultivationNames.size && setup.battleRounds == battleNames.size)
        val cultivation = cultivationNames.map { name ->
            requireNotNull(manager.getCard(name)) { "Fixture Round absent: $name" }
        }
        val battle = battleNames.map { name ->
            requireNotNull(manager.getCard(name)) { "Fixture Round absent: $name" }
        }
        val chosen = cultivation + battle
        chosen.groupingBy { it.name }.eachCount().forEach { (name, count) ->
            check(count <= requireNotNull(manager.getCard(name)).quantity) {
                "Fixture exceeds physical Round copies of $name"
            }
        }
        check(chosen.all {
            it.firstEffect.effect !in CardEffectReadiness.pending &&
                it.secondEffect.effect !in CardEffectReadiness.pending
        }) { "Scheduler fixture includes unfinished Round effects" }
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
