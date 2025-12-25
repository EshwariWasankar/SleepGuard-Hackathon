# Input Schema (Android → Python)

**Required fields only. Android must validate before sending.**

{
"timestamp": "ISO8601 string (e.g. 2025-12-22T17:06:00)",
"caffeine_log": [
{
"type": "string (coffee|espresso|tea|energy_drink)",
"mg": "float (20-300)",
"time": "ISO8601 string"
}
],
"screen_total_minutes_last_2hr": "float (0-180)",
"brightness_level": "float (0.0-1.0)",
"noise_db_last_5min": "float (20-100)",
"sleep_hours_last_night": "float (0-12)",
"sleep_debt_hours": "float (-2 to 6)",
"alarm_time": "ISO8601 string",
"sleep_goal_hours": "float (6-10)"
}


undefined