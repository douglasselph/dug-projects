from __future__ import annotations

from typing import Optional

from src.data.Card import CardComposite, CardCost
from src.data.Decision import DecisionDeck
from src.data.Player import Player
from src.decision.base.BaseDeployChooseCard import BaseDeployChooseCard


class ValidateDeployChooseCard(BaseDeployChooseCard):

    def card_to_acquire(self, player: Player, opponent: Player) -> DecisionDeck:
        stash_card_face_up: Optional[CardComposite] = None
        stash_has_draw = False
        stash_can_afford = False
        stash_card_value = 0
        draw_from_personal_stash = False

        common_draw_deck = self.game.commonDrawDeck
        common_has_draw = False
        common_can_afford = False
        common_card_value = 0
        draw_from_common_draw_deck = False

        opponent_can_afford = False
        opponent_card_value = 0
        draw_from_opponent_stash = False

        if player.num_cards_stash > 0:
            cards = player.stash_cards_face_up
            if len(cards) > 0:
                stash_card_face_up = cards[0]
                stash_can_afford = self._can_afford(stash_card_face_up.cost, player)
                stash_card_value = stash_card_face_up.ff_value

            stash_has_draw = len(player.stash_cards_draw) > 0

        common_cards = common_draw_deck
        if common_cards.cards_total > 0:
            cards = common_cards.face_up_deck
            if len(cards) > 0:
                common_card_face_up = cards[0]
                common_can_afford = self._can_afford(stash_card_face_up.cost, player)
                common_card_value = common_card_face_up.ff_value

            common_has_draw = len(common_cards.draw_deck) > 0

        if opponent.num_cards_stash > 0:
            cards = opponent.stash_cards_face_up
            if len(cards) > 0:
                opponent_card_face_up = cards[0]
                opponent_can_afford = self._can_afford(opponent_card_face_up.cost, player)
                opponent_card_value = opponent_card_face_up.ff_value

        if opponent_card_value > 0 and opponent_can_afford:
            if opponent_card_value > stash_card_value and opponent_card_value > common_card_value:
                draw_from_opponent_stash = True

        if stash_card_value > 0 and stash_can_afford:
            if stash_card_value > common_card_value:
                draw_from_personal_stash = True

        if common_card_value > 0 and common_can_afford:
            draw_from_common_draw_deck = True

        decision: DecisionDeck = DecisionDeck.NONE

        if draw_from_opponent_stash:
           decision = DecisionDeck.OPPONENT_STASH_FACE_UP
        elif draw_from_personal_stash:
            decision = DecisionDeck.PERSONAL_STASH_FACE_UP
        elif draw_from_common_draw_deck:
            decision = DecisionDeck.COMMON_FACE_UP
        elif player.pips > 0:
            if stash_has_draw:
                decision = DecisionDeck.PERSONAL_STASH_DRAW
            elif common_has_draw:
                decision = DecisionDeck.COMMON_DRAW
        return decision

    @staticmethod
    def _can_afford(card: CardCost, player: Player) -> bool:
        if player.pips < card.pips:
            return False
        if not player.plate_has_face_up_sides(card.sides):
            return False
        if player.energy <= 5:
            return False
        return True
