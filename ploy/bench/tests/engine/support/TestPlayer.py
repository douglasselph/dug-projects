from typing import List

from src.data.Player import Player
from src.data.Decision import DecisionLine, DecisionIntention


class TestPlayer(Player):

    def __init__(self):
        super().__init__()
        self.result_is_legal: List[bool] = []
        self.count = 0

    def is_legal_intention(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        value = self.result_is_legal[self.count]
        self.count += 1
        return value

