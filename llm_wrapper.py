# llm_wrapper.py

from dotenv import load_dotenv
load_dotenv()

import json
import time
import os
from typing import Dict, Any
from google import genai  # ✅ YOUR ORIGINAL IMPORT

# =========================
# 🔐 Environment & Client (YOUR WAY)
# =========================
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise RuntimeError("❌ GEMINI_API_KEY not found in environment")

client = genai.Client(api_key=GEMINI_API_KEY)  # ✅ YOUR ORIGINAL
MODEL_NAME = "models/gemini-flash-latest"      # ✅ YOUR ORIGINAL

# =========================
# 📐 Action Schema
# =========================
ACTION_SCHEMA = {
    "action": ["WARN", "ADJUST_ALARM", "REDUCE_BRIGHTNESS", "ENABLE_WHITE_NOISE", "BLOCK_APPS", "NO_ACTION"],
    "urgency": ["LOW", "MEDIUM", "HIGH"]
}

# =========================
# 🧠 LLM Call (YOUR STRUCTURE + FIXED)
# =========================
def call_sleep_agent(risk_bundle: Dict[str, Any], max_retries: int = 3) -> Dict[str, Any]:
    # Load prompts
    try:
        with open("prompts/system_prompt.txt", "r", encoding="utf-8") as f:
            system_prompt = f.read()
        with open("prompts/action_planner.txt", "r", encoding="utf-8") as f:
            action_planner = f.read()
    except:
        return fallback_safe_action()
    
    prompt = f"""
{system_prompt}

Risks: {json.dumps(risk_bundle, indent=2)}

{action_planner}

JSON ONLY:
{{"action":"ADJUST_ALARM","urgency":"HIGH","notification_message":"Caffeine+screen risk","confidence":0.9,"reasoning_summary":"Multi-risk intervention"}}"""
    
    for attempt in range(max_retries):
        try:
            response = client.models.generate_content(
                model=MODEL_NAME,
                contents=prompt,
                config={
                    "response_mime_type": "application/json",
                    "temperature": 0.1
                }
            )
            
            action_json = json.loads(response.text.strip())
            validate_action_schema(action_json)
            return action_json
            
        except Exception as e:
            print(f"⚠️ Attempt {attempt+1}: {e}")
            if attempt == max_retries - 1:
                return fallback_safe_action()
            time.sleep(0.5)
    
    return fallback_safe_action()

# =========================
# ✅ Validation (YOUR ORIGINAL)
# =========================
def validate_action_schema(action: Dict[str, Any]) -> None:
    required_fields = ["action", "urgency", "notification_message", "confidence", "reasoning_summary"]
    
    for field in required_fields:
        if field not in action:
            raise ValueError(f"Missing: {field}")
    
    if action["action"] not in ACTION_SCHEMA["action"]:
        raise ValueError(f"Invalid action: {action['action']}")
    if action["urgency"] not in ACTION_SCHEMA["urgency"]:
        raise ValueError(f"Invalid urgency: {action['urgency']}")
    if not (0.0 <= action["confidence"] <= 1.0):
        raise ValueError("Invalid confidence")

# =========================
# 🛟 Fallback (YOUR ORIGINAL)
# =========================
def fallback_safe_action() -> Dict[str, Any]:
    return {
        "action": "WARN",
        "urgency": "MEDIUM",
        "notification_message": "Potential sleep risk detected. Please consider reducing screen exposure.",
        "confidence": 0.5,
        "reasoning_summary": "Fallback action issued due to LLM processing failure."
    }