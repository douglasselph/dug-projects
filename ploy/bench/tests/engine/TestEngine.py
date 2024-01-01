from __future__ import annotations

import unittest

from src.data.Card import Card
from src.data.Decision import DecisionIntention
from src.data.Decision import DecisionLine
from src.data.Game import Game, PlayerID
from src.decision.Decisions import Decisions
from src.engine.Engine import Engine
from tests.engine.support.TestPlaceCard import TestPlaceCard
from tests.engine.support.TestPlayer import TestPlayer
from tests.engine.support.TestRevealSupportingLine import TestRevealSupportingLine


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
        self.test_reveal_supporting_line = TestRevealSupportingLine()
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
        self.decisions.revealSupportingLine = self.test_reveal_supporting_line
        self.SUT = Engine(self.game, self.decisions)
        self.SUT.draw_hands()
        self.game.initiativeOn = PlayerID.PLAYER_1

    def test_initialization__decisions__all_set_games_were_done(self):
        # Arrange
        # Act
        # Assert
        self.assertEqual(self.game, self.decisions.deployBlock.game)
        self.assertEqual(self.game, self.decisions.deployChooseCard.game)
        self.assertEqual(self.game, self.decisions.placeCard.game)
        self.assertEqual(self.game, self.decisions.revealSupportingLine.game)
        self.assertEqual(self.game, self.decisions.trash.game)

    def test_place_cards__all_face_up_cards_were_placed_when_complete(self):
        # Arrange
        # Act
        self.SUT.place_cards()
        # Assert
        agent_cards = self.game.agentPlayer.draw.face_up_deck
        opponent_cards = self.game.opponent.draw.face_up_deck
        self.assertEqual(0, len(agent_cards))
        self.assertEqual(0, len(opponent_cards))

    def test_place_cards__for_each_card__decision_tree_result_called(self):
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

    def test_reveal_intentions__still_has_cards_to_draw__reveal_intentions_only_if_maxed(self):
        # Arrange
        self.game.agentPlayer = self.test_player
        self.game.opponent = self.test_opponent
        self.SUT.draw_hands()
        # Act
        self.SUT.reveal_intentions()
        # Assert
        self.assertTrue(self.test_player.got_reveal_intentions_if_maxed)
        self.assertTrue(self.test_opponent.got_reveal_intentions_if_maxed)

    def test_reveal_intentions__does_not_still_has_cards_to_draw__reveal_all_intentions(self):
        # Arrange
        self.game.agentPlayer = self.test_player
        self.game.opponent = self.test_opponent
        self.SUT.draw_hands()
        self.SUT.place_cards()
        self.SUT.draw_hands()
        self.SUT.place_cards()
        # Act
        self.SUT.reveal_intentions()
        # Assert
        self.assertFalse(self.test_player.got_reveal_intentions_if_maxed)
        self.assertFalse(self.test_opponent.got_reveal_intentions_if_maxed)
        self.assertTrue(self.test_player.got_reveal_all_intentions)
        self.assertTrue(self.test_opponent.got_reveal_all_intentions)

    def test_resolve_attacks__has_revealed_attack_intention__calls_supporting_line_reveal_decision(self):
        # Arrange
        self.game.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.game.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.game.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.game.agentPlayer.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.game.opponent.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.game.opponent.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.game.opponent.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.game.opponent.play_to_plate(DecisionLine.LINE_2, DecisionIntention.ATTACK)
        self.SUT.reveal_intentions()
        # Act
        self.SUT.resolve_attacks()
        # Assert
        self.assertTrue(self.test_reveal_supporting_line.got_apply)

