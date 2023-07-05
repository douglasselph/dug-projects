from typing import List, Tuple
from src.card import *


class ComputeBustACut:

    precision = 6
    _sides: List[int]

    def __init__(self, sides: List[int]):
        self._sides = sides

    # P(x) = [1 - (1 - p1(x)) * (1 - p2(x)) * (1 - p3(x)) * (1 - p4(x))]
    # Here, p1(x), p2(x), p3(x), and p4(x) represent the probabilities of rolling x as the highest value
    #   on each die respectively.
    def average(self, x: int) -> float:
        cumulate = 1.0
        for side in self._sides:
            cumulate *= 1.0 - probability1(x, side)
        return round(1 - cumulate, self.precision)


# Return the probability of rolling "x" after two rolls of the N sided die.
#
# 1,1=1. (1) [1/16]
# 2,1=2. 1,2=2, 2,2=2. (3) [3/16]
# 1,3=3. 3,1=3. 2,3=3. 3,2=3. 3,3=3 (5) [5/16]
# 1,4=4. 4,1=4. 2,4=4, 4,2=4. 3,4=4. 4,3=4. 4,4=4. (7) [7/16]
#
# 6 sided: total=6x6=16
# 1,1=1. (1) [1/36]
# 2,1=2. 1,2=2, 2,2=2. (3) [3/36]
# 1,3=3. 3,1=3. 2,3=3. 3,2=3. 3,3=3 (5) [5/36]
# 1,4=4. 4,1=4. 2,4=4, 4,2=4. 3,4=4. 4,3=4. 4,4=4. (7) [7/36]
# 1,5=5. 5,1=5. 2,5=5. 5,2=5. 3,5=5. 5,3=5. 4,5=5. 5,4=5. 5,5=5 (9) [9/36]
# 1,6=6. 6,1=6. 2,6=6. 6,2=6. 3,6=6. 6,3=6. 4,6=6. 6,4=6. 5,6=6. 6,5=6. 6,6=6 (11) [11/36]

# F(x) = f(x) / (sides*sides).
# f(1) = 1
# f(2) = f(1) + 2
# f(3) = f(2) + 2
# f(4) = f(3) + 2
# f(x) = x*2 - 1
def probability1(x: int, sides: int) -> float:
    if x > sides or x < 1:
        return 0
    number_of_positive_outcomes = x * 2 - 1
    total_outcomes = sides * sides
    return float(number_of_positive_outcomes) / float(total_outcomes)


# Return the probability of rolling "x" after two rolls of the 2 dice with a different number of sides.
# Sides= 4,6
# 1,1=1. (1) [1/24]
# 2,1=2. 1,2=2, 2,2=2. (3) [3/24]
# 1,3=3. 3,1=3. 2,3=3. 3,2=3. 3,3=3 (5) [5/24]
# 1,4=4. 4,1=4. 2,4=4, 4,2=4. 3,4=4. 4,3=4. 4,4=4. (7) [7/24]
# 1,5=5. 2,5=5. 3,5=5. 4,5=5. (4) [4/24]
# 1,6=6. 2,6=6. 3,6=6. 4,6=6. (4) [4/24]
#
# F(x) = f(x) / (sides1*sides2).
# sidesMin = min(sides1,sides2)
# f(x) = if x <= sidesMin then x*2-1
#        else sidesMin
def probability2(x: int, sides1: int, sides2: int) -> float:
    sides_min = min(sides1, sides2)
    sides_max = max(sides1, sides2)
    if x > sides_max or x < 1:
        return 0
    total_outcomes = float(sides1 * sides2)
    if x <= sides_min:
        number_of_positive_outcomes = x * 2 - 1
        return float(number_of_positive_outcomes) / total_outcomes
    return float(sides_min) / total_outcomes





