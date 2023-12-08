from typing import List, Optional
from src.data.Card import Card
from src.data.Decision import DecisionIntention
from src.data.ManeuverPlate import ManeuverPlate
from src.decision.base import BaseFeelingFeint


class ManeuverFeelingFeint:

    decision: BaseFeelingFeint

    def __init__(self, decision: BaseFeelingFeint):
        self.decision = decision

    def apply(self, plate: ManeuverPlate, coin: DecisionIntention) -> List[Card]:
        times = 0
        for line in plate.lines:
            if line.intention == coin and line.cards_face_up:
                for card in line.cards:
                    if card == Card.MANEUVER_FEELING_FEINT:
                        times += 1
        cards = []
        for time in range(times):
            cards.append(self._maneuver_feeling_feint(plate, coin))
        return cards

    # You may move any face down card on another line into the line with this card
    # and immediately reveal and apply it.
    #
    # Strategy: choose best card not in the current intention, and move it over.
    def _maneuver_feeling_feint(self, plate: ManeuverPlate, coin: DecisionIntention) -> Optional[Card]:

        best_line, best = self.decision.select_card(plate, coin)

        if best != Card.NONE and best_line is not None:
            for line in plate.lines:
                if line.intention == coin and line.cards_face_up:
                    line.add(best)
                    best_line.remove(best)
                    return best
        return None
