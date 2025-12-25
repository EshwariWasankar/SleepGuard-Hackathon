# utils/risk_calculator.py
import json
from datetime import datetime, timedelta
import numpy as np

CAFFEINE_HALF_LIFE_HOURS = 5.0
SCREEN_THRESHOLD_MINUTES = 60
NOISE_HIGH_DB = 65

def calculate_caffeine_remaining(caffeine_log, current_time):
    """Exponential decay model: C(t) = C0 * (0.5)^(t/T_half)"""
    remaining_mg = 0.0
    for intake in caffeine_log:
        intake_time = datetime.fromisoformat(intake['time'])
        time_elapsed = (current_time - intake_time).total_seconds() / 3600
        if time_elapsed > 12: continue
        decayed_mg = intake['mg'] * (0.5 ** (time_elapsed / CAFFEINE_HALF_LIFE_HOURS))
        remaining_mg += decayed_mg
    return round(remaining_mg, 1)

def melatonin_risk(screen_minutes, brightness):
    """Screen exposure + brightness interaction model"""
    base_risk = min(screen_minutes / SCREEN_THRESHOLD_MINUTES, 1.0)
    brightness_factor = 1 + (brightness * 0.5)
    risk = base_risk * brightness_factor
    return round(min(risk, 1.0), 2)

def noise_risk(noise_db):
    """Piecewise linear risk from sleep studies"""
    if noise_db < 45: 
        return 0.0
    elif noise_db < 65: 
        return round((noise_db - 45) / 40, 2)
    else: 
        return round(min(0.8 + (noise_db - 65) / 35 * 0.2, 1.0), 2)

def sleep_debt_risk(sleep_debt_hours):
    """Cognitive impairment thresholds"""
    if sleep_debt_hours < 1.5: return 0.2
    elif sleep_debt_hours < 3.0: return 0.6
    else: return 1.0

def time_pressure_risk(hours_to_sleep):
    """Urgency increases <4hrs from bedtime"""
    return max(0, (4 - hours_to_sleep) / 4)

def bundle_llm_input(raw_data):
    """Convert raw Android JSON → LLM-ready risk bundle"""
    current_time = datetime.fromisoformat(raw_data['timestamp'])
    alarm_time = datetime.fromisoformat(raw_data['alarm_time'])
    time_to_sleep = (alarm_time - current_time).total_seconds() / 3600
    
    caffeine_mg = calculate_caffeine_remaining(raw_data['caffeine_log'], current_time)
    melatonin = melatonin_risk(raw_data['screen_total_minutes_last_2hr'], raw_data['brightness_level'])
    noise = noise_risk(raw_data['noise_db_last_5min'])
    sleep_debt = sleep_debt_risk(raw_data['sleep_debt_hours'])
    
    return {
        "remaining_caffeine_mg": float(caffeine_mg),
        "melatonin_suppression_risk": float(melatonin),
        "noise_disruption_risk": float(noise),
        "sleep_debt_hours": float(raw_data['sleep_debt_hours']),
        "time_to_target_sleep": round(time_to_sleep, 1),
        "current_time": raw_data['timestamp'],
        "next_alarm": raw_data['alarm_time'],
        "context": {
            "sleep_goal_hours": float(raw_data['sleep_goal_hours']),
            "recent_actions": []
        }
    }

# ✅ NEW: Generate executable values for all actions
def generate_action_value(action, risk_bundle):
    """Generate executable value for Android based on action"""
    if action == "REDUCE_BRIGHTNESS":
        return 0.3  # 30% brightness
    elif action == "ENABLE_WHITE_NOISE":
        return True
    elif action == "BLOCK_APPS":
        return "social,games,video"  # App categories
    elif action == "ADJUST_ALARM":
        # Add 30-90min based on sleep debt severity
        minutes = min(int(risk_bundle["sleep_debt_hours"] * 30), 90)
        alarm_time = datetime.fromisoformat(risk_bundle["next_alarm"])
        new_alarm = alarm_time + timedelta(minutes=minutes)
        return new_alarm.strftime("%H:%M")
    return None
