# package: src
from __future__ import annotations
from enum import Enum, auto

import numpy as np
from keras.layers import Input, Embedding, Flatten, Dense, concatenate
from keras.models import Model

from src.data.Game import Game
from src.data.Card import Card
from src.data.ManeuverPlate import ManeuverPlate, IntentionID

_num_unique_cards = len(Card)
_embedding_size = 10


class OutputLine(Enum):
    LINE_1 = 1
    LINE_2 = 2
    LINE_3 = 3
    LINE_4 = 4


class OutputIntention(Enum):
    NONE = 0
    ATTACK = 1
    DEFEND = 2
    DEPLOY = 3


#
# Line: IntentionID, CardID, CardID, CardID...
#
class _AgentLine:

    def __init__(self, max_cards: int):
        self.input_intention_coin = Input(shape=(1,), name=f"{max_cards}: line intention coin")
        self.input_line_cards = Input(shape=(max_cards,), name=f"{max_cards}: line cards")

        embedding_line_cards = \
            Embedding(input_dim=_num_unique_cards, output_dim=_embedding_size)(self.input_line_cards)
        layer_line_cards = Flatten()(embedding_line_cards)

        layer_intention_coin = Dense(8, activation='relu')(self.input_intention_coin)

        combined = concatenate([layer_intention_coin, layer_line_cards])
        self.layer = Dense(12, activation='relu')(combined)


