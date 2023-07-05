import unittest
from unittest.mock import patch, MagicMock
from src.ManeuverFeelingFeint import ManeuverFeelingFeint
from src.DieCollection import DieCollection


class TestManeuverFeelingFeint(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverFeelingFeint()
        self.collection = DieCollection(self.default_sides)

    def test_adjust_some_new_value_added(self):
        # Arrange
        values = self.collection.roll()
        current_total = values.total
        # Act
        self.SUT.adjust(values, 1)
        # Assert
        new_total = values.total
        self.assertTrue(new_total > current_total)

