# package src.data
from typing import List
from src.data.ManeuverPlate import ManeuverPlate
from src.data.Deck import Deck
from src.data.Card import CardComposite
from src.data.Decision import DecisionLine, DecisionIntention


class Player:

    _max_energy = 20
    _loss_energy = 6  # If energy reaches this level this is too close to losing.
    _hand_size = 4

    plate: ManeuverPlate
    energy: int
    pips: int
    stash: Deck
    draw: Deck
    fatal_received: bool

    def __init__(self):
        self.plate = ManeuverPlate()
        self.energy = self._max_energy
        self.pips = 0
        self.draw = Deck()
        self.stash = Deck()
        self.fatal_received = False

    def append_to_draw(self, card: CardComposite):
        self.draw.append(card)

    def extend_to_draw(self, cards: List[CardComposite]):
        self.draw.extend(cards)

    def append_to_stash(self, card: CardComposite):
        self.stash.append(card)

    def extend_to_stash(self, cards: List[CardComposite]):
        self.stash.extend(cards)

    def draw_hand(self):
        self.draw.draw(self._hand_size)

    @property
    def has_cards_to_play(self) -> bool:
        return len(self.draw.faceUp_deck) > 0

    @property
    def has_cards_to_draw(self) -> bool:
        return len(self.draw.draw_deck) > 0

    def play_to_plate(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        card = self.draw.pull_face_up_card()
        return self.plate.add_card(card, line, coin)

    def is_legal(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        line = self.plate.lines[line.value]
        count = len(line.cards)
        max_size = line.maxSize
        if count >= max_size:
            return False
        if count > 0 and coin != DecisionIntention.NONE:
            current = line.intention
            if current != coin and current != DecisionIntention.NONE:
                return False
        line.set_intention(coin)

    def nn_next_cards(self, size: int) -> List[int]:
        return self.draw.nn_next_cards(size)

    @property
    def stash_cards_total(self) -> int:
        return self.stash.cards_total

    def has_intention(self, coin: DecisionIntention) -> bool:
        return self.plate.has_intention(coin)

    @property
    def line_sizes(self):
        return self.plate.line_sizes

    def line_intention_id(self, position: int) -> DecisionIntention:
        return self.plate.line_intention_id(position)

    def line_card_values(self, position: int) -> List[int]:
        return self.plate.line_card_values(position)

    @property
    def lines_num_cards(self) -> List[int]:
        return self.plate.lines_num_cards

    def discard_all(self):
        cards = self.plate.discard_all()
        self.draw.extend(cards)

    @property
    def nn_wound_value(self) -> int:
        return self.draw.nn_wound_value

    @property
    def nn_energy_loss(self) -> int:
        if self.energy < 0:
            return self._loss_energy
        if self.energy >= self._loss_energy:
            return 0
        return self._loss_energy - self.energy

    def reveal_intentions_if_maxed(self):
        self.plate.reveal_intentions_if_maxed()

    def reveal_all_intentions(self):
        self.plate.reveal_intention_on_all_lines()

    def reveal_cards_with_revealed_intentions(self):
        self.plate.reveal_cards_with_revealed_intentions()
