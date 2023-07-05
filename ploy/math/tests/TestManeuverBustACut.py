from typing import List
import unittest
from unittest.mock import patch
from src.ManeuverBustACut import ManeuverBustACut
from src.DieCollection import DieCollection
from src.Die import Die
from src.DieValues import DieValues


class TestManeuverBustACut(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = ManeuverBustACut()
        self.collection = DieCollection(self.default_sides)
        self.values = DieValues()

    @patch('src.Die.Die.rand')
    def test_adjust_re_roll_of_d20(self, mock_rand):
        # Arrange
        values = [2, 3, 4, 5, 6, 10]
        self._setup_values(values)
        mock_rand_value = 18
        mock_rand.return_value = mock_rand_value
        base_values_total = sum(values[:-1])
        expected_total = base_values_total + mock_rand_value
        # Act
        self.SUT.adjust(self.values, 1)
        # Assert
        self.assertEqual(expected_total, self.values.total)

    @patch('src.Die.Die.rand')
    def test_adjust_re_roll_of_d12(self, mock_rand):
        # Arrange
        values = [2, 3, 4, 5, 6, 13]
        self._setup_values(values)
        mock_rand_value = 12
        mock_rand.return_value = mock_rand_value
        base_values_total = sum([2, 3, 4, 5, 13])
        expected_total = base_values_total + mock_rand_value
        # Act
        self.SUT.adjust(self.values, 1)
        # Assert
        self.assertEqual(expected_total, self.values.total)

    @patch('src.Die.Die.rand')
    def test_adjust_re_roll_of_d6_and_d12(self, mock_rand):
        # Arrange
        values = [2, 1, 4, 5, 6, 13]
        self._setup_values(values)
        mock_rand_values = [6, 11]
        mock_rand.side_effect = mock_rand_values
        base_values_total = sum([2, 4, 5, 13])
        expected_total = base_values_total + sum(mock_rand_values)
        # Act
        self.SUT.adjust(self.values, 2)
        # Assert
        self.assertEqual(expected_total, self.values.total)

    @patch('src.Die.Die.rand')
    def test_adjust_re_roll_of_d8_because_holds_a_larger_advantage(self, mock_rand):
        # Arrange
        values = [1, 3, 1, 4, 6, 10]
        self._setup_values(values)
        mock_rand_value = 8
        mock_rand.return_value = mock_rand_value
        base_values_total = sum([1, 3, 4, 6, 10])
        expected_total = base_values_total + mock_rand_value
        # Act
        self.SUT.adjust(self.values, 1)
        # Assert
        self.assertEqual(expected_total, self.values.total)

    def _setup_values(self, use_values: List[int]):
        for index in range(len(use_values)):
            self.values.add(Die(self.default_sides[index], use_values[index]))
