import unittest

from src.decision.validate.ValidateDeployBlock import ValidateDeployBlock


class TestValidateDeployBlock(unittest.TestCase):

    def setUp(self):
        self.SUT = ValidateDeployBlock()
