class Stats:

    def __init__(self):
        self.total: int = 0
        self.tries: int = 0

    def add(self, value: int):
        self.tries += 1
        self.total += value

    @property
    def average(self) -> float:
        return self.total / self.tries

    def reset(self):
        self.total = 0
        self.tries = 0

    def __str__(self) -> str:
        return f"# Tries={self.tries:,}, Average={self.average:.3f}"
