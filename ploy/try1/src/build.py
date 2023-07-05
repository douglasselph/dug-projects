from src.faction import Faction
from src.deck import Deck
from src.card import *


def build_deck(faction: Faction) -> Deck:
    deck = Deck()
    deck.add(Maneuver(20))
    deck.add(Maneuver(12))
    deck.add(Maneuver(10))
    deck.add(Maneuver(8))
    deck.add(Maneuver(6))
    if faction == Faction.SLASH:
        deck.add(PoundingFlurry())
        deck.add(ShearMeOut())
        deck.add(Armor())
        deck.add(Armor())
        deck.add(Backpack())
    elif faction == Faction.FANG:
        deck.add(FreshPiercespective())
        deck.add(HoldYourPierce())
        deck.add(Armor())
        deck.add(Shield(Size.MEDIUM))
        deck.add(Backpack())
    elif faction == Faction.THORN:
        deck.add(CounterBlow())
        deck.add(Feint())
        deck.add(Shield(Size.SMALL))
        deck.add(Backpack())
        deck.add(Backpack())
    elif faction == Faction.RAVEN:
        deck.add(Precision())
        deck.add(Feint())
        deck.add(Armor())
        deck.add(Armor())
        deck.add(Backpack())
    return deck

