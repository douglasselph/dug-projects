from typing import Optional, List

from src.data.Card import CardComposite
from src.data.Card import DieSides, Card, CardWound
from src.data.maneuver.ManeuverCuttingRiposte import ManeuverCuttingRiposte
from src.data.maneuver.ManeuverInHewOf import ManeuverInHewOf
from src.data.maneuver.ManeuverKeepThePierce import ManeuverKeepThePierce
from src.data.maneuver.ManeuverNickToDeath import ManeuverNickToDeath
from src.data.maneuver.ManeuverPrecision import ManeuverPrecision
from src.data.die.DieCollection import DieCollection
from src.data.die.DieValues import DieValues


class IncidentBundle:

    cards: List[CardComposite]
    dice: Optional[DieCollection]
    values: Optional[DieValues]

    def __init__(self):
        self.cards = []
        self.dice = None
        self.values = None

    def pull_dice(self):
        self.dice = DieCollection(self._pull_dice(self.cards))

    def apply_cards_pre_roll(self, opponent: Optional[DieCollection]):
        for card in self.cards:
            self._apply_card_pre_roll(card, self.dice, opponent)

    def roll(self):
        self.values = self.dice.roll()

    def apply_cards_post_roll(self, opponent: Optional[DieValues]):
        for card in self.cards:
            self._apply_card_post_roll(card, self.values, opponent)

    @property
    def wounds(self) -> int:
        wound = 0
        for card in self.cards:
            if isinstance(card, CardWound):
                wound += card.pip_penalty
        return wound

    @property
    def total(self) -> int:
        return self.values.total - self.wounds

    @staticmethod
    def _pull_dice(cards: List[CardComposite]):
        result: List[DieSides] = []
        for card in cards:
            bonus = card.die_bonus
            if bonus != DieSides.NONE:
                result.append(bonus)
        return result

    @staticmethod
    def _apply_card_pre_roll(card: CardComposite, own: DieCollection, opponent: Optional[DieCollection]):
        if card == Card.MANEUVER_CUTTING_RIPOSTE:
            if opponent is not None:
                ManeuverCuttingRiposte().apply(opponent)

    @staticmethod
    def _apply_card_post_roll(card: CardComposite, own: DieValues, opponent: Optional[DieValues]):
        if card == Card.MANEUVER_IN_HEW_OF:
            ManeuverInHewOf().apply(own)
        elif card == Card.MANEUVER_KEEP_THE_PIERCE:
            if opponent is not None:
                ManeuverKeepThePierce().apply(own, opponent)
        elif card == Card.MANEUVER_PRECISION:
            ManeuverPrecision().apply(own)
        elif card == Card.MANEUVER_NICK_TO_DEATH:
            ManeuverNickToDeath().apply(own)
