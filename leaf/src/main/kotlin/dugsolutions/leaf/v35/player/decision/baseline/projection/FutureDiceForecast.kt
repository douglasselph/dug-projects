package dugsolutions.leaf.v35.player.decision.baseline.projection

import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.player.decision.context.PlayerBoardView
import dugsolutions.leaf.v35.round.domain.RoundCardType

/**
 * One physical die in a *snapshot*, not a sampled future roll. IDs distinguish
 * equivalent dice, including during an Upgrade. They are local to the snapshot.
 */
data class FutureDie(val id: String, val sides: Int) {
    init {
        require(id.isNotBlank()) { "Forecast die ID cannot be blank" }
        require(sides in setOf(4, 6, 8, 10, 12, 20)) { "Unsupported forecast die: D$sides" }
    }
}

/**
 * Read-only input zones at the current decision boundary. The current Hand is
 * NOT scored: it is returned to Discard at the start of this future-only model,
 * just as it would be at the end of the current round.
 *
 * Tokens held in Mulch or already placed outside these three zones are not
 * silently returned; such future movements require explicit scenario modeling.
 */
data class FutureDiceZones(
    val supply: List<FutureDie> = emptyList(),
    val hand: List<FutureDie> = emptyList(),
    val discard: List<FutureDie> = emptyList()
) {
    companion object {
        /** Per-zone IDs preserve distinct copies of same-sided dice. */
        fun fromBoard(board: PlayerBoardView): FutureDiceZones = FutureDiceZones(
            supply = board.supply.mapIndexed { index, die -> FutureDie("s$index", die.sides) },
            hand = board.hand.mapIndexed { index, die -> FutureDie("h$index", die.sides) },
            discard = board.discard.mapIndexed { index, die -> FutureDie("d$index", die.sides) }
        )
    }

    init {
        val ids = (supply + hand + discard).map { it.id }
        require(ids.size == ids.toSet().size) { "Forecast die IDs must be unique across zones" }
    }

    /** A normal purchase enters Discard: it is not an immediate extra Draw. */
    fun withPurchaseIntoDiscard(id: String, sides: Int): FutureDiceZones = copy(
        discard = discard + FutureDie(id, sides)
    )

    /** Normal Compost takes a Hand die and puts its replacement in Discard. */
    fun withHandUpgradeIntoDiscard(id: String, newSides: Int): FutureDiceZones {
        val original = hand.singleOrNull { it.id == id }
            ?: throw IllegalArgumentException("No current Hand die with ID $id")
        require(newSides > original.sides) { "Upgrade must increase the die size" }
        return copy(
            hand = hand.filterNot { it.id == id },
            discard = discard + FutureDie(id, newSides)
        )
    }
}

/**
 * Cultivation's opening Draw 3 plus 0, 1 or 2 assumed Main-Action Draws.
 * Battle's opening Draw 3 is independent of this assumption.
 */
enum class CultivationDrawAssumption(val drawsPerRound: Int) {
    OPENING_ONLY(3),
    TYPICAL_ONE_EXTRA(4),
    DRAW_FOCUSED(5)
}

data class FutureDieEncounter(
    val die: FutureDie,
    /** 1-based position among *future* rounds; 1 never means the current round. */
    val futureRound: Int,
    val roundType: RoundCardType,
    val drawNumberInRound: Int,
    val value: ImmediatePipValue
)

data class FutureRoundDiceValue(
    val futureRound: Int,
    val roundType: RoundCardType,
    val plannedDraws: Int,
    val encounters: List<FutureDieEncounter>
) {
    val realizedDraws: Int get() = encounters.size
    val expectedValue: ImmediatePipValue get() = encounters.fold(ImmediatePipValue(0.0)) { sum, encounter ->
        sum + encounter.value
    }
}

data class FutureDieLifetimeValue(
    val die: FutureDie,
    val encounters: Int,
    val firstFutureRound: Int?,
    val expectedValue: ImmediatePipValue
)

data class FutureDiceValue(
    val rounds: List<FutureRoundDiceValue>,
    val byDie: List<FutureDieLifetimeValue>
) {
    val encounters: List<FutureDieEncounter> get() = rounds.flatMap { it.encounters }
    val expectedValue: ImmediatePipValue get() = rounds.fold(ImmediatePipValue(0.0)) { sum, round ->
        sum + round.expectedValue
    }
    val expectedSpendingPips: Double get() = expectedValue.spendingPips
    val expectedPipEquivalents: Double get() = expectedValue.pipEquivalentTotal
}

