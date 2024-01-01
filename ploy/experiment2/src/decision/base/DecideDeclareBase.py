from typing import Optional
from src.data.Game import Game
from src.data.ID import PlayerID
from src.data.Moves import MoveDeclare


class DecideDeclareBase:

    game: Optional[Game]

    def __init__(self):
        self.game = None

    def decide(self, pid: PlayerID) -> Optional[MoveDeclare]:
        return None
