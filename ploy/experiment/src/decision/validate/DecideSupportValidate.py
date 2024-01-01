from typing import Optional

from src.data.ID import PlayerID
from src.data.Moves import MoveSupport
from src.decision.base.DecideSupportBase import DecideSupportBase


class DecideSupportValidate(DecideSupportBase):

    def decide(self, pid: PlayerID) -> Optional[MoveSupport]:
        return None

