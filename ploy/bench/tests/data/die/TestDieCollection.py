import unittest
from unittest.mock import patch, Mock
from src.data.die.DieCollection import DieCollection
from src.data.die.Die import Die, DieSides


class TestDieCollection(unittest.TestCase):

    default_sides = [
        DieSides.D4, DieSides.D6, DieSides.D8, DieSides.D10, DieSides.D12, DieSides.D20
    ]

    def setUp(self):
        self.SUT = DieCollection(self.default_sides)

    @patch('src.data.die.Die.Die.factory')
    def test_roll__single_die(self, mock_factory):
        # Arrange
        mock_die = Mock()
        sample_die_rolls = [2, 3, 4, 5, 6, 10]
        mock_die.roll.side_effect = sample_die_rolls
        mock_factory.return_value = mock_die
        # Act
        values = self.SUT.roll()
        # Assert
        self.assertEqual(len(sample_die_rolls), values.num)
        for index, value in enumerate(values):
            self.assertEqual(sample_die_rolls[index], value)
