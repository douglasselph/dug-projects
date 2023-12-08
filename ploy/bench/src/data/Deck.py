# package src.data
from __future__ import annotations
from typing import List, Optional
from src.data.Card import CardComposite, card_ordinal, card_wound_penalty_value, Card


class Deck:

    _draw: List[CardComposite]
    _faceUp: List[CardComposite]

    def __init__(self):
        self._draw = []
        self._faceUp = []

    # Add a card to the bottom of the draw deck.
    def append(self, value: CardComposite) -> Deck:
        self._draw.append(value)
        return self

    def extend(self, value: List[CardComposite]):
        self._draw.extend(value)

    # Draw the top most draw from the draw deck (element 0)
    # as the new first most face up card (element 0)
    def draw(self, count: int = 1) -> List[CardComposite]:
        cards = []
        for i in range(count):
            if len(self._draw) > 0:
                card = self._draw.pop(0)
                self._faceUp.insert(0, card)
                cards.append(card)
        return cards

    @property
    def can_draw(self) -> bool:
        return len(self._draw) > 0

    # Pull the card that is top most on the deck
    def query_face_up_card(self) -> Optional[CardComposite]:
        if len(self._faceUp) <= 0:
            return None
        return self._faceUp[0]

    def pull_face_up_card(self) -> Optional[CardComposite]:
        if len(self._faceUp) <= 0:
            return None
        return self._faceUp.pop(0)

    @property
    def has_face_up_card(self) -> bool:
        return self.query_face_up_card() is not None

    @property
    def cards_total(self) -> int:
        return len(self._faceUp) + len(self._draw)

    # First card in list is the next card to draw.
    @property
    def draw_deck(self) -> List[CardComposite]:
        return self._draw

    # First card in list is the most face up card.
    @property
    def face_up_deck(self) -> List[CardComposite]:
        return self._faceUp

    #
    # Return an array of the next cards in the deck, starting with the face up cards and proceeding to the
    # drawn cards. Ensure the array returned is of size 'size'. Do not report more than this number of cards.
    # If less than this number of maneuver available, then fill in with zeros.
    #
    def nn_next_cards(self, size: int) -> List[int]:
        face_up_array = [card_ordinal(card) for card in self._faceUp[:size]]
        if len(face_up_array) < size:
            remainder = size - len(face_up_array)
            face_down_array = [card_ordinal(card) for card in self._draw[:remainder]]
            combined = face_up_array + face_down_array
            if len(combined) < size:
                combined += [0] * (size - len(combined))
        else:
            combined = face_up_array
        return combined

    #
    # Return a neural net conditioned array of the face up cards, and only of the face up cards.
    # The first card return is the top most face up card.
    # Must return exactly size elements. If there are more available, then they will be clipped.
    # If there are less, then 0 will be appended.
    # The ordinal values of the cards are returned (see card_ordinal)
    #
    def nn_face_up_cards(self, size: int) -> List[int]:
        face_up_array = [card_ordinal(card) for card in self._faceUp[:size]]
        if len(face_up_array) < size:
            face_up_array += [0] * (size - len(face_up_array))
        return face_up_array

    @property
    def compute_wound_penalty_value(self) -> int:
        cards = self._faceUp + self._draw
        wounds = 0
        for card in cards:
            wounds += card_wound_penalty_value(card)
        return wounds

