package dugsolutions.leaf.simulation.v35.experiment.baseline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainActionFamilyTest {
    @Test fun `recognizes cultivation and final Battle main actions`() {
        assertEquals("Draw", MainActionFamily.classify("Main(Draw)"))
        assertEquals("Draw", MainActionFamily.classify("FinalMain(Draw)"))
        assertEquals("Plant", MainActionFamily.classify("Main(ActivatePlant(Root_05_02#1))"))
        assertEquals("Plant", MainActionFamily.classify("FinalMain(ActivatePlant(Vine_07_01#3))"))
        assertEquals("Round effect", MainActionFamily.classify("Main(RoundEffect1)"))
        assertEquals("Round effect", MainActionFamily.classify("FinalMain(RoundEffect2)"))
        assertEquals("Round effect", MainActionFamily.classify("Main(RoundEffect2, chance=65%)"))
    }

    @Test fun `excludes subordinate choices and ambiguous bare labels`() {
        assertNull(MainActionFamily.classify("Draw"))
        assertNull(MainActionFamily.classify("Plant(Root_05_02#1, FACE_UP)"))
        assertNull(MainActionFamily.classify("Support(PlayWisp(Wisp))"))
        assertNull(MainActionFamily.classify("Purchase(Plant(Root))"))
        assertNull(MainActionFamily.classify("Done"))
        assertEquals("Unrecognized Main", MainActionFamily.classify("Main(NewAction)"))
    }

    @Test fun `denominator excludes subordinate selections`() {
        val labels = listOf("Main(Draw)", "Main(ActivatePlant(Root#1))",
            "FinalMain(RoundEffect2)", "Play(Plant(Root))", "Draw", "Done")
        val counted = labels.mapNotNull(MainActionFamily::classify)
        assertEquals(3, counted.size)
        assertEquals(1, counted.count { it == "Draw" })
        assertEquals(1, counted.count { it == "Plant" })
        assertEquals(1, counted.count { it == "Round effect" })
    }
}
