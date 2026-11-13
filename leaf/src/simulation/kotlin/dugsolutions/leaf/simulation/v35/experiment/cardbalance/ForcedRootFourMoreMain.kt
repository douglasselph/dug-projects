package dugsolutions.leaf.simulation.v35.experiment.cardbalance

import dugsolutions.leaf.v35.battle.BattleState
import dugsolutions.leaf.v35.battle.StrikeResolver
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.chronicle.GameChronicle
import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.common.CardDataFiles
import dugsolutions.leaf.v35.common.FirstGameDefault
import dugsolutions.leaf.v35.effect.DefaultGameEffectExecutor
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.effect.GameEffectConverter
import dugsolutions.leaf.v35.effect.GameEffectPhase
import dugsolutions.leaf.v35.effect.GameEffectRequest
import dugsolutions.leaf.v35.effect.GameEffectSource
import dugsolutions.leaf.v35.game.BattleStructure
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.GameConfig
import dugsolutions.leaf.v35.game.GameRoundSetup
import dugsolutions.leaf.v35.game.PlayerDecisionFactory
import dugsolutions.leaf.v35.game.operation.WoundResolver
import dugsolutions.leaf.v35.grove.Grove
import dugsolutions.leaf.v35.plant.PlantCardRegistry
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.DecisionDirector
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.dice.PlayerDice
import dugsolutions.leaf.v35.random.Randomizer
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.round.RoundCardManager
import dugsolutions.leaf.v35.round.RoundCardRegistry
import dugsolutions.leaf.v35.round.RoundDeck
import dugsolutions.leaf.v35.wisp.WispCardManager
import dugsolutions.leaf.v35.wisp.WispDeck
import java.nio.file.Files
import java.nio.file.Path

/**
 * A2 mechanical intervention: force the SAME legal Root Four More activation in
 * separate fresh v38 Battle states under three seat-0-only effect definitions.
 *
 * Deliberately NOT a player policy, complete-game replay, or balance verdict.
 * The exact target die is selected from legal choices by its initial Hand index.
 */
object ForcedRootFourMoreExperiment {
    const val CARD_ID = "Root_05_02"
    const val SEED = 20261113L

    enum class Condition(val overrideEffect: GameEffect?) {
        OFFICIAL(null),
        SUPPRESSED(GameEffect.RESEARCH_NO_EFFECT),
        RAISE_20(GameEffect.RESEARCH_RAISE_DIE_PLUS_20)
    }

    data class Scenario(val id: String, val ownFace: Int, val opposingFace: Int) {
        init {
            require(ownFace in 2..20 && opposingFace in 2..20)
        }
    }

    val scenarios: List<Scenario> = listOf(
        Scenario("raise4_turns_loss_to_win", 2, 5),
        Scenario("only_raise20_turns_loss_to_win", 2, 9),
        Scenario("raise20_forces_tie", 2, 20),
        Scenario("capped_raises_no_value", 20, 19)
    )

    data class Observation(
        val scenario: String,
        val condition: Condition,
        val preState: String,
        val before: Int,
        val after: Int,
        val effectiveEffect: GameEffect,
        val outputResolutions: Int,
        val actualPips: Int,
        val ownStrikeTotal: Int,
        val opposingStrikeTotal: Int,
        val winner: Boolean,
        val strikeVp: Int,
        val woundedOpponents: Int,
        val actorWounded: Boolean,
        val immediateVp: Int
    ) {
        fun asTsv(): String = listOf(
            scenario, condition.name, preState, before, after, effectiveEffect.name,
            outputResolutions, actualPips, ownStrikeTotal, opposingStrikeTotal,
            winner, strikeVp, woundedOpponents, actorWounded, immediateVp
        ).joinToString("\t")
    }

    const val TSV_HEADER = "scenario\tcondition\tpre_state\tbefore\tafter\teffect\t" +
        "resolutions\tactual_pips\town_total\topposing_total\twinner\t" +
        "strike_vp\twounded_opponents\tactor_wounded\timmediate_vp"

    private class FixedDie(sides: Int, value: Int) : Die(sides) {
        init { adjustTo(value) }
        override fun roll(): Die = this
    }

