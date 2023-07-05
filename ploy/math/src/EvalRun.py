from typing import List
from src.Maneuver import Maneuver
from src.Deck import Deck
from src.EvalLines import EvalLines


class EvalRun:

    maneuvers: List[Maneuver]
    decks: List[Deck]
    count: int
    step: int
    adjust_times_list: List[int]

    def __init__(self):
        self.maneuvers = []
        self.deck_list = []
        self.count = 500000
        self.step = 1
        self.adjust_times_list: List[int] = []

    def run(self):
        for maneuver in self.maneuvers:
            lines = EvalLines(maneuver, self.decks)
            if lines.can_level:
                for adjust_times in self.adjust_times_list:
                    print(f"\nComputing {str(maneuver)} x{adjust_times}:")
                    self._run(lines, adjust_times)
            else:
                print(f"\nComputing {str(maneuver)}:")
                self._run(lines, 1)

    def _run(self, lines: EvalLines, adjust_times: int):
        lines.reset()
        lines.add_cards()
        for i in range(self.count):
            if (i + 1) % self.step == 1 and i > 0:
                print(f"{i:4}\n{lines}:")
            lines.apply(adjust_times)
        print(f"Final:\n{lines}\n{lines.summaries()}")
