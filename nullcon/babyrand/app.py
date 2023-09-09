#!/usr/bin/python3 -u

import random
import os
from mt19937predictor import MT19937Predictor

max_retries = 100

predictor = MT19937Predictor()

for _ in range(max_retries):
    print("Hints:")
    for i in range(9):
        randomNumber = random.getrandbits(32)
        print(randomNumber)
        predictor.setrandbits(randomNumber, 32)
    real = random.getrandbits(32)
    print("Guess:")
    # resp = input()
    resp = predictor.getrandbits(32)
    if int(resp) == real:
        print("FLAG", os.getenv("FLAG"))

print("No tries left, sorry!")