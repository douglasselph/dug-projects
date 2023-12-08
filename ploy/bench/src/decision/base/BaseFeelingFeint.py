from typing import Optional
from src.data.ManeuverPlate import ManeuverPlate, Line
from src.data.Decision import DecisionIntention
from src.data.Card import Card


class BaseFeelingFeint:

    @staticmethod
    def select_card(plate: ManeuverPlate, coin: DecisionIntention) -> (Optional[Line], Card):
        return None, Card.NONE


