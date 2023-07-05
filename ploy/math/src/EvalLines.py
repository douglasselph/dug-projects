from typing import List

from Deck import Deck
from EvalLine import EvalLine
from Maneuver import Maneuver


#
# Will apply an evaluation against several benchmark lines.
#
class EvalLines:

    def __init__(self, maneuver: Maneuver, decks: List[Deck]):
        self.maneuver = maneuver
        self.lines: List[EvalLine] = []
        for deck in decks:
            self.lines.append(EvalLine(maneuver, deck))

    def add_cards(self):
        for line in self.lines:
            line.add_cards()

    def apply(self, adjust_times: int):
        for line in self.lines:
            line.set_stats(line.adjust(line.roll(), adjust_times))

    def reset(self):
        for line in self.lines:
            line.reset()

    def summaries(self) -> str:
        result = ""
        for line in self.lines:
            result += "\t" + str(line.stats.average)
        return result

    @property
    def can_level(self) -> bool:
        return self.maneuver.can_level

    def __str__(self) -> str:
        return '\n'.join(str(line) for line in self.lines)