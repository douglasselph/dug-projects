from Maneuver import Maneuver
from src.DieValues import DieValues
from src.CardType import CardTypeManeuver
from src.Deck import Deck


class ManeuverToDieFour(Maneuver):

    def __init__(self):
        super().__init__()
        self.card_type = CardTypeManeuver.TO_DIE_FOUR
        self.best = 0

    #
    # Feature: D4. Draw a card and apply to this line.
    # ToDieFour Strategy rules:
    #    This one is handled by adding extra dice, there is no modification.
    #
    def adjust(self, dice: DieValues, level: int):
        pass

    #
    # Add the best card left from the draw deck, and then also add a D4.
    #
    def add_cards(self, deck: Deck):
        best = deck.draw.best
        if best > 0:
            deck.hand.add_die(best)
            deck.draw.remove_die(best)
        deck.hand.add_die(4)

