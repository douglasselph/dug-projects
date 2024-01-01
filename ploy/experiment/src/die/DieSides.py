from __future__ import annotations
from enum import Enum
import random


class DieSides(Enum):
    NONE = 0
    D4 = 4
    D6 = 6
    D8 = 8
    D10 = 10
    D12 = 12
    D20 = 20

    def roll(self) -> int:
        if self == DieSides.NONE:
            return 0
        return random.randint(1, self.value)

    @property
    def sides(self) -> int:
        return self.value

    def downgrade(self) -> DieSides:
        if self == DieSides.D20:
            return DieSides.D12
        if self == DieSides.D12:
            return DieSides.D10
        if self == DieSides.D10:
            return DieSides.D8
        if self == DieSides.D8:
            return DieSides.D6
        if self == DieSides.D6:
            return DieSides.D4
        return DieSides.NONE

    def average(self) -> float:
        if self.value == 0:
            return 0
        return (self.value + 1) / 2
