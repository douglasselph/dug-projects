package dugsolutions.leaf.v35.game.buy

import dugsolutions.leaf.v35.error.stateNotNull
import dugsolutions.leaf.v35.error.decisionNotNull
import dugsolutions.leaf.v35.error.decisionCheck
import dugsolutions.leaf.v35.error.stateCheck
import dugsolutions.leaf.v35.chronicle.domain.Moment
import dugsolutions.leaf.v35.chronicle.domain.PurchaseKind
import dugsolutions.leaf.v35.game.Game
import dugsolutions.leaf.v35.game.operation.GraftPlan
import dugsolutions.leaf.v35.game.operation.GraftResolver
import dugsolutions.leaf.v35.player.Player
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.player.decision.buy.BuyChoice
import dugsolutions.leaf.v35.player.decision.buy.BuyCritterResource
import dugsolutions.leaf.v35.player.decision.buy.BuyDieResource
import dugsolutions.leaf.v35.player.decision.buy.BuyItem
import dugsolutions.leaf.v35.player.decision.buy.BuyPayment
import dugsolutions.leaf.v35.player.decision.buy.ChoosePaymentRequest
import dugsolutions.leaf.v35.player.decision.buy.ChoosePurchaseRequest
import dugsolutions.leaf.v35.random.die.Die
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Critter

data class PurchaseResult(
    val playerId: PlayerId,
    val item: BuyItem,
    val cost: Int,
    val paymentTotal: Int
) {
    val overpayment: Int get() = paymentTotal - cost
}

data class BuyPhaseResult(
    val order: List<PlayerId>,
    val purchases: List<PurchaseResult>
)

