# package src.data
from typing import List, Optional
import random
from src.data.ManeuverPlate import ManeuverPlate
from src.data.Deck import Deck
from src.data.Card import CardComposite, DieSides, Card, CardWound
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
        self.draw_one_less_card = False

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
        self._lose_energy_from_wounds(cards)

    def discard(self):
        cards = self.plate.discard()
        self.draw.extend(cards)
        self._lose_energy_from_wounds(cards)

    def upgrade_lowest_wound(self):
        wounds = self.plate.wounds
        lowest: Optional[CardWound] = None
        for wound in wounds:
            if lowest is None or lowest.value > wound.value:
                lowest = wound
        if lowest is None:
            self.draw.append(CardWound.WOUND_MINOR)
        else:
            if lowest.upgrade is None:
                self.fatal_received = True
                self.plate.remove(lowest)
            else:
                self.plate.replace_wound(lowest, lowest.upgrade)

    def upgrade_highest_wound(self):
        wounds = self.plate.wounds
        highest: Optional[CardWound] = None
        for wound in wounds:
            if highest is None or highest.value < wound.value:
                highest = wound
        if highest is None:
            self.draw.append(CardWound.WOUND_MINOR)
        else:
            if highest.upgrade is None:
                self.fatal_received = True
                self.plate.remove(highest)
            else:
                self.plate.replace_wound(highest, highest.upgrade)

    @property
    def draw_cards(self) -> List[CardComposite]:
        return self.draw.faceUp_deck + self.draw.draw_deck

    @property
    def num_draw_cards(self) -> int:
        return len(self.draw_cards)

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

    def reveal_intentions_with_intention(self, coin: DecisionIntention):
        self.plate.reveal_intentions_of(coin)

    def collect_dice_for(self, coin: DecisionIntention) -> List[DieSides]:
        return self.plate.collect_dice_for(coin)

    def collect_face_up_cards_for(self, coin: DecisionIntention) -> List[CardComposite]:
        return self.plate.collect_face_up_cards_for(coin)

    @property
    def central_maneuver_card(self) -> Card:
        return self.plate.central_maneuver_card

    def apply_feeling_feint(self, coin: DecisionIntention):
        self.plate.apply_feeling_feint(coin)

    def apply_to_die_four(self, coin: DecisionIntention):
        for line in self.plate.lines:
            if line.intention == coin and line.intention_face_up:
                for card in line.cards:
                    if card == Card.MANEUVER_TO_DIE_FOUR:
                        card = self.draw.draw()
                        if card != Card.NONE:
                            line.add(card)

    def _lose_energy_from_wounds(self, cards: [CardComposite]):
        for card in cards:
            if isinstance(card, CardWound):
                self.energy -= card.energy_penalty

    def reduce_reach(self):
        self.plate.reduce_reach()

    def add_penalty_coin(self):
        value = random.randint(1, 3)
        for i in range(3):
            coin = self.coin_for(value)
            if self.plate.num_intention_coins_for(coin) > 0:
                self.plate.penalty_coins.append(coin)
                return
            value = value + 1
            if value >= 4:
                value = 1

    @staticmethod
    def coin_for(value) -> Optional[DecisionIntention]:
        for coin in DecisionIntention:
            if coin.value == value:
                return coin
        return None

    def trash_random_card(self) -> Optional[Card]:
        face_up_lines = self.plate.face_up_lines
        line_index = random.randint(0, len(face_up_lines)-1)
        for i in range(len(face_up_lines)):
            line = face_up_lines[line_index]
            card_index = random.randint(0, len(line.cards)-1)
            for j in range(len(line.cards)):
                card = line.cards[card_index]
                if isinstance(card, Card):
                    line.remove(card)
                    return card
                card_index = card_index + 1
                if card_index >= len(line.cards):
                    card_index = 0
            line_index = line_index + 1
            if line_index >= len(face_up_lines):
                line_index = 0
        return None

    def trash(self, card_to_trash: Card) -> bool:
        face_up_lines = self.plate.face_up_lines
        for line_index in range(len(face_up_lines)):
            line = face_up_lines[line_index]
            for card_index in range(len(line.cards)):
                card = line.cards[card_index]
                if isinstance(card, Card) and card == card_to_trash:
                    line.remove(card)
                    return True
        return False


