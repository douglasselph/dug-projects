package dugsolutions.leaf.integration.v35.support.decision

import dugsolutions.leaf.integration.v35.support.DecisionScript
import dugsolutions.leaf.v35.battle.domain.StrikeRow
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.mechanical.effect.MechanicalEffectStrategy
import dugsolutions.leaf.v35.player.decision.effect.ChooseBeeSourceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectBattleDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectButterflyTargetRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCritterDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectCrossPlayerDieSwapRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseRootWellBattleRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDiceRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectDieSizeRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectOpponentPlantWoundRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectPlayerRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseEffectStrikeRowRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOEdelweissRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDiePairRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectDieRequest
import dugsolutions.leaf.v35.player.decision.effect.ChooseOptionalEffectPlantRequest
import dugsolutions.leaf.v35.player.decision.effect.ChoosePetalToDie4Request
import dugsolutions.leaf.v35.player.decision.effect.ChooseWispsToKeepRequest
import dugsolutions.leaf.v35.player.decision.effect.EffectBattleDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectBeeSourceChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectButterflyTargetChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectCritterDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectCrossPlayerDieSwapChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDiceChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDieChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectDiePairChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectOpponentPlantWoundChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectPlantChoice
import dugsolutions.leaf.v35.player.decision.effect.EffectStrategy
import dugsolutions.leaf.v35.player.decision.effect.EffectWispsChoice
import dugsolutions.leaf.v35.player.decision.effect.OEdelweissChoice
import dugsolutions.leaf.v35.player.decision.effect.PetalToDie4Choice
import dugsolutions.leaf.v35.player.decision.effect.RootWellBattleChoice
import dugsolutions.leaf.v35.random.die.DieSides

/**
 * Queue-driven effect policy for integration scenarios.
 *
 * Only decisions explicitly scripted by a test are consumed here. Every other
 * effect decision delegates to the production baseline policy, which keeps a
 * scenario focused on the one behavior it is trying to prove.
 */
