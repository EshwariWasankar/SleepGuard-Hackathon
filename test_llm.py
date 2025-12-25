from llm_wrapper import call_sleep_agent

risk = {
    "remaining_caffeine_mg": 99,
    "melatonin_suppression_risk": 1.0,
    "noise_disruption_risk": 0.3,
    "sleep_debt_hours": 2.1
}

print(call_sleep_agent(risk))
