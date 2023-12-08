import unittest

from src.data.die.DieValues import DieValues
from src.data.die.Die import Die, DieSides
from src.data.maneuver.ManeuverNickToDeath import ManeuverNickToDeath


class TestManeuverNickToDeath(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverNickToDeath()

    def test_apply__one_D4__added_two_to_value(self):
        # Arrange
        values = DieValues()
        values.add(Die(DieSides.D6, 3))
        values.add(Die(DieSides.D4, 2))
        total_expected = 7
        # Act
        self.SUT.apply(values)
        # Assert
        self.assertEqual(total_expected, values.total)


