import unittest

from src.decision.validate.ValidateTrash import ValidateTrash


class TestValidateTrash(unittest.TestCase):

    def setUp(self):
        self.SUT = ValidateTrash()
