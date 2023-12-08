import unittest

from src.engine.RunEngine import *


class TestRunEngine(unittest.TestCase):

    def setUp(self):
        self.params = RunEngineParams()
        self.SUT = RunEngine(self.params)

