# package src.data
from src.data.ManeuverPlate import ManeuverPlate
from src.data.Deck import Deck


class Player:

    plate: ManeuverPlate
    energy: int
    pips: int
    stash: Deck
    draw: Deck
    fatal_received: bool

    def __init__(self):
        self.plate = ManeuverPlate()
        self.energy = 20
        self.pips = 0
        self.draw = Deck()
        self.stash = Deck()
        self.fatal_received = False
