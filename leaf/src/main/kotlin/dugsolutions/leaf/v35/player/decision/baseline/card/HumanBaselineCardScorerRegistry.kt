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
            WispReckoningBaseline,
            PollenTheftBaseline,
            OvergrowthBaseline,
            WispsLastWordBaseline
        )
    }
}
