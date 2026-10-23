package dugsolutions.leaf.v35.player.decision.baseline.card

import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.player.decision.baseline.card.plant.flower.*
import dugsolutions.leaf.v35.player.decision.baseline.card.plant.root.*
import dugsolutions.leaf.v35.player.decision.baseline.card.plant.vine.*
import dugsolutions.leaf.v35.player.decision.baseline.card.wisp.*
import dugsolutions.leaf.v35.player.decision.baseline.influence.BaselineInfluencer
import dugsolutions.leaf.v35.player.decision.context.CreatureCardView
import dugsolutions.leaf.v35.player.decision.context.DecisionContext
import dugsolutions.leaf.v35.wisp.domain.WispCard

/** Single source of truth for every Human Baseline Plant/Wisp scorer. */
class HumanBaselineCardScorerRegistry(
    scorers: List<HumanBaselineCardScorer> = defaultScorers()
) {
    private val byName: Map<String, HumanBaselineCardScorer>
    private val byEffect: Map<GameEffect, List<HumanBaselineCardScorer>>

    init {
        val pairs = scorers.flatMap { scorer -> scorer.cardNames.map { it to scorer } }
        val duplicates = pairs.groupBy { it.first }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate Human Baseline card scorer names: $duplicates" }
        byName = pairs.toMap()
        byEffect = scorers.groupBy { it.effect }.toMutableMap().apply {
            // Pollinating Wisp shares one scorer across its four mechanically parallel effects.
            val pollinating = listOfNotNull(byName["Wisp_Gain_Green"]).distinct()
            if (pollinating.isNotEmpty()) {
                put(GameEffect.GAIN_OR_REFRESH_GREEN_BUTTERFLY, pollinating)
                put(GameEffect.GAIN_OR_REFRESH_PURPLE_BUTTERFLY, pollinating)
                put(GameEffect.GAIN_OR_REFRESH_RED_BUTTERFLY, pollinating)
                put(GameEffect.GAIN_OR_REFRESH_YELLOW_BUTTERFLY, pollinating)
            }
            val recall = listOfNotNull(byName["Root_09_04"]).distinct()
            if (recall.isNotEmpty()) {
                put(GameEffect.DISCARD_UP_TO_2_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE, recall)
                put(GameEffect.DISCARD_UP_TO_3_DICE_AND_REDRAW_OR_REROLL_ONE_IN_BATTLE, recall)
            }
            // Research effect identities must resolve to the same card-specific scorer
            // as their printed identity. Buying/activating a renamed effect must not
            // silently lose the scorer registration merely because its enum differs.
            fun mapResearchVariants(name: String, vararg effects: GameEffect) {
                val scorer = listOfNotNull(byName[name]).distinct()
                if (scorer.isNotEmpty()) effects.forEach { put(it, scorer) }
            }
            mapResearchVariants(
                "Flower_14_04",
                GameEffect.GAIN_OR_STEAL_D4_THEN_DISCARD_D4S_AND_DRAW,
                GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_ONLY_THEN_CYCLE_D4S,
                GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_ONLY_THEN_CYCLE_D4S,
                GameEffect.GAIN_OR_STEAL_D4_TO_DISCARD_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S,
                GameEffect.GAIN_OR_STEAL_D4_TO_HAND_FROM_HAND_OR_DISCARD_THEN_CYCLE_D4S
            )
            mapResearchVariants(
                "Flower_11_03",
                GameEffect.RAISE_DIE_PLUS_2_AND_REDUCE_OPPOSING_DICE_AROUND_ANOTHER_DIE_IN_STRIKE_ROW
            )
            val appreciation = listOfNotNull(byName["Root_07_02"]).distinct()
            if (appreciation.isNotEmpty()) {
                put(GameEffect.GAIN_WORM_AND_BOOST_WORMS_PLUS_1_THIS_ROUND, appreciation)
            }
        }
    }

    fun forName(name: String): HumanBaselineCardScorer =
        requireNotNull(byName[name]) { "No HumanBaseline scorer registered for $name" }

    fun findByName(name: String): HumanBaselineCardScorer? = byName[name]

    fun forPlant(card: PlantCard): HumanBaselineCardScorer = forName(card.name)

    fun forPlant(card: CreatureCardView): HumanBaselineCardScorer = forName(card.name)

    fun forWisp(card: WispCard): HumanBaselineCardScorer = forName(card.name)

    fun forEffect(effect: GameEffect): List<HumanBaselineCardScorer> = byEffect[effect].orEmpty()

    fun influencersForCardName(name: String): List<BaselineInfluencer> =
        findByName(name)?.influencers.orEmpty()

    fun influencersForOwnedCards(context: DecisionContext): List<BaselineInfluencer> =
        context.self.board.creature.flatMap { card ->
            influencersForCardName(card.name)
        }

    fun registeredNames(): Set<String> = byName.keys

    companion object {
        fun defaultScorers(): List<HumanBaselineCardScorer> = listOf(
            RootDoubleDownBaseline,
            RootFourMoreBaseline,
            RootOnARollBaseline,
            RootWellBaseline,
            RootAndScootBaseline,
            RootAppreciationBaseline,
            RootDownPaymentBaseline,
            RootRecallBaseline,
            RootAwakeningBaseline,
            RootCauseBaseline,
            RootKindredBaseline,
            RootLootBaseline,
            BerryImportantBaseline,
            SnipHappensBaseline,
            VineAndAgainBaseline,
            VineYieldBaseline,
            LowAndBeholdBaseline,
            PartingThornBaseline,
            RiseAndVineBaseline,
            VineAndDineBaseline,
            ReapWhatYouRollBaseline,
            SaplinkTrellisBaseline,
            VineAndPunishmentBaseline,
            VinesTheLimitBaseline,
            SappingSnapdragonBaseline,
            AlluringNectarBaseline,
            PetalToDie4Baseline,
            TransplantTulipBaseline,
            BeeLovedBloomBaseline,
            BloomBackboneBaseline,
            BloomBackflipBaseline,
            GustOfPetalsBaseline,
            BurstingBlossomBaseline,
            ForgetMeNotBaseline,
            OEdelweissBaseline,
            QueensBlossomBaseline,
            WispOfHonorBaseline,
            BerryPatientBaseline,
            WhisperingWingsBaseline,
            PollinatingWispBaseline,
            PocketedSparkBaseline,
            WispquakeBaseline,
            PollenTheftBaseline,
            OvergrowthBaseline,
            WispsLastWordBaseline
        )
    }
}
