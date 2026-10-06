package dugsolutions.leaf.simulation.v35.learning.interaction

import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.LearnedBattleSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.battle.main.LearnedBattleMainWeights
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyStrategy
import dugsolutions.leaf.v35.player.decision.learned.buy.LearnedBuyWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.LearnedCultivationMainWeights
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportPolicy
import dugsolutions.leaf.v35.player.decision.learned.cultivation.support.LearnedCultivationSupportWeights
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectPolicy
import dugsolutions.leaf.v35.player.decision.learned.plant.LearnedPlantEffectWeights
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayPolicy
import dugsolutions.leaf.v35.player.decision.learned.wisp.LearnedWispPlayWeights
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PolicyInteractionOptionsTest {
    @Test
    fun `parses independent learned policies and paths`() {
        val o = PolicyInteractionOptions.parse(listOf(
            "--samples", "17",
            "--seed", "111",
            "--strategy-seed", "222",
            "--grove-seed", "333",
            "--players", "3",
            "--rounds", "3/2/2",
            "--buy-policy", "learned", "--buy-weights", "b.weights",
            "--cultivation-main-policy", "learned", "--cultivation-main-weights", "c.weights",
            "--cultivation-support-policy", "learned", "--cultivation-support-weights", "cs.weights",
            "--wisp-policy", "learned", "--wisp-weights", "w.weights",
            "--plant-effect-policy", "learned", "--plant-effect-weights", "p.weights",
            "--battle-support-policy", "learned", "--battle-support-weights", "s.weights",
            "--battle-main-policy", "learned", "--battle-main-weights", "m.weights",
            "--config-output", "config.txt"
        ))
        assertEquals(17, o.games)
        assertEquals(111, o.seed)
        assertEquals(222, o.strategySeed)
        assertEquals(333, o.groveSeed)
        assertEquals(3, o.players)
        assertEquals("3/2/2", o.roundLabel)
        assertEquals("learned", o.buyPolicy); assertEquals("b.weights", o.buyWeights.toString())
        assertEquals("learned", o.cultivationMainPolicy); assertEquals("c.weights", o.cultivationMainWeights.toString())
        assertEquals("learned", o.cultivationSupportPolicy); assertEquals("cs.weights", o.cultivationSupportWeights.toString())
        assertEquals("learned", o.wispPolicy); assertEquals("w.weights", o.wispWeights.toString())
        assertEquals("learned", o.plantEffectPolicy); assertEquals("p.weights", o.plantEffectWeights.toString())
        assertEquals("learned", o.battleSupportPolicy); assertEquals("s.weights", o.battleSupportWeights.toString())
        assertEquals("learned", o.battleMainPolicy); assertEquals("m.weights", o.battleMainWeights.toString())
        assertEquals("config.txt", o.configOutput.toString())
    }


    @Test
    fun `plant targeting detail is quiet by default and opt-in verbose`() {
        val quiet = PolicyInteractionOptions.parse(emptyList())
        assertEquals(false, quiet.verbosePlantTargeting)

        val verbose = PolicyInteractionOptions.parse(listOf("--verbose-plant-targeting"))
        assertEquals(true, verbose.verbosePlantTargeting)
    }

    @Test
    fun `human families do not require weight files`() {
        val o = PolicyInteractionOptions.parse(emptyList())
        o.validateRequiredFiles()
    }

    @Test
    fun `learned family requires its own weight file`() {
        val missing = createTempDirectory("policy-interaction-missing").resolve("missing.weights")
        val o = PolicyInteractionOptions.parse(listOf(
            "--wisp-policy", "learned",
            "--wisp-weights", missing.toString()
        ))
        val ex = assertFailsWith<IllegalArgumentException> { o.validateRequiredFiles() }
        assertContains(ex.message ?: "", "Wisp policy is learned")
        assertContains(ex.message ?: "", missing.toString())
    }

    @Test
    fun `all seven learned weight paths remain independent in metadata`() {
        val dir = createTempDirectory("policy-interaction-weights")
        fun weight(name: String) = dir.resolve(name).also {
            Files.writeString(it, "formatVersion=1\npolicy=$name\ntrainingStatus=test\ntrainedRoundPattern=3/2/2\n")
        }
        val paths = listOf(
            weight("buy"), weight("cult-main"), weight("cult-support"), weight("wisp"),
            weight("plant-effect"), weight("battle-support"), weight("battle-main")
        )
        val o = PolicyInteractionOptions.parse(listOf(
            "--samples", "5", "--seed", "700", "--strategy-seed", "800", "--grove-seed", "900",
            "--buy-policy", "learned", "--buy-weights", paths[0].toString(),
            "--cultivation-main-policy", "learned", "--cultivation-main-weights", paths[1].toString(),
            "--cultivation-support-policy", "learned", "--cultivation-support-weights", paths[2].toString(),
            "--wisp-policy", "learned", "--wisp-weights", paths[3].toString(),
            "--plant-effect-policy", "learned", "--plant-effect-weights", paths[4].toString(),
            "--battle-support-policy", "learned", "--battle-support-weights", paths[5].toString(),
            "--battle-main-policy", "learned", "--battle-main-weights", paths[6].toString()
        ))
        o.validateRequiredFiles()
        val metadata = interactionMetadata(o)
        assertEquals(paths.map { it.toString() }, metadata.policies.map { it.path.toString() })
        assertEquals(7, metadata.policies.mapNotNull { it.sha256 }.distinct().size)
        assertEquals(700, metadata.mechanicalSeedStart)
        assertEquals(704, metadata.mechanicalSeedEnd)
        assertEquals(800, metadata.strategySeedStart)
        assertEquals(804, metadata.strategySeedEnd)
        val rendered = renderMachineConfig(metadata)
        listOf("buy", "cultivation-main", "cultivation-support", "wisp", "plant-effect", "battle-support", "battle-main").forEach {
            assertContains(rendered, "policy.$it.mode=learned")
        }
    }


    @Test
    fun `fully learned modular factory wires each family independently`() {
        val director = modularFactory(
            LearnedBuyWeights.zeros(),
            LearnedCultivationMainWeights.zeros(),
            LearnedCultivationSupportWeights.zeros(),
            LearnedWispPlayWeights.zeros(),
            LearnedPlantEffectWeights.zeros(),
            LearnedBattleSupportWeights.zeros(),
            LearnedBattleMainWeights.zeros()
        ).create()

        assertTrue(director.buy is LearnedBuyStrategy)
        assertTrue(director.cultivationMain is LearnedCultivationMainPolicy)
        assertTrue(director.cultivationSupport is LearnedCultivationSupportPolicy)
        assertTrue(director.wispPlay is LearnedWispPlayPolicy)
        assertTrue(director.plantEffect is LearnedPlantEffectPolicy)
        assertTrue(director.battleSupport is LearnedBattleSupportPolicy)
        assertTrue(director.battleMain is LearnedBattleMainPolicy)
    }

    @Test
    fun `report keeps strategic families distinct`() {
        val out = captureStdout { printReport(InteractionAccumulator(), 1) }
        listOf(
            "CULTIVATION MAIN",
            "CULTIVATION SUPPORT",
            "WISP PLAY",
            "PLANT EXECUTION",
            "BATTLE MAIN",
            "BATTLE SUPPORT",
            "ROUND-EFFECT RESEARCH"
        ).forEach { assertContains(out, it) }
        assertContains(out, "FIRST total/game")
        assertContains(out, "FINAL total/game")
        assertContains(out, "SUNLIGHT-FUNDED total/game")
        assertContains(out, "HOLD")
    }

    @Test
    fun `report includes per player game mulch outcome buckets and threshold views`() {
        val acc = InteractionAccumulator()
        acc.roundMulchOutcomeByCount[0] = MulchOutcomeBucket(
            playerGames = 2, winShare = 0.5, totalVp = 30, battleVp = 18,
            wounds = 8, finalDicePower = 40, unusedMulch = 0
        )
        acc.roundMulchOutcomeByCount[3] = MulchOutcomeBucket(
            playerGames = 2, winShare = 1.5, totalVp = 50, battleVp = 30,
            wounds = 4, finalDicePower = 60, unusedMulch = 2
        )
        acc.battleMulchOutcomeByCount[5] = MulchOutcomeBucket(
            playerGames = 1, winShare = 1.0, totalVp = 28, battleVp = 18,
            wounds = 1, finalDicePower = 35, unusedMulch = 0
        )

        val out = captureStdout { printReport(acc, 4) }
        assertContains(out, "MULCH OUTCOME BY PLAYER-GAME")
        assertContains(out, "Round-card Mulch selections")
        assertContains(out, "Battle Mulch uses")
        assertContains(out, "threshold views:")
        assertContains(out, ">=3: n=2 win=75.00%")
        assertContains(out, "5+")
    }

    @Test
    fun `rejects invalid policy family`() {
        listOf(
            "--buy-policy",
            "--cultivation-main-policy",
            "--cultivation-support-policy",
            "--wisp-policy",
            "--plant-effect-policy",
            "--battle-support-policy",
            "--battle-main-policy"
        ).forEach { option ->
            assertFailsWith<IllegalArgumentException> {
                PolicyInteractionOptions.parse(listOf(option, "magic"))
            }
        }
    }

    private fun captureStdout(block: () -> Unit): String {
        val old = System.out
        val bytes = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(bytes))
        return try {
            block()
            bytes.toString()
        } finally {
            System.setOut(old)
        }
    }
}
