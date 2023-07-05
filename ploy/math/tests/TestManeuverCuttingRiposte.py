import unittest
from unittest.mock import patch, MagicMock
from src.ManeuverCuttingRiposte import ManeuverCuttingRiposte
from src.DieCollection import DieCollection


class TestManeuverCuttingRiposte(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverCuttingRiposte()
        self.collection = DieCollection(self.default_sides)

    def test_adjust_re_roll_of_d20(self):
        # Arrange
        values = self.collection.roll()
        # Act
        self.SUT.adjust(values, 1)
        # Assert