    /**
     * Target the actual D20, not a Hand index. PlayerDice sorts Hand by die size,
     * so the D20 is not necessarily at index 0 even when supplied first.
     */
    private val targetD20: EffectStrategy = object : EffectStrategy by DecisionDirector.mechanicalControl().effect {
        override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
            requireNotNull(request.legalChoices.singleOrNull { it.sides == 20 }) {
                "Forced Root Four More D20 is not uniquely legal: ${request.legalChoices}"
            }
    }

    fun run(): List<Observation> {
        val registry = PlantCardRegistry().apply {
            loadFromCsv(
                CardDataFiles.dataPath(CardDataFiles.ROOT_CARD_LIST),
                CardDataFiles.dataPath(CardDataFiles.VF_CARD_LIST)
            )
        }
        check(registry.getAllCards().size == 36) {
            "Unexpected canonical Plant catalog size: ${registry.getAllCards().size}"
        }
        val market = FirstGameDefault.PLANT_NAMES.map { name ->
            requireNotNull(registry.getCard(name)) { "Unknown current Grove card $name" }
        }
        val root = requireNotNull(registry.getCard(CARD_ID))
        check(root.title == "Root Four More" && root.effect == GameEffect.RAISE_DIE_PLUS_4) {
            "Root Four More official CSV identity/effect changed: $root"
        }
        check(market.any { it.name == CARD_ID }) { "Current nine-slot Grove does not contain Root Four More" }

        return scenarios.flatMap { scenario ->
            val results = Condition.entries.map { condition ->
                runOne(scenario, condition, market)
            }
            check(results.map { it.preState }.distinct().size == 1) {
                "Matched conditions did not start from an identical physical state: ${scenario.id}"
            }
            results
        }
    }

