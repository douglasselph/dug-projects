package dugsolutions.leaf.v35.chronicle

import dugsolutions.leaf.v35.chronicle.domain.GameEntry
import dugsolutions.leaf.v35.chronicle.domain.GraftedPlantSnapshot
import dugsolutions.leaf.v35.chronicle.domain.PlayerRoundSummarySnapshot
import dugsolutions.leaf.v35.chronicle.domain.ChronicleRollRewardPolicy
import dugsolutions.leaf.v35.chronicle.domain.ChroniclePhase
import dugsolutions.leaf.v35.chronicle.domain.RollReason
import dugsolutions.leaf.v35.chronicle.domain.RollRewardKind
import dugsolutions.leaf.v35.chronicle.domain.EffectSourceKind
import dugsolutions.leaf.v35.chronicle.domain.MainActionKind
import dugsolutions.leaf.v35.chronicle.domain.SupportActionKind
import dugsolutions.leaf.v35.effect.GameEffect
import dugsolutions.leaf.v35.plant.domain.PlantCard
import dugsolutions.leaf.v35.plant.domain.PlantType
import dugsolutions.leaf.v35.player.PlayerId
import dugsolutions.leaf.v35.random.die.DieSides
import dugsolutions.leaf.v35.tokens.Butterfly

/**
 * Simple human-readable rendering of a typed Chronicle.
 *
 * This is intentionally diagnostic rather than polished presentation. The
 * Chronicle remains typed data; this renderer gives smoke runs and experiments
 * a stable way to write something a human can inspect after a game.
 */
object ChronicleTextRenderer {

