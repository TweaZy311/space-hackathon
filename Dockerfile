# Используем базовый образ Python
FROM python:3.12-slim

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем requirements.txt в рабочую директорию
COPY requirements.txt .

# Устанавливаем зависимости из requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Копируем все файлы вашего сервиса в рабочую директорию
COPY . .

EXPOSE 5000

# Запускаем ваше приложение
CMD ["python", "main.py"]