/**
 * Deterministic forecast of NORMAL Draws strictly after the current round.
 *
 * Mirrors PlayerDice.draw(): lowest-sided Supply die first; refill Supply from
 * Discard only when Supply is empty; drawn dice remain in Hand until the next
 * round's cleanup. No random rolls or live game mutations are performed.
 *
 * This is a baseline expectation, NOT a predicted policy: extra Draws from
 * unknown future Plants/Round cards, gained dice, Mulch returns, Battle Doom,
 * and later upgrades are absent unless reflected in a separate input scenario.
 * Reward availability is held fixed; it does not forecast physical token/deck
 * depletion or changing Wisp capacity across future rolls.
 */
object FutureDiceForecast {
    fun fromContext(
        context: DecisionContext,
        assumption: CultivationDrawAssumption = CultivationDrawAssumption.TYPICAL_ONE_EXTRA,
        additionalDrawsByFutureRound: Map<Int, Int> = emptyMap()
    ): FutureDiceValue = project(
        zones = FutureDiceZones.fromBoard(context.self.board),
        upcomingRounds = context.progress.upcomingRoundTypes,
        assumption = assumption,
        rewards = RollRewardOpportunities.fromVisibleContext(context),
        additionalDrawsByFutureRound = additionalDrawsByFutureRound
    )

    /**
     * [upcomingRounds] must contain only *future* types, excluding the current
     * revealed round. Additional known Draws are indexed from 1. They must be
     * supplied deliberately; the engine will not invent future card plays.
     */
    fun project(
        zones: FutureDiceZones,
        upcomingRounds: List<RoundCardType>,
        assumption: CultivationDrawAssumption = CultivationDrawAssumption.TYPICAL_ONE_EXTRA,
        rewards: RollRewardOpportunities = RollRewardOpportunities(false, false),
        additionalDrawsByFutureRound: Map<Int, Int> = emptyMap()
    ): FutureDiceValue {
        require(additionalDrawsByFutureRound.all { (round, draws) ->
            round in 1..upcomingRounds.size && draws >= 0
        }) { "Extra Draw assumptions must use a future-round index and nonnegative count" }

        val originalDice = zones.supply + zones.hand + zones.discard
        val supply = zones.supply.toMutableList()
        // Current round finishes before future round 1. Current Hand is NOT
        // counted, but becomes available to the future recycle cycle.
        val discard = (zones.discard + zones.hand).toMutableList()
        val roundValues = upcomingRounds.mapIndexed { index, roundType ->
            val futureRound = index + 1
            val baseDraws = when (roundType) {
                RoundCardType.CULTIVATION -> assumption.drawsPerRound
                RoundCardType.BATTLE -> 3
            }
            val expectedDraws = baseDraws + (additionalDrawsByFutureRound[futureRound] ?: 0)
            val encounters = mutableListOf<FutureDieEncounter>()
            val hand = mutableListOf<FutureDie>()

            repeat(expectedDraws) {
                if (supply.isEmpty()) {
                    supply.addAll(discard)
                    discard.clear()
                }
                // Equivalent-sided dice retain Supply insertion order, just as
                // Dice.drawLowest does. Never inspect the face currently showing.
                val nextIndex = supply.indices.minByOrNull { supply[it].sides }
                if (nextIndex != null) {
                    val die = supply.removeAt(nextIndex)
                    hand.add(die)
                    encounters.add(FutureDieEncounter(
                        die = die,
                        futureRound = futureRound,
                        roundType = roundType,
                        drawNumberInRound = encounters.size + 1,
                        value = ImmediatePipValuation.roll(die.sides, rewards)
                    ))
                }
            }

            // Normal next-round starting zones. Battle removal/Doom and
            // card-driven movements remain deliberately outside this forecast.
            discard.addAll(hand)
            FutureRoundDiceValue(futureRound, roundType, expectedDraws, encounters)
        }

        val allEncounters = roundValues.flatMap { it.encounters }
        val byDie = originalDice.map { die ->
            val seen = allEncounters.filter { it.die.id == die.id }
            FutureDieLifetimeValue(
                die = die,
                encounters = seen.size,
                firstFutureRound = seen.firstOrNull()?.futureRound,
                expectedValue = seen.fold(ImmediatePipValue(0.0)) { sum, encounter ->
                    sum + encounter.value
                }
            )
        }
        return FutureDiceValue(rounds = roundValues, byDie = byDie)
    }
}
