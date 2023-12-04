from typing import Optional
from src.data.ManeuverPlate import ManeuverPlate, Line
from src.data.Decision import DecisionIntention
from src.data.Card import Card


def maneuver_apply_feeling_feint(plate: ManeuverPlate, coin: DecisionIntention):
    times = 0
    for line in plate.lines:
        if line.intention == coin:
            for card in line.cards:
                if card == Card.MANEUVER_FEELING_FEINT:
                    times += 1
    for time in range(times):
        _maneuver_feeling_feint(plate, coin)


# You may move any face down card on another line into the line with this card and immediately reveal and apply it.
# Strategy: choose best card not in the current intention, and move it over.
def _maneuver_feeling_feint(plate: ManeuverPlate, coin: DecisionIntention):
    best: Card = Card.NONE
    best_value = 0
    best_line: Optional[Line] = None
    for line in plate.lines:
        if line.intention != coin:
            for card in line.cards:
                if isinstance(card, Card):
                    value = card.ff_value
                else:
                    value = 0
                if value > best_value:
                    best_value = value
                    best_line = line
                    best = card
    if best != Card.NONE and best_line is not None:
        for line in plate.lines:
            if line.intention == coin and line.intention_face_up:
                line.add(best)
                best_line.remove(best)
                break
