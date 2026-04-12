from flask import Flask, request, jsonify
from app.service.messageService import MessageService

from kafka import KafkaProducer
import json
import os

app = Flask(__name__)
app.config.from_pyfile(
    os.path.join(os.path.dirname(__file__), "config.py")
)

messageService = MessageService()

producer = KafkaProducer(
    bootstrap_servers=["localhost:9092"],
    value_serializer=lambda v: json.dumps(v).encode("utf-8")
)

@app.route("/v1/ds/message", methods=["POST"])
def handle_message():
    data = request.get_json(force=True)
    message = data.get("message")

    result = messageService.process_message(message)

    # Kafka expects dict / json
    producer.send("expense_service", result)

    return jsonify(result)

@app.route("/", methods=["GET"])
def handle_get():
    return "Hello world"

__all__ = ["app"]
