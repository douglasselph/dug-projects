import random
from typing import List, Optional

from src.data.Card import Card, CardComposite
from src.decision.base.BaseTrash import BaseTrash


class ValidateTrash(BaseTrash):

    def select_card_to_trash(self, cards: List[CardComposite]) -> Optional[Card]:
        if not self._should_trash(cards):
            return None
        return self._select_card_to_trash(cards)

    def _should_trash(self, cards: List[CardComposite]) -> bool:
        current_average = self._average_of(cards)
        if current_average < 30 or current_average > 52:
            return False
        if self.player.num_cards_draw < 14:
            return False
        chance = (current_average - 30) / 5
        roll = random.randint(1, 6)
        return roll <= chance

    @staticmethod
    def _average_of(cards: [CardComposite]) -> float:
        result = 0
        for card in cards:
            if isinstance(card, Card):
                result += card.die_bonus.average()
        return result

    @staticmethod
    def _select_card_to_trash(cards: List[CardComposite]) -> Optional[Card]:
        selected_card: Optional[Card] = None
        selected_card_value = 0
        for card in cards:
            if isinstance(card, Card):
                value = card.trash_choice_value
                if value > selected_card_value:
                    selected_card = card
                    selected_card_value = value
        return selected_card
