# data/synthetic_data.py
# Dev-3: Generates realistic synthetic sensor data for testing the full pipeline
# Usage: python data/synthetic_data.py | curl -X POST -d @- http://localhost:5000/process_data

import json
import random
from datetime import datetime, timedelta
import sys

def generate_caffeine_log():
    """Generate 0-3 caffeine intakes with realistic doses and times."""
    log = []
    num_intakes = random.randint(0, 3)
    now = datetime.now()
    for _ in range(num_intakes):
        time_offset = timedelta(hours=random.uniform(-8, 0))
        intake_time = now + time_offset
        drink_type = random.choice(["coffee", "espresso", "tea", "energy_drink"])
        mg_map = {"coffee": 95, "espresso": 80, "tea": 40, "energy_drink": 160}
        mg = random.uniform(mg_map[drink_type] * 0.7, mg_map[drink_type] * 1.3)
        log.append({
            "type": drink_type,
            "mg": round(mg, 1),
            "time": intake_time.isoformat()
        })
    return log

def generate_sensor_data():
    """Generate realistic screen, brightness, noise readings - triggers all actions."""
    scenario = random.choice(["high_noise", "high_screen", "low_risk", "high_caffeine", "mixed"])
    if scenario == "high_noise":
        return {"screen_total_minutes_last_2hr": 30, "brightness_level": 0.3, "noise_db_last_5min": 72}  # → ENABLE_WHITE_NOISE
    elif scenario == "high_screen":
        return {"screen_total_minutes_last_2hr": 110, "brightness_level": 0.9, "noise_db_last_5min": 50}  # → REDUCE_BRIGHTNESS
    elif scenario == "low_risk":
        return {"screen_total_minutes_last_2hr": 20, "brightness_level": 0.2, "noise_db_last_5min": 40}  # → NO_ACTION
    elif scenario == "high_caffeine":
        return {"screen_total_minutes_last_2hr": 80, "brightness_level": 0.7, "noise_db_last_5min": 60}  # → BLOCK_APPS
    else:  # mixed
        return {"screen_total_minutes_last_2hr": 65, "brightness_level": 0.6, "noise_db_last_5min": 55}  # → ADJUST_ALARM

def generate_sleep_context():
    """Sleep debt, goals, alarms - realistic patterns."""
    sleep_goal = random.choice([7.0, 7.5, 8.0])
    actual_sleep = random.normalvariate(sleep_goal * 0.85, 0.8)
    actual_sleep = max(3, min(10, actual_sleep))
    sleep_debt = sleep_goal - actual_sleep
    
    base_hour = random.choice([22, 23, 0, 6, 7])
    alarm_time = datetime.now().replace(hour=base_hour, minute=random.randint(0,59), second=0, microsecond=0)
    if base_hour < 6:
        alarm_time += timedelta(days=1)
    
    return {
        "sleep_hours_last_night": round(actual_sleep, 1),
        "sleep_debt_hours": round(sleep_debt, 1),
        "sleep_goal_hours": sleep_goal,
        "alarm_time": alarm_time.isoformat()
    }

def generate_payload():
    """Full input schema compliant payload."""
    base = {
        "timestamp": datetime.now().isoformat(),
        "caffeine_log": generate_caffeine_log()
    }
    sensors = generate_sensor_data()
    sleep = generate_sleep_context()
    return {**base, **sensors, **sleep}

if __name__ == "__main__":
    payloads = [generate_payload() for _ in range(10)]
    
    if len(sys.argv) > 1 and sys.argv[1] == "--single":
        print(json.dumps(payloads[0], indent=2))
    else:
        print(json.dumps(payloads, indent=2))
# data/synthetic_data.py
# Dev-3: Generates realistic synthetic sensor data for testing the full pipeline
# Usage: python data/synthetic_data.py | curl -X POST -d @- http://localhost:5000/process_data

import json
import random
from datetime import datetime, timedelta
import sys

def generate_caffeine_log():
    """Generate 0-3 caffeine intakes with realistic doses and times."""
    log = []
    num_intakes = random.randint(0, 3)
    now = datetime.now()
    for _ in range(num_intakes):
        time_offset = timedelta(hours=random.uniform(-8, 0))
        intake_time = now + time_offset
        drink_type = random.choice(["coffee", "espresso", "tea", "energy_drink"])
        mg_map = {"coffee": 95, "espresso": 80, "tea": 40, "energy_drink": 160}
        mg = random.uniform(mg_map[drink_type] * 0.7, mg_map[drink_type] * 1.3)
        log.append({
            "type": drink_type,
            "mg": round(mg, 1),
            "time": intake_time.isoformat()
        })
    return log

def generate_sensor_data():
    """Generate realistic screen, brightness, noise readings - triggers all actions."""
    scenario = random.choice(["high_noise", "high_screen", "low_risk", "high_caffeine", "mixed"])
    if scenario == "high_noise":
        return {"screen_total_minutes_last_2hr": 30, "brightness_level": 0.3, "noise_db_last_5min": 72}  # → ENABLE_WHITE_NOISE
    elif scenario == "high_screen":
        return {"screen_total_minutes_last_2hr": 110, "brightness_level": 0.9, "noise_db_last_5min": 50}  # → REDUCE_BRIGHTNESS
    elif scenario == "low_risk":
        return {"screen_total_minutes_last_2hr": 20, "brightness_level": 0.2, "noise_db_last_5min": 40}  # → NO_ACTION
    elif scenario == "high_caffeine":
        return {"screen_total_minutes_last_2hr": 80, "brightness_level": 0.7, "noise_db_last_5min": 60}  # → BLOCK_APPS
    else:  # mixed
        return {"screen_total_minutes_last_2hr": 65, "brightness_level": 0.6, "noise_db_last_5min": 55}  # → ADJUST_ALARM

def generate_sleep_context():
    """Sleep debt, goals, alarms - realistic patterns."""
    sleep_goal = random.choice([7.0, 7.5, 8.0])
    actual_sleep = random.normalvariate(sleep_goal * 0.85, 0.8)
    actual_sleep = max(3, min(10, actual_sleep))
    sleep_debt = sleep_goal - actual_sleep
    
    base_hour = random.choice([22, 23, 0, 6, 7])
    alarm_time = datetime.now().replace(hour=base_hour, minute=random.randint(0,59), second=0, microsecond=0)
    if base_hour < 6:
        alarm_time += timedelta(days=1)
    
    return {
        "sleep_hours_last_night": round(actual_sleep, 1),
        "sleep_debt_hours": round(sleep_debt, 1),
        "sleep_goal_hours": sleep_goal,
        "alarm_time": alarm_time.isoformat()
    }

def generate_payload():
    """Full input schema compliant payload."""
    base = {
        "timestamp": datetime.now().isoformat(),
        "caffeine_log": generate_caffeine_log()
    }
    sensors = generate_sensor_data()
    sleep = generate_sleep_context()
    return {**base, **sensors, **sleep}

if __name__ == "__main__":
    payloads = [generate_payload() for _ in range(10)]
    
    if len(sys.argv) > 1 and sys.argv[1] == "--single":
        print(json.dumps(payloads[0], indent=2))
    else:
        print(json.dumps(payloads, indent=2))