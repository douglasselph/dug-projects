import unittest

from src.data.maneuver.ManeuverInHewOf import ManeuverInHewOf
from src.data.die.DieValues import DieValues


class TestManeuverInHewOf(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverInHewOf()

    def test_apply(self):
        # Arrange
        values = DieValues()
        # Act
        self.SUT.apply(values)
