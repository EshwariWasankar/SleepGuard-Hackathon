from flask import Flask, request, jsonify
from datetime import datetime
import os
from llm_wrapper import call_sleep_agent

app = Flask(__name__)

# In-memory storage (Keep it simple for the hackathon)
user_state = {
    "caffeine_log": [],
    "noise_history": [],
    "screen_time_history": [],
    "sleep_hours_last_night": 7.0, # Default
    "sleep_goal_hours": 8.0
}

@app.route("/process_data", methods=["POST"])
def process_data():
    data = request.get_json()
    if not data:
        return jsonify({"error": "No data received"}), 400

    # 1. Update State
    if "caffeine_log" in data:
        user_state["caffeine_log"].extend(data["caffeine_log"])
    if "noise_db" in data:
        user_state["noise_history"].append(data["noise_db"])
    if "screen_minutes" in data:
        user_state["screen_time_history"].append(data["screen_minutes"])
    
    # 2. Risk Calculations (Logic shared with Dev 3's research)
    caffeine_risk = sum([d['mg'] for d in user_state["caffeine_log"][-2:]]) # Simplified for demo
    
    melatonin_risk = (user_state["screen_time_history"][-1] * data.get("brightness", 0.5)) / 100 if user_state["screen_time_history"] else 0
    
    risk_bundle = {
        "remaining_caffeine_mg": round(caffeine_risk, 2),
        "melatonin_suppression_risk": round(melatonin_risk, 2),
        "noise_disruption_risk": round(max(0, (data.get("noise_db", 0) - 40) / 60), 2),
        "sleep_debt_hours": user_state["sleep_goal_hours"] - data.get("sleep_hours_last_night", 7.0)
    }

    # 3. Agent Execution
    decision = call_sleep_agent(risk_bundle)
    
    return jsonify(decision)

if __name__ == "__main__":
    # Use environment port for deployment compatibility
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)