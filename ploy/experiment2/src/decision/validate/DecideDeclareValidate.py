from typing import Optional

from src.data.ID import PlayerID, IntentionID
from src.data.Moves import MoveDeclare
from src.decision.base import DecideDeclareBase
import random


class DecideDeclareValidate(DecideDeclareBase):

    def decide(self, pid: PlayerID) -> Optional[MoveDeclare]:
        move = MoveDeclare(pid)
        player = self.game.get_player(pid)
        opponent = self.game.get_opponent(pid)
        player_average = player.dice_average
        opponent_average = opponent.dice_average
        cutoff: float = 2.0
        monkey = random.randint(1, 10)
        if monkey <= 1:
            cutoff = 0.75
        elif monkey <= 2:
            cutoff = 1.0
        elif monkey <= 3:
            cutoff = 1.5
        elif monkey <= 4:
            cutoff = 4
        if player_average < opponent_average / cutoff:
            move.intention = IntentionID.DODGE
            move.die = player.die_lowest
        else:
            move.intention = IntentionID.ATTACK
            move.die = player.die_lowest
        return move



