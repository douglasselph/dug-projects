from typing import Optional, List

from src.data.Card import CardComposite
from src.data.Card import DieSides, Card, CardWound
from src.data.maneuver.ManeuverCuttingRiposte import maneuver_cutting_riposte
from src.data.maneuver.ManeuverInHewOf import maneuver_in_hew_of
from src.data.maneuver.ManeuverKeepThePierce import maneuver_keep_the_pierce
from src.data.maneuver.ManeuverNickToDeath import maneuver_nick_to_death
from src.data.maneuver.ManeuverPrecision import maneuver_precision
from src.engine.DieCollection import DieCollection
from src.engine.DieValues import DieValues


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
                maneuver_cutting_riposte(opponent)
        elif card == Card.MANEUVER_NICK_TO_DEATH:
            maneuver_nick_to_death(own)

    @staticmethod
    def _apply_card_post_roll(card: CardComposite, own: DieValues, opponent: Optional[DieValues]):
        if card == Card.MANEUVER_IN_HEW_OF:
            maneuver_in_hew_of(own)
        elif card == Card.MANEUVER_KEEP_THE_PIERCE:
            if opponent is not None:
                maneuver_keep_the_pierce(own, opponent)
        elif card == Card.MANEUVER_PRECISION:
            maneuver_precision(own)
