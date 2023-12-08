import unittest

from src.data.die.DieValues import DieValues
from src.data.maneuver.ManeuverNickToDeath import ManeuverNickToDeath


class TestManeuverNickToDeath(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverNickToDeath()

    def test_apply(self):
        # Arrange
        values = DieValues()
        # Act
        self.SUT.apply(values)
        # Assert


