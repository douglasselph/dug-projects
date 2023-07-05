import unittest
from unittest.mock import patch, MagicMock
from src.DieCollection import DieCollection


class TestDieCollection(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.SUT = DieCollection(self.default_sides)

    @patch('src.Die.Die.factory')
    def test_roll_single_die(self, mock_factory):
        # Arrange
        mock_die = MagicMock()
        sample_die_rolls = [2, 3, 4, 5, 6, 10]
        mock_die.roll.side_effect = sample_die_rolls
        mock_factory.return_value = mock_die
        # Act
        values = self.SUT.roll()
        # Assert
        self.assertEqual(len(sample_die_rolls), values.num)
        for index, value in enumerate(values):
            self.assertEqual(sample_die_rolls[index], value)

