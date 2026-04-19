from flask import Flask
from flask import request, jsonify
from .service.messageService import MessageService
from kafka import KafkaProducer
import json
import os

app = Flask(__name__)
app.config.from_pyfile('config.py')

messageService = MessageService()

# ===============================
# ✅ Kafka Config
# ===============================
kafka_host = os.getenv('KAFKA_HOST', 'localhost')
kafka_port = os.getenv('KAFKA_PORT', '9092')
kafka_bootstrap_servers = f"{kafka_host}:{kafka_port}"

print("Kafka server is " + kafka_bootstrap_servers)
print("\n")

producer = None

try:
    producer = KafkaProducer(
        bootstrap_servers=kafka_bootstrap_servers,
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    print("✅ Kafka connected successfully")
except Exception as e:
    print("⚠️ Kafka not available:", e)
    producer = None

# ===============================
# ✅ API Endpoint
# ===============================
@app.route('/v1/ds/message', methods=['POST'])
def handle_message():
    try:
        user_id = request.headers.get('x-user-id')
        if not user_id:
            return jsonify({'error': 'x-user-id header is required'}), 400

        data = request.get_json()
        message = data.get('message') if data else None

        if not message:
            return jsonify({'error': 'message is required'}), 400

        result = messageService.process_message(message)

        if result is not None:
            serialized_result = result.serialize()
            serialized_result['user_id'] = user_id

            # ===============================
            # ✅ SAFE Kafka send
            # ===============================
            if producer:
                try:
                    producer.send('expense_service', serialized_result)
                    print("📤 Sent to Kafka:", serialized_result)
                except Exception as e:
                    print("⚠️ Kafka send failed:", e)
            else:
                print("⚠️ Kafka skipped (not connected)")

            return jsonify(serialized_result), 200
        else:
            return jsonify({'error': 'Invalid message format'}), 400

    except Exception as e:
        print("❌ Internal error:", e)
        return jsonify({'error': 'Internal server error'}), 500


# ===============================
# ✅ Health & Test APIs
# ===============================
@app.route('/', methods=['GET'])
def handle_get():
    return 'Hello world'


@app.route('/health', methods=['GET'])
def health_check():
    return 'OK'


# ===============================
# ✅ RUN SERVER (IMPORTANT FIX)
# ===============================
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000, debug=True)