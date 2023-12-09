# package src.data
from typing import List, Optional
import random
from src.data.ManeuverPlate import ManeuverPlate
from src.data.Deck import Deck
from src.data.Card import CardComposite, DieSides, Card, CardWound
from src.data.Decision import DecisionLine, DecisionIntention, coin_for


class Player:
    _max_energy = 20
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

    def draw_hand(self) -> List[CardComposite]:
        return self.draw.draw(self._hand_size)

    @property
    def has_face_up_cards(self) -> bool:
        return self.draw.has_face_up_card

    @property
    def has_cards_to_draw(self) -> bool:
        return len(self.draw.draw_deck) > 0

    def play_to_plate(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        card = self.draw.pull_face_up_card()
        if card == Card.NONE:
            return False
        return self.plate.add_card(card, line, coin)

    def is_legal_intention(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        line = self.plate.lines[line.pos]
        count = len(line.cards)
        max_size = line.maxSize
        if count >= max_size:
            return False
        if count > 0 and coin != DecisionIntention.NONE:
            current = line.intention
            if current != coin and current != DecisionIntention.NONE:
                return False
        line.set_intention(coin)
        return True

    def nn_next_cards(self, size: int) -> List[int]:
        return self.draw.nn_next_cards(size)

    @property
    def stash_cards_total(self) -> int:
        return self.stash.cards_total

    def plate_has_intention(self, coin: DecisionIntention) -> bool:
        return self.plate.has_intention(coin)

    def has_revealed_intention(self, coin: DecisionIntention) -> bool:
        return self.plate.has_revealed_intention(coin)

    def line_intention_of(self, line: DecisionLine) -> DecisionIntention:
        return self.plate.line_intention_of(line)

    def nn_line_card_values(self, line: DecisionLine) -> List[int]:
        return self.plate.nn_line_card_values(line)

    # Return list of the number of cards in each line
    @property
    def lines_num_cards(self) -> List[int]:
        return self.plate.lines_num_cards

    def discard_all(self):
        self.draw.extend(self.plate.discard_all())

    def discard_face_up(self):
        self.draw.extend(self.plate.discard_face_up())

    def upgrade_face_up_wounds(self):
        cards = self.plate.wounds_face_up
        for wound in [CardWound.WOUND_ACUTE, CardWound.WOUND_GRAVE, CardWound.WOUND_DIRE]:
            count = cards.count(wound)
            while count > 1:
                self.plate.remove_on_face_up(wound)
                line = self.plate.remove_on_face_up(wound)
                cards.remove(wound)
                if wound == CardWound.WOUND_DIRE:
                    self.fatal_received = True
                else:
                    line.add(wound.upgrade)
                    cards.append(wound.upgrade)
                count -= 2

    def lose_energy_from_face_up_wounds(self):
        cards = self.plate.wounds_face_up
        for card in cards:
            self.energy -= card.energy_penalty

    def upgrade_lowest_face_up_wound(self):
        wounds = self.plate.wounds_face_up
        lowest: Optional[CardWound] = None
        for wound in wounds:
            if lowest is None or lowest.value > wound.value:
                lowest = wound
        if lowest is None:
            self.draw.append(CardWound.WOUND_MINOR)
        else:
            if lowest.upgrade is None:
                self.fatal_received = True
                self.plate.remove_on_face_up(lowest)
            else:
                self.plate.replace_wound_face_up(lowest, lowest.upgrade)

    def upgrade_highest_face_up_wound(self):
        wounds = self.plate.wounds_face_up
        highest: Optional[CardWound] = None
        for wound in wounds:
            if highest is None or highest.value < wound.value:
                highest = wound
        if highest is None:
            self.draw.append(CardWound.WOUND_MINOR)
        else:
            if highest.upgrade is None:
                self.fatal_received = True
                self.plate.remove_on_face_up(highest)
            else:
                self.plate.replace_wound_face_up(highest, highest.upgrade)

    @property
    def all_draw_cards(self) -> List[CardComposite]:
        return self.draw.face_up_deck + self.draw.draw_deck

    @property
    def num_all_draw_cards(self) -> int:
        return len(self.all_draw_cards)

    @property
    def num_cards_stash(self) -> int:
        return self.stash.cards_total

    @property
    def stash_cards_face_up(self) -> List[CardComposite]:
        return self.stash.face_up_deck

    @property
    def stash_cards_draw(self) -> List[CardComposite]:
        return self.stash.draw_deck

    @property
    def energy_loss(self) -> int:
        return self._max_energy - self.energy

    def stash_pull_face_up_card(self) -> CardComposite:
        return self.stash.pull_face_up_card()

    def plate_has_face_up_sides(self, intention: DecisionIntention, card: DieSides) -> bool:
        return self.plate.has_face_up_sides(intention, card)

    def plate_remove_sides(self, intention: DecisionIntention, card: DieSides) -> Optional[Card]:
        return self.plate.remove_sides_on_face_up(intention, card)

    @property
    def compute_draw_wound_penalty_value(self) -> int:
        return self.draw.compute_wound_penalty_value

    def reveal_intentions_if_maxed(self):
        self.plate.reveal_intentions_if_maxed()

    def reveal_all_intentions(self):
        self.plate.reveal_all_intentions()

    def reveal_cards_with_revealed_intentions(self):
        self.plate.reveal_cards_with_revealed_intentions()

    def reveal_intentions_of(self, coin: DecisionIntention):
        self.plate.reveal_intentions_of(coin)

    def collect_dice_for(self, coin: DecisionIntention) -> List[DieSides]:
        return self.plate.collect_dice_for(coin)

    def collect_face_up_cards_for(self, coin: DecisionIntention) -> List[CardComposite]:
        return self.plate.collect_face_up_cards_for(coin)

    @property
    def central_maneuver_card(self) -> Card:
        return self.plate.central_maneuver_card

    def apply_to_die_four(self, coin: DecisionIntention):
        new_cards: [CardComposite] = []
        for line in self.plate.lines:
            if line.intention == coin and line.cards_face_up:
                for card in line.cards:
                    if card == Card.MANEUVER_TO_DIE_FOUR:
                        cards = self.draw.draw()
                        if len(cards) > 0:
                            line.add(cards[0])
                            new_cards.append(cards[0])

        for drawn in new_cards:
            if drawn == Card.MANEUVER_TO_DIE_FOUR:
                self.apply_to_die_four(coin)

    def reduce_reach_on(self, line: DecisionLine):
        self.plate.reduce_reach_on(line)

    def add_random_penalty_coin(self) -> Optional[DecisionIntention]:
        value = self.rand3()
        for i in range(3):
            coin = coin_for(value)
            if self.plate.num_held_intention_coins_for(coin) > 0:
                self.plate.penalty_coins.append(coin)
                return coin
            value += 1
            if value > 3:
                value = 1
        return None

    @staticmethod
    def rand3() -> int:
        return random.randint(1, 3)

    def trash_one_random_face_up_plate_cards(self) -> Optional[Card]:
        face_up_lines = self.plate.face_up_lines
        line_index = random.randint(0, len(face_up_lines) - 1)
        for i in range(len(face_up_lines)):
            line = face_up_lines[line_index]
            num_cards = len(line.cards)
            if num_cards > 0:
                card_index = random.randint(0, num_cards - 1)
                for j in range(num_cards):
                    card = line.cards[card_index]
                    if isinstance(card, Card):
                        line.remove(card)
                        return card
                    card_index = card_index + 1
                    if card_index >= num_cards:
                        card_index = 0
            line_index = line_index + 1
            if line_index >= len(face_up_lines):
                line_index = 0
        return None

    def trash_face_up_card(self, card_to_trash: Card) -> bool:
        face_up_lines = self.plate.face_up_lines
        for line_index in range(len(face_up_lines)):
            line = face_up_lines[line_index]
            for card_index in range(len(line.cards)):
                card = line.cards[card_index]
                if isinstance(card, Card) and card == card_to_trash:
                    line.remove(card)
                    return True
        return False


