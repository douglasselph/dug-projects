
import statistics
from typing import List


class Values:

    values: List[float]

    def __init__(self):
        self.values = []

    def add(self, value: float):
        self.values.append(value)

    def mean(self) -> float:
        return statistics.mean(self.values)

    def std_dev(self) -> float:
        return statistics.stdev(self.values)


class Stats:

    track_min: Values
    track_max: Values
    track_avg: Values

    def __init__(self):
        self.track_min = Values()
        self.track_max = Values()
        self.track_avg = Values()

    def min(self, value):
        self.track_min.add(value)

    def max(self, value):
        self.track_max.add(value)

    def average(self, value):
        self.track_avg.add(value)

    @property
    def min_mean(self) -> float:
        return self.track_min.mean()

    @property
    def max_mean(self) -> float:
        return self.track_max.mean()

    @property
    def avg_mean(self) -> float:
        return self.track_avg.mean()

    @property
    def min_std_dev(self) -> float:
        return self.track_min.std_dev()

    @property
    def max_std_dev(self) -> float:
        return self.track_max.std_dev()

    @property
    def avg_std_dev(self) -> float:
        return self.track_avg.std_dev()

    def show(self):
        print("Mean Min:", self.min_mean)
        print("Mean Max:", self.max_mean)
        print("Mean Avg:", self.avg_mean)
        print("Std Dev Min:", self.min_std_dev)
        print("Std Dev Max:", self.max_std_dev)
        print("Std Dev Avg:", self.avg_std_dev)

    def __repr__(self):
        return f"Stats(Min({self.track_min.mean()},Max({self.track_max.mean()},Avg({self.track_avg.mean()})"