    /**
     * Renders the full Chronicle. Compact mode is the human-facing default:
     * opening Draw 3 activity becomes one HAND line plus one optional REWARD
     * line, and scored decision reasoning is hidden. [detail] restores the
     * original line-by-line diagnostic rendering.
     */
    fun render(
        entries: List<GameEntry>,
        detail: Boolean = false,
        selectedPlantCards: List<PlantCard> = emptyList()
    ): String =
        buildString {
            if (selectedPlantCards.isNotEmpty()) {
                appendPlantCardHeader(selectedPlantCards)
            }

            var roundNumber = 0
            var sequenceWithinRound = 0
            var separatorAlreadyWritten = selectedPlantCards.isNotEmpty()
            var openingRoundActive = false
            val openingDraws = mutableMapOf<PlayerId, OpeningDrawBuffer>()
            val completedOpeningDraws = mutableSetOf<PlayerId>()

            fun appendBody(body: String, depth: Int = 0) {
                separatorAlreadyWritten = false
                sequenceWithinRound += 1
                val roundPrefix = roundNumber.toString().padStart(2, '0')
                val localPrefix = sequenceWithinRound.toString().padStart(3, '0')
                val indent = "  ".repeat(depth.coerceAtLeast(0))
                appendLine("$roundPrefix.$localPrefix  $indent$body")
            }

            fun appendEntry(entry: GameEntry, depth: Int = entry.hierarchyDepth) {
                appendBody(renderBody(entry), depth)
            }

            fun appendCompactRoll(
                roll: GameEntry.DieRolled,
                reward: GameEntry.RollReward? = null,
                depth: Int = roll.hierarchyDepth
            ) {
                val body = buildString {
                    append("${player(roll.playerId)} ROLL D${roll.sides}=${roll.value} reason=${roll.reason}")
                    reward?.let(::compactReward)?.let { append(" REWARD $it") }
                }
                appendBody(body, depth)
            }

            fun descendants(node: ChronicleNode): List<GameEntry> =
                buildList {
                    fun visit(current: ChronicleNode) {
                        current.children.forEach { child ->
                            add(child.entry)
                            visit(child)
                        }
                    }
                    visit(node)
                }

            lateinit var appendNodeCompact: (ChronicleNode, Int) -> Unit

            fun appendChildrenCompact(
                children: List<ChronicleNode>,
                baseDepth: Int
            ) {
                var index = 0
                while (index < children.size) {
                    val child = children[index]
                    val childDepth = baseDepth + (child.entry.hierarchyDepth - children.first().entry.hierarchyDepth)
                    val entry = child.entry

                    if (entry is GameEntry.DecisionReasoning) {
                        index++
                        continue
                    }

                    if (entry is GameEntry.DieRolled) {
                        val next = children.getOrNull(index + 1)?.entry as? GameEntry.RollReward
                        if (next != null && next.playerId == entry.playerId) {
                            appendCompactRoll(entry, next, childDepth)
                            index += 2
                            continue
                        }
                        appendCompactRoll(entry, depth = childDepth)
                        index++
                        continue
                    }

                    appendNodeCompact(child, childDepth)
                    index++
                }
            }

            fun compactRoundEffect(node: ChronicleNode, entry: GameEntry.MainAction, depth: Int) {
                val effectNode = node.children.firstOrNull { it.entry is GameEntry.EffectResolved }
                val effect = effectNode?.entry as? GameEntry.EffectResolved
                if (effect == null) {
                    appendEntry(entry, depth)
                    appendChildrenCompact(node.children, depth + 1)
                    return
                }

                val nested = descendants(effectNode)
                val stored = nested.filterIsInstance<GameEntry.MulchStored>().firstOrNull()
                val upgrade = nested.filterIsInstance<GameEntry.Upgrade>().firstOrNull()
                val change = nested.filterIsInstance<GameEntry.DieValueChanged>()
                    .firstOrNull { it.effect == effect.effect }

                val body = buildString {
                    append("${player(entry.playerId)} ROUND ${roundEffectSlot(effect.sourceName)} ${effect.effect}")
                    entry.decisionProbabilityPercent?.let { append(" ($it%)") }
                    stored?.let { append(" ${it.sides}=${it.value}") }
                    upgrade?.let {
                        append(' ')
                        append(it.from)
                        it.fromValue?.let { value -> append("=$value") }
                        append(" -> ${it.to}")
                    }
                    change?.let {
                        append(" (${it.sides}=${it.before}->${it.after})")
                    }
                }
                appendBody(body, depth)
            }

            fun compactWisp(node: ChronicleNode, entry: GameEntry.SupportAction, depth: Int) {
                val effectNode = node.children.firstOrNull { it.entry is GameEntry.EffectResolved }
                val effect = effectNode?.entry as? GameEntry.EffectResolved
                if (effect == null) {
                    appendEntry(entry, depth)
                    appendChildrenCompact(node.children, depth + 1)
                    return
                }

                val nested = descendants(effectNode)
                when (effect.effect) {
                    GameEffect.UPGRADE_DIE_TWO_STEPS_SKIP_MISSING_AND_USE_NOW -> {
                        val upgrade = nested.filterIsInstance<GameEntry.Upgrade>().firstOrNull()
                        val roll = nested.filterIsInstance<GameEntry.DieRolled>()
                            .firstOrNull { it.reason == RollReason.ROLL }
                        val rewards = nested.filterIsInstance<GameEntry.RollReward>()
                            .mapNotNull(::compactReward)
                        val body = buildString {
                            append("${player(effect.playerId)} ${effect.sourceName}")
                            entry.wispUsePercentage?.let { append(" ($it%)") }
                            upgrade?.let {
                                append(" ${it.from} -> ${it.to}")
                                roll?.let { rolled -> append("=${rolled.value}") }
                            }
                            if (rewards.isNotEmpty()) {
                                append(" REWARD ${rewards.joinToString(" ")}")
                            }
                        }
                        appendBody(body, depth)
                    }

                    GameEffect.GAIN_MULCH_AND_STORE_DIE_FROM_DISCARD -> {
                        val stored = nested.filterIsInstance<GameEntry.MulchStored>().firstOrNull()
                        val body = buildString {
                            append("${player(effect.playerId)} ${effect.sourceName}")
                            entry.wispUsePercentage?.let { append(" ($it%)") }
                            if (stored != null) {
                                append(" ${stored.sides}=${stored.value} -> MULCH")
                            } else {
                                append(" ${effect.effect}")
                            }
                        }
                        appendBody(body, depth)
                    }

                    else -> {
                        // Compact mode does not need a generic SUPPORT WISP line;
                        // the Wisp effect already says what was played and what it did.
                        appendEntry(effect, depth)
                        appendChildrenCompact(effectNode.children, depth + 1)
                    }
                }
            }

            appendNodeCompact = { node, depth ->
                when (val entry = node.entry) {
                    is GameEntry.DecisionReasoning -> Unit

                    is GameEntry.MainAction -> when {
                        entry.action == MainActionKind.ROUND_EFFECT_1 ||
                            entry.action == MainActionKind.ROUND_EFFECT_2 ->
                            compactRoundEffect(node, entry, depth)

                        entry.action == MainActionKind.ACTIVATE_PLANT -> {
                            val effectNode = node.children.firstOrNull { it.entry is GameEntry.EffectResolved }
                            if (effectNode != null) {
                                appendEntry(effectNode.entry, depth)
                                appendChildrenCompact(effectNode.children, depth + 1)
                            } else {
                                appendEntry(entry, depth)
                                appendChildrenCompact(node.children, depth + 1)
                            }
                        }

                        entry.phase == ChroniclePhase.CULTIVATION && entry.action == MainActionKind.DRAW ->
                            appendChildrenCompact(node.children, depth)

                        else -> {
                            appendEntry(entry, depth)
                            appendChildrenCompact(node.children, depth + 1)
                        }
                    }

                    is GameEntry.SupportAction -> {
                        if (entry.action == SupportActionKind.WISP) {
                            compactWisp(node, entry, depth)
                        } else {
                            appendEntry(entry, depth)
                            appendChildrenCompact(node.children, depth + 1)
                        }
                    }

                    is GameEntry.Cleanup -> {
                        val body = buildString {
                            append("${player(entry.playerId)} ${entry.phase} CLEANUP")
                            if (entry.discardedDice != 0) append(" discardedDice=${entry.discardedDice}")
                            if (entry.returnedCritters != 0) append(" returnedCritters=${entry.returnedCritters}")
                            append(" refreshed=${entry.refreshed}")
                        }
                        appendBody(body, depth)
                        appendChildrenCompact(node.children, depth + 1)
                    }

                    is GameEntry.DieRolled -> appendCompactRoll(entry, depth = depth)

                    is GameEntry.RollReward -> compactReward(entry)?.let {
                        appendBody("${player(entry.playerId)} REWARD $it", depth)
                    }

                    else -> {
                        appendEntry(entry, depth)
                        appendChildrenCompact(node.children, depth + 1)
                    }
                }
            }

            val roots = hierarchyForest(entries)
            roots.forEach { node ->
                val entry = node.entry

                if (entry is GameEntry.RoundRevealed) {
                    if (isNotEmpty() && !separatorAlreadyWritten) appendLine()
                    separatorAlreadyWritten = false
                    roundNumber = entry.roundNumber
                    sequenceWithinRound = 0
                    openingRoundActive = true
                    openingDraws.clear()
                    completedOpeningDraws.clear()
                }

                if (!detail && openingRoundActive) {
                    when (entry) {
                        is GameEntry.DieRolled -> {
                            val buffer = openingDraws.getOrPut(entry.playerId, ::OpeningDrawBuffer)
                            if (
                                entry.playerId !in completedOpeningDraws &&
                                entry.reason == RollReason.DRAW &&
                                buffer.dice.size < OPENING_HAND_SIZE
                            ) {
                                buffer.dice += OpeningDie(entry.sides, entry.value)
                                buffer.awaitingReward =
                                    entry.rewardPolicy == ChronicleRollRewardPolicy.NORMAL &&
                                        entry.value in 1..2
                                return@forEach
                            }
                        }

                        is GameEntry.RollReward -> {
                            val buffer = openingDraws[entry.playerId]
                            if (
                                entry.playerId !in completedOpeningDraws &&
                                buffer?.awaitingReward == true
                            ) {
                                compactReward(entry)?.let(buffer.rewards::add)
                                buffer.awaitingReward = false
                                return@forEach
                            }
                        }

                        is GameEntry.OpeningDrawCompleted -> {
                            val buffer = openingDraws.getOrPut(entry.playerId, ::OpeningDrawBuffer)
                            appendBody(renderOpeningHand(entry.playerId, buffer.dice))
                            if (buffer.rewards.isNotEmpty()) {
                                appendBody(
                                    "${player(entry.playerId)} REWARD " +
                                        buffer.rewards.joinToString(" ")
                                )
                            }
                            completedOpeningDraws += entry.playerId
                            return@forEach
                        }

                        else -> Unit
                    }
                }

                if (detail) {
                    fun appendDetailed(current: ChronicleNode) {
                        appendEntry(current.entry)
                        current.children.forEach(::appendDetailed)
                    }
                    appendDetailed(node)
                } else {
                    appendNodeCompact(node, node.entry.hierarchyDepth)
                }

                if (entry is GameEntry.RoundCompleted && entry.playerSummaries.isNotEmpty()) {
                    openingRoundActive = false
                    appendLine()
                    entry.playerSummaries.forEach { summary ->
                        sequenceWithinRound += 1
                        appendLine(
                            renderSummaryWithRoundPrefix(
                                summary = summary,
                                roundNumber = roundNumber,
                                sequenceWithinRound = sequenceWithinRound
                            )
                        )
                    }
                    appendLine()
                    separatorAlreadyWritten = true
                }
            }
        }

