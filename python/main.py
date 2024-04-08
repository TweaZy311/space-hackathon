from flask import Flask, request, jsonify
from methods import *
import requests

app = Flask(__name__)

@app.route('/process_garbage', methods=['POST'])
def process_garbage():
    # print("PLANET START")
    # Получаем JSON из тела запроса
    request_data = request.json
    
    # Проверяем, присутствуют ли ключи planetGarbage и shipGarbage в JSON-запросе
    if 'planetGarbage' not in request_data or 'shipGarbage' not in request_data:
        return jsonify({"error": "Missing required keys"}), 400
    
    # Заполняем ship_garbage
    ship_garbage = request_data.get('shipGarbage')
    board = generate_empty_board(8, 11)
    if ship_garbage is not None:
        for figure in ship_garbage:
            for cell in ship_garbage[figure]:
                board[cell[1]][cell[0]] = figure

    # Получаем planet_garbage
    planet_garbage = request_data.get('planetGarbage')
    # print(f'Изначально: {planet_garbage}')
    start_planet_garbage = planet_garbage

    # Сортируем фигуры из planet_garbage
    figures = {k: v for k, v in sorted(planet_garbage.items(), key=lambda item: len(item[1]))}
    # print(f'После сортировки: {figures}')

    def figure_size(figures, key):
        return len(figures[key])

    def empty_space(board):
        s = 0
        for arr in board:
            for cell in arr:
                if cell != 0:
                    s += 1
        return 88 - s

    # Если трюм пуст, набираем до 30% трюма
    if is_empty(board):
        items_taken = dict()
        current_area = 0
        max_area = len(board) * len(board[0])
        for key, value in figures.items():
            if current_area >= max_area * 0.3:
                break
            current_area += len(value)
            items_taken[key] = value

    # Если трюм не пуст, набираем до 5% трюма
    else:
        items_taken = dict()
        current_area = 0
        max_area = len(board) * len(board[0])
        for key, value in figures.items():
            if current_area >= max_area * 0.05:
                break
            current_area += len(value)
            items_taken[key] = value

    # После всей хуйни удаляем все items_taken из figures, чтобы знать актуальный figures
    for key in items_taken:
        del figures[key]
    # print('Прямо перед первоначальной загрузкой')
    # print(f'items_taken: {items_taken}')
    # print(f'figures: {figures}')
    
    # Производим первоначальную загрузку
    # print('Первоначальная загрузка')
    for y in range(len(board)):
        for x in range(len(board[0])):
            if board[y][x] == 0:
                cell = (x, y)
                for key in items_taken:
                    status, coords = can_fit(board, cell, items_taken[key])
                    if status:
                        board = place_figure(board, key, coords)
                        # print('Figure placed!')
                        del items_taken[key]
                        break
    # print('После первоначальной загрузки:')
    # for arr in board:
    #     print(arr)
    # print(f'items_taken: {items_taken}')

    # Дозагрузка
    # print('Дозагрузка')
    items_taken = dict()
    for key in figures:
        figure_placed = False
        if figure_size(figures, key) > empty_space(board):
            break
        for y in range(len(board)):
            if figure_placed:
                break
            for x in range(len(board[0])):
                if board[y][x] == 0:
                    cell = (x, y)
                    status, coords = can_fit(board, cell, figures[key])
                    if status:
                        board = place_figure(board, key, coords)
                        # print('Figure placed!')
                        items_taken[key] = figures[key]
                        figure_placed = True
                        break
    for key in items_taken:
        del figures[key]
    items_taken = dict()

    # Результат после дозагрузки
    # for arr in board:
    #     print(arr)
    # print(f'items_taken: {items_taken}')
    # print(f'figures: {figures}')
    
    # Планета пуста для джава контейнера
    planet_is_empty = not figures

    # Подсчет заполненности доски
    s = 0
    for row in board:
        for cell in row:
            if cell != 0:
                s += 1
    occupancy = s / (len(board) * len(board[0]))
    process_further = (occupancy < 0.7)

    # print(f'{'Планета пуста' if planet_is_empty else 'На планете остался мусор'}')
    # print(f'Заполненность: {occupancy}')
    
    # Заполнение дикта для JSONa для запроса на сервер хакатона
    request_board = {"garbage": {}}
    for y in range(len(board)):
        for x in range(len(board[0])):
            cell = board[y][x]
            if cell != 0:  # Проверяем, не пустая ли клетка
                if cell not in request_board["garbage"]:
                    request_board["garbage"][cell] = []  # Создаем новую запись для мусора
                request_board["garbage"][cell].append([x, y])  # Добавляем координаты в список

    # print('request_board:')
    # print(request_board)

    # Отправка JSONa на сервер
    URL = 'https://datsedenspace.datsteam.dev/player/collect'
    TOKEN = '66044c57de11b66044c57de121'

    # Заголовок запроса
    headers = {
        'X-Auth-Token': TOKEN,
        'Content-Type': 'application/json'
    }
    
    # Отправка запроса
    response = requests.post(URL, headers=headers, json=request_board)

    # Получение статуса и тела ответа
    # status_code = response.status_code
    # response_body = response.text

    # Печать статуса и тела ответа
    # print("Status Code:", status_code)
    # print("Response Body:", response_body)

    # if status_code == 400:
    #     print(response_body)
    #     print("start_planet_garbage:")
    #     print(start_planet_garbage)
    #     print("\n")
    #     print("request_board:")
    #     print(request_board)

    # Возвращаем результат джаве
    result = {
        "planetIsEmpty": planet_is_empty,
        "ProcessFurther": process_further,
        "occupancy": occupancy
    }

    # print("PLANET END\n\n\n\n")
    print(f'STATUS CODE: {response.status_code}.')
    return jsonify(result), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)