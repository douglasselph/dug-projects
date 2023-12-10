from __future__ import annotations

import unittest
from unittest.mock import Mock

from src.data.Game import Game
from src.engine.Engine import Engine
from src.decision.Decisions import Decisions
from src.data.Decision import DecisionLine
from src.data.Decision import DecisionIntention
from src.data.Player import Player
from tests.engine.support.TestPlaceCard import TestPlaceCard
from tests.engine.support.TestPlayer import TestPlayer
from src.data.Card import Card


class TestEngine(unittest.TestCase):
    sample_cards = [
        Card.D4_D10_SLASH_AND_BURN,
        Card.D6_SLIT_TIGHT,
        Card.D8_UNDERCOVER_CHOP,
        Card.D10_INNER_PIERCE,
        Card.D12_PROFESSIONAL_STABOTAGE,
        Card.D20_CUTASTROPHE,
        Card.MANEUVER_TO_DIE_FOUR,
        Card.MANEUVER_PRECISION
    ]
    sample_lines = [
        DecisionLine.LINE_1,
        DecisionLine.LINE_1,
        DecisionLine.LINE_2,
        DecisionLine.LINE_2,
        DecisionLine.LINE_3,
        DecisionLine.LINE_3,
        DecisionLine.LINE_4,
        DecisionLine.LINE_4
    ]
    sample_coins = [
        DecisionIntention.ATTACK,
        DecisionIntention.ATTACK,
        DecisionIntention.DEFEND,
        DecisionIntention.DEFEND,
        DecisionIntention.DEPLOY,
        DecisionIntention.DEPLOY,
        DecisionIntention.ATTACK,
        DecisionIntention.ATTACK
    ]

    def setUp(self):
        self.game = Game()
        self.game.agentPlayer.extend_to_draw(self.sample_cards)
        self.game.opponent.extend_to_draw(self.sample_cards)
        self.test_place_card = TestPlaceCard()
        self.test_player = TestPlayer()
        self.test_opponent = TestPlayer()
        self.test_player.extend_to_draw(self.sample_cards)
        self.test_opponent.extend_to_draw(self.sample_cards)
        self.test_place_card.agent_result_line.extend(self.sample_lines)
        self.test_place_card.agent_result_coin.extend(self.sample_coins)
        self.test_place_card.opponent_result_line.extend(self.sample_lines)
        self.test_place_card.opponent_result_coin.extend(self.sample_coins)
        self.decisions = Decisions()
        self.decisions.placeCard = self.test_place_card
        self.SUT = Engine(self.game, self.decisions)
        self.SUT.draw_hands()

    def test_initialization__decisions__all_set_games_were_done(self):
        # Arrange
        # Act
        # Assert
        self.assertEqual(self.game, self.decisions.placeCard.game)

    def test_draw_hands__verify_all_players_draw_hand_call_is_made(self):
        # Arrange
        player_mock = Mock(spec=Player)
        opponent_mock = Mock(spec=Player)
        self.SUT.game.agentPlayer = player_mock
        self.SUT.game.opponent = opponent_mock
        # Act
        # Assert
        player_mock.draw_hand.assert_called()
        opponent_mock.draw_hand.assert_called()

    def test_place_cards__all_face_up_cards_were_placed_when_complete(self):
        # Arrange
        # Act
        self.SUT.place_cards()
        # Assert
        agent_cards = self.game.agentPlayer.draw.face_up_deck
        opponent_cards = self.game.opponent.draw.face_up_deck
        self.assertEqual(0, len(agent_cards))
        self.assertEqual(0, len(opponent_cards))

    def test_place_cards__for_each_card__decision_tree_called_with_card(self):
        # Arrange
        # Act
        self.SUT.place_cards()
        # Assert
        self.assertEqual(4, len(self.test_place_card.agent_got_line))
        self.assertEqual(4, len(self.test_place_card.agent_got_coin))
        self.assertEqual(4, len(self.test_place_card.opponent_got_line))
        self.assertEqual(4, len(self.test_place_card.opponent_got_coin))
        self.assertEqual(self.sample_lines[0:4], self.test_place_card.agent_got_line)
        self.assertEqual(self.sample_coins[0:4], self.test_place_card.agent_got_coin)
        self.assertEqual(self.sample_lines[0:4], self.test_place_card.opponent_got_line)
        self.assertEqual(self.sample_coins[0:4], self.test_place_card.opponent_got_coin)

    def test_place_cards__for_each_card__decision__legal__returned_value_applied_to_plate(self):
        # Arrange
        # Act
        self.SUT.place_cards()
        # Assert
        self.assertEqual(2, len(self.game.agentPlayer.plate.lines[DecisionLine.LINE_1.pos].cards))
        self.assertEqual(2, len(self.game.agentPlayer.plate.lines[DecisionLine.LINE_2.pos].cards))
        self.assertEqual(0, len(self.game.agentPlayer.plate.lines[DecisionLine.LINE_3.pos].cards))
        self.assertEqual(0, len(self.game.agentPlayer.plate.lines[DecisionLine.LINE_4.pos].cards))

    def test_place_cards__for_each_card__decision__illegal__decision_tree_informed_of_error(self):
        # Arrange
        pattern = [True, False, True, False, True, False, True]
        self.game.agentPlayer = self.test_player
        self.game.opponent = self.test_opponent
        self.test_player.result_is_legal = pattern
        self.test_opponent.result_is_legal = pattern
        self.SUT.draw_hands()
        # Act
        self.SUT.place_cards()
        # Assert
        self.assertEqual(pattern, self.test_place_card.agent_got_legal)
        self.assertEqual(pattern, self.test_place_card.opponent_got_legal)
