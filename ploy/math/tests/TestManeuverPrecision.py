import unittest
from unittest.mock import patch, MagicMock
from src.ManeuverPrecision import ManeuverPrecision
from src.DieCollection import DieCollection


class TestManeuverPrecision(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverPrecision()
        self.collection = DieCollection(self.default_sides)
        self.values = self.collection.roll()

    def test_adjust_d20_is_set_to_max(self):
        # Arrange
        default_values = [3, 5, 7, 9, 11, 5]
        for index in range(self.values.num):
            self.values[index].value = default_values[index]
        previous_value = default_values[-1]
        expected_total = self.values.total - previous_value + 20
        # Act
        self.SUT.adjust(self.values, 1)
        # Assert
        self.assertEqual(expected_total, self.values.total)

    def test_adjust_d12_is_set_to_max(self):
        # Arrange
        default_values = [3, 3, 3, 4, 1, 18]
        for index in range(self.values.num):
            self.values[index].value = default_values[index]
        previous_value = default_values[-2]
        expected_total = self.values.total - previous_value + 12
        # Act
        self.SUT.adjust(self.values, 1)
        # Assert
        self.assertEqual(expected_total, self.values.total)

    def test_adjust_d8_d10_is_set_to_max(self):
        # Arrange
        default_values = [3, 5, 1, 1, 11, 18]
        for index in range(self.values.num):
            self.values[index].value = default_values[index]
        previous_value_d8 = default_values[2]
        previous_value_d10 = default_values[3]
        expected_total = self.values.total - previous_value_d8 + 8 - previous_value_d10 + 10
        # Act
        self.SUT.adjust(self.values, 2)
        # Assert
        self.assertEqual(expected_total, self.values.total)
