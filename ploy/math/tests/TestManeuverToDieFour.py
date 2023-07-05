import unittest
from unittest.mock import patch, MagicMock
from src.ManeuverToDieFour import ManeuverToDieFour
from src.DieCollection import DieCollection


class TestManeuverToDieFour(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverToDieFour()
        self.collection = DieCollection(self.default_sides)

    def test_adjust(self):
        # Arrange
        values = self.collection.roll()
        # Act
        self.SUT.adjust(values, 1)
        # Assert

