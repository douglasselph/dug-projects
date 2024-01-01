from typing import Optional

from src.data.ID import PlayerID
from src.data.Moves import MoveDeclare
from src.decision.base import DecideDeclareBase


class DecideDeclareHuman(DecideDeclareBase):

    def decide(self, pid: PlayerID) -> Optional[MoveDeclare]:
        return None
