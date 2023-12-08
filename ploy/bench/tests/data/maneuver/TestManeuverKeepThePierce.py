import unittest

from src.data.die.DieValues import DieValues
from src.data.maneuver.ManeuverKeepThePierce import ManeuverKeepThePierce


class TestManeuverKeepThePierce(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverKeepThePierce()

    def test_apply(self):
        # Arrange
        values = DieValues()
        # Act
        self.SUT.apply(values)
        # Assert

