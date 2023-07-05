import unittest
from typing import List

from src.compute import ComputeBustACut
from src.compute import probability1


class TestCompute(unittest.TestCase):

    def test_bust_a_cut_4_sided_probability(self):
        # Arrange
        # 4 sided: total=4x4=16
        sides = 4
        total = sides * sides
        expected_value: [float] = [0.0] * (sides+1)
        # 1,1=1. (1) [1/16]
        expected_value[1] = 1.0 / total
        # 2,1=2. 1,2=2, 2,2=2. (3) [3/16]
        expected_value[2] = 3.0 / total
        # 1,3=3. 3,1=3. 2,3=3. 3,2=3. 3,3=3 (5) [5/16]
        expected_value[3] = 5.0 / total
        # 1,4=4. 4,1=4. 2,4=4, 4,2=4. 3,4=4. 4,3=4. 4,4=4. (7) [7/16]
        expected_value[4] = 7.0 / total
        # Act
        SUT = ComputeBustACut([sides])
        prob: [float] = [0.0] * (sides+1)
        for x in range(1, (sides+1)):
            prob[x] = probability1(x, sides)
        # Assert
        self.assertEqual(total, 1 + 3 + 5 + 7)
        for x in range(1, (sides+1)):
            self.assertEqual(expected_value[x], prob[x])

    def test_bust_a_cut_4_sided_average(self):
        # Arrange
        # 4 sided: total=4x4=16
        sides = 4
        total = sides * sides
        expected_avg: [float] = [0.0] * (sides+1)
        # 1,1=1. (1) [1/16]
        expected_avg[1] = 1.0 / total
        # 2,1=2. 1,2=2, 2,2=2. (3) [3/16]
        expected_avg[2] = 3.0 / total
        # 1,3=3. 3,1=3. 2,3=3. 3,2=3. 3,3=3 (5) [5/16]
        expected_avg[3] = 5.0 / total
        # 1,4=4. 4,1=4. 2,4=4, 4,2=4. 3,4=4. 4,3=4. 4,4=4. (7) [7/16]
        expected_avg[4] = 7.0 / total
        # Act
        SUT = ComputeBustACut([sides])
        avg: [float] = [0.0] * (sides+1)
        for x in range(1, (sides+1)):
            avg[x] = SUT.average(x)
        # Assert
        self.assertEqual(total, 1 + 3 + 5 + 7)
        for x in range(1, (sides+1)):
            self.assertEqual(expected_avg[x], avg[x])

    def test_bust_a_cut_6_sided_average(self):
        # Arrange

        # 6 sided: total=6x6=16
        precision = ComputeBustACut.precision
        sides = 6
        total = sides * sides
        expected_avg: [float] = [0.0] * (sides+1)
        # 1,1=1. (1) [1/36]
        expected_avg[1] = 1.0 / total
        # 2,1=2. 1,2=2, 2,2=2. (3) [3/36]
        expected_avg[2] = 3.0 / total
        # 1,3=3. 3,1=3. 2,3=3. 3,2=3. 3,3=3 (5) [5/36]
        expected_avg[3] = 5.0 / total
        # 1,4=4. 4,1=4. 2,4=4, 4,2=4. 3,4=4. 4,3=4. 4,4=4. (7) [7/36]
        expected_avg[4] = 7.0 / total
        # 1,5=5. 5,1=5. 2,5=5. 5,2=5. 3,5=5. 5,3=5. 4,5=5. 5,4=5. 5,5=5 (9)
        expected_avg[5] = 9.0 / total
        # 1,6=6. 6,1=6. 2,6=6. 6,2=6. 3,6=6. 6,3=6. 4,6=6. 6,4=6. 5,6=6. 6,5=6. 6,6=6 (11)
        expected_avg[6] = 11.0 / total
        # Act
        SUT = ComputeBustACut([sides])
        avg: [float] = [0.0] * (sides+1)
        for x in range(1, sides+1):
            avg[x] = SUT.average(x)
        # Assert
        self.assertEqual(total, 1 + 3 + 5 + 7 + 9 + 11)
        for x in range(1, (sides+1)):
            self.assertEqual(round(expected_avg[x], precision), avg[x])

    def test_bust_a_cut_4_and_6_sided_average(self):
        # Arrange
        # 4 sided: total=4x4=16
        # 6 sides: total=6x6=36
        total = 4 * 6
        expected_avg: [float] = [0.0] * 7
        # 1,1=1. (1) [1/24]
        expected_avg[1] = 1.0 / total
        # 2,1=2. 1,2=2, 2,2=2. (3) [3/24]
        expected_avg[2] = 3.0 / total
        # 1,3=3. 3,1=3. 2,3=3. 3,2=3. 3,3=3 (5) [5/24]
        expected_avg[3] = 5.0 / total
        # 1,4=4. 4,1=4. 2,4=4, 4,2=4. 3,4=4. 4,3=4. 4,4=4. (7) [7/24]
        expected_avg[4] = 7.0 / total
        # 1,5=5. 2,5=5. 3,5=5. 4,5=5. (4) [4/24]
        expected_avg[5] = 9.0 / total
        # 1,6=6. 2,6=6. 3,6=6. 4,6=6. (4) [4/24]
        expected_avg[6] = 11.0 / total
        # Act
        SUT = ComputeBustACut([4, 6])
        avg: [float] = [0.0] * 7
        for x in range(1, 7):
            avg[x] = SUT.average(x)
        # Assert
        self.assertEqual(total, 1 + 3 + 5 + 7 + 4 + 4)
        for x in range(1, 7):
            self.assertEqual(expected_avg[x], avg[x])


