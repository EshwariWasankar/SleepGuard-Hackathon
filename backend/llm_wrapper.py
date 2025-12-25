import os
import json
from google import genai
from google.genai import types

# -----------------------------
# GEMINI CLIENT SETUP
# -----------------------------
API_KEY = os.getenv("GOOGLE_API_KEY")

if not API_KEY:
    raise RuntimeError("GOOGLE_API_KEY environment variable is not set")

# FIX: Explicitly set api_version='v1' to ensure gemini-1.5-flash is found
client = genai.Client(
    api_key=API_KEY,
    http_options={'api_version': 'v1'}
)

MODEL_NAME = "gemini-2.5-flash"

def rule_fallback(risk):
    """Scientific-threshold based fallback if the LLM is unavailable."""
    if risk["melatonin_suppression_risk"] > 1.5 or risk["sleep_debt_hours"] > 2:
        return {
            "action": "DIGITAL_WIND_DOWN",
            "urgency": "MEDIUM",
            "value": None,
            "notification_message": "High blue-light/sleep debt detected. Time to disconnect.",
            "confidence": 0.6,
            "reasoning_summary": "Rule-based safety trigger for melatonin protection."
        }
    return {
        "action": "NONE",
        "urgency": "LOW",
        "value": None,
        "notification_message": "Conditions optimal for sleep.",
        "confidence": 1.0,
        "reasoning_summary": "No critical thresholds breached."
    }

def call_sleep_agent(risk_bundle):
    prompt = f"""
    You are a Sleep Architect Agent. Analyze these risks and return ONE action.
    Risks: {json.dumps(risk_bundle)}
    
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
    try:
        # 1. Removed GenerateContentConfig to avoid the 400 error
        response = client.models.generate_content(
            model=MODEL_NAME,
            contents=prompt
        )
        
        # 2. Extract and Clean JSON
        raw_text = response.text.strip()
        
        # This regex/split logic removes ```json ... ``` if the model adds it
        if "```" in raw_text:
            # Take everything between the first and last triple backtick
            raw_text = raw_text.split("```")
            # If they wrote ```json { ... } ```, it's usually in the second element
            # but we'll find the first element that starts with '{'
            for part in raw_text:
                part = part.replace("json", "").strip()
                if part.startswith("{"):
                    raw_text = part
                    break
        
        decision = json.loads(raw_text)
        decision["_source"] = "LLM"
        return decision

    except Exception as e:
        print(f"⚠️ Agent Error: {e}")
        fb = rule_fallback(risk_bundle)
        fb["_source"] = "RULE_FALLBACK"
        return fb