    private fun runOne(
        scenario: Scenario,
        condition: Condition,
        market: List<dugsolutions.leaf.v35.plant.domain.PlantCard>
    ): Observation {
        val random = Randomizer.create(SEED)
        fun player(id: Int, highFace: Int): Player {
            val effectPolicy = DecisionDirector.mechanicalControl().copy(effect = targetD20)
            return Player(
                id = PlayerId(id),
                decisions = effectPolicy,
                // Keep both smaller dice below the D20 even when the D20 shows 2.
                // PlayerDice sorts by sides, and Battle's value-only stable sort
                // otherwise places a tied D6 ahead of the D20 in the TOP row.
                dice = PlayerDice(hand = listOf(
                    FixedDie(20, highFace), FixedDie(6, 1), FixedDie(4, 1)
                ))
            )
        }
        val actor = player(1, scenario.ownFace)
        val opponent = player(2, scenario.opposingFace)
        val setup = GameRoundSetup.standard()
        val config = GameConfig(
            selectedPlantCards = market,
            playerDecisionFactories = List(2) { PlayerDecisionFactory.mechanicalControl() },
            roundSetup = setup,
            battleStructure = BattleStructure.THREE_ENGAGEMENTS,
            seed = SEED,
            strategySeed = SEED,
            recordResearchEffectOutputs = true,
            researchPlantEffectsBySeat = condition.overrideEffect?.let { effect ->
                mapOf(0 to mapOf(CARD_ID to effect))
            } ?: emptyMap()
        )
        val rounds = RoundCardRegistry(GameEffectConverter()).apply {
            loadFromCsv(CardDataFiles.dataPath(CardDataFiles.ROUND_CARD_LIST))
        }
        val roundDeck = RoundDeck(RoundCardManager().apply { loadCards(rounds.getAllCards()) }, random)
            .apply { setup(setup.roundTypes) }
        val game = Game(
            config = config,
            grove = Grove(market, WispDeck(WispCardManager(), random)),
            players = listOf(actor, opponent),
            chronicle = GameChronicle(),
            roundDeck = roundDeck,
            randomizer = random
        )
        val canonicalRoot = requireNotNull(game.grove.plantMarket.take(requireNotNull(market.singleOrNull { it.name == CARD_ID })))
        val grafted = actor.creature.graft(canonicalRoot, actor.creature.legalPlacements(canonicalRoot).first())
        check(actor.creature.faceUp(grafted.id))
        val source = GameEffectSource.Plant(requireNotNull(actor.creature.get(grafted.id)))
        // Give the opponent a legitimate Wound target, so a 5+ Strike loss
        // exercises Flip-or-Snip rather than the no-Plant fallback.
        val opponentRoot = requireNotNull(market.singleOrNull { it.name == "Root_07_04" })
        val opponentGraft = requireNotNull(game.grove.plantMarket.take(opponentRoot))
        val opponentCard = opponent.creature.graft(
            opponentGraft, opponent.creature.legalPlacements(opponentGraft).first()
        )
        check(opponent.creature.faceUp(opponentCard.id))
        val state = BattleState(listOf(actor, opponent))
        state.activateThreeEngagements()
        state.placeInitialHands()
        val ownDie = actor.dice.hand.single { it.sides == 20 }
        check(state.grid.locationOf(ownDie)?.row == StrikeRow.TOP) {
            "Expected the forced D20 at TOP; actual Hand order=${actor.dice.hand.map { "D${it.sides}:${it.value}" }}"
        }
        val preState = listOf(
            "v38", CARD_ID, "P1=${actor.dice.hand.map { "D${it.sides}:${it.value}" }}",
            "P2=${opponent.dice.hand.map { "D${it.sides}:${it.value}" }}",
            "P1roots=${actor.creature.cards.map { it.card.name }}",
            "P2roots=${opponent.creature.cards.map { it.card.name }}", "seat=0", "row=TOP",
            "rounds=3/2/2", "seed=$SEED"
        ).joinToString("|")
        val before = ownDie.value
        val request = GameEffectRequest(
            game = game,
            actor = actor,
            effect = canonicalRoot.effect,
            source = source,
            phase = GameEffectPhase.BATTLE,
            battleState = state
        )
        val executor = DefaultGameEffectExecutor()
        check(executor.canExecute(request)) { "Forced legal Root Four More activation is not executable" }
        executor.execute(request)
        val actualResolutions = game.chronicle.entries.filterIsInstance<GameEntry.EffectResolved>()
        check(actualResolutions.size == 1 &&
            actualResolutions.single().effect == game.effectFor(actor, canonicalRoot)) {
            "Forced effect was not resolved and recorded exactly once"
        }
        // The forced activation still spends the physical face-up card, as one Main Action would.
        check(actor.creature.faceDown(grafted.id))
        check(game.effectFor(opponent, canonicalRoot) == GameEffect.RAISE_DIE_PLUS_4) {
            "Research control affected the opposing seat"
        }
        val outputs = game.researchEffectOutputs
        check(outputs.size == 1 && outputs.single().playerSeat == 0 && outputs.single().plantId == CARD_ID)
        val out = outputs.single()
        check(out.existingHandDieValueDelta == ownDie.value - before)
        check(out.immediateVpDelta == 0 && out.newHandDice == 0)
        val resolved = StrikeResolver(WoundResolver(game.grove, game.chronicle))
            .resolveRow(game, state, StrikeRow.TOP)
        check(resolved.totals.size == 2)
        return Observation(
            scenario = scenario.id,
            condition = condition,
            preState = preState,
            before = before,
            after = ownDie.value,
            effectiveEffect = game.effectFor(actor, canonicalRoot),
            outputResolutions = actualResolutions.size,
            actualPips = out.existingHandDieValueDelta,
            ownStrikeTotal = resolved.totals.single { it.playerId == actor.id }.total,
            opposingStrikeTotal = resolved.totals.single { it.playerId == opponent.id }.total,
            winner = actor.id in resolved.winnerIds,
            strikeVp = actor.vp,
            woundedOpponents = resolved.woundedPlayerIds.count { it == opponent.id },
            actorWounded = actor.id in resolved.woundedPlayerIds,
            immediateVp = out.immediateVpDelta
        )
    }

    fun writeTsv(output: Path, observations: List<Observation>) {
        require(observations.size == scenarios.size * Condition.entries.size) {
            "Incomplete forced experiment: ${observations.size} rows"
        }
        Files.createDirectories(requireNotNull(output.toAbsolutePath().parent))
        Files.writeString(output, (listOf(TSV_HEADER) + observations.map { it.asTsv() }).joinToString("\n", postfix = "\n"))
    }
}

fun main(args: Array<String>) {
    require(args.size == 2 && args[0] == "--out") {
        "Usage: ForcedRootFourMoreMainKt --out <path-to-results.tsv>"
    }
    val results = ForcedRootFourMoreExperiment.run()
    ForcedRootFourMoreExperiment.writeTsv(Path.of(args[1]), results)
    println("FORCED ROOT FOUR MORE: ${results.size} matched executions, " +
        "${results.count { it.outputResolutions == 1 }} effect resolutions; " +
        "wrote ${args[1]}")
}