#
# Handle the neural net for the placement of a card on a line.
# Will output what line to place the next card on.
# If that line was empty, then it will also indicate the intention ID.
# Thus the output options are:
#   - Lines count= 4
#   - IntentionID count= 4 (includes no intention ID).
#
class PlaceCardModel:

    _line_card_sizes = ManeuverPlate.line_card_sizes
    _place_cards_look_ahead_distance = 8
    _common_face_up_card_look_back_distance = 8
    _num_output_lines = 4
    _num_output_intentions = 4
    _output_size = _num_output_lines * _num_output_intentions
    _model: Model

    def __init__(self):
        #
        # next_cardIDs_to_place (_place_cards_look_ahead_distance)
        #
        input_next_cards_to_place = Input(shape=(self._place_cards_look_ahead_distance,), name="next card to place")
        embedding_next_cards_to_place = \
            Embedding(input_dim=_num_unique_cards, output_dim=_embedding_size)(input_next_cards_to_place)
        layer_next_cards_to_place = Flatten()(embedding_next_cards_to_place)
        #
        # Agent: Energy, PIPS, Personal Stash Deck Size
        #
        input_agent_energy = Input(shape=(1,), name='agent energy')
        input_agent_pips = Input(shape=(1,), name='agent pips')
        input_personal_stash_remaining = Input(shape=(1,), name="personal stash cards remaining")

        layer_agent_energy = Dense(10, activation='relu')(input_agent_energy)
        layer_agent_pips = Dense(10, activation='relu')(input_agent_pips)
        layer_personal_stash_remaining = Dense(10, input_personal_stash_remaining)

        combined_primary = concatenate([
            layer_next_cards_to_place,
            layer_agent_energy,
            layer_agent_pips,
            layer_personal_stash_remaining
        ])
        layer_primary = Dense(20, activation='relu')(combined_primary)
        #
        # Agent ManeuverPlate of 4 lines
        #
        agent_line_1 = _AgentLine(self._line_card_sizes[0])
        agent_line_2 = _AgentLine(self._line_card_sizes[1])
        agent_line_3 = _AgentLine(self._line_card_sizes[2])
        agent_line_4 = _AgentLine(self._line_card_sizes[3])

        combined_plate = concatenate([
            agent_line_1.layer,
            agent_line_2.layer,
            agent_line_3.layer,
            agent_line_4.layer
        ])
        layer_plate = Dense(15, activation='relu')(combined_plate)
        #
        # Opponent observation:
        # Energy, PIPS, Line numbers cards (for Line5, Line4, Line3, Line3)
        #
        input_opponent_energy = Input(shape=(1,), name='opponent energy')
        input_opponent_pips = Input(shape=(1,), name='opponent pips')
        input_opponent_line_num_cards = Input(shape=(4,), name="opponent numbers of cards in line")

        layer_opponent_energy = Dense(10, activation='relu')(input_opponent_energy)
        layer_opponent_pips = Dense(10, activation='relu')(input_opponent_pips)
        layer_opponent_line_num_cards = Dense(10, activation='relu')(input_opponent_line_num_cards)

        combined_opponent = concatenate([
            layer_opponent_energy,
            layer_opponent_pips,
            layer_opponent_line_num_cards
        ])
        layer_opponent = Dense(15, activation='relu')(combined_opponent)
        #
        # Common deck observation:
        # First N cards face up, with top most card first for _common_face_up_card_look_back_distance
        #
        input_common_face_up = Input(shape=(self._common_face_up_card_look_back_distance,),
                                     name="face up cards on common deck")
        embedding_common_face_up = \
            Embedding(input_dim=_num_unique_cards, output_dim=_embedding_size)(input_common_face_up)
        layer_common_face_up = Flatten()(embedding_common_face_up)
        #
        # Blend all top levels into one.
        #
        combine_all = concatenate([
            layer_primary,
            layer_plate,
            layer_opponent,
            layer_common_face_up
        ])
        layer_all = Dense(40, activation='relu')(combine_all)
        layer_all2 = Dense(40, activation='relu')(layer_all)

        output_layer = Dense(16, activation='softmax')(layer_all2)

        all_inputs = [
            input_next_cards_to_place,
            input_agent_energy,
            input_agent_pips,
            input_personal_stash_remaining,
            agent_line_1.input_intention_coin,
            agent_line_1.input_line_cards,
            agent_line_2.input_intention_coin,
            agent_line_2.input_line_cards,
            agent_line_3.input_intention_coin,
            agent_line_3.input_line_cards,
            agent_line_4.input_intention_coin,
            agent_line_4.input_line_cards,
            input_opponent_energy,
            input_opponent_pips,
            input_opponent_line_num_cards,
            input_common_face_up
        ]
        self._model = Model(inputs=all_inputs, outputs=output_layer)

    def predict(self, game: Game) -> (OutputLine, OutputIntention):
        #
        # next_cardIDs_to_place (_place_cards_look_ahead_distance)
        #
        data_next_cards_to_place = np.array(game.nn_next_cards(self._place_cards_look_ahead_distance))
        data_agent_energy = np.array([game.agent_energy], dtype=float).reshape((1,))
        data_agent_pips = np.array([game.agent_pips], dtype=float).reshape((1,))
        data_personal_stash_remaining = np.array([game.agent_stash_cards_total], dtype=float).reshape((1,))
        data_agent_lines = self._gather_lines(game.agentPlayer)
        data_opponent_energy = np.array([game.opponent_energy], dtype=float).reshape((1,))
        data_opponent_pips = np.array([game.opponent_pips], dtype=float).reshape((1,))
        data_opponent_line_num_cards = np.array([game.opponent_lines_num_cards], dtype=float).reshape((4,))
        data_common_face_up = \
            np.array(game.nn_common_draw_deck_face_up_cards(self._common_face_up_card_look_back_distance))

        input_data = [
            data_next_cards_to_place,
            data_agent_energy,
            data_agent_pips,
            data_personal_stash_remaining
        ] + data_agent_lines + [
            data_opponent_energy,
            data_opponent_pips,
            data_opponent_line_num_cards,
            data_common_face_up
        ]
        predicted_output = self._model.predict(input_data)
        return self.decode_prediction(predicted_output)

    def _gather_lines(self, game: Game) -> [np.ndarray]:
        data_agent_lines = []
        for i in range(len(self._line_card_sizes)):
            intention_coin_data = np.array([game.agent_line_intention_id(i).value], dtype=float).reshape((1,))
            line_cards_data = np.array(game.agent_line_card_values(i), dtype=int).reshape((self._line_card_sizes[i],))
            data_agent_lines.append(intention_coin_data)
            data_agent_lines.append(line_cards_data)
        return data_agent_lines

    @staticmethod
    def one_hot_encode(line: OutputLine, intention: OutputIntention) -> np.ndarray:
        # Calculate the index
        index = (line.value - 1) * 4 + intention.value
        # Create a one-hot encoded vector
        one_hot = np.zeros(16)
        one_hot[index] = 1
        return one_hot

    @staticmethod
    def decode_prediction(prediction: float) -> (OutputLine, OutputIntention):
        # Find the index of the highest probability
        index = np.argmax(prediction)

        # Determine line and intention based on the index
        line_index = index // 4  # Integer division to find the line
        intention_index = index % 4  # Modulo operation to find the intention

        line = OutputLine(line_index + 1)  # Adding 1 because enum starts at 1
        intention = OutputIntention(intention_index)

        return line, intention
