# from flask import Flask
# from flask import request, jsonify
# from .service.messageService import MessageService
# from kafka import KafkaProducer
# import json
# import os

# app = Flask(__name__)
# app.config.from_pyfile('config.py')

# messageService = MessageService()

# # ===============================
# # ✅ Kafka Config
# # ===============================
# kafka_host = os.getenv('KAFKA_HOST', 'localhost')
# kafka_port = os.getenv('KAFKA_PORT', '9092')
# kafka_bootstrap_servers = f"{kafka_host}:{kafka_port}"

# print("Kafka server is " + kafka_bootstrap_servers)
# print("\n")

# producer = None

# try:
#     producer = KafkaProducer(
#         bootstrap_servers=kafka_bootstrap_servers,
#         value_serializer=lambda v: json.dumps(v).encode('utf-8')
#     )
#     print("✅ Kafka connected successfully")
# except Exception as e:
#     print("⚠️ Kafka not available:", e)
#     producer = None

# # ===============================
# # ✅ API Endpoint
# # ===============================
# @app.route('/v1/ds/message', methods=['POST'])
# def handle_message():
#     try:
#         user_id = request.headers.get('x-user-id')
#         if not user_id:
#             return jsonify({'error': 'x-user-id header is required'}), 400

#         data = request.get_json()
#         message = data.get('message') if data else None

#         if not message:
#             return jsonify({'error': 'message is required'}), 400

#         result = messageService.process_message(message)

#         if result is not None:
#             serialized_result = result.serialize()
#             serialized_result['user_id'] = user_id

#             # ===============================
#             # ✅ SAFE Kafka send
#             # ===============================
#             if producer:
#                 try:
#                     producer.send('expense_service', serialized_result)
#                     print("📤 Sent to Kafka:", serialized_result)
#                 except Exception as e:
#                     print("⚠️ Kafka send failed:", e)
#             else:
#                 print("⚠️ Kafka skipped (not connected)")

#             return jsonify(serialized_result), 200
#         else:
#             return jsonify({'error': 'Invalid message format'}), 400

#     except Exception as e:
#         print("❌ Internal error:", e)
#         return jsonify({'error': 'Internal server error'}), 500


# # ===============================
# # ✅ Health & Test APIs
# # ===============================
# @app.route('/', methods=['GET'])
# def handle_get():
#     return 'Hello world'


# @app.route('/health', methods=['GET'])
# def health_check():
#     return 'OK'


# # ===============================
# # ✅ RUN SERVER (IMPORTANT FIX)
# # ===============================
# if __name__ == "__main__":
#     app.run(host="0.0.0.0", port=8000, debug=True)

#NEW CLAUDE CODE


import os
import re
import json

from flask import Flask, request, jsonify
from flask_cors import CORS
from kafka import KafkaProducer
from dotenv import load_dotenv
from langchain_core.prompts import ChatPromptTemplate
from langchain_mistralai import ChatMistralAI
import requests as http_requests

load_dotenv()

app = Flask(__name__)
app.config.from_pyfile('config.py')

CORS(app, origins=["http://localhost:8080", "http://localhost:5173", "http://localhost:3000"])

# ===============================
# Kafka Config (optional)
# ===============================
kafka_host = os.getenv('KAFKA_HOST', 'localhost')
kafka_port = os.getenv('KAFKA_PORT', '9092')
kafka_bootstrap_servers = f"{kafka_host}:{kafka_port}"

print("Kafka server is " + kafka_bootstrap_servers)

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

# ExpenseService direct URL (Kafka backup)
EXPENSE_SERVICE_URL = os.getenv('EXPENSE_SERVICE_URL', 'http://localhost:9820')

# ===============================
# Mistral (merchant-only extraction)
# ===============================
merchant_llm = ChatMistralAI(api_key=os.getenv('OPENAI_API_KEY'), model="mistral-large-latest", temperature=0)
merchant_prompt = ChatPromptTemplate.from_messages([
    (
        "system",
        "Extract only the merchant/payee name from this Indian bank SMS. "
        "Return ONLY the merchant name, nothing else. "
        "If it's a UPI payment, extract the app or person name. "
        "If no merchant found, return 'Unknown'."
    ),
    ("human", "SMS: {message}")
])
merchant_chain = merchant_prompt | merchant_llm

# ===============================
# STEP 1 - Bank SMS filter
# ===============================
BANK_SMS_KEYWORDS = [
    "Dr.", "Cr.", "Debited", "Credited", "A/C", "A/c",
    "AvlBal", "Avlbal", "Bal:", "UPI Ref", "NEFT", "IMPS",
    "debited", "credited", "transaction"
]

def is_bank_sms(message):
    return any(keyword in message for keyword in BANK_SMS_KEYWORDS)

