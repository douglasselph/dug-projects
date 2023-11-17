from src.data.Game import Game
from src.data.Card import Card
import numpy as np
from keras.layers import Input, Embedding, Flatten, Dense, concatenate
from keras.models import Model


class InputModel:

    num_unique_cards = len(Card)
    embedding_size = 10
    draw_deck_max_size = 50
    personal_stash_max_size = 20
    common_draw_deck_max_size = 50

    def __init__(self, game: Game):
        self.game = game

    def define_model(self):
        #
        # next_cardID_to_place, energy, pips, central_maneuver_card, level
        #
        input_next_card_to_place = Input(shape=(1,), name="next card to place")
        embedding_next_card_to_place = \
            Embedding(input_dim=self.num_unique_cards, output_dim=self.embedding_size)(input_next_card_to_place)
        layer_next_card_to_place = Flatten()(embedding_next_card_to_place)

        layer_agent_player = self._build_player()
        layer_opponent = self._build_player()



        combined_general_agent_info_layer = Dense(128, activation='relu')(combined_general_agent_info)

        list = []
        for line_values in self.game.agentPlayer.plate.agent_line_values():
            list.extend(line_values)

        list.extend(self.game.agentPlayer.draw.nn_value_all_visible(self.draw_deck_max_size))
        list.extend(self.game.agentPlayer.stash.nn_value_all_visible(self.personal_stash_max_size))

        # Importance of Position: Neural networks, especially  those without recurrent or convolutional
        # layers, don't inherently process positional information in the input. If the position of a card in the array
        # is important (e.g., the first number being the most significant), you might need to ensure that the network
        # architecture can understand this positional significance. This could be addressed through feature
        # engineering or by choosing a network architecture that can handle sequence data, like an RNN or LSTM.
        list.extend([
                self.game.opponent.energy,
                self.game.opponent.pips,
                self.game.opponent.plate.central_maneuver_card,
                self.game.opponent.plate.level
            ]
        )
        for line_values in self.game.opponent.plate.opponent_line_values():
            list.extend(line_values)

        list.extend(self.game.agentPlayer.draw.nn_value_hidden_draw(self.draw_deck_max_size))
        list.extend(self.game.agentPlayer.stash.nn_value_hidden_draw(self.personal_stash_max_size))

        list.extend(self.game.commonDrawDeck.nn_value_hidden_draw(self.common_draw_deck_max_size))

        return list

    def _build_player(self) -> Dense:
        input_energy = Input(shape=(1,), name='energy')
        input_pips = Input(shape=(1,), name='pips')

        layer_energy = Dense(8, activation='relu')(input_energy)
        layer_pips = Dense(8, activation='relu')(input_pips)

        input_central_maneuver_card = Input(shape=(1,), name="central maneuver card")

        embedding_central_maneuver_card = \
            Embedding(input_dim=self.num_unique_cards, output_dim=self.embedding_size)(input_central_maneuver_card)
        layer_central_maneuver_card = Flatten()(embedding_central_maneuver_card)

        input_level = Input(shape=(1,), name="level")
        layer_level = Dense(4, activation='relu')(input_level)
        #
        # Agent ManeuverPlate of 4 lines
        #
        layer_line_5 = self._build_line(5)
        layer_line_4 = self._build_line(4)
        layer_line_3a = self._build_line(3)
        layer_line_3b = self._build_line(3)

        combined = concatenate([
            layer_energy,
            layer_pips,
            layer_central_maneuver_card,
            layer_level,
            layer_line_5,
            layer_line_4,
            layer_line_3a,
            layer_line_3b
        ])
        return Dense(16, activation='relu')(combined)

    def _build_line(self, max_cards: int) -> Dense:
        input_line_visible = Input(shape=(1,), name=f"{max_cards}: line visible")
        input_intention_coin = Input(shape=(1,), name=f"{max_cards}: line intention coin")
        input_line_cards = Input(shape=(max_cards,), name=f"{max_cards}: line cards")

        embedding_line_cards = \
            Embedding(input_dim=self.num_unique_cards, output_dim=self.embedding_size)(input_line_cards)
        layer_line_cards = Flatten()(embedding_line_cards)

        layer_line_visible = Dense(8, activation='relu')(input_line_visible)
        layer_intention_coin = Dense(8, activation='relu')(input_intention_coin)

        combined = concatenate([layer_line_visible, layer_intention_coin, layer_line_cards])
        return Dense(8, activation='relu')(combined)


