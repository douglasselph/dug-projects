import unittest
from src.deck import Deck
from src.card import *
from src.build import build_deck
from src.faction import Faction
from src.game import Game


class TestGame(unittest.TestCase):

    ROUNDS = 1000
    SUT: Game

    def setUp(self) -> None:
        self.SUT = Game(build_deck(Faction.SLASH))

    def test_game_slash_stats_show(self):
        # Arrange
        # Act
        stats = self.SUT.rounds(self.ROUNDS)
        # Appear
        print("SLASH:")
        stats.show()

    def test_game_all_factions_num_cards_10(self):
        # Loop
        for faction in Faction.__members__.values():
            # Arrange
            self.SUT = Game(build_deck(faction))
            # Act
            num_cards = len(self.SUT.deck.cards)
            # Appear
            self.assertEqual(10, num_cards)

    def test_game_all_factions_stats_show(self):
        # Loop
        for faction in Faction.__members__.values():
            # Arrange
            self.SUT = Game(build_deck(faction))
            # Act
            stats = self.SUT.rounds(self.ROUNDS)
            # Appear
            print(f"{faction}")
            stats.show()

    def test_game_just_maneuver_show(self):
        # Arrange
        deck = Deck()
        deck.add(Maneuver(20))
        deck.add(Maneuver(12))
        deck.add(Maneuver(10))
        deck.add(Maneuver(8))
        deck.add(Maneuver(6))
        deck.add(Armor())
        deck.add(Armor())
        deck.add(Armor())
        deck.add(Armor())
        deck.add(Armor())
        # Act
        stats = self.SUT.rounds(self.ROUNDS)
        # Appear
        print("MANEUVER:")
        stats.show()

    def test_game_slash_match_show(self):
        # Arrange
        deck1 = Deck()
        deck1.add(Maneuver(20)) # 10.5
        deck1.add(Armor())
        deck1.add(Armor())
        deck1.add(Armor())
        deck1.add(Armor())
        deck2 = Deck()
        deck2.add(Maneuver(8)) # 4.5
        deck2.add(Maneuver(6)) # 3.5
        deck2.add(Maneuver(4)) # 2.5
        deck2.add(Armor())
        deck2.add(Armor())
        game1 = Game(deck1)
        game2 = Game(deck2)
        # Act
        stats1 = game1.rounds(self.ROUNDS)
        stats2 = game2.rounds(self.ROUNDS)
        # Appear
        print("DECK 1 (20):")
        stats1.show()
        print("DECK 2 (8,6,4):")
        stats2.show()




