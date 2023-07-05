import unittest
from unittest.mock import patch, MagicMock
from src.ManeuverNickToDeath import ManeuverNickToDeath
from src.DieCollection import DieCollection


class TestManeuverNickToDeath(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverNickToDeath()
        self.collection = DieCollection(self.default_sides)
        self.values = self.collection.roll()

    @patch('src.Die.Die.rand')
    def test_adjust_d4s_were_added(self, mock_rand):
        # Arrange
        d4_roll_values = [3, 4, 2, 3]
        mock_rand.side_effect = d4_roll_values
        expected_add = sum(d4_roll_values[1:])
        expected_total = self.values.total + expected_add
        # Act
        self.SUT.adjust(self.values, 1)
        # Assert
        self.assertEqual(expected_total, self.values.total)

    @patch('src.Die.Die.rand')
    def test_adjust_2_d4s_were_added(self, mock_rand):
        # Arrange
        d4_roll_values = [3, 4, 2, 3, 2, 4, 1]
        mock_rand.side_effect = d4_roll_values
        expected_add = sum([4, 2, 3, 4, 1])
        expected_total = self.values.total + expected_add
        # Act
        self.SUT.adjust(self.values, 2)
        # Assert
        self.assertEqual(expected_total, self.values.total)

