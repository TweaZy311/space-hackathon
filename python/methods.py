def find_centroid(points: list) -> list:
  """Находит центр масс фигуры."""
  xs = [x for x, y in points]
  ys = [y for x, y in points]

  max_coord = max(xs)
  ys.append(max_coord)
  max_coord = max(ys)
  center = [int(max_coord/2), int(max_coord/2)]

  return center

def rotate(points: list) -> list:

  center = find_centroid(points)
  # print(f'center: {center}')
  """Поворачивает фигуру на 90 градусов по часовой стрелке относительно заданного центра."""
  cx, cy = center
  # Перенос к началу координат
  shifted_points = [[x - cx, y - cy] for x, y in points]
  # Поворот на 90 градусов
  rotated_points = [[-y, x] for x, y in shifted_points]
  result = [[x + cx, y + cy] for x, y in rotated_points]
  ys = [y for x, y in result]
  xs = [x for x, y in result]
  result = [(x-min(xs), y-min(ys)) for x, y in result]
  return result

def generate_empty_board(x, y):
  board = [[0 for _ in range(x)] for __ in range(y)]
  return board

def is_empty(board):
  for arr in board:
    for el in arr:
      if el != 0:
        return False
  return True

def can_fit(board, cell, figure):
    """
    Функция для проверки влезания фигуры в клетку двумерного массива
    """
    for _ in range(4):  # Проверяем каждый из четырех поворотов
        initial_cell_x = figure[0][0]
        initial_cell_y = figure[0][1]
        # offset_x = initial_cell_x + cell[0]
        # offset_y = initial_cell_y + cell[1]
        # if 0 <= offset_x < len(board[0]) and 0 <= offset_y < len(board):
        coordinates = [[cell[0], cell[1]]]
          # print(coordinates)  
        # else:
          # break
        for i in range(1, len(figure)):
            new_x = cell[0] + figure[i][0] - initial_cell_x
            new_y = cell[1] + figure[i][1] - initial_cell_y
            if 0 <= new_x < len(board[0]) and 0 <= new_y < len(board):
                if board[new_y][new_x] != 0:
                    break  # Переходим к следующему повороту
                else:
                    coordinates.append([new_x, new_y])
                    # print(f'new_x: {new_x}, new_y: {new_y}')
            else:
                break  # Переходим к следующему повороту
        else:  # Срабатывает, если цикл завершился без break, т.е. фигура влезла
            # print(f'In can_fit: {coordinates}')
            return True, coordinates
        figure = rotate(figure)  # Поворачиваем фигуру для проверки следующего поворота
    return False, None  # Фигура не влезает ни в один из четырех поворотов

def place_figure(board, title, coords):
  for x,y in coords:
    board[y][x] = title
  return board

def collect_garbage(board, figures):
  # 1. Отсортировать фигуры по размеру
  figures.sort(key=len)
    # figures = {k: v for k, v in sorted(figures.items(), key=lambda item: len(item[1]))}

  # 2.1. Если board пуст, набираем на 30% загрузки (для успешной загрузки)
  if is_empty(board):
    items_taken = []
    current_area = 0
    max_area = len(board) * len(board[0])
    for figure in figures:
      if current_area >= max_area * 0.3:
        break
      current_area += len(figure)
      items_taken.append(figure)

  # 2.2. Если board не пуст, набираем на 5% загрузки (для успешной загрузки)
  else:
    items_taken = []
    current_area = 0
    max_area = len(board) * len(board[0])
    for figure in figures:
      if current_area >= max_area * 0.05:
        break
      current_area += len(figure)
      items_taken.append(figure)

  for item in items_taken:  # после всей хуйни удаляем все items_taken из figures, чтобы вернуть актуальный figures
    figures.remove(item)

  print(f'items_taken: {items_taken}')
  print(f'figures: {figures}')
  

  # 3. Производим первоначальную загрузку
  for y in range(len(board)):
    for x in range(len(board[0])):
      if board[y][x] == 0:
        cell = (x, y)
        for figure in items_taken:
          status, coords = can_fit(board, cell, figure)
          if status:
            board = place_figure(board, coords)
            # print('Figure placed!')
            items_taken.remove(figure)
            break

  
  # 4. Дозагрузка
  print('Переходим на дозагрузку')
  items_taken = []
  for figure in figures:
    figure_placed = False
    for y in range(len(board)):
      if figure_placed:
        break
      for x in range(len(board[0])):
        if board[y][x] == 0:
          cell = (x, y)
          status, coords = can_fit(board, cell, figure)
          if status:
            board = place_figure(board, coords)
            # print('Figure placed!')
            items_taken.append(figure)
            figure_placed = True
            break
  for item in items_taken:
    figures.remove(item)
  items_taken = []


  for arr in board:
    print(arr)
  print(f'items_taken: {items_taken}')
  print(f'figures: {figures}')


  return board