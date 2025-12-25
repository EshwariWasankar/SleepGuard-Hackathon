import sys
from os.path import dirname, join
from llm_wrapper import call_sleep_agent

def calculate_risks(java_data):
    """
    Recreates the exact 'Risk Calculations' logic from your teammate's app.py
    """
    # 1. GET RAW VALUES
    caffeine_mg = float(java_data.get("remaining_caffeine_mg", 0))
    sleep_debt = float(java_data.get("sleep_debt_hours", 0))

    # 2. MELATONIN RISK (Screen Time)
    # Java sends us a "0-1" normalized value. Let's convert it back to minutes for the formula.
    # (Assuming 1.0 = 600 minutes/10 hours for simulation)
    raw_light_input = float(java_data.get("melatonin_suppression_risk", 0))
    estimated_screen_minutes = raw_light_input * 600

    # Formula from app.py: (screen_minutes * brightness) / 100
    # We assume brightness is 0.5 (avg) since we can't read it easily on Android
    melatonin_risk = (estimated_screen_minutes * 0.5) / 100

    # 3. NOISE RISK (Decibels)
    # Java sends "0-1" (from slider). Let's map it to 30dB - 100dB.
    raw_noise_input = float(java_data.get("noise_disruption_risk", 0))
    estimated_db = 30 + (raw_noise_input * 70)

    # Formula from app.py: (noise_db - 40) / 60
    # Logic: If db < 40, risk is 0.
    noise_risk = max(0, (estimated_db - 40) / 60)

    # 4. PACK IT UP
    return {
        "remaining_caffeine_mg": round(caffeine_mg, 2),
        "sleep_debt_hours": round(sleep_debt, 2),
        "melatonin_suppression_risk": round(melatonin_risk, 2),
        "noise_disruption_risk": round(noise_risk, 2),
        "user_query": java_data.get("user_query", "")
    }

def run_agent_bridge(java_data):
    """
    The Bridge: Receives Java Data -> Calculates Risks -> Calls Agent
    """
    try:
        # Step 1: Perform the math (The "process_data" logic)
        risk_bundle = calculate_risks(java_data)

        # Step 2: Call the LLM (The "call_sleep_agent" logic)
        decision = call_sleep_agent(risk_bundle)
        return decision

    except Exception as e:
        return {
            "action": "ERROR",
            "reasoning_summary": f"Calculation Error: {str(e)}",
            "urgency": "HIGH"
        }