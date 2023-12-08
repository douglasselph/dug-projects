import unittest
from unittest.mock import patch, Mock

from src.data.maneuver.ManeuverInHewOf import ManeuverInHewOf
from src.data.die.DieValues import DieValues
from src.data.die.Die import DieSides, Die


class TestManeuverInHewOf(unittest.TestCase):

    def setUp(self):
        self.SUT = ManeuverInHewOf()

    @patch('src.data.die.Die.Die.rand')
    def test_apply__one_rerolled(self, mock_rand):
        # Arrange
        mock_rand.return_value = 2
        values = DieValues()
        values.add(Die(DieSides.D4, 1))
        values.add(Die(DieSides.D20, 2))
        values.add(Die(DieSides.D8, 1))
        # Act
        self.SUT.apply(values)
        # Assert
        for die in values.values:
            self.assertTrue(die.value != 1)
