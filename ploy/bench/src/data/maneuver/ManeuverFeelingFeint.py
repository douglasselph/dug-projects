from src.data.Card import Card
from src.data.Decision import DecisionIntention
from src.data.ManeuverPlate import ManeuverPlate
from src.decision.base import BaseFeelingFeint


class ManeuverFeelingFeint:

    decision: BaseFeelingFeint

    def __init__(self, decision: BaseFeelingFeint):
        self.decision = decision

    def apply(self, plate: ManeuverPlate, coin: DecisionIntention):
        times = 0
        for line in plate.lines:
            if line.intention == coin:
                for card in line.cards:
                    if card == Card.MANEUVER_FEELING_FEINT:
                        times += 1
        for time in range(times):
            self._maneuver_feeling_feint(plate, coin)

    # You may move any face down card on another line into the line with this card
    # and immediately reveal and apply it.
    #
    # Strategy: choose best card not in the current intention, and move it over.
    def _maneuver_feeling_feint(self, plate: ManeuverPlate, coin: DecisionIntention):

        best_line, best = self.decision.select_card(plate, coin)

        if best != Card.NONE and best_line is not None:
            for line in plate.lines:
                if line.intention == coin and line.intention_face_up:
                    line.add(best)
                    best_line.remove(best)
                    break
