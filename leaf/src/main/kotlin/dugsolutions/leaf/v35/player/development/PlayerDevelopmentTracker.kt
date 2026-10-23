package dugsolutions.leaf.v35.player.development

import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.creature.CreatureCard

/** One physical Plant acquisition: kept even when subsequently Snipped. */
data class PlantDevelopment(
    val copyId: Int,
    val cardName: String,
    val cardId: String,
    val category: String,
    val cost: Int,
    val side: String,
    val x: Int,
    val y: Int,
    val fate: String
)

/** A die identity records original size, current size, and eventual fate. */
data class DieDevelopment(
    val assetId: Long,
    val acquiredSides: Int,
    val finalSides: Int,
    val fate: String,
    val upgrades: Int
)

data class PlayerDevelopmentSnapshot(
    val plants: List<PlantDevelopment>,
    val dice: List<DieDevelopment>
) {
    fun countPlants(name: String, fate: String? = null) =
        plants.count { it.cardName == name && (fate == null || it.fate == fate) }

    fun countDice(sides: Int, fate: String? = null) =
        dice.count { it.finalSides == sides && (fate == null || it.fate == fate) }

    val upgradeCount: Int get() = dice.sumOf { it.upgrades }
}

/**
 * Optional D4 research instrumentation, attached once per Player per game.
 * Observes real ownership changes; never makes a decision or mutates gameplay.
 * Uses unique creature copy IDs and stable die IDs instead of shared card definitions.
 *
 * Attach at game setup, before the first action. If attached later, dice already
 * owned are labeled STARTING (because historical acquisition time is unknowable).
 * The current snapshot reflects Plant and dice fates without collapsing duplicates.
 */
class PlayerDevelopmentTracker private constructor(private val player: Player) {
    private data class PlantHistory(val card: CreatureCard, var fate: String = "GRAFTED")
    private data class DieHistory(
        val assetId: Long,
        val acquiredSides: Int,
        var finalSides: Int,
        var fate: String,
        var upgrades: Int = 0
    )

    private val plants = linkedMapOf<Int, PlantHistory>()
    private val dice = linkedMapOf<Long, DieHistory>()

    init {
        player.creature.cards.forEach(::grafted)
        (player.dice.supply + player.dice.hand + player.dice.discard).forEach { die ->
            acquired(player.dice.stableDieId(die), die.sides, "STARTING")
        }
        player.creature.onGraft = ::grafted
        player.creature.onSnip = { snipped ->
            plants[snipped.id.value]?.fate = "SNIPPED"
        }
        player.dice.onAcquired = { id, sides -> acquired(id, sides, "ACQUIRED") }
        player.dice.onRemoved = { id, sides, reason ->
            val history = dice[id]
            if (history == null) acquired(id, sides, "ACQUIRED")
            dice[id]?.fate = reason
        }
        player.dice.onUpgrade = { id, from, to ->
            val history = dice[id]
            if (history == null) acquired(id, from, "STARTING")
            dice[id]?.apply { finalSides = to; upgrades++ }
        }
    }

    private fun grafted(card: CreatureCard) {
        plants.putIfAbsent(card.id.value, PlantHistory(card))
    }

    private fun acquired(id: Long, sides: Int, origin: String) {
        dice.putIfAbsent(id, DieHistory(id, sides, sides, origin))
    }

    fun snapshot(): PlayerDevelopmentSnapshot = PlayerDevelopmentSnapshot(
        plants = plants.values.map { (card, fate) ->
            PlantDevelopment(
                copyId = card.id.value,
                cardName = card.card.title,
                cardId = card.card.name,
                category = card.card.type.name,
                cost = card.card.cost,
                side = card.side.name,
                x = card.position.x,
                y = card.position.y,
                fate = fate
            )
        },
        dice = dice.values.map { die ->
            DieDevelopment(die.assetId, die.acquiredSides, die.finalSides,
                if (die.fate == "STARTING" || die.fate == "ACQUIRED") "OWNED" else die.fate,
                die.upgrades)
        }
    )

    companion object {
        /** Only one tracker should be attached to any given Player at once. */
        fun attach(player: Player): PlayerDevelopmentTracker = PlayerDevelopmentTracker(player)
    }
}
