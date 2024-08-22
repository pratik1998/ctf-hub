from enum import IntEnum
from typing import Dict, List, Tuple

class Color(IntEnum):
  ORANGE = 1
  YELLOW = 2
  GREEN = 3
  BLUE = 4
  INDIGO = 5
  VIOLET = 6
  RED = 0


class Shape(IntEnum):
  SQUARE = 0
  CIRCLE = 1
  TRIANGLE = 2
  PENTAGONE = 3

EQUATIONS: Dict[Tuple[Color, Shape], List[Tuple[Color, Shape]]] = {
  (Color.RED, Shape.SQUARE): [(Color.RED, Shape.SQUARE), (Color.RED, Shape.SQUARE)],
  (Color.YELLOW, Shape.SQUARE): [(Color.ORANGE, Shape.SQUARE), (Color.ORANGE, Shape.SQUARE)],
  (Color.GREEN, Shape.SQUARE): [(Color.ORANGE, Shape.SQUARE), (Color.YELLOW, Shape.SQUARE)],
  (Color.BLUE, Shape.SQUARE): [(Color.YELLOW, Shape.SQUARE), (Color.YELLOW, Shape.SQUARE)],
  (Color.VIOLET, Shape.SQUARE): [(Color.GREEN, Shape.SQUARE), (Color.GREEN, Shape.SQUARE)],
  (Color.INDIGO, Shape.SQUARE): [(Color.YELLOW, Shape.SQUARE), (Color.GREEN, Shape.SQUARE)],
  (Color.RED, Shape.CIRCLE): [(Color.VIOLET, Shape.SQUARE), (Color.ORANGE, Shape.SQUARE)],
  (Color.YELLOW, Shape.TRIANGLE): [(Color.YELLOW, Shape.CIRCLE), (Color.RED, Shape.CIRCLE)],
  (Color.INDIGO, Shape.PENTAGONE): [(Color.INDIGO, Shape.CIRCLE), (Color.RED, Shape.TRIANGLE)],
  (Color.GREEN, Shape.SQUARE): [(Color.GREEN, Shape.TRIANGLE), (Color.RED, Shape.TRIANGLE)],
  (Color.RED, Shape.SQUARE): [(Color.VIOLET, Shape.PENTAGONE), (Color.ORANGE, Shape.SQUARE)],
  (Color.ORANGE, Shape.CIRCLE): [(Color.GREEN, Shape.TRIANGLE), (Color.INDIGO, Shape.TRIANGLE)],
  (Color.INDIGO, Shape.SQUARE): [(Color.ORANGE, Shape.PENTAGONE), (Color.BLUE, Shape.CIRCLE)],
  (Color.INDIGO, Shape.PENTAGONE): [(Color.GREEN, Shape.PENTAGONE), (Color.INDIGO, Shape.CIRCLE), (Color.BLUE, Shape.TRIANGLE)],
}


TOTAL = 28

def get_char(mixture_value: int, offset: int):
  mixture_value = (mixture_value + offset) % 28
  if (mixture_value == 27):
    return '!'
  elif mixture_value == 26:
    return '?'
  else:
    return chr(ord('A') + mixture_value)

def get_bottle_score(bottle: Tuple[Color, Shape]):
  color, shape = bottle
  return 7 * int(shape) + int(color)


def solve(puzzle: List[List[Tuple[Color, Shape]]]):
  # Make sure that we satisfy all the equations
  for lhs_term in EQUATIONS.keys():
    lhs_value = get_bottle_score(lhs_term)
    rhs_value = 0
    for term in EQUATIONS[lhs_term]:
      rhs_value = (rhs_value + get_bottle_score(term)) % 28
    assert lhs_value == rhs_value
  
  group_size = 3
  total_groups = int(len(puzzle) / group_size)
  answer = ""
  for i in range(total_groups):
    mixture_value = 0
    for j in range(group_size):
      mixture_value = mixture_value + get_bottle_score(puzzle[group_size * i + j])
    answer += get_char(mixture_value, 0)
  print(answer)
      

def main():
  puzzle: List  = [
    (Color.YELLOW, Shape.PENTAGONE), (Color.GREEN, Shape.CIRCLE), (Color.INDIGO, Shape.CIRCLE),
    (Color.ORANGE, Shape.CIRCLE), (Color.ORANGE, Shape.CIRCLE), (Color.INDIGO, Shape.PENTAGONE),
    (Color.ORANGE, Shape.TRIANGLE), (Color.INDIGO, Shape.PENTAGONE), (Color.VIOLET, Shape.SQUARE),
    (Color.GREEN, Shape.PENTAGONE), (Color.GREEN, Shape.TRIANGLE), (Color.RED, Shape.TRIANGLE),
    
    (Color.YELLOW, Shape.CIRCLE), (Color.YELLOW, Shape.PENTAGONE), (Color.RED, Shape.CIRCLE),
    (Color.BLUE, Shape.TRIANGLE), (Color.BLUE, Shape.CIRCLE), (Color.GREEN, Shape.SQUARE),
    (Color.ORANGE, Shape.PENTAGONE), (Color.RED, Shape.CIRCLE), (Color.BLUE, Shape.SQUARE),
    (Color.INDIGO, Shape.TRIANGLE), (Color.BLUE, Shape.PENTAGONE), (Color.GREEN, Shape.SQUARE),
    
    (Color.VIOLET, Shape.PENTAGONE), (Color.INDIGO, Shape.SQUARE), (Color.VIOLET, Shape.PENTAGONE),
    (Color.VIOLET, Shape.CIRCLE), (Color.ORANGE, Shape.SQUARE), (Color.GREEN, Shape.SQUARE),
    (Color.RED, Shape.CIRCLE), (Color.YELLOW, Shape.PENTAGONE), (Color.INDIGO, Shape.CIRCLE),
    (Color.INDIGO, Shape.CIRCLE), (Color.RED, Shape.CIRCLE), (Color.GREEN, Shape.PENTAGONE),
    
    (Color.VIOLET, Shape.PENTAGONE), (Color.BLUE, Shape.PENTAGONE), (Color.ORANGE, Shape.TRIANGLE),
    (Color.VIOLET, Shape.TRIANGLE), (Color.INDIGO, Shape.CIRCLE), (Color.GREEN, Shape.PENTAGONE),
    (Color.GREEN, Shape.PENTAGONE), (Color.VIOLET, Shape.PENTAGONE), (Color.YELLOW, Shape.PENTAGONE),
    (Color.GREEN, Shape.PENTAGONE), (Color.RED, Shape.SQUARE), (Color.YELLOW, Shape.PENTAGONE),
  ]
  solve(puzzle)

if __name__=="__main__":
    main()