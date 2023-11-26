import argparse
import math
import sys
from typing import List

from src.CardType import card_type_from, card_type_possibilities_string
from src.Deck import Deck
from src.EvalRun import EvalRun
from src.Maneuver import Maneuver
from src.maneuver_from import maneuver_from


class MainArgs:

    DEFAULT_TIMES = 500000
    card_types: List[str]
    monster: bool
    levels: List[int]
    step: int
    times: int

    def __init__(self, cmdline_args):
        parser = argparse.ArgumentParser(description="Process the command-line arguments.")

        # Required argument
        parser.add_argument("card_types", type=str, default=None, nargs='*', help="A list of card types")

        # Optional arguments with default values
        parser.add_argument("-times", type=int, default=self.DEFAULT_TIMES,
                            help=f"Number of simulations to run. Defaults to {self.DEFAULT_TIMES}.")
        parser.add_argument("-step", type=int, default=1,
                            help="Number of summaries to print during computation. Must be at least 1. Defaults to 1.")
        parser.add_argument("-levels", type=int, default=[1], nargs='+',
                            help="The list of the number of times the maneuver is applied due to a level increase.")
        parser.add_argument("-monster", action='store_true', default=False,
                            help="Apply on a monster not a player. Defaults to False.")

        # Initially, assume there is no error.
        self.has_error = False
        self.error_message = ""

        try:
            args = parser.parse_args(cmdline_args)

            # Check for any post-parsing validation, e.g., the summaries check.
            if args.step < 1:
                self.has_error = True
                self.error_message = "Error: step must be at least 1!"
                return
            if len(args.card_types) == 0:
                self.has_error = True
                self.error_message = "Error: must specify a card type:\n\tall\nor:\t" + \
                                     '\n\t'.join(card_type_possibilities_string())
                return

            # If everything is fine, populate the instance variables
            self.card_types = args.card_types
            self.times = args.times
            self.step = args.step
            self.monster = args.monster
            self.levels = args.levels
        except Exception as e:
            self.has_error = True
            self.error_message = str(e)

    def print_error(self):
        print(self.error_message)


def main(cmdline_args):
    args = MainArgs(cmdline_args)
    if args.has_error:
        args.print_error()
    else:
        process_args(args)


def process_args(args: MainArgs):
    run = EvalRun()
    run.maneuvers = parse_card_types(args.card_types)
    if len(run.maneuvers) == 0:
        return
    run.step = math.ceil(args.times / args.step)
    run.count = args.times

    if args.monster:
        run.decks = [
            # The best draw after 1 turn
            Deck([20, 12, 10, 20, 12, 10], [8, 8, 6, 4]),
            # The best draw after 2 turns
            Deck([20, 12, 10, 8, 20, 12, 10, 8, 6, 4], []),
            # The best draw after pulling the best from the personal stash limited by max reach (18)
            Deck([20, 20, 20, 20, 12, 12, 12, 12, 10, 10, 10, 10, 8, 8, 8, 8, 6, 6], [4, 4]),
        ]
    else:
        run.decks = [
            # The best draw after 1 turn
            Deck([20, 12, 10, 8], [6, 4]),
            # The best draw after 2 turns
            Deck([20, 12, 10, 8, 6, 4], []),
            # The best draw after pulling the personal stash to the limit of the max reach (12)
            Deck([20, 12, 10, 8, 6, 4, 20, 12, 10, 8, 6, 4], []),
        ]
    run.adjust_times_list = args.levels
    run.run()


def parse_card_types(card_types: List[str]) -> List[Maneuver]:
    maneuvers: List[Maneuver] = []
    do_all = False
    for card_type_str in card_types:
        if card_type_str == "all":
            do_all = True
            continue
        card_type = card_type_from(card_type_str)
        if card_type is None:
            print(f"Unrecognized card type: {card_type_str}")
            return []
        maneuver = maneuver_from(card_type)
        if maneuver is None:
            print(f"Maneuver not yet implemented: {card_type_str}")
            return []
        if maneuver not in maneuvers:
            maneuvers.append(maneuver)
    if do_all:
        all_types = card_type_possibilities_string()
        for card_type_str in all_types:
            card_type = card_type_from(card_type_str)
            if card_type is None:
                continue
            maneuver = maneuver_from(card_type)
            if maneuver is None:
                continue
            if maneuver not in maneuvers:
                maneuvers.append(maneuver)
    return maneuvers


if __name__ == "__main__":
    # sys.argv includes the script name as the first argument,
    # so we pass the rest of the arguments to the main function
    main(sys.argv[1:])
