import argparse
import sys
from typing import List

from src.engine.RunEngine import RunEngine


class MainArgs:

    DEFAULT_TIMES = 500000
    card_types: List[str]
    monster: bool
    levels: List[int]
    step: int
    times: int

    def __init__(self, cmdline_args):
        parser = argparse.ArgumentParser(description="Process the command-line arguments.")

        parser.add_argument("-times", type=int, default=self.DEFAULT_TIMES,
                            help=f"Number of games to run. Defaults to {self.DEFAULT_TIMES}.")
        parser.add_argument("-step", type=int, default=1,
                            help="Number of summaries to print during computation. Must be at least 1. Defaults to 1.")

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

            # If everything is fine, populate the instance variables
            self.times = args.times
            self.step = args.step
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
        run = RunEngine(args.times, args.step)
        run.run()


if __name__ == "__main__":
    # sys.argv includes the script name as the first argument,
    # so we pass the rest of the arguments to the main function
    main(sys.argv[1:])
