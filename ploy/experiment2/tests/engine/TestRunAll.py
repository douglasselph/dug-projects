import unittest
from src.engine.RunAll import RunAll


class TestRunEngine(unittest.TestCase):

    def setUp(self):
        self.SUT = RunAll()