    private data class ChronicleNode(
        val entry: GameEntry,
        val children: MutableList<ChronicleNode> = mutableListOf()
    )

    private fun hierarchyForest(entries: List<GameEntry>): List<ChronicleNode> {
        val roots = mutableListOf<ChronicleNode>()
        val stack = mutableListOf<ChronicleNode>()

        entries.forEach { entry ->
            val depth = entry.hierarchyDepth.coerceAtLeast(0)
            require(depth <= stack.size) {
                "Chronicle hierarchy jumps from depth ${stack.size} to $depth at sequence ${entry.sequence}"
            }
            while (stack.size > depth) stack.removeAt(stack.lastIndex)

            val node = ChronicleNode(entry)
            if (depth == 0) {
                roots += node
            } else {
                stack[depth - 1].children += node
            }

            if (stack.size == depth) {
                stack += node
            } else {
                stack[depth] = node
                while (stack.size > depth + 1) stack.removeAt(stack.lastIndex)
            }
        }

        return roots
    }


    private fun roundEffectSlot(sourceName: String): String =
        when (sourceName.substringAfterLast(':')) {
            "FIRST" -> "EFFECT_1"
            "SECOND" -> "EFFECT_2"
            else -> "EFFECT"
        }

    private fun roundMainKind(sourceName: String): MainActionKind? =
        when (sourceName.substringAfterLast(':')) {
            "FIRST" -> MainActionKind.ROUND_EFFECT_1
            "SECOND" -> MainActionKind.ROUND_EFFECT_2
            else -> null
        }


