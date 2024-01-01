from typing import Optional, List

from src.data.ID import PlayerID, IntentionID
from src.data.Moves import MoveSupport
from src.decision.base.DecideSupportBase import DecideSupportBase
from src.die.DieSides import DieSides
from src.die.DieCollection import DieCollection
import random


class DecideSupportValidate(DecideSupportBase):

    def decide(self, pid: PlayerID) -> Optional[MoveSupport]:
        move = MoveSupport(pid)
        player = self.game.get_player(pid)
        maneuvers_left = self.game.maneuvers_left
        if maneuvers_left <= 1:
            move.dice = player.dice.dup()
        elif player.declaration == IntentionID.ATTACK:
            monkey = random.randint(1, 10) - 1
            cutoff: float = 1.0 - monkey * .1
            move.dice = DieCollection(self.collect_attack(player.dice, cutoff, maneuvers_left-1))
        else:
            move.dice = None
        return move

    @staticmethod
    def collect_attack(dice: DieCollection, cutoff: float, must_retain: int) -> List[DieSides]:
        target = dice.average * cutoff
        shuffled = dice.dup().sides
        random.shuffle(shuffled)
        reduced_dice = shuffled[:-must_retain]
        result: List[DieSides] = []
        total = 0
        while total <= target and len(reduced_dice) > 0:
            next_die = reduced_dice.pop()
            result.append(next_die)
            total += next_die.average
        return result





