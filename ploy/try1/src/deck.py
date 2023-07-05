import random
from typing import List

from src.card import Card
from src.hand import Hand


class Deck:

    cards: List[Card]

    _remaining: List[Card]
    _next_turn_add: List[Card]
    _next_turn_replace: List[Card]

    def __init__(self):
        self.cards = []
        self._remaining = []
        self._next_turn_add = []
        self._next_turn_replace = []

    def add(self, card: Card):
        self.cards.append(card)

    @property
    def has_cards(self) -> bool:
        return len(self._remaining) > 0

    def next_turn_add(self, stack: List[Card]):
        self._next_turn_add += stack

    def next_turn_replace(self, stack: List[Card]):
        self._next_turn_replace += stack

    def deal(self) -> Hand:
        use: List[Card] = []
        draw_size = 5
        # ADD
        use += self._next_turn_add
        self._next_turn_add = []
        # REPLACE
        if len(self._next_turn_replace) > 0:
            draw_size -= len(self._next_turn_replace)
            use += self._next_turn_replace
        # Detect shuffle
        if len(self._remaining) == 0:
            self.shuffle()
        # Draw
        if len(self._remaining) >= draw_size:
            use += self._remaining[:draw_size]
            self._remaining = self._remaining[draw_size:]
        else:
            use += self._remaining
            draw_size -= len(self._remaining)
            self._remaining = []
            if draw_size > 0:
                self.shuffle()
                use += self._remaining[:draw_size]
                self._remaining = self._remaining[draw_size:]
        return Hand(use)

    def shuffle(self):
        random.shuffle(self.cards)
        self._remaining = self.cards.copy()




