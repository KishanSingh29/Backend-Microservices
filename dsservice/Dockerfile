FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --timeout=300 -r requirements.txt
COPY . .
ENV PYTHONPATH=/app/src
WORKDIR /app/src/app
CMD ["python", "run.py"]