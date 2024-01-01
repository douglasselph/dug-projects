from __future__ import annotations
from typing import List
from src.data.ID import PlayerID, IntentionID
from src.die.DieSides import DieSides
from src.die.DieCollection import DieCollection


class Move:

    def __init__(self):
        self.who: PlayerID = PlayerID.NONE
        self.bout: int = 0


class MoveDeclare(Move):

    def __init__(self):
        super().__init__()
        self.intention: IntentionID = IntentionID.NONE
        self.die: DieSides = DieSides.NONE


class MoveSupport(Move):

    def __init__(self):
        super().__init__()
        self.dice: DieCollection = DieCollection([])


class MoveExecute(Move):

    def __init__(self):
        super().__init__()
        self.self_roll_value = 0
        self.other_roll_value = 0
        self.post_hp = 0


class Moves:

    def __init__(self):
        self.moves: List[Move] = []

    def add(self, move: Move):
        self.moves.append(move)

    def extend(self, moves: Moves):
        self.moves.extend(moves.moves)

    def set_bout(self, bout: int):
        for move in self.moves:
            move.bout = bout


