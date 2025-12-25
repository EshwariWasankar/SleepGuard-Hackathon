# demo/scheduled_pipeline.py
import time
import requests
import json
from data.synthetic_data import generate_payload

DEMO_SERVER = "http://localhost:5000/process_data"
SLEEP_DEBT_ACCUMULATOR = 2.3

def run_demo_cycle(cycle_num):
    """One full agent cycle with accumulating state"""
    global SLEEP_DEBT_ACCUMULATOR
    
    payload = generate_payload()
    payload["sleep_debt_hours"] = SLEEP_DEBT_ACCUMULATOR
    
    print(f"\n=== CYCLE {cycle_num} ===")
    print("📱 Raw sensors:", json.dumps(payload, indent=2))
    
    try:
        response = requests.post(DEMO_SERVER, json=payload, timeout=30)
        if response.status_code == 200:
            decision = response.json()
            print("✅ Agent decision:", json.dumps(decision, indent=2))
            SLEEP_DEBT_ACCUMULATOR += 0.1 if decision.get("urgency") == "LOW" else -0.3
        else:
            print("❌ Server error:", response.status_code, response.text)
    except Exception as e:
        print("❌ Connection error:", e)
    
    return payload

if __name__ == "__main__":
    print("🚀 SleepGuard Demo: 15min autonomous cycles (accelerated)")
    print("💡 Start server first: python test_server.py")
    
    try:
        for i in range(8):
            run_demo_cycle(i+1)
            time.sleep(3)  # 15min → 3sec accelerated
    except KeyboardInterrupt:
        print("\n⏹ Demo stopped")