    private fun StringBuilder.appendPlantCardHeader(cards: List<PlantCard>) {
        appendLine("PLANTS (${cards.size})")
        cards
            .sortedWith(compareBy<PlantCard>({ plantTypeOrder(it.type) }, { it.cost }, { it.name }))
            .forEach { card ->
                appendLine(
                    "  ${plantTypeAbbreviation(card.type)}${card.cost}  ${card.title} [${card.name}]"
                )
            }
        appendLine()
    }

    private data class OpeningDie(
        val sides: Int,
        val value: Int
    )

    private class OpeningDrawBuffer {
        val dice = mutableListOf<OpeningDie>()
        val rewards = mutableListOf<String>()
        var awaitingReward: Boolean = false
    }

    private fun renderOpeningHand(playerId: PlayerId, dice: List<OpeningDie>): String =
        buildString {
            append("${player(playerId)} HAND")
            if (dice.isEmpty()) {
                append(" -")
            } else {
                dice.forEach { die ->
                    append(" D${die.sides}=${die.value}")
                }
            }
        }

    private fun compactReward(entry: GameEntry.RollReward): String? =
        when (entry.kind) {
            RollRewardKind.CRITTER_GAINED -> entry.critter?.name
            RollRewardKind.WISP_GAINED,
            RollRewardKind.WISP_PLAYED_IMMEDIATELY -> entry.wispName?.uppercase()
            RollRewardKind.IGNORED,
            RollRewardKind.CRITTER_UNAVAILABLE,
            RollRewardKind.WISP_UNAVAILABLE -> null
        }

    /**
     * Renders a standalone entry without round context. Full Chronicle reports
     * should call [render] with the complete entry list so they receive the
     * round-local `01.001`, `01.002`, ... numbering scheme.
     */
    fun render(entry: GameEntry): String {
        val prefix = entry.sequence.toString().padStart(4, '0')
        val indent = "  ".repeat(entry.hierarchyDepth.coerceAtLeast(0))
        return "$prefix  $indent${renderBody(entry)}"
    }

    private fun renderSummaryWithRoundPrefix(
        summary: PlayerRoundSummarySnapshot,
        roundNumber: Int,
        sequenceWithinRound: Int
    ): String {
        val roundPrefix = roundNumber.toString().padStart(2, '0')
        val localPrefix = sequenceWithinRound.toString().padStart(3, '0')
        return "$roundPrefix.$localPrefix  ${renderPlayerSummary(summary)}"
    }

