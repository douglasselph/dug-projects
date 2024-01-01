from typing import List

from src.data.Player import Player
from src.data.Decision import DecisionLine, DecisionIntention


class TestPlayer(Player):

    def __init__(self):
        super().__init__()
        self.result_is_legal: List[bool] = []
        self.count = 0
        self.got_reveal_intentions_if_maxed = False
        self.got_reveal_all_intentions = False

    def is_legal_intention(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        if self.count < len(self.result_is_legal):
            value = self.result_is_legal[self.count]
            self.count += 1
            return value
        else:
            return super().is_legal_intention(line, coin)

    def reveal_intentions_if_maxed(self):
        super().reveal_intentions_if_maxed()
        self.got_reveal_intentions_if_maxed = True

    def reveal_all_intentions(self):
        super().reveal_all_intentions()
        self.got_reveal_all_intentions = True

