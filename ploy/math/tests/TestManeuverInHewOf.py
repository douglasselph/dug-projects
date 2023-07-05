import unittest
from unittest.mock import patch, MagicMock
from src.ManeuverInHewOf import ManeuverInHewOf
from src.DieValues import DieValues
from src.Die import Die


class TestManeuverInHewOf(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverInHewOf()
        self.values = DieValues()
        for side in self.default_sides:
            self.values.add(Die(side, 1))

    def test_adjust_no_ones_are_found(self):
        # Arrange
        # Act
        self.SUT.adjust(self.values, 10)
        # Assert
        for die in self.values:
            self.assertFalse(die.value == 1)

