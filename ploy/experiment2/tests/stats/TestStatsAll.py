import unittest
from src.stat.StatsAll import StatsAll


class TestStatsAll(unittest.TestCase):

    def setUp(self):
        self.SUT = StatsAll()
