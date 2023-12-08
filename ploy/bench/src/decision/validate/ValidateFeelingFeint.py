from typing import Optional
from src.decision.base.BaseFeelingFeint import BaseFeelingFeint
from src.data.Card import Card
from src.data.ManeuverPlate import ManeuverPlate, Line
from src.data.Decision import DecisionIntention


class ValidateFeelingFeint(BaseFeelingFeint):

    @staticmethod
    def select_card(plate: ManeuverPlate, coin: DecisionIntention) -> (Optional[Line], Card):
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

        return best_line, best

