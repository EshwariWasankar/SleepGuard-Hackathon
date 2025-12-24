# test_server.py - Complete standalone server
from flask import Flask, request, jsonify
from utils.risk_calculator import bundle_llm_input, generate_action_value
from llm_wrapper import call_sleep_agent
import logging

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)

@app.route('/process_data', methods=['POST'])
def process_data():
    raw_data = request.json
    logging.info(f"📱 Raw: {raw_data}")
    
    llm_input = bundle_llm_input(raw_data)
    logging.info(f"🧮 ML bundle: {llm_input}")
    
    logging.info("🧠 Calling LLM...")
    decision = call_sleep_agent(llm_input)
    logging.info("✅ LLM returned")

    # ✅ ADDED: Generate executable value for Android
    decision["value"] = generate_action_value(decision["action"], llm_input)
    
    logging.info(f"🤖 Decision: {decision}")
    return jsonify(decision)

if __name__ == '__main__':
    app.run(port=5000, debug=True, threaded=True)