class ScriptedEffectStrategy(
    private val fallback: EffectStrategy = MechanicalEffectStrategy()
) : EffectStrategy {
    private val dice = DecisionScript<ChooseEffectDieRequest, EffectDieChoice>("Effect die choices")
    private val battleDice = DecisionScript<ChooseEffectBattleDieRequest, EffectBattleDieChoice>("Effect Battle-die choices")
    private val rootWell = DecisionScript<ChooseRootWellBattleRequest, RootWellBattleChoice>("Root Well Battle choices")
    private val swaps = DecisionScript<ChooseEffectCrossPlayerDieSwapRequest, EffectCrossPlayerDieSwapChoice>("Effect cross-player swaps")
    private val optionalDice = DecisionScript<ChooseOptionalEffectDieRequest, EffectDieChoice?>("Optional effect die choices")
    private val diceSets = DecisionScript<ChooseEffectDiceRequest, EffectDiceChoice>("Effect dice-set choices")
    private val diePairs = DecisionScript<ChooseEffectDiePairRequest, EffectDiePairChoice>("Effect die-pair choices")
    private val optionalPairs = DecisionScript<ChooseOptionalEffectDiePairRequest, EffectDiePairChoice?>("Optional effect die-pair choices")
    private val critterDice = DecisionScript<ChooseEffectCritterDieRequest, EffectCritterDieChoice>("Effect Critter/die choices")
    private val petal = DecisionScript<ChoosePetalToDie4Request, PetalToDie4Choice>("Petal To Die 4 choices")
    private val beeSources = DecisionScript<ChooseBeeSourceRequest, EffectBeeSourceChoice>("Bee source choices")
    private val butterflies = DecisionScript<ChooseEffectButterflyTargetRequest, EffectButterflyTargetChoice>("Butterfly target choices")
    private val optionalPlants = DecisionScript<ChooseOptionalEffectPlantRequest, EffectPlantChoice?>("Optional Plant choices")
    private val opponentPlants = DecisionScript<ChooseEffectOpponentPlantWoundRequest, EffectOpponentPlantWoundChoice>("Opponent Plant wound choices")
    private val plantEffects = DecisionScript<ChooseEffectPlantRequest, EffectPlantChoice>("Plant-effect choices")
    private val edelweiss = DecisionScript<ChooseOEdelweissRequest, OEdelweissChoice>("O Edelweiss choices")
    private val wispKeeps = DecisionScript<ChooseWispsToKeepRequest, EffectWispsChoice>("Wisp keep choices")
    private val dieSizes = DecisionScript<ChooseEffectDieSizeRequest, DieSides>("Effect die-size choices")
    private val players = DecisionScript<ChooseEffectPlayerRequest, PlayerId>("Effect player choices")
    private val rows = DecisionScript<ChooseEffectStrikeRowRequest, StrikeRow>("Effect Strike-row choices")

    fun thenDie(selector: (ChooseEffectDieRequest) -> EffectDieChoice) = apply { dice.then(selector) }
    fun thenBattleDie(selector: (ChooseEffectBattleDieRequest) -> EffectBattleDieChoice) = apply { battleDice.then(selector) }
    fun thenRootWellBattle(selector: (ChooseRootWellBattleRequest) -> RootWellBattleChoice) = apply { rootWell.then(selector) }
    fun thenCrossPlayerSwap(selector: (ChooseEffectCrossPlayerDieSwapRequest) -> EffectCrossPlayerDieSwapChoice) = apply { swaps.then(selector) }
    fun thenOptionalDie(selector: (ChooseOptionalEffectDieRequest) -> EffectDieChoice?) = apply { optionalDice.then(selector) }
    fun thenDice(selector: (ChooseEffectDiceRequest) -> EffectDiceChoice) = apply { diceSets.then(selector) }
    fun thenDiePair(selector: (ChooseEffectDiePairRequest) -> EffectDiePairChoice) = apply { diePairs.then(selector) }
    fun thenOptionalDiePair(selector: (ChooseOptionalEffectDiePairRequest) -> EffectDiePairChoice?) = apply { optionalPairs.then(selector) }
    fun thenCritterAndDie(selector: (ChooseEffectCritterDieRequest) -> EffectCritterDieChoice) = apply { critterDice.then(selector) }
    fun thenPetalToDie4(selector: (ChoosePetalToDie4Request) -> PetalToDie4Choice) = apply { petal.then(selector) }
    fun thenBeeSource(selector: (ChooseBeeSourceRequest) -> EffectBeeSourceChoice) = apply { beeSources.then(selector) }
    fun thenButterflyTarget(selector: (ChooseEffectButterflyTargetRequest) -> EffectButterflyTargetChoice) = apply { butterflies.then(selector) }
    fun thenOptionalPlant(selector: (ChooseOptionalEffectPlantRequest) -> EffectPlantChoice?) = apply { optionalPlants.then(selector) }
    fun thenOpponentPlantWound(selector: (ChooseEffectOpponentPlantWoundRequest) -> EffectOpponentPlantWoundChoice) = apply { opponentPlants.then(selector) }
    fun thenPlantEffect(selector: (ChooseEffectPlantRequest) -> EffectPlantChoice) = apply { plantEffects.then(selector) }
    fun thenOEdelweiss(selector: (ChooseOEdelweissRequest) -> OEdelweissChoice) = apply { edelweiss.then(selector) }
    fun thenWispsToKeep(selector: (ChooseWispsToKeepRequest) -> EffectWispsChoice) = apply { wispKeeps.then(selector) }
    fun thenDieSize(selector: (ChooseEffectDieSizeRequest) -> DieSides) = apply { dieSizes.then(selector) }
    fun thenPlayer(selector: (ChooseEffectPlayerRequest) -> PlayerId) = apply { players.then(selector) }
    fun thenStrikeRow(selector: (ChooseEffectStrikeRowRequest) -> StrikeRow) = apply { rows.then(selector) }

    override fun chooseDie(request: ChooseEffectDieRequest): EffectDieChoice =
        legal(dice.nextOrElse(request, fallback::chooseDie), request.legalChoices, "effect die")

    override fun chooseBattleDie(request: ChooseEffectBattleDieRequest): EffectBattleDieChoice =
        legal(battleDice.nextOrElse(request, fallback::chooseBattleDie), request.legalChoices, "Battle die")

    override fun chooseRootWellBattle(request: ChooseRootWellBattleRequest): RootWellBattleChoice =
        legal(rootWell.nextOrElse(request, fallback::chooseRootWellBattle), request.legalChoices, "Root Well Battle")

    override fun chooseCrossPlayerDieSwap(request: ChooseEffectCrossPlayerDieSwapRequest): EffectCrossPlayerDieSwapChoice =
        legal(swaps.nextOrElse(request, fallback::chooseCrossPlayerDieSwap), request.legalChoices, "cross-player swap")

    override fun chooseOptionalDie(request: ChooseOptionalEffectDieRequest): EffectDieChoice? {
        val chosen = optionalDice.nextOrElse(request, fallback::chooseOptionalDie)
        require(chosen == null || chosen in request.legalChoices) {
            "Scripted optional effect die is not legal: $chosen; legal=${request.legalChoices}"
        }
        return chosen
    }

    override fun chooseDice(request: ChooseEffectDiceRequest): EffectDiceChoice {
        val chosen = diceSets.nextOrElse(request, fallback::chooseDice)
        require(chosen.selected.all { it in request.legalChoices }) {
            "Scripted effect dice include an illegal choice: ${chosen.selected}; legal=${request.legalChoices}"
        }
        require(chosen.selected.size in request.minChoices..request.maxChoices) {
            "Scripted effect dice count ${chosen.selected.size} is outside " +
                "${request.minChoices}..${request.maxChoices}"
        }
        return chosen
    }

    override fun chooseDiePair(request: ChooseEffectDiePairRequest): EffectDiePairChoice =
        legal(diePairs.nextOrElse(request, fallback::chooseDiePair), request.legalChoices, "effect die pair")

    override fun chooseOptionalDiePair(request: ChooseOptionalEffectDiePairRequest): EffectDiePairChoice? {
        val chosen = optionalPairs.nextOrElse(request, fallback::chooseOptionalDiePair)
        require(chosen == null || chosen in request.legalChoices) {
            "Scripted optional die pair is not legal: $chosen; legal=${request.legalChoices}"
        }
        return chosen
    }

    override fun chooseCritterAndDie(request: ChooseEffectCritterDieRequest): EffectCritterDieChoice =
        legal(critterDice.nextOrElse(request, fallback::chooseCritterAndDie), request.legalChoices, "Critter/die")

    override fun choosePetalToDie4(request: ChoosePetalToDie4Request): PetalToDie4Choice =
        legal(petal.nextOrElse(request, fallback::choosePetalToDie4), request.legalChoices, "Petal To Die 4")

    override fun chooseBeeSource(request: ChooseBeeSourceRequest): EffectBeeSourceChoice =
        legal(beeSources.nextOrElse(request, fallback::chooseBeeSource), request.legalChoices, "Bee source")

    override fun chooseButterflyTarget(request: ChooseEffectButterflyTargetRequest): EffectButterflyTargetChoice =
        legal(butterflies.nextOrElse(request, fallback::chooseButterflyTarget), request.legalChoices, "Butterfly target")

    override fun chooseOptionalPlant(request: ChooseOptionalEffectPlantRequest): EffectPlantChoice? {
        val chosen = optionalPlants.nextOrElse(request, fallback::chooseOptionalPlant)
        require(chosen == null || chosen in request.legalChoices) {
            "Scripted optional Plant is not legal: $chosen; legal=${request.legalChoices}"
        }
        return chosen
    }

    override fun chooseOpponentPlantWound(request: ChooseEffectOpponentPlantWoundRequest): EffectOpponentPlantWoundChoice =
        legal(opponentPlants.nextOrElse(request, fallback::chooseOpponentPlantWound), request.legalChoices, "opponent Plant wound")

    override fun choosePlantEffect(request: ChooseEffectPlantRequest): EffectPlantChoice =
        legal(plantEffects.nextOrElse(request, fallback::choosePlantEffect), request.legalChoices, "Plant effect")

    override fun chooseOEdelweiss(request: ChooseOEdelweissRequest): OEdelweissChoice =
        legal(edelweiss.nextOrElse(request, fallback::chooseOEdelweiss), request.legalChoices, "O Edelweiss")

    override fun chooseWispsToKeep(request: ChooseWispsToKeepRequest): EffectWispsChoice {
        val chosen = wispKeeps.nextOrElse(request, fallback::chooseWispsToKeep)
        require(chosen.selected.all { it in request.legalChoices }) {
            "Scripted Wisp keep choice includes an illegal Wisp: ${chosen.selected}"
        }
        require(chosen.selected.size == request.keepLimit) {
            "Scripted Wisp keep choice must keep exactly ${request.keepLimit}; " +
                "selected=${chosen.selected.size}"
        }
        return chosen
    }

    override fun chooseDieSize(request: ChooseEffectDieSizeRequest): DieSides =
        legal(dieSizes.nextOrElse(request, fallback::chooseDieSize), request.legalChoices, "die size")

    override fun choosePlayer(request: ChooseEffectPlayerRequest): PlayerId =
        legal(players.nextOrElse(request, fallback::choosePlayer), request.legalChoices, "player")

    override fun chooseStrikeRow(request: ChooseEffectStrikeRowRequest): StrikeRow =
        legal(rows.nextOrElse(request, fallback::chooseStrikeRow), request.legalChoices, "Strike Row")

    fun assertExhausted() {
        dice.assertExhausted()
        battleDice.assertExhausted()
        rootWell.assertExhausted()
        swaps.assertExhausted()
        optionalDice.assertExhausted()
        diceSets.assertExhausted()
        diePairs.assertExhausted()
        optionalPairs.assertExhausted()
        critterDice.assertExhausted()
        petal.assertExhausted()
        beeSources.assertExhausted()
        butterflies.assertExhausted()
        optionalPlants.assertExhausted()
        opponentPlants.assertExhausted()
        plantEffects.assertExhausted()
        edelweiss.assertExhausted()
        wispKeeps.assertExhausted()
        dieSizes.assertExhausted()
        players.assertExhausted()
        rows.assertExhausted()
    }

    private fun <T> legal(chosen: T, legal: List<T>, label: String): T {
        require(chosen in legal) {
            "Scripted $label choice is not legal: $chosen; legal=$legal"
        }
        return chosen
    }
}
