import os
import json
import requests
from typing import Dict, Any

# =========================
# 🔐 CONFIGURATION
# =========================
# We use standard HTTP requests to avoid Android crashes
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY")
URL = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key={GEMINI_API_KEY}"
HEADERS = {"Content-Type": "application/json"}

# =========================
# 🧠 TEAMMATE'S LOGIC (ADAPTED)
# =========================

def rule_fallback(risk: Dict[str, Any]) -> Dict[str, Any]:
    """
    Safety net: If internet fails, use these hard-coded science rules.
    (From your teammate's code)
    """
    # High blue light OR High sleep debt
    if risk.get("melatonin_suppression_risk", 0) > 1.5 or risk.get("sleep_debt_hours", 0) > 2:
        return {
            "action": "DIGITAL_WIND_DOWN",
            "urgency": "MEDIUM",
            "value": None,
            "notification_message": "High blue-light detected. Time to disconnect.",
            "confidence": 0.6,
            "reasoning_summary": "Rule-based safety trigger for melatonin protection."
        }

    # High Caffeine (Added this rule for your specific demo)
    if risk.get("remaining_caffeine_mg", 0) > 350:
         return {
            "action": "ADJUST_ALARM",
            "urgency": "HIGH",
            "value": "30",
            "notification_message": "High caffeine detected. Extending sleep schedule.",
            "confidence": 0.9,
            "reasoning_summary": "Caffeine half-life requires extended recovery time."
        }

    return {
        "action": "NONE",
        "urgency": "LOW",
        "value": None,
        "notification_message": "Conditions optimal for sleep.",
        "confidence": 1.0,
        "reasoning_summary": "No critical thresholds breached."
    }

def call_sleep_agent(risk_bundle: Dict[str, Any]) -> Dict[str, Any]:
    """
    Sends the calculated risks to Gemini to get a smart decision.
    """
    # 1. THE TEAMMATE'S PROMPT
    prompt = f"""
    You are a Sleep Architect Agent. Analyze these risks and return ONE action.

    DATA CONTEXT:
    - Caffeine > 300mg is High Risk.
    - Sleep Debt > 2 hours is Critical.
    - Blue Light (Melatonin Risk) > 5 is High.

    USER RISKS: {json.dumps(risk_bundle)}

    Return ONLY valid JSON matching this schema:
    {{
      "action": "ADJUST_ALARM | DIGITAL_WIND_DOWN | PLAY_WHITE_NOISE | NONE",
      "urgency": "LOW | MEDIUM | HIGH",
      "value": "string or null",
      "notification_message": "string",
      "confidence": float,
      "reasoning_summary": "string"
    }}
    """

    # 2. ANDROID-SAFE API CALL
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"response_mime_type": "application/json", "temperature": 0.1}
    }

    try:
        if not GEMINI_API_KEY:
            raise ValueError("API Key missing")

        response = requests.post(URL, headers=HEADERS, json=payload, timeout=10)
        response.raise_for_status()

        # 3. PARSE RESPONSE
        result_json = response.json()
        raw_text = result_json["candidates"][0]["content"]["parts"][0]["text"]

        # Clean up any Markdown formatting
        if "```" in raw_text:
            raw_text = raw_text.replace("```json", "").replace("```", "").strip()

        decision = json.loads(raw_text)
        decision["_source"] = "LLM"
        return decision

    except Exception as e:
        print(f"⚠️ Agent Error: {e}")
        fb = rule_fallback(risk_bundle)
        fb["_source"] = "RULE_FALLBACK"
        return fb