class BuyCoordinator(
    private val graftResolver: GraftResolver,
    private val createDie: (DieSides) -> Die
) {
    fun execute(game: Game): BuyPhaseResult {
        val order = BuyOrder.determine(game.players, game.randomizer)
        game.chronicle.record(
            Moment.BuyOrder(
                order = order.map { it.id }
            )
        )

        val purchases = mutableListOf<PurchaseResult>()
        order.forEach { player ->
            while (true) {
                val legalItems = legalItems(game, player)
                if (legalItems.isEmpty()) break

                val choice = player.decisions.buy.choosePurchase(
                    ChoosePurchaseRequest(legalItems)
                )
                if (choice == BuyChoice.Done) break
                decisionCheck(choice is BuyChoice.Purchase && choice.item in legalItems) {
                    "BuyStrategy returned a purchase that was not offered: $choice"
                }

                val item = choice.item
                val payment = player.decisions.buy.choosePayment(
                    ChoosePaymentRequest(
                        item = item,
                        availableDice = player.dice.hand.map {
                            BuyDieResource(it.sides, it.value)
                        },
                        availableCritters = player.critters.all.map { critter ->
                            BuyCritterResource(
                                critter = critter,
                                value = player.critterValues.valueOf(critter)
                            )
                        }
                    )
                )
                val resolvedPayment = validatePayment(player, item, payment)
                decisionCheck(item in legalItems(game, player)) {
                    "Selected purchase is no longer available: $item"
                }

                val graftPlan = when (item) {
                    is BuyItem.Plant -> decisionNotNull(
                        graftResolver.prepare(player, item.card)
                    ) { "Selected Plant no longer has a legal graft placement" }
                    is BuyItem.Die -> null
                }
                val boughtDie = when (item) {
                    is BuyItem.Die -> createDie(item.sides)
                    is BuyItem.Plant -> null
                }

                commitPurchase(
                    game = game,
                    player = player,
                    item = item,
                    payment = resolvedPayment,
                    graftPlan = graftPlan,
                    boughtDie = boughtDie
                )

                val result = PurchaseResult(
                    playerId = player.id,
                    item = item,
                    cost = item.cost,
                    paymentTotal = resolvedPayment.total
                )
                purchases.add(result)
                game.chronicle.record(
                    Moment.Purchase(
                        playerId = player.id,
                        kind = when (item) {
                            is BuyItem.Die -> PurchaseKind.DIE
                            is BuyItem.Plant -> PurchaseKind.PLANT
                        },
                        itemName = itemName(item),
                        cost = item.cost,
                        paymentTotal = resolvedPayment.total
                    )
                )
            }
        }

        return BuyPhaseResult(
            order = order.map { it.id },
            purchases = purchases.toList()
        )
    }

    private fun legalItems(game: Game, player: Player): List<BuyItem> {
        val purchasingPower =
            player.dice.hand.sumOf { it.value } +
                player.critters.all.sumOf {
                    player.critterValues.valueOf(it)
                }

        return buildList {
            game.grove.plantMarket.availableStacks
                .map { BuyItem.Plant(it.card) }
                .filterTo(this) {
                    it.cost >= 0 &&
                        it.cost <= purchasingPower &&
                        player.creature.legalPlacements(it.card).isNotEmpty()
                }

            DieSides.entries
                .filter { it != DieSides.D4 && game.grove.graftBed.has(it) }
                .map { BuyItem.Die(it) }
                .filterTo(this) { it.cost <= purchasingPower }
        }
    }

    private fun validatePayment(
        player: Player,
        item: BuyItem,
        payment: BuyPayment
    ): ResolvedPayment {
        val remainingDice = player.dice.hand.toMutableList()
        val actualDice = payment.dice.map { resource ->
            val index = remainingDice.indexOfFirst {
                it.sides == resource.sides && it.value == resource.value
            }
            decisionCheck(index >= 0) { "Payment contains an unavailable Hand die: $resource" }
            remainingDice.removeAt(index)
        }

        val remainingCritters = player.critters.all.toMutableList()
        val actualCritters = payment.critters.map { resource ->
            decisionCheck(resource.value == player.critterValues.valueOf(resource.critter)) {
                "Payment contains a stale Critter value: $resource"
            }
            decisionCheck(remainingCritters.remove(resource.critter)) {
                "Payment contains an unavailable Critter: $resource"
            }
            resource.critter
        }

        val total =
            actualDice.sumOf { it.value } +
                payment.critters.sumOf { it.value }
        decisionCheck(total >= item.cost) {
            "Payment does not meet purchase cost: paid=$total cost=${item.cost}"
        }
        decisionCheck(total > 0 || item.cost == 0) {
            "Non-zero purchase requires at least one payment resource"
        }
        return ResolvedPayment(actualDice, actualCritters, total)
    }

    private fun commitPurchase(
        game: Game,
        player: Player,
        item: BuyItem,
        payment: ResolvedPayment,
        graftPlan: GraftPlan?,
        boughtDie: Die?
    ) {
        when (item) {
            is BuyItem.Die -> stateCheck(game.grove.graftBed.take(item.sides)) {
                "Purchased die became unavailable: ${item.sides}"
            }
            is BuyItem.Plant -> stateCheck(game.grove.plantMarket.take(item.card) != null) {
                "Purchased Plant became unavailable: ${item.card.name}"
            }
        }

        payment.dice.forEach { die ->
            stateCheck(player.dice.removeFromHand(die) != null) {
                "Validated payment die could not be removed"
            }
            player.dice.addToDiscard(die)
        }
        payment.critters.forEach { critter ->
            stateCheck(player.critters.remove(critter)) {
                "Validated payment Critter could not be removed"
            }
            game.grove.critters.add(critter)
        }

        when (item) {
            is BuyItem.Die -> player.dice.addToDiscard(stateNotNull(boughtDie) { "Validated bought die was not created" })
            is BuyItem.Plant -> graftResolver.resolve(player, stateNotNull(graftPlan) { "Validated graft plan was not retained" })
        }
    }

    private fun itemName(item: BuyItem): String = when (item) {
        is BuyItem.Die -> item.sides.name
        is BuyItem.Plant -> item.card.name
    }

    private data class ResolvedPayment(
        val dice: List<Die>,
        val critters: List<Critter>,
        val total: Int
    )
}
