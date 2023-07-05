import unittest
from src.hand import Hand
from src.card import *


class TestHand(unittest.TestCase):

    cards: List[Card]
    SUT: Hand

    def setUp(self) -> None:
        self.cards = [Maneuver(20), Maneuver(12), Maneuver(10), Maneuver(8), Maneuver(6), Maneuver(4)]
        self.SUT = Hand(self.cards.copy())

    def test_has_card_true(self):
        # Arrange
        # Act
        value = self.SUT.has_cards
        # Assert
        self.assertTrue(value)

    def test_min(self):
        # Arrange
        expected_value = len(self.SUT.cards)
        # Act
        value = self.SUT.min()
        # Assert
        self.assertEqual(expected_value, value)

    def test_min_with_one_precision(self):
        # Arrange
        self.SUT = Hand([Precision(), Maneuver(20)])
        # Act
        value = self.SUT.min()
        # Assert
        # The 20 sided should now be a 20.
        self.assertEqual(20, value)

    def test_max(self):
        # Arrange
        # 20 + 12 + 10 + 8 + 6 + 4 = 60
        expected_value = 60
        # Act
        value = self.SUT.max()
        # Assert
        self.assertEqual(expected_value, value)

    def test_average(self):
        # Arrange
        # 10.5 + 6.5 + 5.5 + 4.5 + 3.5 + 2.5 = 33
        expected_value = 33
        # Act
        value = self.SUT.average()
        # Assert
        self.assertEqual(expected_value, value)

    def test_average_with_one_precision(self):
        # Arrange
        self.SUT.cards.append(Precision())
        # 20 + 6.5 + 5.5 + 4.5 + 3.5 + 2.5 = 42.5
        expected_value = 42.5
        # Act
        value = self.SUT.average()
        # Assert
        self.assertEqual(expected_value, value)

    def test_average_with_two_precisions(self):
        # Arrange
        self.SUT.cards.append(Precision())
        self.SUT.cards.append(Precision())
        # 20 + 12 + 5.5 + 4.5 + 3.5 + 2.5 = 48
        expected_value = 48
        # Act
        value = self.SUT.average()
        # Assert
        self.assertEqual(expected_value, value)

    def test_use_next_turn_instead_with_fresh_perspective(self):
        # Arrange
        self.SUT.cards.append(FreshPiercespective())
        expected_value = [FreshPiercespective()]
        # Act
        value = self.SUT.use_next_turn_instead()
        # Assert
        self.assertEqual(expected_value, value)
        self.assertEqual(len(self.cards), len(self.SUT.cards))

    def test_use_next_turn_instead_with_fresh_perspective_and_hold_your_pierce(self):
        # Arrange
        self.SUT.cards.append(FreshPiercespective())
        self.SUT.cards.append(HoldYourPierce())
        expected_value = [FreshPiercespective(), HoldYourPierce()]
        # Act
        value = self.SUT.use_next_turn_instead()
        # Assert
        self.assertEqual(expected_value, value)
        self.assertEqual(len(self.cards), len(self.SUT.cards))


