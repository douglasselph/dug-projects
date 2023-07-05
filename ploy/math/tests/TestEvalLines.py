import unittest
from unittest.mock import MagicMock, patch

from src.Deck import Deck
from src.EvalLines import EvalLines
from src.ManeuverToDieFour import ManeuverToDieFour


class TestEvalLines(unittest.TestCase):

    default_sides = [4, 6, 8, 10, 12, 20]

    def setUp(self):
        self.deck = Deck(self.default_sides, self.default_sides)
        self.decks = [self.deck]
        self.maneuver = MagicMock(spec=ManeuverToDieFour)
        self.SUT = EvalLines(self.maneuver, self.decks)

    def test_add_cards_called_maneuver_add_cards_with_expected_arg(self):
        # Arrange
        # Act
        self.SUT.add_cards()
        # Assert
        self.maneuver.add_cards.assert_called_once_with(self.deck)

    def test_apply(self):
        # Arrange
        # Act
        self.SUT.apply(1)
        # Assert

    def test_reset(self):
        # Arrange
        # Act
        self.SUT.reset()
        # Assert

    def test_summaries(self):
        # Arrange
        self.SUT.apply(2)
        # Act
        self.SUT.summaries()
        # Assert