    private fun renderPlayerSummary(summary: PlayerRoundSummarySnapshot): String =
        buildList {
            add(player(summary.playerId))
            add("S=${renderDiceCounts(summary.supplyDice)}")
            add("D=${renderDiceCounts(summary.discardDice)}")
            if (summary.beeCount > 0) add("B=${summary.beeCount}")
            if (summary.wormCount > 0) add("W=${summary.wormCount}")
            if (summary.waterCount > 0) add("Wa=${summary.waterCount}")
            if (summary.mulchDice.isNotEmpty()) {
                add(
                    "M=${summary.mulchDice.size}[" +
                        summary.mulchDice.joinToString(",") { it?.toString() ?: "?" } +
                        "]"
                )
            }
            if (summary.wispCount > 0) add("Wi=${summary.wispCount}")
            if (summary.butterflies.isNotEmpty()) {
                add(
                    "BF=${summary.butterflies.size}[" +
                        summary.butterflies
                            .sortedBy { it.ordinal }
                            .joinToString(",", transform = ::butterflyAbbreviation) +
                        "]"
                )
            }
            add(renderGraftedPlants(summary.graftedPlants))
        }.joinToString(" ")

    private fun renderGraftedPlants(plants: List<GraftedPlantSnapshot>): String {
        if (plants.isEmpty()) return "G[]"

        val counts = plants.groupingBy { it }.eachCount()
        val body = counts.entries
            .sortedWith(
                compareBy<Map.Entry<GraftedPlantSnapshot, Int>>(
                    { plantTypeOrder(it.key.type) },
                    { it.key.cost }
                )
            )
            .joinToString(" ") { (plant, count) ->
                "$count${plantTypeAbbreviation(plant.type)}${plant.cost}"
            }
        return "G[$body]"
    }

    private fun plantTypeOrder(type: PlantType): Int =
        when (type) {
            PlantType.ROOT -> 0
            PlantType.VINE -> 1
            PlantType.FLOWER -> 2
        }

    private fun plantTypeAbbreviation(type: PlantType): String =
        when (type) {
            PlantType.ROOT -> "R"
            PlantType.VINE -> "V"
            PlantType.FLOWER -> "F"
        }

    private fun renderDiceCounts(dice: List<DieSides>): String {
        if (dice.isEmpty()) return "-"
        val counts = dice.groupingBy { it }.eachCount()
        return DieSides.entries
            .mapNotNull { sides ->
                counts[sides]?.let { count -> "$count$sides" }
            }
            .joinToString(",")
    }

    private fun butterflyAbbreviation(butterfly: Butterfly): String =
        when (butterfly) {
            Butterfly.GREEN -> "GB"
            Butterfly.YELLOW -> "YB"
            Butterfly.RED -> "RB"
            Butterfly.PURPLE -> "PB"
        }

