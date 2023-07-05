import unittest
from unittest.mock import patch, MagicMock, PropertyMock

from src.EvalLine import EvalLine
from src.Maneuver import Maneuver
from src.ManeuverBustACut import ManeuverBustACut
from src.ManeuverToDieFour import ManeuverToDieFour
from src.Deck import Deck
from src.DieValues import DieValues
from src.Stats import Stats


class TestEvalLine(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.deck = Deck(self.default_sides, self.default_sides)
        self.maneuver = ManeuverBustACut()
        self.maneuver2 = ManeuverToDieFour()
        self.SUT = EvalLine(self.maneuver, self.deck)

    @patch('src.Deck.DieCollection.roll')
    def test_roll_returns_mocked_die_values(self, mock_roll):
        # Arrange
        mock_die_values = MagicMock()
        mock_roll.return_value = mock_die_values
        # Act
        rolled = self.SUT.roll()
        # Assert
        self.assertIs(rolled, mock_die_values)
        mock_roll.assert_called_once()

    def test_add_cards_calls_maneuver_add_cards_with_deck(self):
        # Arrange
        self.maneuver = MagicMock(spec=Maneuver)
        self.SUT = EvalLine(self.maneuver, self.deck)
        # Act
        self.SUT.add_cards()
        # Assert
        self.maneuver.add_cards.assert_called_once_with(self.deck)

    def test_adjust_calls_maneuver_adjust_multiple_times(self):
        # Arrange
        adjust_times = 5
        dice = self.deck.hand.roll()
        self.maneuver = MagicMock(spec=Maneuver)
        self.SUT = EvalLine(self.maneuver, self.deck)
        # Act
        self.SUT.adjust(dice, adjust_times)
        # Assert
        self.maneuver.adjust.assert_called_with(dice, adjust_times)

    def test_set_stats_calls_add_with_correct_total(self):
        # Arrange
        dice = self.deck.hand.roll()
        # Act
        self.SUT.set_stats(dice)
        # Assert
        self.assertEqual(dice.total, self.SUT.stats.total)

