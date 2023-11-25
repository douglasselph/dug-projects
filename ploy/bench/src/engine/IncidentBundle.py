from typing import Optional, List
import random
from src.engine.DieCollection import DieCollection
from src.engine.DieValues import DieValues
from src.data.Card import CardComposite
from src.data.Card import DieSides, Card, CardWound
from src.data.maneuver.ManeuverCuttingRiposte import maneuver_cutting_riposte
from src.data.maneuver.ManeuverInHewOf import maneuver_in_hew_of
from src.data.maneuver.ManeuverKeepThePierce import maneuver_keep_the_pierce
from src.data.maneuver.ManeuverNickToDeath import maneuver_nick_to_death
from src.data.maneuver.ManeuverPrecision import maneuver_precision


class IncidentBundle:
    attacker_cards: List[CardComposite]
    defender_cards: List[CardComposite]
    attacker_dice: Optional[DieCollection]
    defender_dice: Optional[DieCollection]
    attacker_values: Optional[DieValues]
    defender_values: Optional[DieValues]

    def __init__(self):
        self.attacker_cards = []
        self.defender_cards = []
        self.attacker_dice = None
        self.defender_dice = None
        self.attacker_values = None
        self.defender_values = None

    def attacker_pull_dice(self):
        self.attacker_dice = DieCollection(self._pull_dice(self.attacker_cards))

    def defender_pull_dice(self):
        self.defender_dice = DieCollection(self._pull_dice(self.defender_cards))

    @staticmethod
    def _pull_dice(cards: List[CardComposite]):
        result: List[DieSides] = []
        for card in cards:
            bonus = card.die_bonus
            if bonus != DieSides.NONE:
                result.append(bonus)
        return result

    def apply_cards_pre_roll(self):
        for card in self.attacker_cards:
            self._apply_card_pre_roll(card, self.defender_dice, self.attacker_dice)
        for card in self.defender_cards:
            self._apply_card_pre_roll(card, self.attacker_dice, self.defender_dice)

    @staticmethod
    def _apply_card_pre_roll(card: CardComposite, own: DieCollection, opponent: DieCollection):
        if card == Card.MANEUVER_CUTTING_RIPOSTE:
            maneuver_cutting_riposte(opponent)
        elif card == Card.MANEUVER_NICK_TO_DEATH:
            maneuver_nick_to_death(own)

    def roll(self):
        self.attacker_values = self.attacker_dice.roll()
        self.defender_values = self.defender_dice.roll()

    def apply_cards_post_roll(self):
        for card in self.attacker_cards:
            self._apply_card_post_roll(card, self.attacker_values, self.defender_values)
        for card in self.defender_cards:
            self._apply_card_post_roll(card, self.defender_values, self.attacker_values)

    @staticmethod
    def _apply_card_post_roll(card: CardComposite, own: DieValues, opponent: DieValues):
        if card == Card.MANEUVER_IN_HEW_OF:
            maneuver_in_hew_of(own)
        elif card == Card.MANEUVER_KEEP_THE_PIERCE:
            maneuver_keep_the_pierce(own, opponent)
        elif card == Card.MANEUVER_PRECISION:
            maneuver_precision(own)

    @property
    def attacker_total(self) -> int:
        return self.attacker_values.total - self.attacker_wounds

    @property
    def defender_total(self) -> int:
        return self.defender_values.total - self.defender_wounds

    @property
    def attacker_wounds(self) -> int:
        wound = 0
        for card in self.attacker_cards:
            if isinstance(card, CardWound):
                wound += card.pip_penalty
        return wound

    @property
    def defender_wounds(self) -> int:
        wound = 0
        for card in self.defender_cards:
            if isinstance(card, CardWound):
                wound += card.pip_penalty
        return wound