    private fun renderBody(entry: GameEntry): String =
        when (entry) {
            is GameEntry.Marker ->
                "MARKER ${entry.message}"

            is GameEntry.RoundRevealed ->
                "ROUND ${entry.roundNumber} REVEAL ${entry.cardType}: ${entry.cardName} " +
                    "[${entry.firstEffect} | ${entry.secondEffect}]"

            is GameEntry.RoundCompleted ->
                "ROUND ${entry.roundNumber} COMPLETE ${entry.cardType}: ${entry.cardName}"

            is GameEntry.DieRolled ->
                "${player(entry.playerId)} ROLL D${entry.sides}=${entry.value} " +
                    "reason=${entry.reason} rewards=${entry.rewardPolicy}"

            is GameEntry.RollReward ->
                buildString {
                    append("${player(entry.playerId)} ROLL REWARD ${entry.kind}")
                    entry.critter?.let { append(" critter=$it") }
                    entry.wispName?.let { append(" wisp=$it") }
                }

            is GameEntry.OpeningDrawCompleted ->
                "${player(entry.playerId)} ${entry.phase} OPENING DRAW complete (${entry.count} dice)"

            is GameEntry.MainAction ->
                buildString {
                    append("${player(entry.playerId)} ${entry.phase} MAIN ${entry.action}")
                    entry.actionNumber?.let { append(" #$it") }
                    entry.battleStage?.let { append(" stage=$it") }
                    entry.decisionProbabilityPercent?.let { append(" chance=$it%") }
                }

            is GameEntry.SupportAction ->
                buildString {
                    append("${player(entry.playerId)} ${entry.phase} SUPPORT ${entry.action}")
                    entry.row?.let { append(" row=$it") }
                    entry.wispUsePercentage?.let { append(" chance=$it%") }
                }

            is GameEntry.EffectResolved ->
                "${player(entry.playerId)} ${entry.phase} EFFECT ${entry.sourceKind} " +
                    "${entry.sourceName}: ${entry.effect}"

            is GameEntry.DecisionReasoning ->
                buildString {
                    append("${player(entry.playerId)} DECISION ${entry.choiceLabel} ")
                    append("score=${entry.total} (base=${entry.baseScore}")
                    entry.adjustments.forEach { adjustment ->
                        val sign = if (adjustment.amount >= 0) "+" else ""
                        append(", $sign${adjustment.amount} ${adjustment.reason}")
                    }
                    append(')')
                }

            is GameEntry.BuyOrder ->
                "BUY ORDER " + entry.order.mapIndexed { index, playerId ->
                    buildString {
                        append(player(playerId))
                        if (index == 0) {
                            entry.leaderDie?.let { append("(${it.sides}=${it.value})") }
                        }
                    }
                }.joinToString(" -> ")

            is GameEntry.Purchase ->
                "${player(entry.playerId)} PURCHASE ${entry.kind} ${entry.itemName} " +
                    "cost=${entry.cost} paid=${entry.paymentTotal} overpay=${entry.overpayment}"

            is GameEntry.Graft ->
                "${player(entry.playerId)} GRAFT ${entry.plantName}"

            is GameEntry.BattleOrder ->
                "BATTLE ORDER " + entry.order.joinToString(" -> ") { playerId ->
                    val high = entry.highestDice.firstOrNull { it.playerId == playerId }
                    buildString {
                        append(player(playerId))
                        high?.let { append("(${it.sides}=${it.value})") }
                    }
                }

            is GameEntry.StrikeResolved ->
                buildString {
                    append("STRIKE ${entry.row} ")
                    append(
                        entry.totals.joinToString(", ") { total ->
                            "${player(total.playerId)}=${total.total}" +
                                "(dice=${total.diceTotal},critters=${total.critterTotal})"
                        }
                    )
                    append(" winners=")
                    append(entry.winnerIds.joinToString(", ") { player(it) }.ifBlank { "none" })
                    append(" wounded=")
                    append(entry.woundedPlayerIds.joinToString(", ") { player(it) }.ifBlank { "none" })
                    append(" vpPerWinner=${entry.vpPerWinner}")
                }

            is GameEntry.Wound ->
                "${player(entry.playerId)} WOUND ${entry.kind} ${entry.plantName}"

            is GameEntry.Doom ->
                buildString {
                    append("DOOM count=${entry.count}")
                    if (entry.dice.isNotEmpty()) {
                        append(" [")
                        append(
                            entry.dice.joinToString(", ") { die ->
                                "${player(die.playerId)} ${die.row} ${die.sides}=${die.value}" +
                                    if (die.returnedToGraftBed) " -> GraftBed" else " -> out"
                            }
                        )
                        append(']')
                    }
                }

            is GameEntry.Refresh ->
                "${player(entry.playerId)} REFRESH"

            is GameEntry.Cleanup ->
                "${player(entry.playerId)} ${entry.phase} CLEANUP " +
                    "discardedDice=${entry.discardedDice} returnedCritters=${entry.returnedCritters} " +
                    "refreshed=${entry.refreshed}"

            is GameEntry.Upgrade ->
                buildString {
                    append("${player(entry.playerId)} UPGRADE ${entry.from}")
                    entry.fromValue?.let { append("=$it") }
                    append(" -> ${entry.to} destination=${entry.destination}")
                }

            is GameEntry.MulchStored ->
                "${player(entry.playerId)} MULCH STORE ${entry.sides}=${entry.value} " +
                    "from=${if (entry.fromDiscard) "DISCARD" else "HAND"}"

            is GameEntry.DieValueChanged ->
                "${player(entry.playerId)} DIE VALUE ${entry.effect} " +
                    "${entry.sides}=${entry.before}->${entry.after}"

            is GameEntry.TrashDie ->
                "${player(entry.playerId)} TRASH ${entry.sides} destination=${entry.destination}"

            is GameEntry.FinalScore ->
                "FINAL SCORE ${player(entry.playerId)} total=${entry.totalVp} " +
                    "existing=${entry.existingVp} plants=${entry.plantVp} " +
                    "wisps=${entry.unplayedWispVp} graftedPlants=${entry.graftedPlantCount}"

            is GameEntry.FinalWinners ->
                "WINNER(S) ${entry.winnerIds.joinToString(", ") { player(it) }.ifBlank { "none" }}"

            is GameEntry.GameCompleted ->
                "GAME COMPLETE rounds=${entry.roundsCompleted}"
        }

    private fun player(playerId: PlayerId): String =
        "P${playerId.value}"

    private const val OPENING_HAND_SIZE = 3
}
