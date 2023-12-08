import unittest

from src.data.die.DieValues import DieValues
from src.data.maneuver.ManeuverPrecision import ManeuverPrecision


class TestManeuverPrecision(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverPrecision()

    def test_apply(self):
        # Arrange
        values = DieValues()
        # Act
        self.SUT.apply(values)
        # Assert

