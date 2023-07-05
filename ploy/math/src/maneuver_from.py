from typing import Optional

from src.CardType import CardType, CardTypeManeuver
from src.Maneuver import Maneuver
from src.ManeuverBustACut import ManeuverBustACut
from src.ManeuverPrecision import ManeuverPrecision
from src.ManeuverInHewOf import ManeuverInHewOf
from src.ManeuverToDieFour import ManeuverToDieFour
from src.ManeuverCuttingRiposte import ManeuverCuttingRiposte
from src.ManeuverFeelingFeint import ManeuverFeelingFeint
from src.ManeuverKeepThePierce import ManeuverKeepThePierce
from src.ManeuverNickToDeath import ManeuverNickToDeath


def maneuver_from(card_type: CardType) -> Optional[Maneuver]:
    if card_type == CardTypeManeuver.BUST_A_CUT:
        return ManeuverBustACut()
    elif card_type == CardTypeManeuver.CUTTING_RIPOSTE:
        return ManeuverCuttingRiposte()
    elif card_type == CardTypeManeuver.FEELING_FEINT:
        return ManeuverFeelingFeint()
    elif card_type == CardTypeManeuver.IN_HEW_OF:
        return ManeuverInHewOf()
    elif card_type == CardTypeManeuver.KEEP_THE_PIERCE:
        return ManeuverKeepThePierce()
    elif card_type == CardTypeManeuver.NICK_TO_DEATH:
        return ManeuverNickToDeath()
    elif card_type == CardTypeManeuver.PRECISION:
        return ManeuverPrecision()
    elif card_type == CardTypeManeuver.TO_DIE_FOUR:
        return ManeuverToDieFour()
    else:
        return None
