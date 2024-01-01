from typing import Optional
from src.data.Game import Game
from src.data.ID import PlayerID
from src.data.Moves import MoveSupport


class DecideSupportBase:

    game: Optional[Game]

    def decide(self, pid: PlayerID) -> Optional[MoveSupport]:
        return None