# ===============================
# STEP 2 - Amount extraction
# ===============================
AMOUNT_PATTERN = re.compile(r'(?:Rs\.?\s*|INR\s*|₹\s*)(\d+(?:\.\d+)?)', re.IGNORECASE)

def extract_amount(message):
    match = AMOUNT_PATTERN.search(message)
    return match.group(1) if match else None

# ===============================
# STEP 3 - Transaction type detection
# ===============================
DEBIT_KEYWORDS = ["Dr.", "Debited", "debited", "Debit", "debit"]
CREDIT_KEYWORDS = ["Cr.", "Credited", "credited", "Credit", "credit"]

def detect_transaction_type(message):
    if any(keyword in message for keyword in DEBIT_KEYWORDS):
        return "debit"
    if any(keyword in message for keyword in CREDIT_KEYWORDS):
        return "credit"
    return "debit"

# ===============================
# STEP 4 - Merchant extraction (Mistral) + UPI id cleanup
# ===============================
KNOWN_MERCHANTS = {
    "pinelabs": "Pine Labs",
    "paytm": "Paytm",
    "flipkart": "Flipkart",
    "amazon": "Amazon",
    "spotify": "Spotify",
    "airtel": "Airtel",
    "jio": "Jio",
    "swiggy": "Swiggy",
    "zomato": "Zomato",
    "uber": "Uber",
    "ola": "Ola",
    "netflix": "Netflix",
    "google": "Google",
    "phonepe": "PhonePe",
    "gpay": "Google Pay",
    "bhim": "BHIM"
}

def match_known_merchant(text):
    lowered = text.lower()
    for key, name in KNOWN_MERCHANTS.items():
        if key in lowered:
            return name
    return None

def clean_merchant(merchant):
    if not merchant or merchant.strip().lower() == "unknown":
        return "Unknown"

    text = merchant.strip()

    # UPI id / raw text mein known merchant mile to wahi use karo
    known = match_known_merchant(text)
    if known:
        return known

    if '@' in text:
        text = text.split('@')[0]
    if '.' in text:
        text = text.split('.')[0]
    text = re.sub(r'\d+$', '', text)
    text = re.sub(r'(?<!^)(?=[A-Z])', ' ', text)
    text = text.replace('_', ' ').replace('-', ' ')
    text = ' '.join(text.split())

    return text.title() if text else "Unknown"

def extract_merchant(message):
    try:
        response = merchant_chain.invoke({"message": message})
        raw_merchant = response.content.strip()
    except Exception as e:
        print("⚠️ Merchant extraction failed:", e)
        raw_merchant = "Unknown"

    return clean_merchant(raw_merchant)

# ===============================
# SMS -> Expense pipeline
# ===============================
def process_bank_sms(message):
    if not is_bank_sms(message):
        return {"error": "not a bank SMS"}

    return {
        "amount": extract_amount(message),
        "currency": "INR",
        "merchant": extract_merchant(message),
        "transaction_type": detect_transaction_type(message),
    }

# ===============================
# API Endpoint
# ===============================
@app.route('/v1/ds/message', methods=['POST', 'OPTIONS'])
def handle_message():
    if request.method == 'OPTIONS':
        return '', 204

    try:
        user_id = request.headers.get('x-user-id')
        if not user_id:
            return jsonify({'error': 'x-user-id header is required'}), 400

        data = request.get_json()
        message = data.get('message') if data else None

        if not message:
            return jsonify({'error': 'message is required'}), 400

        result = process_bank_sms(message)

        if 'error' in result:
            return jsonify(result), 400

        result['user_id'] = user_id

        # ✅ Pehle Kafka try karo
        kafka_sent = False
        if producer:
            try:
                producer.send('expense_service', result)
                producer.flush()
                print("📤 Sent to Kafka:", result)
                kafka_sent = True
            except Exception as e:
                print("⚠️ Kafka send failed:", e)

        # ✅ Kafka fail ho toh directly ExpenseService ko call karo
        if not kafka_sent:
            try:
                expense_payload = {
                    "amount": result.get("amount"),
                    "merchant": result.get("merchant"),
                    "currency": result.get("currency"),
                    "transaction_type": result.get("transaction_type"),
                }
                resp = http_requests.post(
                    f"{EXPENSE_SERVICE_URL}/expense/v1/addExpense",
                    json=expense_payload,
                    headers={"X-User-Id": user_id},
                    timeout=5
                )
                print(f"📤 Sent directly to ExpenseService: {resp.status_code}")
            except Exception as e:
                print("⚠️ Direct expense save failed:", e)

        return jsonify(result), 200

    except Exception as e:
        print("❌ Internal error:", e)
        return jsonify({'error': 'Internal server error'}), 500


@app.route('/', methods=['GET'])
def handle_get():
    return 'Hello world'

@app.route('/health', methods=['GET'])
def health_check():
    return 'OK'

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8000, debug=True)
