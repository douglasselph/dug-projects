import unittest
from src.deck import Deck
from src.card import *


class TestDeck(unittest.TestCase):

    SUT: Deck

    def setUp(self) -> None:
        self.SUT = Deck()
        self.SUT.add(Maneuver(20))
        self.SUT.add(Maneuver(12))
        self.SUT.add(Maneuver(10))
        self.SUT.add(Maneuver(8))
        self.SUT.add(Maneuver(6))
        self.SUT.add(Maneuver(4))
        self.SUT.add(Precision())
        self.SUT.add(BustACut())
        self.SUT.add(Armor())
        self.SUT.add(Shield(Size.SMALL))

    def test_5_cards_on_deal_each_time(self):
        # Arrange
        # Act
        hand = self.SUT.deal()
        hand2 = self.SUT.deal()
        hand3 = self.SUT.deal()
        hand4 = self.SUT.deal()
        # Assert
        self.assertEqual(5, len(hand.cards))
        self.assertEqual(5, len(hand2.cards))
        self.assertEqual(5, len(hand3.cards))
        self.assertEqual(5, len(hand4.cards))


