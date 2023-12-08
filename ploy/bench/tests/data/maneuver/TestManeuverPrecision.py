import unittest

from src.data.die.DieValues import DieValues
from src.data.die.Die import Die, DieSides
from src.data.maneuver.ManeuverPrecision import ManeuverPrecision


class TestManeuverPrecision(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverPrecision(True)

    def test_apply__D20_okay_and_is_D20__apply_to_D20(self):
        # Arrange
        values = DieValues()
        values.add(Die(DieSides.D10, 4))
        values.add(Die(DieSides.D20, 5))
        # Act
        was_d20 = self.SUT.apply(values)
        # Assert
        self.assertTrue(was_d20)
        self.assertEqual(4, values.values[0].value)
        self.assertEqual(20, values.values[1].value)

    def test_apply__D20_not_okay_has_D20__apply_to_D10(self):
        # Arrange
        self.SUT = ManeuverPrecision(False)
        values = DieValues()
        values.add(Die(DieSides.D10, 4))
        values.add(Die(DieSides.D20, 5))
        # Act
        was_d20 = self.SUT.apply(values)
        # Assert
        self.assertFalse(was_d20)
        self.assertEqual(10, values.values[0].value)
        self.assertEqual(5, values.values[1].value)

    def test_apply__many_values__apply_to_die_with_greatest_diff(self):
        # Arrange
        values = DieValues()
        values.add(Die(DieSides.D4, 4))
        values.add(Die(DieSides.D6, 4))
        values.add(Die(DieSides.D10, 2))
        values.add(Die(DieSides.D12, 10))
        values.add(Die(DieSides.D20, 15))
        # Act
        was_d20 = self.SUT.apply(values)
        # Assert
        self.assertFalse(was_d20)
        self.assertEqual(4, values.values[0].value)
        self.assertEqual(4, values.values[1].value)
        self.assertEqual(10, values.values[2].value)
        self.assertEqual(10, values.values[3].value)
        self.assertEqual(15, values.values[4].value)

