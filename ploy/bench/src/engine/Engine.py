from src.data.Card import *
from src.data.Decision import *
from src.data.Game import Game, PlayerID
from src.data.Player import Player
from src.data.stat.StatsAttack import StatsAttack, StatsDeploy
from src.decision.Decisions import Decisions
from src.data.die.Die import Die
from src.engine.IncidentBundle import IncidentBundle
from src.data.maneuver.ManeuverFeelingFeint import ManeuverFeelingFeint


class Engine:

    def __init__(self, game: Game, decisions: Decisions):
        self.game = game
        self.decisions = decisions
        self.decisions.placeCard.set_game(self.game)

    ############################################################################
    # Draw Hand of 4 maneuver for all players.
    def draw_hands(self):
        self.game.agentPlayer.draw_hand()
        self.game.opponent.draw_hand()

    ############################################################################
    # Place the next card in the draw hand to the indicated place on the plate.
    # Return True if successful.
    # Return False if illegal indication.

    def place_cards(self):

        while self.game.agentPlayer.has_face_up_cards or self.game.opponent.has_face_up_cards:

            if self.game.agentPlayer.has_face_up_cards:
                line, coin = self.decisions.placeCard.decision_agent()
                was_legal = self._agent_place_card(line, coin)
                self.decisions.placeCard.result_agent(line, coin, was_legal)

            if self.game.opponent.has_face_up_cards:
                line, coin = self.decisions.placeCard.decision_opponent()
                was_legal = self._opponent_place_card(line, coin)
                self.decisions.placeCard.result_opponent(line, coin, was_legal)

    def _agent_place_card(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        return self._place_card(self.game.agentPlayer, line, coin)

    def _opponent_place_card(self, line: DecisionLine, coin: DecisionIntention) -> bool:
        return self._place_card(self.game.opponent, line, coin)

    @staticmethod
    def _place_card(pl: Player, line: DecisionLine, coin: DecisionIntention) -> bool:
        if not pl.is_legal_intention(line, coin):
            return False
        return pl.play_to_plate(line, coin)

    @property
    def agent_has_cards_to_place(self) -> bool:
        return self.game.agentPlayer.has_face_up_cards

    @property
    def opponent_has_cards_to_place(self) -> bool:
        return self.game.opponent.has_face_up_cards

    ###############################################################################
    # Reveal: any plate line at max has its intention coin revealed
    def reveal_intentions(self):
        self._reveal_intentions(self.game.agentPlayer)
        self._reveal_intentions(self.game.opponent)

    @staticmethod
    def _reveal_intentions(player: Player):
        if player.has_cards_to_draw:
            player.reveal_intentions_if_maxed()
        else:
            player.reveal_all_intentions()

    ###############################################################################
    # Attack Resolve:
    #   In initiative order:
    #     Attacker reveal supporting lines
    #     Defender reveal supporting lines
    #     Resolve
    def resolve_attacks(self) -> StatsAttack:
        if self.game.initiativeOn == PlayerID.PLAYER_1:
            stats1 = self._resolve_attack(self.game.agentPlayer, self.game.opponent)
            stats2 = self._resolve_attack(self.game.opponent, self.game.agentPlayer)
            if stats2.did_attack:
                self.game.initiativeOn = PlayerID.PLAYER_2
        else:
            stats2 = self._resolve_attack(self.game.opponent, self.game.agentPlayer)
            stats1 = self._resolve_attack(self.game.agentPlayer, self.game.opponent)
            if stats1.did_attack:
                self.game.initiativeOn = PlayerID.PLAYER_1
        return StatsAttack().add(stats1, stats2)

    def _resolve_attack(self, attacker: Player, defender: Player) -> StatsAttack:

        stats = StatsAttack()

        if not attacker.plate_has_intention(DecisionIntention.ATTACK):
            return stats

        attacker.reveal_intentions_of(DecisionIntention.ATTACK)
        defender.reveal_intentions_of(DecisionIntention.DEFEND)
        attacker.reveal_cards_with_revealed_intentions()
        defender.reveal_cards_with_revealed_intentions()

        attacker.apply_to_die_four(DecisionIntention.ATTACK)
        defender.apply_to_die_four(DecisionIntention.DEFEND)

        self._apply_feeling_feint(attacker, DecisionIntention.ATTACK)

        if defender.has_revealed_intention(DecisionIntention.DEFEND):
            self._apply_feeling_feint(defender, DecisionIntention.DEFEND)

        incident_attacker = IncidentBundle()
        incident_defender = IncidentBundle()
        incident_attacker.cards = attacker.collect_face_up_cards_for(DecisionIntention.ATTACK)
        incident_defender.cards = defender.collect_face_up_cards_for(DecisionIntention.DEFEND)
        incident_attacker.cards.append(attacker.central_maneuver_card)
        incident_defender.cards.append(defender.central_maneuver_card)
        incident_attacker.pull_dice()
        incident_defender.pull_dice()

        bonus_attacker = self._apply_trash_pre_roll(incident_attacker, attacker)
        bonus_defender = self._apply_trash_pre_roll(incident_defender, defender)

        incident_attacker.apply_cards_pre_roll(incident_defender.dice)
        incident_defender.apply_cards_pre_roll(incident_attacker.dice)

        incident_attacker.roll()
        incident_defender.roll()

        incident_attacker.apply_cards_post_roll(incident_defender.values)
        incident_defender.apply_cards_post_roll(incident_attacker.values)

        self._apply_trash_post_roll(incident_attacker, bonus_attacker)
        self._apply_trash_post_roll(incident_defender, bonus_defender)

        wounds = incident_attacker.total - incident_defender.total

        if wounds >= 0:
            wound = self._apply_wound_to(defender, wounds, attacker)
        else:
            wound = self._apply_wound_to(attacker, wounds, defender)

        stats.set(incident_attacker.total, incident_defender.total, wound)

        return stats

    def _apply_feeling_feint(self, player: Player, coin: DecisionIntention):
        ManeuverFeelingFeint(self.decisions.feelingFeint).apply(player.plate, coin)

    def _apply_wound_to(self, player: Player, wounds: int, attacker: Player) -> Optional[CardWound]:
        wound: Optional[CardWound] = None
        if wounds <= 6:
            return None
        if wounds <= 16:
            wound = CardWound.WOUND_MINOR
        elif wounds <= 22:
            wound = CardWound.WOUND_MINOR
            self._apply_secondary_affliction(player, DieSides.D4)
        elif wounds <= 29:
            wound = CardWound.WOUND_ACUTE
            self._apply_secondary_affliction(player, DieSides.D6)
        elif wounds <= 36:
            wound = CardWound.WOUND_ACUTE
            self._apply_secondary_affliction(player, DieSides.D8)
        elif wounds <= 44:
            wound = CardWound.WOUND_GRAVE
            self._apply_secondary_affliction(player, DieSides.D10)
        elif wounds <= 52:
            wound = CardWound.WOUND_GRAVE
            self._apply_secondary_affliction(player, DieSides.D12)
        elif wounds <= 59:
            wound = CardWound.WOUND_DIRE
            self._apply_secondary_affliction(player, DieSides.D12)
        elif wounds <= 69:
            player.fatal_received = True
        elif wounds <= 76:
            player.fatal_received = True
            attacker.energy -= 1
        elif wounds <= 84:
            player.fatal_received = True
            attacker.energy -= 2
        else:
            player.fatal_received = True
            attacker.energy -= 3
        player.draw.append(wound)
        return wound

    def _apply_secondary_affliction(self, player: Player, sides: DieSides):
        value = Die(sides).roll()
        if value == 1:
            player.pips = 0
        elif value == 2:
            player.draw_one_less_card = True
        elif value == 3:
            player.discard_all()
        elif value == 4:
            player.energy -= 1
        elif value == 5:
            player.energy -= 1
        elif value == 6:
            player.upgrade_lowest_face_up_wound()
        elif value == 7:
            player.add_penalty_coin()
        elif value == 8:
            player.reduce_reach()
        elif value == 9:
            card = player.trash_one_random_face_up_plate_cards()
            self._add_card_to_trash(card)
        elif value == 10:
            player.reduce_reach()
            player.reduce_reach()
        elif value == 11:
            player.upgrade_highest_face_up_wound()
        else:
            self._apply_secondary_affliction(player, sides)
            self._apply_secondary_affliction(player, sides)

    def _add_card_to_trash(self, card: Card):
        self.game.trash.append(card)

    def _apply_trash_pre_roll(self, incident: IncidentBundle, player: Player) -> TrashBonus:
        bonus = self._select_card_to_trash(incident, player)
        if isinstance(bonus, TrashBonusDie):
            incident.dice.add_die(bonus.sides)
        return bonus

    @staticmethod
    def _apply_trash_post_roll(incident: IncidentBundle, bonus: TrashBonus):
        if isinstance(bonus, TrashBonusPips):
            incident.values.add(Die(DieSides.D4, bonus.pips))

    def _select_card_to_trash(self, incident: IncidentBundle, player: Player) -> TrashBonus:
        card = self.decisions.trash. \
            set_game(self.game). \
            set_player(player). \
            select_card_to_trash(incident.cards)
        if card:
            player.trash_face_up_card(card)
            self.game.trash.append(card)
            return card.trash
        return TrashBonus()

    ###############################################################################
    # Deploy Resolve:

    def resolve_deploy(self) -> StatsDeploy:

        stats = StatsDeploy()

        stats.agent_roll = self._resolve_deploy_gather_pips(self.game.agentPlayer)
        stats.opponent_roll = self._resolve_deploy_gather_pips(self.game.opponent)

        agent_okay = True
        opponent_okay = True

        while agent_okay or opponent_okay:
            if self.game.initiativeOn == PlayerID.PLAYER_1:
                if agent_okay:
                    agent_okay = self._resolve_deploy_buy_card(
                        self.game.agentPlayer,
                        self.game.opponent,
                        stats
                    )
                if opponent_okay:
                    opponent_okay = self._resolve_deploy_buy_card(
                        self.game.opponent,
                        self.game.agentPlayer,
                        stats
                    )
            else:
                if opponent_okay:
                    opponent_okay = self._resolve_deploy_buy_card(
                        self.game.opponent,
                        self.game.agentPlayer,
                        stats
                    )
                if agent_okay:
                    agent_okay = self._resolve_deploy_buy_card(
                        self.game.agentPlayer,
                        self.game.opponent,
                        stats
                    )

        return stats

    @staticmethod
    def _resolve_deploy_gather_pips(player: Player) -> int:

        player.reveal_intentions_of(DecisionIntention.DEPLOY)
        player.reveal_cards_with_revealed_intentions()
        player.apply_feeling_feint(DecisionIntention.DEPLOY)

        if player.has_revealed_intention(DecisionIntention.DEPLOY):
            player.apply_feeling_feint(DecisionIntention.DEPLOY)

        incident = IncidentBundle()
        incident.cards = player.collect_face_up_cards_for(DecisionIntention.DEPLOY)
        incident.cards.append(player.central_maneuver_card)
        incident.pull_dice()

        incident.apply_cards_pre_roll(incident.dice)
        incident.roll()
        incident.apply_cards_post_roll(incident.values)

        player.pips += incident.total

        return incident.total

    def _resolve_deploy_buy_card(self, player: Player, opponent: Player, stats: StatsDeploy) -> bool:

        self.decisions.deployChooseCard.set_game(self.game)
        decision = self.decisions.deployChooseCard.card_to_acquire(player, opponent)
        if decision == DecisionDeck.NONE:
            return False
        if decision == DecisionDeck.PERSONAL_STASH_DRAW:
            player.draw.draw()
            player.pips -= 1
            return True
        if decision == DecisionDeck.COMMON_DRAW:
            self.game.commonDrawDeck.draw()
            player.pips -= 1
            return True
        if decision == DecisionDeck.OPPONENT_STASH_FACE_UP:
            card = opponent.stash_cards_face_up[0]
        elif decision == DecisionDeck.PERSONAL_STASH_FACE_UP:
            card = player.stash_cards_face_up[0]
        else:
            card = self.game.common_cards_face_up[0]

        block_value = self.decisions.deployBlock \
            .set_game(self.game) \
            .set_player(opponent).acquire_block_value(decision, card)

        if block_value > 0:
            opponent.pips -= block_value
            stats.blocks += 1
            return True

        # Buy card
        player.pips -= card.cost.pips

        if card.cost.sides != DieSides.NONE and player.plate_has_face_up_sides(card.cost.sides):
            trashed_card = player.plate_remove_sides(card.cost.sides)
            self.game.trash.append(trashed_card)
        elif card.cost.energy > 0:
            player.energy -= card.cost.energy

        player.draw.append(card)

        if decision == DecisionDeck.OPPONENT_STASH_FACE_UP:
            opponent.stash_pull_face_up_card()
            return True
        elif decision == DecisionDeck.PERSONAL_STASH_FACE_UP:
            player.stash_pull_face_up_card()
            return True
        else:
            self.game.common_pull_face_up_card()
            return True

    ###############################################################################
    # Cleanup:
    def cleanup(self):
        self.game.cleanup()
