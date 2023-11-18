# package: src
from enum import Enum, auto

import numpy as np
from keras.layers import Input, Embedding, Flatten, Dense, concatenate
from keras.models import Model

from src.data.Game import Game
from src.data.Card import Card


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
# Handle the neural net for the placement of a card on a line.
# Will output what line to place the next card on.
# If that line was empty, then it will also indicate the intention ID.
# Thus the output options are:
#   - Lines count= 4
#   - IntentionID count= 4 (includes no intention ID).
#
class InputModelPlaceCard:
    _num_unique_cards = len(Card)
    _place_cards_look_ahead_distance = 8
    _common_face_up_card_look_back_distance = 8
    _embedding_size = 10
    _num_output_lines = 4
    _num_output_intentions = 4
    _output_size = _num_output_lines * _num_output_intentions

    _model: Model

    def define_model(self):
        #
        # next_cardIDs_to_place (_place_cards_look_ahead_distance)
        #
        input_next_cards_to_place = Input(shape=(self._place_cards_look_ahead_distance,), name="next card to place")
        embedding_next_cards_to_place = \
            Embedding(input_dim=self._num_unique_cards, output_dim=self._embedding_size)(input_next_cards_to_place)
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
        layer_agent_line_5 = self._build_agent_line(5)
        layer_agent_line_4 = self._build_agent_line(4)
        layer_agent_line_3a = self._build_agent_line(3)
        layer_agent_line_3b = self._build_agent_line(3)

        combined_plate = concatenate([
            layer_agent_line_5,
            layer_agent_line_4,
            layer_agent_line_3a,
            layer_agent_line_3b
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
            Embedding(input_dim=self._num_unique_cards, output_dim=self._embedding_size)(input_common_face_up)
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
        self._model = Model(inputs=layer_all2, outputs=output_layer)

    #
    # Line: IntentionID, CardID, CardID, CardID...
    #
    def _build_agent_line(self, max_cards: int) -> Dense:
        input_intention_coin = Input(shape=(1,), name=f"{max_cards}: line intention coin")
        input_line_cards = Input(shape=(max_cards,), name=f"{max_cards}: line cards")

        embedding_line_cards = \
            Embedding(input_dim=self._num_unique_cards, output_dim=self._embedding_size)(input_line_cards)
        layer_line_cards = Flatten()(embedding_line_cards)

        layer_intention_coin = Dense(8, activation='relu')(input_intention_coin)

        combined = concatenate([layer_intention_coin, layer_line_cards])
        return Dense(12, activation='relu')(combined)

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

    def predict(self, game: Game) -> (OutputLine, OutputIntention):
        #
        # next_cardIDs_to_place (_place_cards_look_ahead_distance)
        #
        data_next_cards_to_place = np.array(game.nn_next_cards(self._place_cards_look_ahead_distance))
        data_agent_energy = np.array([game.agent_energy()], dtype=float).reshape((1,))
        data_agent_pips = np.array([game.agent_pips()], dtype=float).reshape((1,))
        data_personal_stash_remaining = np.array([game.agent_stash_cards_total()], dtype=float).reshape((1,))
        data_agent_lines = []
        
        data_opponent_energy = np.random.rand(1)
        data_opponent_pips = np.random.rand(1)
        data_opponent_line_num_cards = np.random.randint(0, 10, size=(4,))
        data_common_face_up = np.random.randint(0, _num_unique_cards, size=(_common_face_up_card_look_back_distance,))

        #
        # Agent: Energy, PIPS, Personal Stash Deck Size
        #
        #
        # Agent ManeuverPlate of 4 lines.
        # Each Line: IntentionID, CardID, CardID, CardID...
        #
        #
        # Opponent observation:
        # Energy, PIPS, Line numbers cards (for Line5, Line4, Line3, Line3)
        #
        #
        # Common deck observation:
        # First N cards face up, with top most card first for _common_face_up_card_look_back_distance
        #
        predicted_output = self._model.predict(input_data)
        return self.decode_prediction(predicted_output)

